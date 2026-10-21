package com.thelightphone.sdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.LayoutOptions
import com.thelightphone.lp3Keyboard.ui.viewmodel.EnQwertyLp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback
import com.thelightphone.lp3Keyboard.ui.viewmodel.defaultEmojis
import com.thelightphone.sdk.ui.keyboard.LightEmbeddedLp3Keyboard
import com.thelightphone.sdk.ui.keyboard.TextInputKeyboardCallback
import com.thelightphone.sdk.ui.keyboard.TextInputTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun LightTextInputEditor(
    title: String,
    state: TextFieldState,
    onSubmit: (CharSequence) -> Unit,
    onBack: () -> Unit,
    keyboardOptionsFlow: StateFlow<KeyboardOptions>,
    modifier: Modifier = Modifier,
    submitLabel: String = "SAVE",
    submitIcon: LightIconConfiguration? = null,
    showBackButton: Boolean = true,
    singleLine: Boolean = false,
    editorKey: Any = title,
) {
    val currentOnSubmit by rememberUpdatedState(onSubmit)
    var textRevision by remember(editorKey) { mutableIntStateOf(0) }

    // Stable target + callback for the lifetime of this editorKey. The ViewModel keeps the
    // callback; we retarget [state] every recomposition so later fields (e.g. address)
    // still receive keystrokes.
    val inputTarget = remember(editorKey) { TextInputTarget() }
    inputTarget.state = state
    inputTarget.singleLine = singleLine
    inputTarget.onReturn = { currentOnSubmit(state.text) }
    inputTarget.onStateChanged = { textRevision++ }

    val keyboardCallback = remember(editorKey) {
        TextInputKeyboardCallback(inputTarget)
    }

    val keyboardViewModel: Lp3KeyboardViewModel<Nothing> = viewModel<EnQwertyLp3KeyboardViewModel<Nothing>>(
        key = "LightTextInputEditor-$editorKey",
        factory = factory(keyboardCallback, keyboardOptionsFlow),
    )

    LightTextInputEditor(
        title = title,
        state = state,
        onSubmit = onSubmit,
        onBack = onBack,
        viewModel = keyboardViewModel,
        modifier = modifier,
        submitLabel = submitLabel,
        submitIcon = submitIcon,
        showBackButton = showBackButton,
        singleLine = singleLine,
        textRevision = textRevision,
    )
}

/**
 * Full-screen text entry matching LightOS `DisplayWithKeyboardPortrait`
 */
