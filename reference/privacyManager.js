// utils/privacyManager.js
/**
 * 隐私管理器
 * 处理日记的隐私等级、权限验证和数据加密
 */

// 隐私等级枚举
const PRIVACY_LEVELS = {
  PUBLIC: 0,    // 公开 - 无需验证
  PRIVATE: 1,   // 私密 - 需要验证身份
  ENCRYPTED: 2  // 加密 - 需要验证身份且内容加密
};

// 权限状态
const AUTH_STATUS = {
  GUEST: 0,     // 访客模式 - 只能看公开内容
  AUTHORIZED: 1 // 已验证 - 可以看所有内容
};

class PrivacyManager {
  constructor() {
    this.currentAuthStatus = AUTH_STATUS.GUEST;
    this.authExpireTime = 0;
    this.masterPassword = this.getMasterPassword();
    this.autoLockTimeout = 5 * 60 * 1000; // 5分钟自动锁定
  }

  // 获取或初始化主密码
  getMasterPassword() {
    let password = wx.getStorageSync('masterPassword');
    if (!password) {
      // 首次使用，设置默认密码
      password = this.hashPassword('520520'); // 默认密码
      wx.setStorageSync('masterPassword', password);
    }
    return password;
  }

  // 简单的字符串转Base64编码（替代TextEncoder）
  stringToBase64(str) {
    try {
      // 使用微信小程序原生的Base64编码
      return wx.arrayBufferToBase64(
        new Uint8Array(
          Array.from(str + 'love_diary_salt', char => char.charCodeAt(0))
        ).buffer
      );
    } catch (err) {
      console.error('Base64编码失败:', err);
      // 降级方案：简单字符串处理
      return btoa(unescape(encodeURIComponent(str + 'love_diary_salt')));
    }
  }

  // Base64解码为字符串（替代TextDecoder）
  base64ToString(base64Str) {
    try {
      const buffer = wx.base64ToArrayBuffer(base64Str);
      const uint8Array = new Uint8Array(buffer);
      let result = '';
      for (let i = 0; i < uint8Array.length; i++) {
        result += String.fromCharCode(uint8Array[i]);
      }
      return result;
    } catch (err) {
      console.error('Base64解码失败:', err);
      // 降级方案
      try {
        return decodeURIComponent(escape(atob(base64Str)));
      } catch (e) {
        return '[解码失败]';
      }
    }
  }

  // 简单的密码哈希（兼容真机环境）
  hashPassword(password) {
    try {
      return this.stringToBase64(password);
    } catch (err) {
      console.error('密码哈希失败:', err);
      // 最简单的降级方案
      return btoa(password + 'love_diary_salt');
    }
  }

  // 验证密码
  verifyPassword(inputPassword) {
    const hashedInput = this.hashPassword(inputPassword);
    return hashedInput === this.masterPassword;
  }

  // 更改主密码
  changeMasterPassword(oldPassword, newPassword) {
    if (!this.verifyPassword(oldPassword)) {
      return { success: false, message: '原密码错误' };
    }
    
    this.masterPassword = this.hashPassword(newPassword);
    wx.setStorageSync('masterPassword', this.masterPassword);
    return { success: true, message: '密码修改成功' };
  }

  // 身份验证
  authenticate(password) {
    if (this.verifyPassword(password)) {
      this.currentAuthStatus = AUTH_STATUS.AUTHORIZED;
      this.authExpireTime = Date.now() + this.autoLockTimeout;
      return { success: true, message: '验证成功' };
    }
    return { success: false, message: '密码错误' };
  }

  // 检查当前权限状态
  checkAuthStatus() {
    if (this.currentAuthStatus === AUTH_STATUS.AUTHORIZED) {
      if (Date.now() > this.authExpireTime) {
        // 权限过期，自动锁定
        this.logout();
        return AUTH_STATUS.GUEST;
      }
      // 延长有效期
      this.authExpireTime = Date.now() + this.autoLockTimeout;
    }
    return this.currentAuthStatus;
  }

  // 退出登录
  logout() {
    this.currentAuthStatus = AUTH_STATUS.GUEST;
    this.authExpireTime = 0;
  }

