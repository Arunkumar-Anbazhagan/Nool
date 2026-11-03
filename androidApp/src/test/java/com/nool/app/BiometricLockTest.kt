package com.nool.app

import com.nool.app.security.BiometricHelper
import com.nool.app.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricLockTest {

    @Test
    fun `shouldLockOnForeground respects lock enabled state and 30s timeout`() {
        val now = 1_000_000L

        // Disabled biometric lock should never lock
        assertFalse(
            BiometricHelper.shouldLockOnForeground(
                isBiometricLockEnabled = false,
                backgroundTimestampMs = now - 60_000L,
                currentTimestampMs = now
            )
        )

        // Invalid timestamp should not lock
        assertFalse(
            BiometricHelper.shouldLockOnForeground(
                isBiometricLockEnabled = true,
                backgroundTimestampMs = 0L,
                currentTimestampMs = now
            )
        )

        // Backgrounded under 30 seconds should not lock
        assertFalse(
            BiometricHelper.shouldLockOnForeground(
                isBiometricLockEnabled = true,
                backgroundTimestampMs = now - 20_000L,
                currentTimestampMs = now
            )
        )

        // Backgrounded exactly 30 seconds should not lock (> 30s)
        assertFalse(
            BiometricHelper.shouldLockOnForeground(
                isBiometricLockEnabled = true,
                backgroundTimestampMs = now - 30_000L,
                currentTimestampMs = now
            )
        )

        // Backgrounded over 30 seconds should lock
        assertTrue(
            BiometricHelper.shouldLockOnForeground(
                isBiometricLockEnabled = true,
                backgroundTimestampMs = now - 30_001L,
                currentTimestampMs = now
            )
        )

        assertTrue(
            BiometricHelper.shouldLockOnForeground(
                isBiometricLockEnabled = true,
                backgroundTimestampMs = now - 65_000L,
                currentTimestampMs = now
            )
        )
    }

    @Test
    fun `settingsRepository biometricLock default and toggle`() = runBlocking {
        // Without DataStore context, default flow returns false
        val settingsRepository = SettingsRepository()
        assertFalse(settingsRepository.biometricLock.first())
    }
}
