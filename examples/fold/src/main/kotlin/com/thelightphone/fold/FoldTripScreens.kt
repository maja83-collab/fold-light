package com.thelightphone.fold

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

@Composable
internal fun TripDayContent(
    trip: Trip,
    dayIndex: Int,
    onBack: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWeekly: () -> Unit,
    onOpenDayDetail: () -> Unit,
    onAddEvent: () -> Unit,
) {
    val days = groupEventsByDay(trip.events)
    val safeIndex = dayIndex.coerceIn(0, (days.size - 1).coerceAtLeast(0))
    val dayEntry = days.getOrNull(safeIndex)
    val dayKey = dayEntry?.first
    val dayEvents = dayEntry?.second.orEmpty()
    val swipeThresholdPx = with(LocalDensity.current) { 3f.gridUnitsAsDp().toPx() }

    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            leftButton = LightBarButton.LightIcon(
                icon = LightIcons.BACK,
                onClick = onBack,
                contentDescription = "Back",
            ),
            center = LightTopBarCenter.Text(trip.name, variant = FoldTypography.TripName),
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(days.size, safeIndex) {
                    if (days.size <= 1) return@pointerInput
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = {
                            when {
                                totalDrag <= -swipeThresholdPx -> onNextDay()
                                totalDrag >= swipeThresholdPx -> onPreviousDay()
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            totalDrag += dragAmount
                            change.consume()
                        },
                    )
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 1f.gridUnitsAsDp()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 0.5f.gridUnitsAsDp()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (safeIndex > 0) {
                    LightText(
                        text = "‹",
                        variant = FoldTypography.Title,
                        modifier = Modifier.lightClickable(onClick = onPreviousDay),
                    )
                } else {
                    LightText(text = "", variant = FoldTypography.Title)
                }

                LightText(
                    text = dayKey?.let(::formatDayTitle) ?: "No events yet",
                    variant = FoldTypography.Hero,
                    align = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )

                if (safeIndex < days.lastIndex) {
                    LightText(
                        text = "›",
                        variant = FoldTypography.Title,
                        modifier = Modifier.lightClickable(onClick = onNextDay),
                    )
                } else {
                    LightText(text = "", variant = FoldTypography.Title)
                }
            }

            if (dayEvents.isEmpty()) {
                LightText(
                    text = "No events for this day.",
                    variant = FoldTypography.Body,
                    lighten = true,
                    modifier = Modifier.padding(bottom = 0.75f.gridUnitsAsDp()),
                )
            } else {
                val countLabel = if (dayEvents.size == 1) "1 event" else "${dayEvents.size} events"
                LightText(
                    text = countLabel,
                    variant = FoldTypography.Body,
                    lighten = true,
                    modifier = Modifier.padding(bottom = 0.75f.gridUnitsAsDp()),
                )
                dayEvents.forEach { event ->
                    DayEventSummaryRow(event = event)
                }
            }
        }

        FoldBottomBar(
            items = listOf(
                foldSettingsButton(onClick = onOpenSettings),
                LightBarButton.Text(text = "THIS WEEK", onClick = onOpenWeekly),
                if (dayEvents.isNotEmpty()) {
                    LightBarButton.Text(text = "INFO", onClick = onOpenDayDetail)
                } else {
                    LightBarButton.Text(text = "ADD", onClick = onAddEvent)
                },
            ),
        )
    }
}

@Composable
internal fun TripWeeklyContent(
    trip: Trip,
    onBack: () -> Unit,
    onOpenDay: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val days = groupEventsByDay(trip.events)

    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            leftButton = LightBarButton.LightIcon(
                icon = LightIcons.BACK,
                onClick = onBack,
                contentDescription = "Back",
            ),
            center = LightTopBarCenter.Text(trip.name, variant = FoldTypography.TripName),
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )
        LightScrollView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 1f.gridUnitsAsDp()),
        ) {
            if (days.isEmpty()) {
                LightText(
                    text = "No events yet.",
                    variant = FoldTypography.Body,
                    lighten = true,
                    modifier = Modifier.padding(vertical = 0.75f.gridUnitsAsDp()),
                )
            } else {
                days.forEachIndexed { index, (dayKey, events) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 0.75f.gridUnitsAsDp())
                            .lightClickable { onOpenDay(index) },
                    ) {
                        LightText(text = formatDayTitle(dayKey), variant = FoldTypography.Title)
                        val summary = events.joinToString(" · ") { it.main }
                        LightText(
                            text = summary,
                            variant = FoldTypography.Body,
                            lighten = true,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp()),
                        )
                    }
                }
            }
        }
        FoldBottomBar(
            items = listOf(
                foldSettingsButton(onClick = onOpenSettings),
            ),
        )
    }
}

