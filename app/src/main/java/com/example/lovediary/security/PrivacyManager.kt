package com.example.lovediary.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.lovediary.data.entity.Diary
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 隐私管理器
 * 处理日记的隐私等级、权限验证和数据加密
 */
class PrivacyManager(private val context: Context) {
    // 当前权限状态
    private var currentAuthStatus: Int = AuthStatus.GUEST
    // 权限过期时间
    private var authExpireTime: Long = 0L
    // 主密码
    private var masterPassword: String = ""
    // 自动锁定超时时间
    private val autoLockTimeout: Long = PrivacyManagerConstants.AUTO_LOCK_TIMEOUT
    // 加密密钥
    private val encryptionKey: SecretKey by lazy { getOrCreateEncryptionKey() }
    // 加密共享偏好设置
    private val encryptedSharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "privacy_preferences",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    init {
        // 初始化时从存储中读取状态
        loadState()
    }

    /**
     * 加载状态
     */
    private fun loadState() {
        // 加载密码
        masterPassword = encryptedSharedPreferences.getString(
            PrivacyManagerConstants.PASSWORD_KEY,
            hashPassword(PrivacyManagerConstants.DEFAULT_PASSWORD)
        ) ?: hashPassword(PrivacyManagerConstants.DEFAULT_PASSWORD)

        // 加载权限状态
        currentAuthStatus = encryptedSharedPreferences.getInt(
            PrivacyManagerConstants.AUTH_STATUS_KEY,
            AuthStatus.GUEST
        )

        // 加载过期时间
        authExpireTime = encryptedSharedPreferences.getLong(
            PrivacyManagerConstants.AUTH_EXPIRE_KEY,
            0L
        )

        // 检查权限是否过期
        if (currentAuthStatus == AuthStatus.AUTHORIZED && System.currentTimeMillis() > authExpireTime) {
            logout()
        }
    }

    /**
     * 保存状态
     */
    private fun saveState() {
        encryptedSharedPreferences.edit()
            .putInt(PrivacyManagerConstants.AUTH_STATUS_KEY, currentAuthStatus)
            .putLong(PrivacyManagerConstants.AUTH_EXPIRE_KEY, authExpireTime)
            .apply()
    }

    /**
     * 密码哈希
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 验证密码
     */
    fun verifyPassword(inputPassword: String): Boolean {
        return hashPassword(inputPassword) == masterPassword
    }

    /**
     * 更改主密码
     */
    fun changeMasterPassword(oldPassword: String, newPassword: String): Result<Unit> {
        if (!verifyPassword(oldPassword)) {
            return Result.failure(Exception("原密码错误"))
        }

        masterPassword = hashPassword(newPassword)
        encryptedSharedPreferences.edit()
            .putString(PrivacyManagerConstants.PASSWORD_KEY, masterPassword)
            .apply()

        return Result.success(Unit)
    }

    /**
     * 身份验证
     */
    fun authenticate(password: String): Result<Unit> {
        if (verifyPassword(password)) {
            currentAuthStatus = AuthStatus.AUTHORIZED
            authExpireTime = System.currentTimeMillis() + autoLockTimeout
            saveState()
            return Result.success(Unit)
        }
        return Result.failure(Exception("密码错误"))
    }

    /**
     * 检查当前权限状态
     */
    fun checkAuthStatus(): Int {
        if (currentAuthStatus == AuthStatus.AUTHORIZED) {
            if (System.currentTimeMillis() > authExpireTime) {
                // 权限过期，自动锁定
                logout()
                return AuthStatus.GUEST
            }
            // 延长有效期
            authExpireTime = System.currentTimeMillis() + autoLockTimeout
            saveState()
        }
        return currentAuthStatus
    }

    /**
     * 退出登录
     */
    fun logout() {
        currentAuthStatus = AuthStatus.GUEST
        authExpireTime = 0L
        saveState()
    }

    /**
     * 获取或创建加密密钥
     */
    private fun getOrCreateEncryptionKey(): SecretKey {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        val alias = "diary_encryption_key"

        // 检查密钥是否存在
        if (!keyStore.containsAlias(alias)) {
            // 创建密钥生成参数
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            // 生成密钥
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }

        // 获取密钥
        return keyStore.getKey(alias, null) as SecretKey
    }

    /**
     * 加密文本内容
     */
    fun encryptText(text: String): String {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(text.toByteArray())
            
            // 将IV和加密数据组合，使用Base64编码
            val combined = iv + encryptedBytes
            return "encrypted:${Base64.encodeToString(combined, Base64.NO_WRAP)}"
        } catch (e: Exception) {
            Log.e("PrivacyManager", "加密失败", e)
            return text
        }
    }

    /**
     * 解密文本内容
     */
    fun decryptText(encryptedText: String): String {
        try {
            if (!encryptedText.startsWith("encrypted:")) {
                return encryptedText // 不是加密内容
            }

            val encrypted = encryptedText.replace("encrypted:", "")
            val combined = Base64.decode(encrypted, Base64.NO_WRAP)
            
            // 分离IV和加密数据
            val iv = combined.copyOfRange(0, 12) // GCM IV长度为12字节
            val encryptedBytes = combined.copyOfRange(12, combined.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmParameterSpec = GCMParameterSpec(128, iv) // 128位认证标签
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmParameterSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes)
        } catch (e: Exception) {
            Log.e("PrivacyManager", "解密失败", e)
            return "[加密内容解析失败]"
        }
    }

    /**
     * 过滤日记列表（根据当前权限）
     */
    fun filterDiaries(diaries: List<Diary>): List<Diary> {
        val currentAuth = checkAuthStatus()

        return diaries.map { diary ->
            val privacyLevel = diary.privacyLevel

            // 访客模式只能看公开内容
            if (currentAuth == AuthStatus.GUEST && privacyLevel != PrivacyLevels.PUBLIC) {
                diary.copy(
                    content = getPrivacyPlaceholder(privacyLevel),
                    privacyLevel = privacyLevel
                )
            } else {
                // 已验证用户可以看所有内容
                diary.copy(
                    content = diary.content,
                    privacyLevel = privacyLevel
                )
            }
        }
    }

    /**
     * 获取隐私占位文本
     */
    fun getPrivacyPlaceholder(privacyLevel: Int): String {
        return when (privacyLevel) {
            PrivacyLevels.PRIVATE -> "🔒 这是一篇私密日记，需要验证身份后查看"
            else -> "🔒 受保护的内容"
        }
    }

    /**
     * 处理日记保存
     */
    fun processDiaryForSave(diary: Diary): Diary {
        // 直接返回日记，不做额外处理
        return diary
    }

    /**
     * 获取隐私等级选项
     */
    fun getPrivacyLevelOptions(): List<PrivacyOption> {
        return listOf(
            PrivacyOption(
                value = PrivacyLevels.PUBLIC,
                label = "🌐 公开",
                desc = "所有人都可以查看"
            ),
            PrivacyOption(
                value = PrivacyLevels.PRIVATE,
                label = "🔒 私密",
                desc = "需要验证身份后查看"
            )
        )
    }

    /**
     * 隐私等级选项数据类
     */
    data class PrivacyOption(
        val value: Int,
        val label: String,
        val desc: String
    )
}