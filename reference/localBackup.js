/**
 * 本地备份与恢复工具类
 * 提供文件夹备份、压缩包备份、二进制压缩包等完整的本地数据备份解决方案
 * 解决云端环境试用期限制，确保数据安全的本地保存机制
 */

class LocalBackupManager {
  constructor() {
    this.maxFileSize = 15 * 1024 * 1024; // 15MB 限制
    this.chunkSize = 1024 * 1024; // 1MB 分块大小
    this.maxRetries = 3; // 最大重试次数
  }

  // ==================== 文件夹备份功能 ====================
  
  /**
   * 导出日记为文件夹格式
   * 每篇日记单独保存为.bak文件，支持3级图片压缩
   */
  async exportDiariesAsFolder(diaries) {
    try {
      console.log('开始文件夹备份，日记数量:', diaries.length);
      wx.showLoading({ title: '准备备份...' });
      
      if (diaries.length === 0) {
        wx.hideLoading();
        wx.showToast({ title: '暂无日记可备份', icon: 'none' });
        return;
      }

      // 询问导出范围
      console.log('准备询问导出范围');
      const exportRange = await this.askExportRange(diaries.length);
      console.log('导出范围选择结果:', exportRange);
      
      if (!exportRange) {
        wx.hideLoading();
        // 用户取消了操作，给一个提示
        console.log('用户取消了导出范围选择');
        wx.showToast({ title: '已取消备份', icon: 'none' });
        return;
      }

      // 根据范围过滤日记
      const filteredDiaries = diaries.slice(exportRange.start - 1, exportRange.end);
      console.log('过滤后的日记数量:', filteredDiaries.length);
      //const filteredDiaries = diaries.slice(1, 10);

      // 预估总大小并询问压缩选项
      console.log('准备询问压缩选项');
      const compressionOption = await this.askCompressionLevel(filteredDiaries.length);
      console.log('压缩选项选择结果:', compressionOption);
      
      if (!compressionOption) {
        wx.hideLoading();
        // 用户取消了操作，给一个提示
        console.log('用户取消了压缩选项选择');
        wx.showToast({ title: '已取消备份', icon: 'none' });
        return;
      }

      wx.showLoading({ title: '检查存储空间...' });
      
      // 检查可用存储空间
      try {
        const fs = wx.getFileSystemManager();
        // 列出用户数据目录下的文件，估算已使用空间
        const fileList = fs.readdirSync(wx.env.USER_DATA_PATH);
        console.log('用户数据目录文件列表:', fileList);
        
        let totalUsedSize = 0;
        for (const file of fileList) {
          try {
            const filePath = `${wx.env.USER_DATA_PATH}/${file}`;
            const stat = fs.statSync(filePath);
            if (stat.isFile()) {
              totalUsedSize += stat.size;
              console.log(`文件 ${file}: ${(stat.size / 1024 / 1024).toFixed(2)}MB`);
            } else if (stat.isDirectory()) {
              // 递归计算目录大小
              const dirSize = this.calculateDirectorySize(fs, filePath);
              totalUsedSize += dirSize;
              console.log(`目录 ${file}: ${(dirSize / 1024 / 1024).toFixed(2)}MB`);
            }
          } catch (err) {
            console.warn(`无法获取文件 ${file} 信息:`, err);
          }
        }
        
        const usedMB = (totalUsedSize / 1024 / 1024).toFixed(2);
        console.log(`已使用存储空间: ${usedMB}MB`);
        
        // 微信小程序通常有10MB左右的存储限制
        if (totalUsedSize > 9 * 1024 * 1024) { // 超过9MB给出警告
          wx.showModal({
            title: '⚠️ 存储空间警告',
            content: `您的小程序存储空间已使用 ${usedMB}MB，接近上限。建议先清理不需要的备份文件再进行新备份。\n\n可以选择较少的日记数量进行备份。`,
            confirmText: '继续备份',
            cancelText: '取消',
            success: (res) => {
              if (!res.confirm) {
                wx.hideLoading();
                wx.showToast({ title: '已取消备份', icon: 'none' });
                return;
              }
            }
          });
        }
      } catch (err) {
        console.warn('无法获取存储空间信息:', err);
      }

      wx.showLoading({ title: '创建备份文件夹...' });
      
      // 创建备份文件夹名称
      const backupFolderName = `love_diary_backup_${this.formatDate(new Date()).replace(/-/g, '')}`;
      const folderPath = `${wx.env.USER_DATA_PATH}/${backupFolderName}`;
      
      // 创建文件夹
      const fs = wx.getFileSystemManager();
      try {
        fs.mkdirSync(folderPath);
      } catch (err) {
        if (err.errMsg && !err.errMsg.includes('file already exists')) {
          throw new Error('创建文件夹失败: ' + err.errMsg);
        }
      }

      // 处理每个日记文件
      let successCount = 0;
      let totalImageCount = 0;
      let totalProcessedSize = 0; // 总处理大小
      const processedFiles = [];
      
      for (let i = 0; i < filteredDiaries.length; i++) {
        const diary = filteredDiaries[i];
        const actualIndex = exportRange.start + i;
        wx.showLoading({ title: `处理日记 ${actualIndex}/${exportRange.end}...` });
        
        try {
          const result = await this.createSingleDiaryFile(diary, folderPath, actualIndex, compressionOption);
          if (result.success) {
            successCount++;
            totalImageCount += result.imageCount;
            processedFiles.push(result.fileName);
            
            // 尝试获取文件大小进行统计
            try {
              const fileInfo = fs.statSync(`${folderPath}/${result.fileName}`);
              totalProcessedSize += fileInfo.size;
              console.log(`当前文件大小: ${(fileInfo.size / 1024 / 1024).toFixed(2)}MB, 累计大小: ${(totalProcessedSize / 1024 / 1024).toFixed(2)}MB`);
            } catch (statErr) {
              console.warn('无法获取文件信息:', statErr);
            }
          } else {
            console.error(`处理日记${actualIndex}失败:`, result.error);
            // 如果是因为存储限制导致的错误，提示用户
            if (result.error && (result.error.includes('storage limit') || result.error.includes('exceeded'))) {
              wx.showModal({
                title: '❌ 存储空间不足',
                content: `已处理${successCount}个文件，大小: ${(totalProcessedSize / 1024 / 1024).toFixed(2)}MB

请尝试：
1. 选择较少的日记数量
2. 清理不需要的备份文件
3. 使用智能压缩选项`,
                confirmText: '确定',
                showCancel: false
              });
              // 中断备份过程
              wx.hideLoading();
              return;
            }
          }
        } catch (err) {
          console.error(`处理日记${actualIndex}失败:`, err);
          
          // 特别处理存储空间不足的错误
          if (err.message && (err.message.includes('storage limit') || err.message.includes('exceeded'))) {
            wx.hideLoading();
            wx.showModal({
              title: '❌ 存储空间不足',
              content: `备份过程中存储空间不足，已处理${successCount}个文件，大小: ${(totalProcessedSize / 1024 / 1024).toFixed(2)}MB

请尝试：
1. 选择较少的日记数量
2. 清理不需要的备份文件
3. 使用智能压缩选项`,
              confirmText: '确定',
              showCancel: false
            });
            return;
          }
        }
      }

      console.log(`总共处理了 ${successCount} 个文件，总大小: ${(totalProcessedSize / 1024 / 1024).toFixed(2)}MB`);
      
      // 创建备份信息文件
      const backupInfo = {
        version: '3.0',
        type: 'folder_backup',
        exportTime: new Date().toISOString(),
        totalCount: filteredDiaries.length,
        successCount: successCount,
        compressionLevel: compressionOption,
        description: `情侣日记文件夹备份（第${exportRange.start}-${exportRange.end}篇）`,
        files: processedFiles,
        exportRange: exportRange,
        totalSize: totalProcessedSize,
        totalSizeMB: (totalProcessedSize / 1024 / 1024).toFixed(2)
      };
      
      const infoFilePath = `${folderPath}/backup_info.json`;
      const infoJsonData = JSON.stringify(backupInfo);
      console.log(`备份信息文件大小: ${(infoJsonData.length / 1024 / 1024).toFixed(2)}MB`);
      
      fs.writeFileSync(infoFilePath, infoJsonData, 'utf8');
      
      wx.hideLoading();
      
      // 显示备份结果
      this.showFolderBackupResult(backupFolderName, folderPath, successCount, filteredDiaries.length, totalImageCount);
      
    } catch (err) {
      wx.hideLoading();
      console.error('文件夹备份失败:', err);
      
      // 特别处理存储空间不足的错误
      if (err.message && (err.message.includes('storage limit') || err.message.includes('exceeded'))) {
        wx.showModal({
          title: '❌ 存储空间不足',
          content: `备份过程中存储空间不足

请尝试：
1. 选择较少的日记数量
2. 清理不需要的备份文件
3. 使用智能压缩选项`,
          confirmText: '确定',
          showCancel: false
        });
      } else {
        wx.showModal({
          title: '❌ 备份失败',
          content: `备份过程中出现错误：\n${err.message}\n\n请尝试重新备份`,
          confirmText: '重试',
          cancelText: '取消',
          success: (res) => {
            if (res.confirm) {
              setTimeout(() => {
                this.exportDiariesAsFolder(diaries);
              }, 1000);
            }
          }
        });
      }
    }
  }

