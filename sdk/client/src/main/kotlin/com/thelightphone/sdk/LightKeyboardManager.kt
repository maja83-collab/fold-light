package com.thelightphone.sdk

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.parseEmojiString
import com.thelightphone.lp3Keyboard.ui.viewmodel.defaultEmojis
import com.thelightphone.sdk.shared.LightServiceMethod
import com.thelightphone.sdk.shared.error
import com.thelightphone.sdk.shared.getOrNull
import com.thelightphone.sdk.ui.defaultKeyboardOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "LightKeyboardManager"

suspend fun refreshKeyboardOptions(): KeyboardOptions? {
    val result =
        callRemoteServiceMethod(LightServiceMethod.GetKeyboardOptions, Unit).let { result ->
            result.error?.let {
                Log.e(TAG, "Error getting keyboard options, code:${it.code}, message:${it.extra}")
                return null
            }
            val options = result.getOrNull()
            if (options == null) {
                Log.e(TAG, "Keyboard options returned null")
                return null
            }
            options
        }

    return KeyboardOptions(
        emojis = parseEmojiString(result.emojisAsString) ?: defaultEmojis,
        displayReturn = true,
        displayVoice = result.displayVoice,
        enableKeyAnimation = result.enableKeyAnimation,
        swipeEnabled = false,
    )
}

private var cachedOptions = defaultKeyboardOptions()

@Composable
fun rememberKeyboardOptions(
    initialOptions: KeyboardOptions = cachedOptions
): StateFlow<KeyboardOptions> {
    val flow = remember { MutableStateFlow(initialOptions) }

    // Refresh once — not on every recomposition (that cancelled mid-keystroke and
    // could drop letters when hopping hotel fields).
    LaunchedEffect(Unit) {
        refreshKeyboardOptions()?.let {
            cachedOptions = it
            flow.value = it
        }
    }
    return flow
}
