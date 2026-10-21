package com.thelightphone.fold

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightBottomBarItem
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTextVariant

internal const val FOLD_BOTTOM_ICON_SIZE_UNITS = 1.5f

internal fun foldSettingsButton(onClick: () -> Unit): LightBarButton.LightIcon =
    LightBarButton.LightIcon(
        icon = LightIcons.SETTINGS,
        onClick = onClick,
        contentDescription = "Settings",
        sizeUnits = FOLD_BOTTOM_ICON_SIZE_UNITS,
    )

@Composable
internal fun FoldBottomBar(
    items: List<LightBottomBarItem?>,
    modifier: Modifier = Modifier,
) {
    LightBottomBar(
        items = items,
        modifier = modifier,
        textVariant = LightTextVariant.Detail,
    )
}