  /**
   * 询问导出范围
   */
  askExportRange(totalCount) {
    return new Promise((resolve) => {
      console.log('准备显示导出范围选择，日记总数:', totalCount);
      
      // 如果没有日记，直接返回
      if (totalCount <= 0) {
        wx.showToast({ title: '暂无日记可备份', icon: 'none' });
        resolve(null);
        return;
      }

      // 简化实现：直接使用动作面板显示所有范围选项
      const options = [];
      
      // 添加预设选项
      options.push('全部导出');
      options.push('前10篇');
      options.push('前20篇');
      options.push('20篇以后');

      wx.showActionSheet({
        itemList: options,
        title: `📋 导出范围选择（共 ${totalCount} 篇）`,
        success: (res) => {
          console.log('用户选择了选项:', res);
          let start = 1;
          let end = totalCount;
          
          // 根据用户选择设置范围
          switch (res.tapIndex) {
            case 0: // 全部导出
              start = 1;
              end = totalCount;
              break;
            case 1: // 前10篇
              start = 1;
              end = Math.min(10, totalCount);
              break;
            case 2: // 前20篇
              start = 1;
              end = Math.min(20, totalCount);
              break;
            case 3: // 20篇以后
            start = Math.min(20, totalCount);
            end = Math.min(20, totalCount);
              break;
            default:
              start = 1;
              end = totalCount;
          }
          
          resolve({ start, end });
        },
        fail: (err) => {
          // 如果用户取消选择或其他错误
          console.log('用户取消选择或发生错误:', err);
          wx.showToast({ title: '已取消备份', icon: 'none' });
          resolve(null);
        }
      });
    });
  }

