package com.nool.app.data.git

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import java.io.ByteArrayOutputStream
import java.io.File

class GitCredentialsStore(private val context: Context? = null) {

    @Volatile
    private var prefs: SharedPreferences? = null
    private val prefsLock = Any()

    private fun getPrefs(): SharedPreferences {
        prefs?.let { return it }
        return synchronized(prefsLock) {
            prefs ?: run {
                val p = if (context == null) {
                    InMemorySharedPreferences()
                } else {
                    try {
                        val masterKey = MasterKey.Builder(context)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build()
                        EncryptedSharedPreferences.create(
                            context,
                            "nool_git_secure_prefs",
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )
                    } catch (e: Exception) {
                        context.getSharedPreferences("nool_git_fallback_prefs", Context.MODE_PRIVATE)
                    }
                }
                prefs = p
                p
            }
        }
    }

    companion object {
        private const val KEY_AUTH_TYPE = "auth_type"
        private const val KEY_HTTPS_TOKEN = "https_token"
        private const val KEY_HTTPS_USER = "https_user"
        private const val KEY_SSH_PRIVATE = "ssh_private_key"
        private const val KEY_SSH_PUBLIC = "ssh_public_key"
        private const val KEY_SSH_PASSPHRASE = "ssh_passphrase"
        private const val KEY_AUTHOR_NAME = "author_name"
        private const val KEY_AUTHOR_EMAIL = "author_email"
        private const val KEY_REMOTE_URL = "remote_url"
        private const val KEY_REMOTE_NAME = "remote_name"
        private const val KEY_ALLOW_SELF_SIGNED = "allow_self_signed"
        private const val KEY_REPO_SUBFOLDER = "repo_subfolder"
    }

    fun getRepoSubfolder(): String = getPrefs().getString(KEY_REPO_SUBFOLDER, "") ?: ""
    fun setRepoSubfolder(subfolder: String) = getPrefs().edit().putString(KEY_REPO_SUBFOLDER, subfolder).apply()

    fun getRemoteUrl(): String = getPrefs().getString(KEY_REMOTE_URL, "") ?: ""
    fun setRemoteUrl(url: String) = getPrefs().edit().putString(KEY_REMOTE_URL, url).apply()

    fun getRemoteName(): String = getPrefs().getString(KEY_REMOTE_NAME, "origin") ?: "origin"
    fun setRemoteName(name: String) = getPrefs().edit().putString(KEY_REMOTE_NAME, name).apply()

    fun getAuthorName(): String = getPrefs().getString(KEY_AUTHOR_NAME, "Nool Mobile") ?: "Nool Mobile"
    fun setAuthorName(name: String) = getPrefs().edit().putString(KEY_AUTHOR_NAME, name).apply()

    fun getAuthorEmail(): String = getPrefs().getString(KEY_AUTHOR_EMAIL, "nool@device.local") ?: "nool@device.local"
    fun setAuthorEmail(email: String) = getPrefs().edit().putString(KEY_AUTHOR_EMAIL, email).apply()

    fun isAllowSelfSigned(): Boolean = getPrefs().getBoolean(KEY_ALLOW_SELF_SIGNED, false)
    fun setAllowSelfSigned(allow: Boolean) = getPrefs().edit().putBoolean(KEY_ALLOW_SELF_SIGNED, allow).apply()

    fun getAuthType(): String = getPrefs().getString(KEY_AUTH_TYPE, "NONE") ?: "NONE"
    fun setAuthType(type: String) = getPrefs().edit().putString(KEY_AUTH_TYPE, type).apply()

    fun getHttpsToken(): String? = getPrefs().getString(KEY_HTTPS_TOKEN, null)
    fun getHttpsUser(): String = getPrefs().getString(KEY_HTTPS_USER, "token") ?: "token"
    fun setHttpsCredentials(token: String, username: String = "token") {
        getPrefs().edit()
            .putString(KEY_AUTH_TYPE, "HTTPS")
            .putString(KEY_HTTPS_TOKEN, token)
            .putString(KEY_HTTPS_USER, username)
            .apply()
    }