@Composable
fun LightTextInputEditor(
    title: String,
    state: TextFieldState,
    onSubmit: (CharSequence) -> Unit,
    onBack: () -> Unit,
    viewModel: Lp3KeyboardViewModel<Nothing>,
    modifier: Modifier = Modifier,
    submitLabel: String = "SAVE",
    submitIcon: LightIconConfiguration? = null,
    showBackButton: Boolean = true,
    singleLine: Boolean = false,
    textRevision: Int = 0,
) {
    val colors = LightThemeTokens.colors
    val inputStyle = lightInputTextStyle()
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scrollState = rememberScrollState()
    val displayText = state.text.toString()
    val cursorPos = state.selection.min

    LaunchedEffect(textRevision) {
        // Ensure display refreshes when the LP3 keyboard mutates TextFieldState.
    }

    Surface {
        Column(modifier = modifier.fillMaxSize()) {
            LightTopBar(
                leftButton = if (showBackButton) {
                    LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = onBack,
                    )
                } else {
                    null
                },
                center = LightTopBarCenter.Text(title),
                modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 2f.gridUnitsAsDp())
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            textLayout?.let { layout ->
                                state.edit {
                                    selection = TextRange(layout.getOffsetForPosition(down.position))
                                }
                            }
                            drag(down.id) { change ->
                                textLayout?.let { layout ->
                                    state.edit {
                                        selection = TextRange(layout.getOffsetForPosition(change.position))
                                    }
                                }
                                change.consume()
                            }
                        }
                    },
            ) {
                val density = LocalDensity.current
                val viewportHeightPx = with(density) { maxHeight.roundToPx() }
                val scrollMarginPx = with(density) { 1.5f.gridUnitsAsDp().roundToPx() }
                val textBottomPadding = if (singleLine) Modifier else Modifier.padding(bottom = 8f.gridUnitsAsDp())

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (singleLine) Modifier else Modifier.verticalScroll(scrollState)),
                    contentAlignment = Alignment.TopStart,
                ) {
                    BasicText(
                        text = displayText,
                        style = inputStyle,
                        onTextLayout = { textLayout = it },
                        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                        softWrap = !singleLine,
                        overflow = if (singleLine) TextOverflow.StartEllipsis else TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(textBottomPadding),
                    )
                }

                textLayout?.let { layout ->
                    val safeCursor = cursorPos.coerceIn(0, layout.layoutInput.text.length)
                    val rect = layout.getCursorRect(safeCursor)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(rect.left.toInt(), rect.top.toInt()) }
                            .width(2.dp)
                            .height(with(density) { rect.height.toDp() })
                            .background(colors.content),
                    )
                }

                if (!singleLine) {
                    LaunchedEffect(displayText, cursorPos, textLayout, viewportHeightPx, scrollMarginPx) {
                        val currentLayout = textLayout ?: return@LaunchedEffect
                        val safeCursor = cursorPos.coerceIn(0, currentLayout.layoutInput.text.length)
                        val rect = currentLayout.getCursorRect(safeCursor)
                        val target = (rect.bottom + scrollMarginPx - viewportHeightPx * 0.85f)
                            .coerceAtLeast(0f)
                            .toInt()
                        scrollState.scrollTo(target)
                    }
                }
            }

            // LP3 keyboard is fixed 164dp; bottom chrome matches LightBottomBar (4 + 1 grid).
            // Do not pass PC/emulator hardware keys — they fight on-screen taps.
            LightEmbeddedLp3Keyboard(
                viewModel = viewModel,
                additionalBottomHeight = 5f.gridUnitsAsDp(),
                handleHardwareKeyboardInput = false,
                bottomBar = {
                    LightBottomBar(
                        items = listOf(
                            when (val icon = submitIcon) {
                                null -> LightBarButton.Text(
                                    text = submitLabel,
                                    onClick = { onSubmit(state.text) },
                                )
                                else -> LightBarButton.LightIcon(
                                    icon = icon,
                                    onClick = { onSubmit(state.text) },
                                    contentDescription = submitLabel,
                                )
                            },
                        ),
                    )
                },
            )
        }
    }
}

private fun factory(
    callback: Lp3RepeatableKeyboardCallback,
    keyboardOptionsFlow: StateFlow<KeyboardOptions>,
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EnQwertyLp3KeyboardViewModel<Nothing>(
                passedCallback = callback,
                keyboardOptionsFlow = keyboardOptionsFlow,
                optionsForLayout = {
                    LayoutOptions(displayCloseButton = !it.isRootLayout)
                },
            ) as T
        }
    }

@Composable
private fun lightInputTextStyle(): TextStyle {
    val colors = LightThemeTokens.colors
    val t = LightThemeTokens.typography
    return t.fine
        .copy(
            color = colors.content,
            textDecoration = TextDecoration.Underline,
        )
        .scaledForScreenHeight()
}

@Preview(widthDp = 1080 / 3, heightDp = 1240 / 3, showBackground = true)
@Composable
private fun PreviewLightTextInputEditorDark() {
    val state = rememberTextFieldState("hi")
    LightTheme(colors = LightThemeColors.Dark) {
        LightTextInputEditor(
            title = "Name",
            state = state,
            keyboardOptionsFlow = MutableStateFlow(defaultKeyboardOptions()),
            onSubmit = {},
            onBack = {},
        )
    }
}

fun defaultKeyboardOptions() = KeyboardOptions(
    defaultEmojis,
    displayReturn = true,
    displayVoice = false,
    enableKeyAnimation = true,
    swipeEnabled = false,
)