  /**
   * 询问压缩级别
   */
  askCompressionLevel(diaryCount) {
    console.log('准备显示压缩选项，日记数量:', diaryCount);
    return new Promise((resolve) => {
      wx.showModal({
        title: '🗂️ 备份压缩选项',
        content: `即将备份 ${diaryCount} 篇日记

请选择压缩方式：
• 智能压缩：自动调整至合适大小（推荐）
• 不压缩：保持原图（可能导致存储超出限制）`,
        confirmText: '智能压缩',
        cancelText: '不压缩',
        success: (res) => {
          console.log('用户选择了压缩选项:', res);
          if (res.confirm) {
            resolve({ level: 'smart', compress: true });
          } else {
            // 给用户一个警告提示
            wx.showModal({
              title: '⚠️ 注意',
              content: '不压缩可能会因为存储空间限制导致备份失败，建议使用智能压缩选项。是否仍要继续？',
              confirmText: '继续',
              cancelText: '使用压缩',
              success: (confirmRes) => {
                if (confirmRes.confirm) {
                  resolve({ level: 'none', compress: false });
                } else {
                  resolve({ level: 'smart', compress: true });
                }
              },
              fail: () => {
                // 默认使用智能压缩
                resolve({ level: 'smart', compress: true });
              }
            });
          }
        },
        fail: (err) => {
          console.log('压缩选项选择失败:', err);
          // 默认使用智能压缩以避免存储限制问题
          resolve({ level: 'smart', compress: true });
        }
      });
    });
  }

