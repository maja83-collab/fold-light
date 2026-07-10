package com.thelightphone.fold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightQrCodeScanner
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

// Original FOLD uses ~13–15px body and ~12px secondary; aligned with Weather (Copy for body).
private const val APP_DISPLAY_NAME = "Fold Light"

@InitialScreen
internal class FoldHomeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, FoldViewModel>(sealedActivity) {

    override val viewModelClass = FoldViewModel::class.java

    override fun createViewModel(): FoldViewModel = foldViewModelFactory(lightContext.dataStore)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()
        val keyboardOptionsFlow = rememberKeyboardOptions()

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                when (val mode = state.mode) {
                    is FoldMode.TextInput -> {
                        val textFieldState = key(state.textInputSession, mode.kind) {
                            rememberTextFieldState(
                                viewModel.textInputInitialValue(
                                    mode.kind,
                                    state.trips,
                                    state.eventDraft,
                                ),
                            )
                        }
                        LightTextInputEditor(
                            title = viewModel.textInputTitle(mode.kind),
                            editorKey = "${state.textInputSession}-${mode.kind}",
                            keyboardOptionsFlow = keyboardOptionsFlow,
                            state = textFieldState,
                            onSubmit = viewModel::submitTextInput,
                            onBack = viewModel::cancelTextInput,
                            submitLabel = "SAVE",
                            showBackButton = true,
                            singleLine = !viewModel.textInputMultiline(mode.kind),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    is FoldMode.PasteQr -> {
                        val textFieldState = key(state.textInputSession) {
                            rememberTextFieldState("")
                        }
                        LightTextInputEditor(
                            title = "Paste ticket",
                            editorKey = state.textInputSession,
                            keyboardOptionsFlow = keyboardOptionsFlow,
                            state = textFieldState,
                            onSubmit = { value ->
                                viewModel.submitPasteQr(mode.tripId, mode.eventId, value)
                            },
                            onBack = { viewModel.cancelPasteQr(mode.tripId, mode.eventId) },
                            submitLabel = "SAVE",
                            showBackButton = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    FoldMode.Loading -> LoadingContent()
                    FoldMode.TripList -> TripListContent(
                        trips = state.trips,
                        onOpenTrip = viewModel::openTrip,
                        onNewTrip = viewModel::startNewTrip,
                    )
                    is FoldMode.TripDetail -> {
                        val trip = state.trips.firstOrNull { it.id == mode.tripId }
                        if (trip == null) {
                            LaunchedEffect(mode.tripId) { viewModel.backToTripList() }
                        } else {
                            TripDayContent(
                                trip = trip,
                                dayIndex = state.selectedDayIndex,
                                onBack = viewModel::backToTripList,
                                onPreviousDay = { viewModel.previousDay(trip.id) },
                                onNextDay = { viewModel.nextDay(trip.id) },
                                onOpenSettings = { viewModel.openSettings(trip.id) },
                                onOpenWeekly = { viewModel.openWeekly(trip.id) },
                                onOpenDayDetail = { viewModel.openDayDetail(trip.id) },
                                onAddEvent = { viewModel.startAddEvent(trip.id) },
                            )
                        }
                    }
                    is FoldMode.TripWeekly -> {
                        val trip = state.trips.firstOrNull { it.id == mode.tripId }
                        if (trip == null) {
                            LaunchedEffect(mode.tripId) { viewModel.backToTripList() }
                        } else {
                            TripWeeklyContent(
                                trip = trip,
                                onBack = { viewModel.backToTripDay(trip.id) },
                                onOpenDay = { index -> viewModel.selectDay(trip.id, index) },
                                onOpenSettings = { viewModel.openSettings(trip.id) },
                            )
                        }
                    }
                    is FoldMode.TripDayDetail -> {
                        val trip = state.trips.firstOrNull { it.id == mode.tripId }
                        if (trip == null) {
                            LaunchedEffect(mode.tripId) { viewModel.backToTripList() }
                        } else {
                            TripDayDetailContent(
                                trip = trip,
                                dayIndex = state.selectedDayIndex,
                                onBack = { viewModel.backToTripDay(trip.id) },
                                onCopyRequest = viewModel::openCopySheet,
                                onScanQr = { eventId -> viewModel.startScanQr(trip.id, eventId) },
                                onPasteQr = { eventId -> viewModel.startPasteQr(trip.id, eventId) },
                                onViewQr = { eventId, index -> viewModel.openViewQr(trip.id, eventId, index) },
                                onEditEvent = { event -> viewModel.startEditEvent(trip.id, event) },
                                onDeleteEvent = { eventId -> viewModel.confirmDeleteEvent(trip.id, eventId) },
                            )
                        }
                    }
                    is FoldMode.TripSettings -> {
                        val trip = state.trips.firstOrNull { it.id == mode.tripId }
                        if (trip == null) {
                            LaunchedEffect(mode.tripId) { viewModel.backToTripList() }
                        } else {
                            TripSettingsContent(
                                trip = trip,
                                onBack = { viewModel.backToTripDay(trip.id) },
                                onRename = { viewModel.startRenameTrip(trip.id) },
                                onViewPack = { viewModel.openPackList(trip.id) },
                                onDeleteTrip = { viewModel.confirmDeleteTrip(trip.id, fromSettings = true) },
                                onAddEvent = { viewModel.startAddEvent(trip.id) },
                            )
                        }
                    }
                    is FoldMode.ViewPack -> {
                        val trip = state.trips.firstOrNull { it.id == mode.tripId }
                        if (trip == null) {
                            LaunchedEffect(mode.tripId) { viewModel.backToTripList() }
                        } else {
                            PackListContent(
                                packList = trip.whatToPack,
                                onBack = { viewModel.closePackList(trip.id) },
                                onEdit = { viewModel.startEditPack(trip.id, returnToView = true) },
                            )
                        }
                    }
                    is FoldMode.PickEventType -> PickEventTypeContent(
                        onPick = { type -> viewModel.pickEventType(type, mode.tripId) },
                    )
                    is FoldMode.ConfirmDeleteTrip -> ConfirmModal(
                        title = "Delete trip?",
                        message = "This removes the trip from this phone.",
                        onCancel = { viewModel.cancelDeleteTrip(mode.tripId, mode.fromList, mode.fromSettings) },
                        onConfirm = { viewModel.deleteTrip(mode.tripId) },
                    )
                    is FoldMode.ConfirmDeleteEvent -> ConfirmModal(
                        title = "Delete event?",
                        message = "This cannot be undone.",
                        onCancel = { viewModel.cancelDeleteEvent(mode.tripId) },
                        onConfirm = { viewModel.deleteEvent(mode.tripId, mode.eventId) },
                    )
                    is FoldMode.ScanQr -> ScanQrContent(
                        onScanned = { value -> viewModel.appendQrCode(mode.tripId, mode.eventId, value) },
                        onBack = { viewModel.cancelScanQr(mode.tripId, mode.eventId) },
                        onPasteInstead = { viewModel.startPasteQr(mode.tripId, mode.eventId) },
                    )
                    is FoldMode.ViewQr -> {
                        val payload = viewModel.qrPayload(mode.tripId, mode.eventId, mode.index).orEmpty()
                        ViewQrContent(
                            label = if (payload.length > 40) "Ticket ${mode.index + 1}" else payload,
                            payload = payload,
                            onBack = { viewModel.closeViewQr(mode.tripId, mode.eventId) },
                            onDelete = {
                                viewModel.removeQrCode(mode.tripId, mode.eventId, mode.index)
                            },
                        )
                    }
                }

                state.copySheet?.let { sheet ->
                    CopySheet(
                        label = sheet.label,
                        text = sheet.text,
                        onCopy = { viewModel.markCopied() },
                        onClose = viewModel::closeCopySheet,
                        copyText = sheet.text,
                    )
                }

                state.copiedMessage?.let { message ->
                    CopiedToast(message = message, onDismiss = viewModel::dismissCopied)
                }

                state.errorModal?.let { message ->
                    LightFullscreenModal(
                        message = message,
                        onClose = viewModel::dismissError,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 1f.gridUnitsAsDp()),
    ) {
        LightTopBar(center = LightTopBarCenter.Text(APP_DISPLAY_NAME), modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()))
        LightText(
            text = "Loading…",
            variant = FoldTypography.Body,
            modifier = Modifier.padding(top = 1f.gridUnitsAsDp()),
        )
    }
}

@Composable
private fun TripListContent(
    trips: List<Trip>,
    onOpenTrip: (String) -> Unit,
    onNewTrip: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(center = LightTopBarCenter.Text(APP_DISPLAY_NAME), modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()))
        LightScrollView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 1f.gridUnitsAsDp()),
        ) {
            if (trips.isEmpty()) {
                LightText(
                    text = "No trips yet. Create one for your next journey.",
                    variant = FoldTypography.Body,
                    lighten = true,
                    modifier = Modifier.padding(vertical = 0.75f.gridUnitsAsDp()),
                )
            } else {
                trips.forEach { trip ->
                    LightText(
                        text = trip.name,
                        variant = FoldTypography.TripNameList,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 0.75f.gridUnitsAsDp())
                            .lightClickable { onOpenTrip(trip.id) },
                    )
                }
            }
        }
        FoldBottomBar(
            items = listOf(
                LightBarButton.Text(text = "NEW TRIP", onClick = onNewTrip),
            ),
        )
    }
}

