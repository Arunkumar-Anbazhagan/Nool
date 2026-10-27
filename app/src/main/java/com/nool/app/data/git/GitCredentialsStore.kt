package com.nool.app.data.git

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import java.io.ByteArrayOutputStream
import java.io.File

class GitCredentialsStore(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = try {
        EncryptedSharedPreferences.create(
            context,
            "nool_git_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback for devices where Keystore keystore initialization fails
        context.getSharedPreferences("nool_git_fallback_prefs", Context.MODE_PRIVATE)
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

    fun getRepoSubfolder(): String = securePrefs.getString(KEY_REPO_SUBFOLDER, "") ?: ""
    fun setRepoSubfolder(subfolder: String) = securePrefs.edit().putString(KEY_REPO_SUBFOLDER, subfolder).apply()

    fun getRemoteUrl(): String = securePrefs.getString(KEY_REMOTE_URL, "") ?: ""
    fun setRemoteUrl(url: String) = securePrefs.edit().putString(KEY_REMOTE_URL, url).apply()

    fun getRemoteName(): String = securePrefs.getString(KEY_REMOTE_NAME, "origin") ?: "origin"
    fun setRemoteName(name: String) = securePrefs.edit().putString(KEY_REMOTE_NAME, name).apply()

    fun getAuthorName(): String = securePrefs.getString(KEY_AUTHOR_NAME, "Nool Mobile") ?: "Nool Mobile"
    fun setAuthorName(name: String) = securePrefs.edit().putString(KEY_AUTHOR_NAME, name).apply()

    fun getAuthorEmail(): String = securePrefs.getString(KEY_AUTHOR_EMAIL, "nool@device.local") ?: "nool@device.local"
    fun setAuthorEmail(email: String) = securePrefs.edit().putString(KEY_AUTHOR_EMAIL, email).apply()

    fun isAllowSelfSigned(): Boolean = securePrefs.getBoolean(KEY_ALLOW_SELF_SIGNED, false)
    fun setAllowSelfSigned(allow: Boolean) = securePrefs.edit().putBoolean(KEY_ALLOW_SELF_SIGNED, allow).apply()

    fun getAuthType(): String = securePrefs.getString(KEY_AUTH_TYPE, "NONE") ?: "NONE"
    fun setAuthType(type: String) = securePrefs.edit().putString(KEY_AUTH_TYPE, type).apply()

    fun getHttpsToken(): String? = securePrefs.getString(KEY_HTTPS_TOKEN, null)
    fun getHttpsUser(): String = securePrefs.getString(KEY_HTTPS_USER, "token") ?: "token"
    fun setHttpsCredentials(token: String, username: String = "token") {
        securePrefs.edit()
            .putString(KEY_AUTH_TYPE, "HTTPS")
            .putString(KEY_HTTPS_TOKEN, token)
            .putString(KEY_HTTPS_USER, username)
            .apply()
    }

    fun getSshPrivateKey(): String? = securePrefs.getString(KEY_SSH_PRIVATE, null)
    fun getSshPublicKey(): String? = securePrefs.getString(KEY_SSH_PUBLIC, null)
    fun getSshPassphrase(): String? = securePrefs.getString(KEY_SSH_PASSPHRASE, null)

    fun setSshCredentials(privateKey: String, publicKey: String, passphrase: String? = null) {
        securePrefs.edit()
            .putString(KEY_AUTH_TYPE, "SSH")
            .putString(KEY_SSH_PRIVATE, privateKey)
            .putString(KEY_SSH_PUBLIC, publicKey)
            .putString(KEY_SSH_PASSPHRASE, passphrase)
            .apply()
    }

    fun clearCredentials() {
        securePrefs.edit()
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
        val file = File(context.filesDir, "git_known_hosts")
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