  /**
   * 创建单个日记文件
   */
  async createSingleDiaryFile(diary, folderPath, index, compressionOption) {
    try {
      console.log('开始创建单个日记文件，索引:', index, '日记ID:', diary.id);
      // 创建文件名
      const createDate = new Date(diary.createTime);
      const dateStr = this.formatDate(createDate).replace(/-/g, '');
      const fileName = `diary_${dateStr}_${String(index).padStart(3, '0')}.bak`;
      const filePath = `${folderPath}/${fileName}`;
      
      console.log('文件路径:', filePath);
      
      // 处理日记数据
      const processedDiary = {
        id: diary.id,
        content: diary.content,
        createTime: diary.createTime,
        updateTime: diary.updateTime,
        privacyLevel: diary.privacyLevel || 0,
        images: []
      };
      
      let imageCount = 0;
      let totalImageDataSize = 0; // 记录图片数据总大小
      
      console.log('日记内容长度:', diary.content ? diary.content.length : 0);
      console.log('日记图片数量:', diary.images ? diary.images.length : 0);
      
      // 处理图片：压缩并转换为Base64
      if (diary.images && diary.images.length > 0) {
        for (let imgIndex = 0; imgIndex < diary.images.length; imgIndex++) {
          const imageData = diary.images[imgIndex];
          try {
            console.log(`处理第${imgIndex+1}张图片，数据:`, imageData);
            // 检查和解析图片路径
            let imagePath;
            if (typeof imageData === 'string') {
              // 原始格式：直接是路径字符串
              imagePath = imageData;
            } else if (typeof imageData === 'object' && imageData !== null) {
              // 云端恢复格式：包含 cloudPath, originalPath 等字段
              imagePath = imageData.originalPath || imageData.cloudPath || imageData;
              // 如果仍然是对象，尝试获取其中的路径
              if (typeof imagePath === 'object') {
                imagePath = imagePath.tempFilePath || imagePath.url || null;
              }
            } else {
              console.warn('无效的图片数据类型:', typeof imageData, imageData);
              continue;
            }
            
            // 验证图片路径
            if (!imagePath || typeof imagePath !== 'string') {
              console.error('图片路径无效:', imageData);
              // 记录失败的图片
              processedDiary.images.push({
                fileName: 'invalid_image.jpg',
                originalPath: JSON.stringify(imageData),
                error: '路径无效'
              });
              continue;
            }
            
            console.log('图片路径:', imagePath);
            
            // 获取原图文件信息
            const fs = wx.getFileSystemManager();
            let originalFileSize = 0;
            try {
              const fileInfo = fs.statSync(imagePath);
              originalFileSize = fileInfo.size;
              console.log(`原图大小: ${(originalFileSize / 1024 / 1024).toFixed(2)}MB`);
            } catch (statErr) {
              console.warn('无法获取原图大小:', statErr);
            }
            
            // 处理图片（根据压缩选项）
            let processedPath;
            if (compressionOption.compress) {
              // 智能压缩模式
              console.log('开始图片压缩处理');
              processedPath = await this.compressImage(imagePath, compressionOption);
              console.log('图片压缩完成，路径:', processedPath);
            } else {
              // 不压缩模式
              processedPath = imagePath;
            }
            
            // 转换为Base64
            console.log('开始转换图片为Base64');
            const base64Data = await this.imageToBase64(processedPath);
            const base64Size = base64Data.length;
            totalImageDataSize += base64Size;
            console.log(`Base64数据大小: ${(base64Size / 1024 / 1024).toFixed(2)}MB`);
            
            // 获取最终文件大小用于统计
            let finalSize = originalFileSize;
            try {
              const finalFileInfo = fs.statSync(processedPath);
              finalSize = finalFileInfo.size;
            } catch (statErr) {
              console.warn('无法获取处理后图片大小:', statErr);
            }
            
            processedDiary.images.push({
              fileName: this.getFileNameFromPath(imagePath),
              base64Data: base64Data,
              originalPath: imagePath,
              originalSize: originalFileSize,
              base64Size: base64Size,
              compressed: compressionOption.compress,
              compressionType: compressionOption.compress ? 'smart' : 'none',
              finalSize: finalSize,
              finalSizeMB: (finalSize / 1024 / 1024).toFixed(2)
            });
            
            imageCount++;
          } catch (err) {
            console.error('图片处理失败:', imageData, err);
            // 记录失败的图片
            processedDiary.images.push({
              fileName: this.getFileNameFromPath(imageData),
              originalPath: typeof imageData === 'string' ? imageData : JSON.stringify(imageData),
              error: '处理失败'
            });
          }
        }
      }
      
      console.log(`处理完${imageCount}张图片，Base64数据总大小: ${(totalImageDataSize / 1024 / 1024).toFixed(2)}MB`);
      
      // 保存为紧凑JSON格式（不美化，减少大小）
      const jsonData = JSON.stringify(processedDiary);
      const jsonSize = jsonData.length;
      console.log(`JSON数据大小: ${(jsonSize / 1024 / 1024).toFixed(2)}MB`);
      console.log(`预计总文件大小: ${((totalImageDataSize + jsonSize) / 1024 / 1024).toFixed(2)}MB`);
      
      // 在写入前检查数据大小
      console.log('准备写入文件，数据大小:', jsonData.length, '字节');
      if (jsonData.length > this.maxFileSize) {
        console.warn('单个文件数据超过最大限制:', (jsonData.length / 1024 / 1024).toFixed(2), 'MB');
        throw new Error(`单个文件过大 (${(jsonData.length / 1024 / 1024).toFixed(2)} MB)，超过小程序存储限制`);
      }
      
      // 写入文件
      console.log('开始写入文件到路径:', filePath);
      const fs = wx.getFileSystemManager();
      
      // 在写入前检查目录是否存在，如果不存在则创建
      try {
        const dirPath = folderPath;
        fs.accessSync(dirPath);
      } catch (err) {
        // 目录不存在，创建它
        try {
          fs.mkdirSync(folderPath, true); // recursive: true
        } catch (mkdirErr) {
          console.error('创建目录失败:', mkdirErr);
        }
      }
      
      fs.writeFileSync(filePath, jsonData, 'utf8');
      console.log('文件写入成功');
      
      return {
        success: true,
        fileName: fileName,
        imageCount: imageCount
      };
      
    } catch (err) {
      console.error('创建单个日记文件失败:', err);
      
      // 特别处理存储空间不足的错误
      if (err.message && (err.message.includes('storage limit') || err.message.includes('exceeded'))) {
        wx.showToast({
          title: '存储空间不足',
          icon: 'none',
          duration: 3000
        });
        return {
          success: false,
          error: 'storage limit exceeded'
        };
      }
      
      wx.showToast({
        title: '文件创建失败，请尝试压缩选项',
        icon: 'none',
        duration: 3000
      });
      return {
        success: false,
        error: err.message
      };
    }
  }