@Composable
internal fun TripDayDetailContent(
    trip: Trip,
    dayIndex: Int,
    onBack: () -> Unit,
    onCopyRequest: (String, String) -> Unit,
    onScanQr: (String) -> Unit,
    onPasteQr: (String) -> Unit,
    onViewQr: (String, Int) -> Unit,
    onEditEvent: (TripEvent) -> Unit,
    onDeleteEvent: (String) -> Unit,
) {
    val days = groupEventsByDay(trip.events)
    val safeIndex = dayIndex.coerceIn(0, (days.size - 1).coerceAtLeast(0))
    val dayEntry = days.getOrNull(safeIndex)
    val dayEvents = dayEntry?.second.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            leftButton = LightBarButton.LightIcon(
                icon = LightIcons.BACK,
                onClick = onBack,
                contentDescription = "Back",
            ),
            center = LightTopBarCenter.Text(dayEntry?.first?.let(::formatDayTitle) ?: "Day"),
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )
        LightScrollView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 1f.gridUnitsAsDp()),
        ) {
            if (dayEvents.isEmpty()) {
                LightText(
                    text = "No events for this day.",
                    variant = FoldTypography.Body,
                    lighten = true,
                    modifier = Modifier.padding(vertical = 0.75f.gridUnitsAsDp()),
                )
            } else {
                dayEvents.forEach { event ->
                    EventDetailBlock(
                        event = event,
                        onCopyRequest = onCopyRequest,
                        onScanQr = { onScanQr(event.id) },
                        onPasteQr = { onPasteQr(event.id) },
                        onViewQr = { index -> onViewQr(event.id, index) },
                        onEdit = { onEditEvent(event) },
                        onDelete = { onDeleteEvent(event.id) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun TripSettingsContent(
    trip: Trip,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onViewPack: () -> Unit,
    onDeleteTrip: () -> Unit,
    onAddEvent: () -> Unit,
) {
    val hasPackList = trip.whatToPack.isNotBlank()

    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            leftButton = LightBarButton.LightIcon(
                icon = LightIcons.BACK,
                onClick = onBack,
                contentDescription = "Back",
            ),
            center = LightTopBarCenter.Text("Settings"),
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )
        LightScrollView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 1f.gridUnitsAsDp()),
        ) {
            SettingsRow(label = "Trip", value = trip.name, onClick = onRename)
            SettingsRow(
                label = "What to pack",
                value = if (hasPackList) "Tap to view" else "Nothing listed",
                onClick = onViewPack,
            )
            Spacer(modifier = Modifier.height(3f.gridUnitsAsDp()))
            LightText(
                text = "Delete trip",
                variant = FoldTypography.Body,
                lighten = true,
                modifier = Modifier
                    .padding(bottom = 0.75f.gridUnitsAsDp())
                    .lightClickable(onClick = onDeleteTrip),
            )
        }
        FoldBottomBar(
            items = listOf(
                LightBarButton.Text(text = "ADD EVENT", onClick = onAddEvent),
            ),
        )
    }
}

@Composable
internal fun PackListContent(
    packList: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            leftButton = LightBarButton.LightIcon(
                icon = LightIcons.BACK,
                onClick = onBack,
                contentDescription = "Back",
            ),
            center = LightTopBarCenter.Text("What to pack"),
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )
        LightScrollView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 1f.gridUnitsAsDp()),
        ) {
            LightText(
                text = packList.ifBlank { "Nothing listed yet." },
                variant = FoldTypography.Body,
                lighten = packList.isBlank(),
                modifier = Modifier.padding(vertical = 0.5f.gridUnitsAsDp()),
            )
        }
        FoldBottomBar(
            items = listOf(
                LightBarButton.Text(text = "EDIT", onClick = onEdit),
            ),
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.75f.gridUnitsAsDp())
            .lightClickable(onClick = onClick),
    ) {
        LightText(text = label, variant = FoldTypography.Sub, lighten = true)
        LightText(
            text = value,
            variant = FoldTypography.SettingsValue,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp()),
        )
    }
}