    fun getSshPrivateKey(): String? = getPrefs().getString(KEY_SSH_PRIVATE, null)
    fun getSshPublicKey(): String? = getPrefs().getString(KEY_SSH_PUBLIC, null)
    fun getSshPassphrase(): String? = getPrefs().getString(KEY_SSH_PASSPHRASE, null)

    fun setSshCredentials(privateKey: String, publicKey: String, passphrase: String? = null) {
        getPrefs().edit()
            .putString(KEY_AUTH_TYPE, "SSH")
            .putString(KEY_SSH_PRIVATE, privateKey)
            .putString(KEY_SSH_PUBLIC, publicKey)
            .putString(KEY_SSH_PASSPHRASE, passphrase)
            .apply()
    }

    fun clearCredentials() {
        getPrefs().edit()
            .remove(KEY_AUTH_TYPE)
            .remove(KEY_HTTPS_TOKEN)
            .remove(KEY_HTTPS_USER)
            .remove(KEY_SSH_PRIVATE)
            .remove(KEY_SSH_PUBLIC)
            .remove(KEY_SSH_PASSPHRASE)
            .apply()
    }

    fun getActiveAuth(): GitAuth {
        return when (getAuthType()) {
            "HTTPS" -> {
                val token = getHttpsToken()
                if (!token.isNullOrBlank()) {
                    GitAuth.Token(token = token, username = getHttpsUser())
                } else GitAuth.None
            }
            "SSH" -> {
                val privateKey = getSshPrivateKey()
                if (!privateKey.isNullOrBlank()) {
                    GitAuth.Ssh(
                        privateKey = privateKey,
                        passphrase = getSshPassphrase(),
                        knownHosts = getKnownHostsFile().absolutePath
                    )
                } else GitAuth.None
            }
            else -> GitAuth.None
        }
    }

    /**
     * Generates a new 256-bit Ed25519 SSH Keypair.
     */
    fun generateEd25519Keypair(comment: String = "nool@android"): Pair<String, String> {
        val jsch = JSch()
        val keyPair = KeyPair.genKeyPair(jsch, KeyPair.ED25519)

        val privStream = ByteArrayOutputStream()
        val pubStream = ByteArrayOutputStream()

        keyPair.writePrivateKey(privStream)
        keyPair.writePublicKey(pubStream, comment)

        val privKey = privStream.toString("UTF-8")
        val pubKey = pubStream.toString("UTF-8")

        setSshCredentials(privKey, pubKey, null)
        keyPair.dispose()

        return privKey to pubKey
    }

    fun getKnownHostsFile(): File {
        val dir = context?.filesDir ?: File(System.getProperty("java.io.tmpdir") ?: ".")
        val file = File(dir, "git_known_hosts")
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

    /**
     * Store a verified host key fingerprint (TOFU).
     */
    fun saveHostFingerprint(host: String, type: String, keyBase64: String) {
        val line = "$host $type $keyBase64\n"
        getKnownHostsFile().appendText(line)
    }

    /**
     * Redacts credentials from remote URLs for safe logging.
     */
    fun redactUrl(url: String): String {
        return url.replace(Regex("://[^@]+@"), "://***@")
    }
}

private class InMemorySharedPreferences : SharedPreferences {
    private val data = java.util.concurrent.ConcurrentHashMap<String, Any>()

    override fun getAll(): MutableMap<String, *> = HashMap(data)
    override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (data[key] as? Set<String>)?.toMutableSet() ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = (data[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (data[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (data[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (data[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = data.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor(this)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class Editor(private val prefs: InMemorySharedPreferences) : SharedPreferences.Editor {
        private val temp = HashMap<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor { if (key != null) temp[key] = value; return this }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { if (key != null) temp[key] = values?.toSet(); return this }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor { if (key != null) temp[key] = value; return this }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor { if (key != null) temp[key] = value; return this }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { if (key != null) temp[key] = value; return this }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { if (key != null) temp[key] = value; return this }
        override fun remove(key: String?): SharedPreferences.Editor { if (key != null) temp[key] = this; return this }
        override fun clear(): SharedPreferences.Editor { clear = true; return this }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            if (clear) prefs.data.clear()
            for ((k, v) in temp) {
                if (v === this) prefs.data.remove(k)
                else if (v != null) prefs.data[k] = v
                else prefs.data.remove(k)
            }
        }
    }
}
