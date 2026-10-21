package com.thelightphone.sdk.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.ui.DarkKeyboardColors
import com.thelightphone.lp3Keyboard.ui.LightKeyboardColors
import com.thelightphone.lp3Keyboard.ui.LocalKeyboardColors
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardTheme
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardWrapper
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3KeyboardViewModel
import com.thelightphone.sdk.ui.LightThemeColors
import com.thelightphone.sdk.ui.LightThemeTokens

@Composable
fun LightEmbeddedLp3Keyboard(
    viewModel: Lp3KeyboardViewModel<*>, // SwipeResult unused for text input
    additionalBottomHeight: Dp = 0.dp,
    bottomBar: (@Composable () -> Unit)? = null,
    handleHardwareKeyboardInput: Boolean = false,
) {
    val layout by viewModel.layoutFlow.collectAsState()
    val keyboardOptions by viewModel.keyboardOptionsFlow.collectAsState()
    val layoutOptions by viewModel.layoutOptionsFlow.collectAsState()

    val keyboardColors = if (LightThemeTokens.colors == LightThemeColors.Light) {
        LightKeyboardColors
    } else {
        DarkKeyboardColors
    }
    Lp3KeyboardTheme(keyboardColors) {
        val colors = LocalKeyboardColors.current
        // No extra top padding here — Lp3KeyboardWrapper already has Spacer(10.dp).
        // Extra padding was making the keys feel cramped on LP3 (1080×1240).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background),
        ) {
            Lp3KeyboardWrapper(
                layout = layout,
                keyboardOptions = keyboardOptions,
                layoutOptions = layoutOptions,
                callback = viewModel,
                swipeCallback = null,
                handleHardwareKeyboardInput = handleHardwareKeyboardInput,
                additionalBottomHeight = additionalBottomHeight,
                bottomBar = bottomBar,
            )
        }
    }
}