  /**
   * 计算目录大小
   */
  calculateDirectorySize(fs, dirPath) {
    try {
      let totalSize = 0;
      const files = fs.readdirSync(dirPath);
      
      for (const file of files) {
        try {
          const filePath = `${dirPath}/${file}`;
          const stat = fs.statSync(filePath);
          
          if (stat.isFile()) {
            totalSize += stat.size;
          } else if (stat.isDirectory()) {
            // 递归计算子目录大小
            totalSize += this.calculateDirectorySize(fs, filePath);
          }
        } catch (err) {
          console.warn(`无法访问文件 ${file}:`, err);
        }
      }
      
      return totalSize;
    } catch (err) {
      console.error(`计算目录 ${dirPath} 大小失败:`, err);
      return 0;
    }
  }

  /**
   * 显示文件夹备份结果
   */
  showFolderBackupResult(folderName, folderPath, successCount, totalCount, imageCount) {
    // 计算备份文件夹大小
    let backupSize = '未知';
    try {
      const fs = wx.getFileSystemManager();
      const size = this.calculateDirectorySize(fs, folderPath);
      backupSize = `${(size / 1024 / 1024).toFixed(2)}MB`;
    } catch (err) {
      console.warn('无法计算备份大小:', err);
    }
    
    wx.showModal({
      title: '✅ 备份完成',
      content: `📁 备份文件夹：${folderName}
💾 备份大小：${backupSize}

📊 备份统计：
• 成功：${successCount}/${totalCount} 篇日记
• 图片：${imageCount} 张

🔄 恢复方法：
点击【从文件夹恢复】选择备份文件夹`,
      cancelText: '知道了',
      confirmText: '查看备份',
      success: (res) => {
        if (res.confirm) {
          this.shareFolderBackup(folderPath, folderName);
        }
      }
    });
  }

  // ==================== 文件夹恢复功能 ====================
  
  /**
   * 从文件夹恢复日记
   */
  importFromFolder() {
    // 显示选择方式弹窗
    wx.showModal({
      title: '📂 选择恢复方式',
      content: '请选择备份文件的来源：\n\n• 聊天记录：从微信聊天中选择备份文件\n• 手动导入：通过文件恢复备份数据',
      confirmText: '手动导入',
      cancelText: '聊天记录',
      success: (res) => {
        if (res.confirm) {
          this.chooseFromChat();
        } else {
          this.showManualImport();
        }
      }
    });
  }

  /**
   * 从聊天记录选择文件
   */
  chooseFromChat() {
    wx.chooseMessageFile({
      count: 10, // 支持选择多个文件
      type: 'file',
      success: (res) => {
        const files = res.tempFiles;
        
        if (files.length === 0) {
          wx.showToast({ title: '请选择备份文件', icon: 'none' });
          return;
        }

        // 过滤.bak文件
        const bakFiles = files.filter(file => 
          file.name.endsWith('.bak') || file.name.endsWith('.json')
        );

        if (bakFiles.length === 0) {
          wx.showToast({ title: '未找到有效的备份文件', icon: 'none' });
          return;
        }

        this.processFolderRestore(bakFiles);
      },
      fail: (err) => {
        console.error('选择文件失败:', err);
        wx.showToast({ 
          title: '选择失败，请尝试手动导入', 
          icon: 'none',
          duration: 2000
        });
      }
    });
  }