@Composable
private fun ScanQrContent(
    onScanned: (String) -> Unit,
    onBack: () -> Unit,
    onPasteInstead: () -> Unit,
) {
    var pendingScan by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LightQrCodeScanner(
            title = "Scan ticket",
            onScanned = { pendingScan = it },
            onBack = onBack,
            modifier = Modifier
                .fillMaxSize()
                .background(LightThemeTokens.colors.background),
        )

        FoldBottomBar(
            items = listOf(
                LightBarButton.Text(text = "PASTE", onClick = onPasteInstead),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    LaunchedEffect(pendingScan) {
        val value = pendingScan ?: return@LaunchedEffect
        onScanned(value)
        pendingScan = null
    }
}

@Composable
private fun ViewQrContent(
    label: String,
    payload: String,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LightTopBar(
                leftButton = LightBarButton.LightIcon(
                    icon = LightIcons.BACK,
                    onClick = onBack,
                    contentDescription = "Back",
                ),
                center = LightTopBarCenter.Text("Ticket"),
                modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1.5f.gridUnitsAsDp()),
                ) {
                    if (label.isNotBlank()) {
                        LightText(
                            text = label,
                            variant = FoldTypography.Body,
                            align = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 0.5f.gridUnitsAsDp()),
                        )
                    }
                    FoldQrDisplay(
                        payload = payload,
                        modifier = Modifier.fillMaxWidth(0.82f),
                    )
                }
            }
            FoldBottomBar(
                items = listOf(
                    LightBarButton.Text(text = "DELETE", onClick = { showDeleteConfirm = true }),
                ),
            )
        }

        if (showDeleteConfirm) {
            ConfirmModal(
                title = "Delete this ticket?",
                message = "The QR code will be removed from this event.",
                onCancel = { showDeleteConfirm = false },
                onConfirm = {
                    showDeleteConfirm = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun PickEventTypeContent(onPick: (EventType) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(center = LightTopBarCenter.Text("Add event"), modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()))
        LightScrollView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 1f.gridUnitsAsDp()),
        ) {
            EventType.entries.forEach { type ->
                LightText(
                    text = type.label(),
                    variant = FoldTypography.Body,
                    modifier = Modifier
                        .fillMaxWidth()
                        .lightClickable { onPick(type) }
                        .padding(vertical = 0.75f.gridUnitsAsDp()),
                )
            }
        }
    }
}

