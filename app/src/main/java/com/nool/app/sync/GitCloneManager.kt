package com.nool.app.sync

import android.content.Context
import com.nool.app.data.git.CloneJobState
import com.nool.app.data.git.CloneJobStatus
import com.nool.app.data.git.CloneStrategy
import com.nool.app.data.git.GitCredentialsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GitCloneManager(
    private val context: Context,
    private val credentialsStore: GitCredentialsStore
) {
    private val _cloneJobState = MutableStateFlow(credentialsStore.getCloneJobState())
    val cloneJobState: StateFlow<CloneJobState> = _cloneJobState.asStateFlow()

    init {
        // If state was left RUNNING from a previously killed process, keep it recoverable
        val persisted = credentialsStore.getCloneJobState()
        _cloneJobState.value = persisted
    }

    fun updateState(state: CloneJobState) {
        _cloneJobState.value = state
        credentialsStore.saveCloneJobState(state)
    }

    fun startClone(
        remoteUrl: String,
        targetSubfolder: String,
        authType: String,
        strategy: CloneStrategy
    ) {
        val initial = CloneJobState(
            url = remoteUrl,
            subfolder = targetSubfolder,
            authType = authType,
            strategy = strategy,
            status = CloneJobStatus.RUNNING,
            phase = "Initiating clone job...",
            progress = 0f
        )
        updateState(initial)
        GitCloneForegroundService.start(context, remoteUrl, targetSubfolder, authType, strategy)
    }

    fun clearState() {
        val cleared = CloneJobState()
        _cloneJobState.value = cleared
        credentialsStore.clearCloneJobState()
    }
}