  /**
   * 显示手动导入界面
   */
  showManualImport() {
    // 跳转到手动导入页面或显示输入框
    wx.showModal({
      title: '📋 手动导入',
      content: '请将备份文件内容复制粘贴到小程序中：\n\n1. 打开文件管理器找到备份文件夹\n2. 打开.bak文件，复制全部内容\n3. 返回小程序，点击确定粘贴',
      confirmText: '开始导入',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          this.showTextImportDialog();
        }
      }
    });
  }

  /**
   * 显示文本导入对话框
   */
  showTextImportDialog() {
    // 创建一个页面来处理文本输入
    wx.navigateTo({
      url: '/pages/display/display?mode=import&title=手动导入备份'
    }).catch(() => {
      // 如果没有display页面，使用简单的输入方式
      wx.showToast({
        title: '请通过聊天记录方式导入',
        icon: 'none',
        duration: 2000
      });
    });
  }

  /**
   * 处理文件夹恢复
   */
  async processFolderRestore(files) {
    try {
      wx.showLoading({ title: '正在恢复数据...' });
      
      const restoredDiaries = [];
      const failedFiles = [];
      let totalImageCount = 0;

      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        wx.showLoading({ title: `处理文件 ${i + 1}/${files.length}...` });
        
        try {
          const diary = await this.restoreSingleDiaryFile(file);
          if (diary) {
            restoredDiaries.push(diary);
            totalImageCount += (diary.images ? diary.images.length : 0);
          }
        } catch (err) {
          console.error(`恢复文件 ${file.name} 失败:`, err);
          failedFiles.push(file.name);
        }
      }

      if (restoredDiaries.length > 0) {
        await this.saveRestoredDiaries(restoredDiaries);
      }

      wx.hideLoading();
      this.showRestoreResult(restoredDiaries.length, failedFiles.length, totalImageCount, failedFiles);

    } catch (err) {
      wx.hideLoading();
      console.error('文件夹恢复失败:', err);
      wx.showModal({
        title: '❌ 恢复失败',
        content: `恢复过程中出现错误：\n${err.message}`,
        confirmText: '确定',
        showCancel: false
      });
    }
  }

  // ==================== 压缩包备份功能 ====================
  
  /**
   * 压缩包备份功能
   */
  async exportAsZip(diaries) {
    try {
      wx.showLoading({ title: '准备压缩包备份...' });
      
      if (diaries.length === 0) {
        wx.hideLoading();
        wx.showToast({ title: '暂无日记可备份', icon: 'none' });
        return;
      }

      // 询问压缩格式
      const formatChoice = await this.askBackupFormat();
      if (!formatChoice) {
        wx.hideLoading();
        return;
      }

      if (formatChoice === 'binary') {
        await this.createBinaryZipBackup(diaries);
      } else {
        await this.createJsonZipBackup(diaries);
      }

    } catch (err) {
      wx.hideLoading();
      console.error('压缩包备份失败:', err);
      this.handleBackupError(err, () => this.exportAsZip(diaries));
    }
  }

  // ==================== 工具方法 ====================
  
  /**
   * 智能压缩图片，根据文件大小自动调整压缩参数
   * 目标：将图片大小控制在2M左右
   */
  async smartCompressImage(imagePath) {
    try {
      // 检查参数类型
      if (!imagePath || typeof imagePath !== 'string') {
        throw new Error(`图片路径参数错误: ${typeof imagePath}, 值: ${imagePath}`);
      }
      
      // 获取原图文件信息
      const fs = wx.getFileSystemManager();
      const fileInfo = fs.statSync(imagePath);
      const originalSize = fileInfo.size; // 字节
      const targetSize = 2 * 1024 * 1024; // 2MB
      
      console.log(`原图大小: ${(originalSize / 1024 / 1024).toFixed(2)}MB`);
      
      // 如果原图小于2MB，使用轻度压缩
      if (originalSize <= targetSize) {
        console.log('原图小于2MB，使用轻度压缩');
        const compressedPath = await this.compressImageWithQuality(imagePath, 90);
        // 检查压缩后的大小
        try {
          const compressedInfo = fs.statSync(compressedPath);
          console.log(`压缩后大小: ${(compressedInfo.size / 1024 / 1024).toFixed(2)}MB`);
        } catch (statErr) {
          console.warn('无法获取压缩后文件大小:', statErr);
        }
        return compressedPath;
      }
      
      // 根据大小计算初始压缩质量
      let quality;
      if (originalSize > 10 * 1024 * 1024) { // > 10MB
        quality = 40;
      } else if (originalSize > 5 * 1024 * 1024) { // > 5MB
        quality = 50;
      } else if (originalSize > 3 * 1024 * 1024) { // > 3MB
        quality = 60;
      } else {
        quality = 70;
      }
      
      console.log(`初始压缩质量: ${quality}`);
      
      // 逐步压缩，直到达到目标大小
      let compressedPath = imagePath;
      let attempts = 0;
      const maxAttempts = 5;
      
      while (attempts < maxAttempts) {
        const testPath = await this.compressImageWithQuality(compressedPath, quality);
        const testFileInfo = fs.statSync(testPath);
        const compressedSize = testFileInfo.size;
        
        console.log(`第${attempts + 1}次压缩 - 质量: ${quality}, 大小: ${(compressedSize / 1024 / 1024).toFixed(2)}MB`);
        
        // 如果达到目标大小或质量太低，停止压缩
        if (compressedSize <= targetSize || quality <= 30) {
          console.log(`压缩完成，最终大小: ${(compressedSize / 1024 / 1024).toFixed(2)}MB`);
          return testPath;
        }
        
        // 调整压缩质量
        compressedPath = testPath;
        quality = Math.max(30, quality - 10); // 逐步降低质量，但不低于30
        attempts++;
      }
      
      return compressedPath;
      
    } catch (err) {
      console.warn('智能压缩失败，使用原图:', err);
      return imagePath;
    }
  }
  
  /**
   * 按指定质量压缩图片
   */
  async compressImageWithQuality(imagePath, quality) {
    try {
      const compressedPath = await new Promise((resolve, reject) => {
        wx.compressImage({
          src: imagePath,
          quality: quality,
          success: (res) => resolve(res.tempFilePath),
          fail: reject
        });
      });
      return compressedPath;
    } catch (err) {
      console.warn(`压缩失败（质量: ${quality}），使用原图:`, err);
      return imagePath;
    }
  }

  /**
   * 图片压缩功能（入口方法）
   */
  async compressImage(imagePath, compressionOption) {
    try {
      // 检查参数类型
      if (!imagePath || typeof imagePath !== 'string') {
        throw new Error(`图片路径参数错误: ${typeof imagePath}, 值: ${imagePath}`);
      }
      
      // 根据压缩选项决定处理方式
      if (compressionOption && compressionOption.compress) {
        // 使用智能压缩
        console.log('使用智能压缩模式');
        return await this.smartCompressImage(imagePath);
      } else {
        // 不压缩，直接返回原图
        console.log('不压缩模式，使用原图');
        return imagePath;
      }
    } catch (err) {
      console.warn('图片处理失败，使用原图:', err);
      return imagePath;
    }
  }

  /**
   * 将图片转换为Base64
   */
  imageToBase64(imagePath) {
    return new Promise((resolve, reject) => {
      try {
        // 检查参数类型
        if (!imagePath || typeof imagePath !== 'string') {
          reject(new Error(`图片路径参数错误: ${typeof imagePath}, 值: ${imagePath}`));
          return;
        }
        
        const fs = wx.getFileSystemManager();
        fs.readFile({
          filePath: imagePath,
          encoding: 'base64',
          success: (res) => {
            resolve(res.data);
          },
          fail: (err) => {
            reject(err);
          }
        });
      } catch (err) {
        reject(err);
      }
    });
  }

  /**
   * 将Base64数据转换为图片文件（使用永久路径）
   */
  base64ToImage(base64Data, fileName) {
    return new Promise((resolve, reject) => {
      try {
        const fs = wx.getFileSystemManager();
        
        // 使用永久目录而不是临时目录
        const permanentDir = `${wx.env.USER_DATA_PATH}/restored_images`;
        const filePath = `${permanentDir}/restored_${Date.now()}_${fileName}`;
        
        // 确保目录存在
        try {
          fs.mkdirSync(permanentDir);
        } catch (err) {
          // 目录已存在，忽略错误
          if (!err.errMsg || !err.errMsg.includes('file already exists')) {
            console.warn('创建恢复图片目录失败:', err);
          }
        }
        
        // 将Base64数据写入文件
        fs.writeFile({
          filePath: filePath,
          data: base64Data,
          encoding: 'base64',
          success: () => {
            resolve(filePath);
          },
          fail: (err) => {
            reject(err);
          }
        });
      } catch (err) {
        reject(err);
      }
    });
  }

  /**
   * 从路径中提取文件名
   */
  getFileNameFromPath(path) {
    if (!path) return 'unknown.jpg';
    
    // 处理对象类型的路径
    if (typeof path === 'object') {
      if (path.originalPath) {
        path = path.originalPath;
      } else if (path.cloudPath) {
        path = path.cloudPath;
      } else {
        return 'object_image.jpg';
      }
    }
    
    // 确保是字符串
    if (typeof path !== 'string') {
      return 'unknown_type.jpg';
    }
    
    const parts = path.split('/');
    const fileName = parts[parts.length - 1];
    return fileName || `image_${Date.now()}.jpg`;
  }

  /**
   * 格式化日期
   */
  formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * 询问备份格式
   */
  askBackupFormat() {
    return new Promise((resolve) => {
      wx.showModal({
        title: '🗜️ 选择备份格式',
        content: '请选择压缩包格式：\n\n• 二进制格式：体积更小，处理更快\n• JSON格式：兼容性更好，易于查看',
        confirmText: '二进制',
        cancelText: 'JSON',
        success: (res) => {
          resolve(res.confirm ? 'binary' : 'json');
        },
        fail: () => resolve(null)
      });
    });
  }

  /**
   * 处理备份错误
   */
  handleBackupError(err, retryCallback) {
    wx.showModal({
      title: '❌ 备份失败',
      content: `备份过程中出现错误：\n${err.message}\n\n请尝试重新备份`,
      confirmText: '重试',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm && retryCallback) {
          setTimeout(retryCallback, 1000);
        }
      }
    });
  }

  /**
   * 显示恢复结果
   */
  showRestoreResult(successCount, failedCount, imageCount, failedFiles) {
    let content = `📊 恢复统计：\n• 成功：${successCount} 篇日记\n• 图片：${imageCount} 张`;
    
    if (failedCount > 0) {
      content += `\n• 失败：${failedCount} 个文件`;
      if (failedFiles.length > 0) {
        content += `

失败文件：
${failedFiles.slice(0, 3).join('\n')}`;
        if (failedFiles.length > 3) {
          content += `\n...等${failedFiles.length}个文件`;
        }
      }
    }

    wx.showModal({
      title: successCount > 0 ? '✅ 恢复完成' : '❌ 恢复失败',
      content: content,
      confirmText: '确定',
      showCancel: false
    });
  }

  // 占位方法，具体实现可以在后续添加
  async restoreSingleDiaryFile(file) {
    try {
      const fs = wx.getFileSystemManager();
      const fileContent = fs.readFileSync(file.path, 'utf8');
      const diary = JSON.parse(fileContent);
      
      // 处理图片恢复
      if (diary.images && diary.images.length > 0) {
        const restoredImages = [];
        for (const imageInfo of diary.images) {
          if (imageInfo.base64Data) {
            try {
              const imagePath = await this.base64ToImage(imageInfo.base64Data, imageInfo.fileName);
              restoredImages.push(imagePath);
            } catch (err) {
              console.error('图片恢复失败:', imageInfo.fileName, err);
            }
          }
        }
        diary.images = restoredImages;
      }
      
      return diary;
    } catch (err) {
      console.error('恢复单个文件失败:', err);
      return null;
    }
  }

  async saveRestoredDiaries(diaries) {
    try {
      // 获取现有的日记
      const existingDiaries = wx.getStorageSync('diaries') || [];
      
      // 合并日记，避免重复
      const mergedDiaries = [...existingDiaries];
      let addedCount = 0;
      
      for (const newDiary of diaries) {
        // 检查是否已存在（根据id或创建时间）
        const exists = existingDiaries.some(existing => 
          existing.id === newDiary.id || 
          existing.createTime === newDiary.createTime
        );
        
        if (!exists) {
          // 生成新的本地ID
          newDiary.id = 'local_' + Date.now().toString() + '_' + Math.random().toString(36).substr(2, 9);
          mergedDiaries.push(newDiary);
          addedCount++;
        }
      }
      
      // 按创建时间排序
      mergedDiaries.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
      
      // 保存到本地
      wx.setStorageSync('diaries', mergedDiaries);
      
      return addedCount;
    } catch (err) {
      console.error('保存恢复的日记失败:', err);
      throw err;
    }
  }

  async createBinaryZipBackup(diaries) {
    // TODO: 实现二进制压缩包备份
    wx.showToast({ title: '二进制格式正在开发中', icon: 'none' });
  }

  async createJsonZipBackup(diaries) {
    // 简单的JSON压缩包备份实现
    wx.showToast({ title: 'JSON格式正在开发中', icon: 'none' });
  }

  shareFolderBackup(folderPath, folderName) {
    // 文件夹分享功能占位
    wx.showToast({ title: '分享功能正在开发中', icon: 'none' });
  }

  // 处理二进制压缩包恢复（占位）
  async processBinaryZipRestore(file, fs) {
    // TODO: 实现二进制数据解析
    throw new Error('二进制格式正在开发中');
  }

  // 处理JSON压缩包恢复（占位）
  async processJsonZipRestore(file, fs) {
    // TODO: 实现JSON数据解析
    throw new Error('JSON格式正在开发中');
  }
}

// 导出单例
const localBackupManager = new LocalBackupManager();

module.exports = {
  LocalBackupManager,
  localBackupManager
};