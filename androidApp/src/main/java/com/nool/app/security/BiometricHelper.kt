package com.nool.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    const val BACKGROUND_LOCK_TIMEOUT_MS = 30_000L

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val status = biometricManager.canAuthenticate(authenticators)
        return status == BiometricManager.BIOMETRIC_SUCCESS ||
                status == BiometricManager.BIOMETRIC_STATUS_UNKNOWN
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Nool",
        subtitle: String = "Confirm your identity to access your notes",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> }
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }
            }
        )

        prompt.authenticate(promptInfo)
    }

    /**
     * Determines whether the app should lock after returning from background.
     */
    fun shouldLockOnForeground(
        isBiometricLockEnabled: Boolean,
        backgroundTimestampMs: Long,
        currentTimestampMs: Long = System.currentTimeMillis(),
        timeoutMs: Long = BACKGROUND_LOCK_TIMEOUT_MS
    ): Boolean {
        if (!isBiometricLockEnabled) return false
        if (backgroundTimestampMs <= 0) return false
        return (currentTimestampMs - backgroundTimestampMs) > timeoutMs
    }
}