@Composable
private fun DayEventSummaryRow(event: TripEvent) {
    LightText(
        text = event.main,
        variant = FoldTypography.EventTitle,
        bold = true,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 0.5f.gridUnitsAsDp()),
    )
}

@Composable
private fun EventDetailBlock(
    event: TripEvent,
    onCopyRequest: (String, String) -> Unit,
    onScanQr: () -> Unit,
    onPasteQr: () -> Unit,
    onViewQr: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp())) {
        LightText(
            text = event.main,
            variant = FoldTypography.EventTitle,
            bold = true,
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )
        EventDetailBody(
            event = event,
            onCopyRequest = onCopyRequest,
            onScanQr = onScanQr,
            onPasteQr = onPasteQr,
            onViewQr = onViewQr,
            onEdit = onEdit,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun EventDetailBody(
    event: TripEvent,
    onCopyRequest: (String, String) -> Unit,
    onScanQr: () -> Unit,
    onPasteQr: () -> Unit,
    onViewQr: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp())) {
        if (event.datetime.isNotBlank()) {
            EventSubLine(displayDateTime(event.datetime)) {
                onCopyRequest("Time", event.datetime)
            }
        }
        val route = listOf(event.from, event.to).filter { it.isNotBlank() }.joinToString(" → ")
        if (route.isNotBlank()) {
            EventSubLine(route) { onCopyRequest("Route", route) }
        }
        if (event.address.isNotBlank()) {
            EventSubLine(event.address) { onCopyRequest("Address", event.address) }
        }
        if (event.workingHours.isNotBlank()) {
            EventSubLine(event.workingHours) { onCopyRequest("Hours", event.workingHours) }
        }
        if (event.notes.isNotBlank()) {
            LightText(
                text = event.notes,
                variant = FoldTypography.Detail,
                lighten = true,
                modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp()),
            )
        }
        EventActionsRow(
            qrCount = event.qrCodes.size,
            onScan = onScanQr,
            onPaste = onPasteQr,
            onView = onViewQr,
            onEdit = onEdit,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun EventActionChip(
    text: String,
    lighten: Boolean,
    onClick: () -> Unit,
) {
    LightText(
        text = text,
        variant = FoldTypography.Action,
        lighten = lighten,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(vertical = 0.35f.gridUnitsAsDp())
            .lightClickable(onClick = onClick),
    )
}

@Composable
private fun EventActionsRow(
    qrCount: Int,
    onScan: () -> Unit,
    onPaste: () -> Unit,
    onView: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.5f.gridUnitsAsDp()),
        horizontalArrangement = Arrangement.spacedBy(0.5f.gridUnitsAsDp()),
    ) {
        repeat(qrCount) { index ->
            EventActionChip(
                text = if (qrCount == 1) "QR" else "QR ${index + 1}",
                lighten = false,
                onClick = { onView(index) },
            )
        }
        if (qrCount < 3) {
            EventActionChip(text = "Scan", lighten = false, onClick = onScan)
            EventActionChip(text = "Paste", lighten = true, onClick = onPaste)
        }
        EventActionChip(text = "Edit", lighten = false, onClick = onEdit)
        EventActionChip(text = "Delete", lighten = true, onClick = onDelete)
    }
}

@Composable
private fun EventSubLine(
    text: String,
    onCopyRequest: () -> Unit,
) {
    LightText(
        text = text,
        variant = FoldTypography.Detail,
        lighten = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.2f.gridUnitsAsDp(), bottom = 0.1f.gridUnitsAsDp())
            .lightClickable(onClick = onCopyRequest),
    )
}

private fun displayDateTime(datetime: String): String = formatDateTimeDisplay(datetime)
