package com.hamburg274.piano.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Pure, unit-testable gating policy for the Google Play In-App Update prompt:
 * - Enforces a 24-hour dismissal cooldown per [availableVersionCode].
 * - Bypasses cooldown immediately when a newer versionCode is released.
 * - Bypasses cooldown immediately when an update is already DOWNLOADED and ready to restart/install.
 * - Bypasses cooldown on explicit manual "Check for Updates" triggers.
 */
object UpdateDismissalPolicy {
    const val COOLDOWN_MILLIS: Long = 24L * 60L * 60L * 1000L

    fun shouldShowPrompt(
        availableVersionCode: Int,
        isDownloaded: Boolean,
        manualCheck: Boolean,
        dismissedVersionCode: Int,
        dismissedAtMillis: Long,
        nowMillis: Long
    ): Boolean {
        if (manualCheck || isDownloaded) return true
        if (dismissedVersionCode <= 0 || availableVersionCode > dismissedVersionCode) return true
        return (nowMillis - dismissedAtMillis) >= COOLDOWN_MILLIS
    }
}

data class UpdatePromptState(
    val visible: Boolean = false,
    val availableVersionCode: Int = 0,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val isDownloaded: Boolean = false,
    val isChecking: Boolean = false,
    val statusMessage: String? = null
)

class PlayUpdateController(private val context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(appContext)

    private val _state = MutableStateFlow(UpdatePromptState())
    val state: StateFlow<UpdatePromptState> = _state.asStateFlow()

    private var cachedUpdateInfo: AppUpdateInfo? = null

    private val installListener = InstallStateUpdatedListener { installState ->
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = installState.totalBytesToDownload().coerceAtLeast(1L)
                val progress = (installState.bytesDownloaded().toFloat() / total.toFloat()).coerceIn(0f, 1f)
                _state.update {
                    it.copy(
                        visible = true,
                        isDownloading = true,
                        downloadProgress = progress,
                        isDownloaded = false
                    )
                }
            }
            InstallStatus.DOWNLOADED -> {
                _state.update {
                    it.copy(
                        visible = true,
                        isDownloading = false,
                        downloadProgress = 1f,
                        isDownloaded = true,
                        statusMessage = "Update downloaded. Restart to finish installing."
                    )
                }
            }
            InstallStatus.FAILED, InstallStatus.CANCELED -> {
                _state.update {
                    it.copy(
                        isDownloading = false,
                        statusMessage = "In-app update canceled. You can update anytime in Google Play."
                    )
                }
            }
            else -> Unit
        }
    }

    fun register() {
        appUpdateManager.registerListener(installListener)
    }

    fun unregister() {
        appUpdateManager.unregisterListener(installListener)
    }

    fun checkForUpdates(manual: Boolean) {
        _state.update { it.copy(isChecking = true, statusMessage = null) }
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                cachedUpdateInfo = info
                val availability = info.updateAvailability()
                val isDownloaded = info.installStatus() == InstallStatus.DOWNLOADED
                val isAvailable = availability == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

                if (isDownloaded || isAvailable) {
                    val versionCode = info.availableVersionCode()
                    val show = UpdateDismissalPolicy.shouldShowPrompt(
                        availableVersionCode = versionCode,
                        isDownloaded = isDownloaded,
                        manualCheck = manual,
                        dismissedVersionCode = prefs.getInt(KEY_DISMISSED_VERSION, -1),
                        dismissedAtMillis = prefs.getLong(KEY_DISMISSED_AT, 0L),
                        nowMillis = System.currentTimeMillis()
                    )
                    _state.update {
                        it.copy(
                            visible = show,
                            availableVersionCode = versionCode,
                            isDownloaded = isDownloaded,
                            isChecking = false,
                            statusMessage = if (isDownloaded) "Update ready to install." else null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            visible = false,
                            isChecking = false,
                            statusMessage = if (manual) "You are on the latest version." else null
                        )
                    }
                }
            }
            .addOnFailureListener {
                _state.update {
                    it.copy(
                        isChecking = false,
                        statusMessage = if (manual) "Unable to reach Google Play right now. You can check the Play Store directly." else null
                    )
                }
            }
    }

    fun startFlexibleUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val info = cachedUpdateInfo
        if (_state.value.isDownloaded) {
            appUpdateManager.completeUpdate()
            return
        }
        if (info == null) {
            openPlayStoreListing()
            return
        }
        val started = runCatching {
            appUpdateManager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
            )
        }.getOrDefault(false)

        if (!started) {
            openPlayStoreListing()
        }
    }

    fun dismissForCooldown() {
        val code = _state.value.availableVersionCode
        if (code > 0 && !_state.value.isDownloaded) {
            prefs.edit()
                .putInt(KEY_DISMISSED_VERSION, code)
                .putLong(KEY_DISMISSED_AT, System.currentTimeMillis())
                .apply()
        }
        _state.update { it.copy(visible = false) }
    }

    fun clearStatusMessage() {
        _state.update { it.copy(statusMessage = null) }
    }

    fun openPlayStoreListing() {
        val pkg = appContext.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { appContext.startActivity(marketIntent) }
            .onFailure { runCatching { appContext.startActivity(webIntent) } }
    }

    companion object {
        private const val PREFS_NAME = "hamburg274_play_updates"
        private const val KEY_DISMISSED_VERSION = "dismissed_version_code"
        private const val KEY_DISMISSED_AT = "dismissed_at_millis"
    }
}
