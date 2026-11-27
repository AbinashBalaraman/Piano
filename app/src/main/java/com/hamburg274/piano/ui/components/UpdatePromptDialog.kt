package com.hamburg274.piano.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamburg274.piano.ui.theme.BorderHairline
import com.hamburg274.piano.ui.theme.ConcertBrass
import com.hamburg274.piano.ui.theme.ConcertBrassBright
import com.hamburg274.piano.ui.theme.CormorantGaramondFamily
import com.hamburg274.piano.ui.theme.IvoryText
import com.hamburg274.piano.ui.theme.JetBrainsMonoFamily
import com.hamburg274.piano.ui.theme.MutedStoneText
import com.hamburg274.piano.ui.theme.ObsidianBg
import com.hamburg274.piano.ui.theme.SurfaceControl
import com.hamburg274.piano.ui.theme.SurfaceElevated
import com.hamburg274.piano.ui.theme.SurfaceSelected
import com.hamburg274.piano.ui.theme.WarmSandText
import com.hamburg274.piano.update.UpdatePromptState
import kotlin.math.roundToInt

@Composable
fun UpdatePromptDialog(
    state: UpdatePromptState,
    onUpdateNow: () -> Unit,
    onViewInStore: () -> Unit,
    onLater: () -> Unit
) {
    if (!state.visible) return

    BackHandler { onLater() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f))
            .clickable { onLater() }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceElevated)
                .border(1.dp, ConcertBrass.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clickable(enabled = false) {}
                .padding(18.dp)
                .testTag("play_update_dialog"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceSelected)
                        .border(1.dp, ConcertBrass.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = ConcertBrassBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = if (state.isDownloaded) "Update Ready to Install" else "New Concert Update Available",
                        fontFamily = CormorantGaramondFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IvoryText
                    )
                    Text(
                        text = "Hamburg 274 · Build ${state.availableVersionCode}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.5.sp,
                        color = ConcertBrass
                    )
                }
            }

            Text(
                text = if (state.isDownloaded) {
                    "The latest Hamburg 274 release has finished downloading. Restart now to apply audio and repertoire improvements."
                } else {
                    "A newer version of Hamburg 274 is available on Google Play with acoustic DSP refinements and bug fixes."
                },
                fontSize = 12.5.sp,
                color = WarmSandText
            )

            if (state.isDownloading) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { state.downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ConcertBrass,
                        trackColor = SurfaceControl
                    )
                    Text(
                        text = "Downloading update… ${(state.downloadProgress * 100f).roundToInt()}%",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.5.sp,
                        color = MutedStoneText
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("update_later_button")
                        .clip(RoundedCornerShape(9.dp))
                        .background(ObsidianBg)
                        .border(1.dp, BorderHairline, RoundedCornerShape(9.dp))
                        .clickable { onLater() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Later", fontSize = 12.sp, color = MutedStoneText)
                }

                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(40.dp)
                        .testTag("update_store_button")
                        .clip(RoundedCornerShape(9.dp))
                        .background(SurfaceControl)
                        .border(1.dp, BorderHairline, RoundedCornerShape(9.dp))
                        .clickable { onViewInStore() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("View in Store", fontSize = 12.sp, color = WarmSandText)
                }

                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(40.dp)
                        .testTag("update_now_button")
                        .clip(RoundedCornerShape(9.dp))
                        .background(ConcertBrass)
                        .clickable(enabled = !state.isDownloading) { onUpdateNow() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.isDownloaded) "Restart Now" else "Update Now",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsidianBg
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