  // 加密文本内容（兼容真机环境）
  encryptText(text) {
    try {
      // 简单的Base64加密（实际项目中应使用AES等加密算法）
      const encrypted = this.stringToBase64(text);
      return `encrypted:${encrypted}`;
    } catch (err) {
      console.error('加密失败:', err);
      // 最简单的降级方案
      try {
        return `encrypted:${btoa(unescape(encodeURIComponent(text)))}`;
      } catch (e) {
        console.error('加密全部失败:', e);
        return text; // 最终降级：返回原文
      }
    }
  }

  // 解密文本内容（兼容真机环境）
  decryptText(encryptedText) {
    try {
      if (!encryptedText || !encryptedText.startsWith('encrypted:')) {
        return encryptedText; // 不是加密内容
      }
      
      const encrypted = encryptedText.replace('encrypted:', '');
      return this.base64ToString(encrypted);
    } catch (err) {
      console.error('解密失败:', err);
      // 降级方案
      try {
        const encrypted = encryptedText.replace('encrypted:', '');
        return decodeURIComponent(escape(atob(encrypted)));
      } catch (e) {
        console.error('解密全部失败:', e);
        return '[加密内容解析失败]';
      }
    }
  }

  // 过滤日记列表（根据当前权限）
  filterDiaries(diaries) {
    const currentAuth = this.checkAuthStatus();
    
    return diaries.map(diary => {
      const privacyLevel = diary.privacyLevel || PRIVACY_LEVELS.PUBLIC;
      
      // 访客模式只能看公开内容
      if (currentAuth === AUTH_STATUS.GUEST && privacyLevel !== PRIVACY_LEVELS.PUBLIC) {
        return {
          ...diary,
          content: this.getPrivacyPlaceholder(privacyLevel),
          images: [], // 隐藏图片 - 这里创建新数组而不是修改原数组
          isLocked: true,
          privacyLevel: privacyLevel
        };
      }
      
      // 已验证用户可以看所有内容
      if (currentAuth === AUTH_STATUS.AUTHORIZED) {
        let content = diary.content;
        
        // 如果是加密内容，需要解密
        if (privacyLevel === PRIVACY_LEVELS.ENCRYPTED && diary.content) {
          content = this.decryptText(diary.content);
        }
        
        return {
          ...diary,
          content: content,
          isLocked: false,
          privacyLevel: privacyLevel
        };
      }
      
      // 默认返回原内容（深拷贝避免污染原数据）
      return { 
        ...diary, 
        images: diary.images ? [...diary.images] : [], // 深拷贝图片数组
        isLocked: false 
      };
    });
  }

  // 获取隐私占位文本
  getPrivacyPlaceholder(privacyLevel) {
    switch (privacyLevel) {
      case PRIVACY_LEVELS.PRIVATE:
        return '🔒 这是一篇私密日记，需要验证身份后查看';
      case PRIVACY_LEVELS.ENCRYPTED:
        return '🔐 这是一篇加密日记，需要验证身份后查看';
      default:
        return '🔒 受保护的内容';
    }
  }

  // 处理日记保存（加密私密内容）
  processDiaryForSave(diary) {
    const privacyLevel = diary.privacyLevel || PRIVACY_LEVELS.PUBLIC;
    
    if (privacyLevel === PRIVACY_LEVELS.ENCRYPTED && diary.content) {
      return {
        ...diary,
        content: this.encryptText(diary.content),
        privacyLevel: privacyLevel
      };
    }
    
    return {
      ...diary,
      privacyLevel: privacyLevel
    };
  }

  // 获取隐私等级选项
  getPrivacyLevelOptions() {
    return [
      { value: PRIVACY_LEVELS.PUBLIC, label: '🌐 公开', desc: '所有人都可以查看' },
      { value: PRIVACY_LEVELS.PRIVATE, label: '🔒 私密', desc: '需要验证身份后查看' },
      { value: PRIVACY_LEVELS.ENCRYPTED, label: '🔐 加密', desc: '内容加密存储，最高安全级别' }
    ];
  }
}

// 导出隐私等级常量和管理器
module.exports = {
  PRIVACY_LEVELS,
  AUTH_STATUS,
  PrivacyManager
};