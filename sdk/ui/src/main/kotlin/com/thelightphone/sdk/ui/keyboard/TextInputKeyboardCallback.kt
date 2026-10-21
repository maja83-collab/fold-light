package com.thelightphone.sdk.ui.keyboard

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextRange
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback

/**
 * Mutable target for [TextInputKeyboardCallback] so the keyboard ViewModel can keep a
 * stable callback instance while the active [TextFieldState] changes between fields.
 */
@Stable
internal class TextInputTarget {
    @Volatile
    var state: TextFieldState? = null

    @Volatile
    var singleLine: Boolean = false

    @Volatile
    var onReturn: () -> Unit = {}

    @Volatile
    var onStateChanged: () -> Unit = {}
}

/**
 * LP3 keyboard may skip [onKeyReleased] when a long-press job was scheduled.
 * Insert on press so every tap registers; repeats still use [onKeyRepeated].
 *
 * Reads the active [TextInputTarget] on every key event so letters keep going to
 * the current field (hotel name → address → notes, etc.).
 */
internal class TextInputKeyboardCallback(
    private val target: TextInputTarget,
) : Lp3RepeatableKeyboardCallback {

    override fun onKeyPressed(code: Int) {
        insertCodePoint(code)
    }

    override fun onSpecialKeyPressed(key: SpecialKey) {
        if (key == SpecialKey.Space) insertAtCursor(" ")
    }

    override fun onKeyReleased(code: Int) = Unit

    override fun onSpecialKeyReleased(key: SpecialKey) {
        val state = target.state ?: return
        when (key) {
            SpecialKey.Backspace -> {
                val before = state.text.subSequence(0, state.selection.min)
                deleteBeforeCursor(surrogateAwareDeleteCount(before, 1))
            }
            SpecialKey.Return -> if (target.singleLine) target.onReturn() else insertAtCursor("\n")
            else -> Unit
        }
    }

    override fun onKeyLongPressed(code: Int) = Unit

    override fun onSpecialKeyLongPressed(key: SpecialKey) {
        val state = target.state ?: return
        if (key == SpecialKey.Backspace) {
            val before = state.text.subSequence(0, state.selection.min)
            deleteBeforeCursor(deleteWordCount(before))
        }
    }

    override fun onKeyRepeated(code: Int) {
        insertCodePoint(code)
    }

    override fun onSpecialKeyRepeated(specialKey: SpecialKey) {
        if (specialKey == SpecialKey.Space) insertAtCursor(" ")
    }

    override fun onSubmitWord(word: CharSequence) {
        insertAtCursor(word.toString())
    }

    private fun insertCodePoint(code: Int) {
        if (!Character.isValidCodePoint(code)) return
        insertAtCursor(buildString { appendCodePoint(code) })
    }

    private fun insertAtCursor(text: String) {
        val state = target.state ?: return
        state.edit {
            val start = selection.min
            val end = selection.max
            replace(start, end, text)
            selection = TextRange(start + text.length)
        }
        target.onStateChanged()
    }

    private fun deleteBeforeCursor(count: Int) {
        if (count <= 0) return
        val state = target.state ?: return
        state.edit {
            val end = selection.min
            if (end == 0) return@edit
            val start = (end - count).coerceAtLeast(0)
            delete(start, end)
            selection = TextRange(start)
        }
        target.onStateChanged()
    }
}

private fun surrogateAwareDeleteCount(value: CharSequence, defaultCount: Int): Int {
    if (value.isEmpty()) return 0
    val last = value[value.length - 1]
    return if (Character.isLowSurrogate(last)) 2 else defaultCount
}

private fun deleteWordCount(value: CharSequence): Int {
    val trimmed = value.trimEnd()
    val lastSpace = trimmed.indexOfLast { it.isWhitespace() }
    return value.length - if (lastSpace >= 0) lastSpace + 1 else 0
}
