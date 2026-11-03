package com.nool.app.data.git

import com.jcraft.jsch.HostKey
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.eclipse.jgit.util.FS

class SshSessionFactoryImpl(
    private val credentialsStore: GitCredentialsStore
) : JschConfigSessionFactory() {

    override fun configure(hc: OpenSshConfig.Host?, session: Session?) {
        session?.let { s ->
            // Trust On First Use (TOFU) host key repository check
            val knownHosts = credentialsStore.getKnownHostsFile()
            if (knownHosts.length() == 0L) {
                // If known_hosts is empty, allow first connection and save host key
                s.setConfig("StrictHostKeyChecking", "ask")
            } else {
                s.setConfig("StrictHostKeyChecking", "yes")
            }
            s.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password")
        }
    }

    override fun createDefaultJSch(fs: FS?): JSch {
        val jsch = super.createDefaultJSch(fs)

        val knownHosts = credentialsStore.getKnownHostsFile()
        if (knownHosts.exists()) {
            jsch.setKnownHosts(knownHosts.absolutePath)
        }

        val privateKey = credentialsStore.getSshPrivateKey()
        val passphrase = credentialsStore.getSshPassphrase()

        if (!privateKey.isNullOrBlank()) {
            val keyBytes = privateKey.toByteArray(Charsets.UTF_8)
            val passBytes = passphrase?.toByteArray(Charsets.UTF_8)
            jsch.addIdentity("nool_identity", keyBytes, null, passBytes)
        }

        return jsch
    }
}
