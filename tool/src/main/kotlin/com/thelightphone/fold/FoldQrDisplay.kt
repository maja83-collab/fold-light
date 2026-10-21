package com.thelightphone.fold

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.gridUnitsAsDp
import `in`.procyk.qrcodegen.QrCode

@Composable
internal fun FoldQrDisplay(
    payload: String,
    modifier: Modifier = Modifier,
) {
    val qr = remember(payload) {
        runCatching {
            QrCode.encodeText(payload, QrCode.Ecc.MEDIUM)
        }.getOrNull()
    }

    if (qr == null) {
        LightText(
            text = "Could not render this ticket QR.",
            variant = LightTextVariant.Copy,
            modifier = modifier.padding(1f.gridUnitsAsDp()),
        )
        return
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.White)
            .padding(1f.gridUnitsAsDp()),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val modules = qr.size
            val quiet = 2
            val total = modules + quiet * 2
            val modulePx = size.minDimension / total

            drawRect(Color.White, size = Size(size.width, size.height))

            for (y in 0 until modules) {
                for (x in 0 until modules) {
                    if (!qr.getModule(x, y)) continue
                    val left = (x + quiet) * modulePx
                    val top = (y + quiet) * modulePx
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(left, top),
                        size = Size(modulePx, modulePx),
                    )
                }
            }
        }
    }
}
