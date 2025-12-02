package com.example.lovediary.security

/**
 * 隐私等级枚举
 */
object PrivacyLevels {
    const val PUBLIC = 0    // 公开 - 无需验证
    const val PRIVATE = 1   // 私密 - 需要验证身份
    const val ENCRYPTED = 2  // 加密 - 需要验证身份且内容加密
}

/**
 * 权限状态枚举
 */
object AuthStatus {
    const val GUEST = 0     // 访客模式 - 只能看公开内容
    const val AUTHORIZED = 1 // 已验证 - 可以看所有内容
}

/**
 * 隐私管理器常量
 */
object PrivacyManagerConstants {
    const val AUTO_LOCK_TIMEOUT = 5 * 60 * 1000L // 5分钟自动锁定（毫秒）
    const val DEFAULT_PASSWORD = "520520" // 默认密码
    const val PASSWORD_KEY = "master_password" // 密码存储键名
    const val AUTH_EXPIRE_KEY = "auth_expire_time" // 权限过期时间存储键名
    const val AUTH_STATUS_KEY = "auth_status" // 权限状态存储键名
    const val ENCRYPTION_KEY = "diary_encryption_key" // 加密密钥存储键名
}