@Composable
private fun CopySheet(
    label: String,
    text: String,
    copyText: String,
    onCopy: () -> Unit,
    onClose: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightThemeTokens.colors.background),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 1f.gridUnitsAsDp()),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LightText(text = label.uppercase(), variant = FoldTypography.Sub)
                LightText(
                    text = text,
                    variant = FoldTypography.Body,
                    align = TextAlign.Center,
                    modifier = Modifier.padding(top = 0.75f.gridUnitsAsDp()),
                )
                LightText(
                    text = "Tap COPY to paste this elsewhere.",
                    variant = FoldTypography.Body,
                    lighten = true,
                    align = TextAlign.Center,
                    modifier = Modifier.padding(top = 0.75f.gridUnitsAsDp()),
                )
            }
        }
        FoldBottomBar(
            items = listOf(
                LightBarButton.LightIcon(
                    icon = LightIcons.CLOSE,
                    onClick = onClose,
                    contentDescription = "Close",
                ),
                LightBarButton.Text(
                    text = "COPY",
                    onClick = {
                        clipboard.setText(AnnotatedString(copyText.trim()))
                        onCopy()
                    },
                ),
            ),
        )
    }
}

@Composable
private fun CopiedToast(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(1200)
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightThemeTokens.colors.background.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        LightText(text = message, variant = FoldTypography.Body)
    }
}

@Composable
private fun ConfirmModal(
    title: String,
    message: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightThemeTokens.colors.background),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 1f.gridUnitsAsDp()),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                LightText(text = title, variant = FoldTypography.Title)
                LightText(
                    text = message,
                    variant = FoldTypography.Body,
                    lighten = true,
                    modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                )
            }
        }
        FoldBottomBar(
            items = listOf(
                LightBarButton.Text(text = "CANCEL", onClick = onCancel),
                LightBarButton.Text(text = "DELETE", onClick = onConfirm),
            ),
        )
    }
}
