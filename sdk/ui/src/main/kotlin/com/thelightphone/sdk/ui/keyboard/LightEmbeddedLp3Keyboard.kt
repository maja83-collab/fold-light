package com.thelightphone.sdk.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import com.thelightphone.lp3Keyboard.ui.DarkKeyboardColors
import com.thelightphone.lp3Keyboard.ui.LightKeyboardColors
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardTheme
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.LocalKeyboardColors
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardWrapper
import com.thelightphone.sdk.ui.LightThemeColors
import com.thelightphone.sdk.ui.LightThemeTokens

/** LP3 portrait display: 1080×1240 px at 3× density → 360×413.33 dp. */
private const val LP3_WIDTH_DP = 1080f / 3f
private const val LP3_HEIGHT_DP = 1240f / 3f

@Composable
fun LightEmbeddedLp3Keyboard(viewModel: Lp3KeyboardViewModel) {
    val layout by viewModel.layoutFlow.collectAsState()
    val keyboardOptions by viewModel.keyboardOptionsFlow.collectAsState()
    val layoutOptions by viewModel.layoutOptionsFlow.collectAsState()

    val keyboardColors = if (LightThemeTokens.colors == LightThemeColors.Light) {
        LightKeyboardColors
    } else {
        DarkKeyboardColors
    }

    val config = LocalConfiguration.current
    val scaleX = config.screenWidthDp / LP3_WIDTH_DP
    val scaleY = config.screenHeightDp / LP3_HEIGHT_DP
    val scale = minOf(scaleX, scaleY).coerceAtLeast(1f)

    Lp3KeyboardTheme(keyboardColors) {
        val colors = LocalKeyboardColors.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier.graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                ),
            ) {
                Lp3KeyboardWrapper(
                    layout = layout,
                    keyboardOptions = keyboardOptions,
                    layoutOptions = layoutOptions,
                    callback = viewModel,
                )
            }
        }
    }
}
