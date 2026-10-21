package com.thelightphone.fold

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed class FoldMode {
    data object Loading : FoldMode()
    data object TripList : FoldMode()
    data class TripDetail(val tripId: String) : FoldMode()
    data class TripWeekly(val tripId: String) : FoldMode()
    data class TripDayDetail(val tripId: String) : FoldMode()
    data class TripSettings(val tripId: String) : FoldMode()
    data class TextInput(val kind: TextInputKind) : FoldMode()
    data class PickEventType(val tripId: String) : FoldMode()
    /** All event fields on one screen; tap a row to edit, then SAVE once. */
    data object EventForm : FoldMode()
    data class ConfirmDeleteTrip(val tripId: String, val fromList: Boolean = false, val fromSettings: Boolean = false) : FoldMode()
    data class ConfirmDeleteEvent(val tripId: String, val eventId: String) : FoldMode()
    data class ScanQr(val tripId: String, val eventId: String) : FoldMode()
    data class PasteQr(val tripId: String, val eventId: String) : FoldMode()
    data class ViewPack(val tripId: String) : FoldMode()
    data class ViewQr(val tripId: String, val eventId: String, val index: Int) : FoldMode()
}

internal sealed class TextInputKind {
    data object NewTrip : TextInputKind()
    data class RenameTrip(val tripId: String, val fromList: Boolean = false) : TextInputKind()
    data class PackList(val tripId: String, val returnToView: Boolean = false) : TextInputKind()
    data class EventField(val field: EventFormField) : TextInputKind()
}

internal enum class EventFormField {
    MAIN,
    DATETIME,
    CHECK_OUT,
    RETURN_DATETIME,
    FROM,
    TO,
    ADDRESS,
    WORKING_HOURS,
    NOTES,
}

internal data class EventFormDraft(
    val tripId: String,
    val type: EventType,
    val editingEventId: String? = null,
    val main: String = "",
    val datetime: String = "",
    val checkOutDatetime: String = "",
    val returnDatetime: String = "",
    val from: String = "",
    val to: String = "",
    val address: String = "",
    val workingHours: String = "",
    val notes: String = "",
    val nextField: EventFormField = EventFormField.MAIN,
)

internal data class CopySheetState(
    val label: String,
    val text: String,
)

internal data class FoldUiState(
    val mode: FoldMode = FoldMode.Loading,
    val trips: List<Trip> = emptyList(),
    val selectedDayIndex: Int = 0,
    val textInputSession: Int = 0,
    val formDraft: EventFormField? = null,
    val eventDraft: EventFormDraft? = null,
    val copySheet: CopySheetState? = null,
    val copiedMessage: String? = null,
    val errorModal: String? = null,
)

internal class FoldViewModel(
    private val repository: TripRepository,
) : LightViewModel<Unit>() {

    private val _uiState = MutableStateFlow(FoldUiState())
    val uiState: StateFlow<FoldUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.load() }
                .onSuccess { store ->
                    withContext(Dispatchers.Main) {
                        _uiState.value = FoldUiState(
                            mode = FoldMode.TripList,
                            trips = store.trips,
                        )
                    }
                }
                .onFailure {
                    withContext(Dispatchers.Main) {
                        _uiState.value = FoldUiState(
                            mode = FoldMode.TripList,
                            errorModal = "Could not load trips.",
                        )
                    }
                }
        }
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
    }

    private fun persist(trips: List<Trip>, transform: (FoldUiState) -> FoldUiState) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.save(TripStore(trips)) }
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        _uiState.update { transform(it.copy(trips = trips)) }
                    }
                }
                .onFailure {
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(errorModal = "Could not save.") }
                    }
                }
        }
    }

    fun openTrip(tripId: String) {
        val trip = _uiState.value.trips.firstOrNull { it.id == tripId }
        _uiState.update {
            it.copy(
                mode = FoldMode.TripDetail(tripId),
                selectedDayIndex = trip?.let { t -> defaultDayIndex(t.events) } ?: 0,
            )
        }
    }

    fun backToTripList() {
        _uiState.update {
            it.copy(
                mode = FoldMode.TripList,
                selectedDayIndex = 0,
                eventDraft = null,
            )
        }
    }

    fun openWeekly(tripId: String) {
        _uiState.update { it.copy(mode = FoldMode.TripWeekly(tripId)) }
    }

    fun openDayDetail(tripId: String) {
        _uiState.update { it.copy(mode = FoldMode.TripDayDetail(tripId)) }
    }

    fun openSettings(tripId: String) {
        _uiState.update { it.copy(mode = FoldMode.TripSettings(tripId)) }
    }

    fun backToTripDay(tripId: String) {
        _uiState.update { it.copy(mode = FoldMode.TripDetail(tripId)) }
    }

    fun previousDay(tripId: String) {
        val trip = _uiState.value.trips.firstOrNull { it.id == tripId } ?: return
        val dayCount = groupEventsByDay(trip.events).size
        if (dayCount == 0) return
        _uiState.update {
            it.copy(
                selectedDayIndex = (it.selectedDayIndex - 1).coerceAtLeast(0),
            )
        }
    }

    fun nextDay(tripId: String) {
        val trip = _uiState.value.trips.firstOrNull { it.id == tripId } ?: return
        val dayCount = groupEventsByDay(trip.events).size
        if (dayCount == 0) return
        _uiState.update {
            it.copy(
                selectedDayIndex = (it.selectedDayIndex + 1).coerceAtMost(dayCount - 1),
            )
        }
    }

    fun selectDay(tripId: String, dayIndex: Int) {
        _uiState.update {
            it.copy(
                mode = FoldMode.TripDetail(tripId),
                selectedDayIndex = dayIndex.coerceAtLeast(0),
            )
        }
    }

    fun startNewTrip() {
        _uiState.update {
            it.copy(
                mode = FoldMode.TextInput(TextInputKind.NewTrip),
                textInputSession = it.textInputSession + 1,
                errorModal = null,
            )
        }
    }

    fun startRenameTrip(tripId: String, fromList: Boolean = false) {
        _uiState.update {
            it.copy(
                mode = FoldMode.TextInput(TextInputKind.RenameTrip(tripId, fromList)),
                textInputSession = it.textInputSession + 1,
            )
        }
    }

    fun openPackList(tripId: String) {
        _uiState.update { it.copy(mode = FoldMode.ViewPack(tripId)) }
    }

    fun closePackList(tripId: String) {
        _uiState.update { it.copy(mode = FoldMode.TripSettings(tripId)) }
    }

    fun startEditPack(tripId: String, returnToView: Boolean = false) {
        _uiState.update {
            it.copy(
                mode = FoldMode.TextInput(TextInputKind.PackList(tripId, returnToView)),
                textInputSession = it.textInputSession + 1,
            )
        }
    }

    fun cancelTextInput() {
        _uiState.update { state ->
            val currentMode = state.mode
            val fallback = when (val mode = currentMode) {
                is FoldMode.TextInput -> when (mode.kind) {
                    is TextInputKind.NewTrip -> FoldMode.TripList
                    is TextInputKind.RenameTrip -> {
                        if (mode.kind.fromList) FoldMode.TripList else FoldMode.TripSettings(mode.kind.tripId)
                    }
                    is TextInputKind.PackList -> packListFallbackMode(mode.kind)
                    is TextInputKind.EventField -> FoldMode.EventForm
                }
                else -> currentMode
            }
            val keepDraft = currentMode is FoldMode.TextInput &&
                (currentMode as FoldMode.TextInput).kind is TextInputKind.EventField
            state.copy(
                mode = fallback,
                eventDraft = if (keepDraft) state.eventDraft else null,
            )
        }
    }

    fun submitTextInput(raw: CharSequence) {
        val text = raw.toString().trim()
        val state = _uiState.value
        when (val mode = state.mode) {
            is FoldMode.TextInput -> when (val kind = mode.kind) {
                TextInputKind.NewTrip -> {
                    if (text.isEmpty()) {
                        _uiState.update { it.copy(errorModal = "Enter a trip name.") }
                        return
                    }
                    val trip = Trip(id = System.currentTimeMillis().toString(), name = text)
                    val trips = state.trips + trip
                    persist(trips) {
                        it.copy(
                            mode = FoldMode.TripDetail(trip.id),
                            errorModal = null,
                        )
                    }
                }

                is TextInputKind.RenameTrip -> {
                    if (text.isEmpty()) {
                        _uiState.update { it.copy(errorModal = "Enter a trip name.") }
                        return
                    }
                    val trips = state.trips.map { trip ->
                        if (trip.id == kind.tripId) trip.copy(name = text) else trip
                    }
                    persist(trips) {
                        it.copy(
                            mode = if (kind.fromList) FoldMode.TripList else FoldMode.TripSettings(kind.tripId),
                            errorModal = null,
                        )
                    }
                }

                is TextInputKind.PackList -> {
                    val trips = state.trips.map { trip ->
                        if (trip.id == kind.tripId) trip.copy(whatToPack = text) else trip
                    }
                    persist(trips) {
                        it.copy(
                            mode = packListFallbackMode(kind),
                            errorModal = null,
                        )
                    }
                }

                is TextInputKind.EventField -> submitEventField(kind.field, text)
            }
            else -> Unit
        }
    }

    fun confirmDeleteTrip(tripId: String, fromList: Boolean = false, fromSettings: Boolean = false) {
        _uiState.update { it.copy(mode = FoldMode.ConfirmDeleteTrip(tripId, fromList, fromSettings)) }
    }

    fun deleteTrip(tripId: String) {
        val trips = _uiState.value.trips.filter { it.id != tripId }
        persist(trips) { it.copy(mode = FoldMode.TripList) }
    }

    fun cancelDeleteTrip(tripId: String, fromList: Boolean = false, fromSettings: Boolean = false) {
        _uiState.update {
            it.copy(
                mode = when {
                    fromList -> FoldMode.TripList
                    fromSettings -> FoldMode.TripSettings(tripId)
                    else -> FoldMode.TripDetail(tripId)
                },
            )
        }
    }

    fun startAddEvent(tripId: String) {
        _uiState.update {
            it.copy(
                mode = FoldMode.PickEventType(tripId),
                eventDraft = null,
            )
        }
    }

    fun cancelPickEventType(tripId: String) {
        _uiState.update {
            it.copy(
                mode = FoldMode.TripDetail(tripId),
                eventDraft = null,
            )
        }
    }

    fun pickEventType(type: EventType, tripId: String) {
        val draft = EventFormDraft(tripId = tripId, type = type)
        _uiState.update {
            it.copy(
                eventDraft = draft,
                mode = FoldMode.EventForm,
            )
        }
    }

    fun startEditEvent(tripId: String, event: TripEvent) {
        val tripEvents = _uiState.value.trips.firstOrNull { it.id == tripId }?.events.orEmpty()
        val draft = draftForEdit(tripId, event, tripEvents)
        _uiState.update {
            it.copy(
                eventDraft = draft,
                mode = FoldMode.EventForm,
            )
        }
    }

    fun cancelEventForm() {
        val draft = _uiState.value.eventDraft
        _uiState.update {
            it.copy(
                mode = when {
                    draft == null -> FoldMode.TripList
                    draft.editingEventId != null -> FoldMode.TripDayDetail(draft.tripId)
                    else -> FoldMode.TripDetail(draft.tripId)
                },
                eventDraft = null,
            )
        }
    }

    fun openEventFormField(field: EventFormField) {
        if (_uiState.value.eventDraft == null) return
        _uiState.update {
            it.copy(
                mode = FoldMode.TextInput(TextInputKind.EventField(field)),
                textInputSession = it.textInputSession + 1,
            )
        }
    }

    fun saveEventForm() {
        val draft = _uiState.value.eventDraft ?: return
        if (draft.main.isBlank()) {
            _uiState.update { it.copy(errorModal = "Enter a title.") }
            return
        }
        val dateFields = listOf(
            draft.datetime,
            draft.checkOutDatetime,
            draft.returnDatetime,
        )
        for (raw in dateFields) {
            if (raw.isBlank()) continue
            val normalized = normalizeDateTimeInput(raw)
            if (parseDateTime(normalized) == null) {
                _uiState.update {
                    it.copy(errorModal = "Use YYYY-MM-DD or DD.MM.YYYY. (optional HH:MM).")
                }
                return
            }
        }
        saveEventFromDraft(
            draft.copy(
                datetime = normalizeDateTimeInput(draft.datetime),
                checkOutDatetime = normalizeDateTimeInput(draft.checkOutDatetime),
                returnDatetime = normalizeDateTimeInput(draft.returnDatetime),
            ),
        )
    }

    fun eventFormFields(): List<EventFormField> {
        val type = _uiState.value.eventDraft?.type ?: return emptyList()
        return fieldsForType(type)
    }

    fun eventFormFieldLabel(field: EventFormField): String =
        eventFieldTitle(field, _uiState.value.eventDraft?.type)

    fun eventFormFieldPlaceholder(field: EventFormField): String =
        textInputPlaceholder(TextInputKind.EventField(field))

    fun eventFormFieldValue(field: EventFormField): String {
        val draft = _uiState.value.eventDraft ?: return ""
        return valueForField(draft, field)
    }

    private fun draftForEdit(
        tripId: String,
        event: TripEvent,
        tripEvents: List<TripEvent>,
    ): EventFormDraft {
        var main = event.main
        var datetime = event.datetime
        var checkOut = ""
        var returnDt = ""

        when {
            event.type == EventType.HOTEL && main.endsWith(" (Check-in)") -> {
                main = main.removeSuffix(" (Check-in)")
                val sibling = tripEvents.firstOrNull {
                    it.id != event.id &&
                        it.type == EventType.HOTEL &&
                        it.main == "$main (Check-out)"
                }
                checkOut = sibling?.datetime.orEmpty()
            }
            event.type == EventType.HOTEL && main.endsWith(" (Check-out)") -> {
                main = main.removeSuffix(" (Check-out)")
                checkOut = event.datetime
                val sibling = tripEvents.firstOrNull {
                    it.id != event.id &&
                        it.type == EventType.HOTEL &&
                        it.main == "$main (Check-in)"
                }
                datetime = sibling?.datetime.orEmpty()
            }
            main.endsWith(" (Return)") -> {
                main = main.removeSuffix(" (Return)")
                returnDt = event.datetime
            }
        }

        return EventFormDraft(
            tripId = tripId,
            type = event.type,
            editingEventId = event.id,
            main = main,
            datetime = datetime,
            checkOutDatetime = checkOut,
            returnDatetime = returnDt,
            from = event.from,
            to = event.to,
            address = event.address,
            workingHours = event.workingHours,
            notes = event.notes,
            nextField = EventFormField.MAIN,
        )
    }

    private fun fieldsForType(type: EventType): List<EventFormField> = when (type) {
        EventType.FLIGHT -> listOf(
            EventFormField.MAIN,
            EventFormField.DATETIME,
            EventFormField.FROM,
            EventFormField.TO,
            EventFormField.NOTES,
        )
        EventType.HOTEL -> listOf(
            EventFormField.MAIN,
            EventFormField.DATETIME,
            EventFormField.CHECK_OUT,
            EventFormField.ADDRESS,
            EventFormField.NOTES,
        )
        EventType.BUS, EventType.TRAIN -> listOf(
            EventFormField.MAIN,
            EventFormField.DATETIME,
            EventFormField.FROM,
            EventFormField.TO,
            EventFormField.NOTES,
        )
        EventType.TAXI, EventType.CAR -> listOf(
            EventFormField.MAIN,
            EventFormField.DATETIME,
            EventFormField.FROM,
            EventFormField.TO,
            EventFormField.NOTES,
        )
        EventType.PLACE -> listOf(
            EventFormField.MAIN,
            EventFormField.DATETIME,
            EventFormField.ADDRESS,
            EventFormField.WORKING_HOURS,
            EventFormField.NOTES,
        )
    }

    private fun submitEventField(field: EventFormField, value: String) {
        val draft = _uiState.value.eventDraft ?: return

        val isDateField = field == EventFormField.DATETIME ||
            field == EventFormField.CHECK_OUT ||
            field == EventFormField.RETURN_DATETIME
        if (isDateField && value.isNotBlank()) {
            val normalized = normalizeDateTimeInput(value)
            if (parseDateTime(normalized) == null) {
                _uiState.update {
                    it.copy(errorModal = "Use YYYY-MM-DD or DD.MM.YYYY. (optional HH:MM).")
                }
                return
            }
        }

        val normalizedValue = if (isDateField) normalizeDateTimeInput(value) else value

        val updated = when (field) {
            EventFormField.MAIN -> draft.copy(main = normalizedValue)
            EventFormField.DATETIME -> draft.copy(datetime = normalizedValue)
            EventFormField.CHECK_OUT -> draft.copy(checkOutDatetime = normalizedValue)
            EventFormField.RETURN_DATETIME -> draft.copy(returnDatetime = normalizedValue)
            EventFormField.FROM -> draft.copy(from = normalizedValue)
            EventFormField.TO -> draft.copy(to = normalizedValue)
            EventFormField.ADDRESS -> draft.copy(address = normalizedValue)
            EventFormField.WORKING_HOURS -> draft.copy(workingHours = normalizedValue)
            EventFormField.NOTES -> draft.copy(notes = normalizedValue)
        }

        // Always return to the full form — one SAVE there, not after every field.
        _uiState.update {
            it.copy(
                eventDraft = updated,
                mode = FoldMode.EventForm,
            )
        }
    }

    private fun saveEventFromDraft(draft: EventFormDraft) {
        val normalizedDraft = draft.copy(
            datetime = normalizeDateTimeInput(draft.datetime),
            checkOutDatetime = normalizeDateTimeInput(draft.checkOutDatetime),
            returnDatetime = normalizeDateTimeInput(draft.returnDatetime),
        )
        val tripEvents = _uiState.value.trips
            .firstOrNull { it.id == normalizedDraft.tripId }
            ?.events
            .orEmpty()
        val existing = normalizedDraft.editingEventId?.let { id ->
            tripEvents.firstOrNull { it.id == id }
        }

        val trips = _uiState.value.trips.map { trip ->
            if (trip.id != normalizedDraft.tripId) return@map trip
            val events = if (normalizedDraft.editingEventId != null) {
                applyEditedEvents(trip.events, normalizedDraft, existing)
            } else {
                trip.events + buildEventsFromDraft(normalizedDraft, existing)
            }
            trip.copy(events = events)
        }

        val focusDate = when {
            normalizedDraft.type == EventType.HOTEL &&
                existing?.main?.endsWith(" (Check-out)") == true ->
                normalizedDraft.checkOutDatetime.ifBlank { normalizedDraft.datetime }
            else -> normalizedDraft.datetime
        }

        persist(trips) { state ->
            val trip = trips.firstOrNull { it.id == normalizedDraft.tripId }
            val dayIndex = trip?.let { dayIndexForDate(it.events, parseDateKey(focusDate)) } ?: 0
            state.copy(
                mode = if (normalizedDraft.editingEventId != null) {
                    FoldMode.TripDayDetail(normalizedDraft.tripId)
                } else {
                    FoldMode.TripDetail(normalizedDraft.tripId)
                },
                eventDraft = null,
                errorModal = null,
                selectedDayIndex = dayIndex,
            )
        }
    }

    private fun applyEditedEvents(
        events: List<TripEvent>,
        draft: EventFormDraft,
        existing: TripEvent?,
    ): List<TripEvent> {
        if (draft.type == EventType.HOTEL) {
            val baseName = draft.main.removeSuffix(" (Check-in)").removeSuffix(" (Check-out)")
            val checkInId = when {
                existing?.main?.endsWith(" (Check-in)") == true -> existing.id
                else -> events.firstOrNull {
                    it.type == EventType.HOTEL && it.main == "$baseName (Check-in)"
                }?.id
            }
            val checkOutId = when {
                existing?.main?.endsWith(" (Check-out)") == true -> existing.id
                else -> events.firstOrNull {
                    it.type == EventType.HOTEL && it.main == "$baseName (Check-out)"
                }?.id
            }

            return events.map { event ->
                when (event.id) {
                    checkInId -> event.copy(
                        main = "$baseName (Check-in)",
                        datetime = draft.datetime,
                        from = draft.from,
                        to = draft.to,
                        address = draft.address,
                        workingHours = draft.workingHours,
                        notes = draft.notes,
                    )
                    checkOutId -> event.copy(
                        main = "$baseName (Check-out)",
                        datetime = draft.checkOutDatetime.ifBlank { draft.datetime },
                        from = draft.from,
                        to = draft.to,
                        address = draft.address,
                        workingHours = draft.workingHours,
                        notes = draft.notes,
                    )
                    draft.editingEventId -> buildSingleEvent(draft, event.id, event)
                    else -> event
                }
            }
        }

        return events.map { event ->
            if (event.id == draft.editingEventId) {
                buildSingleEvent(draft, event.id, event)
            } else {
                event
            }
        }
    }

    private fun buildSingleEvent(
        draft: EventFormDraft,
        id: String,
        existing: TripEvent?,
    ): TripEvent {
        val main = when {
            draft.type == EventType.HOTEL && existing?.main?.endsWith(" (Check-out)") == true ->
                "${draft.main} (Check-out)"
            draft.type == EventType.HOTEL && existing?.main?.endsWith(" (Check-in)") == true ->
                "${draft.main} (Check-in)"
            draft.main.endsWith(" (Return)") || existing?.main?.endsWith(" (Return)") == true ->
                "${draft.main.removeSuffix(" (Return)")} (Return)"
            else -> draft.main
        }
        val datetime = when {
            draft.type == EventType.HOTEL && existing?.main?.endsWith(" (Check-out)") == true ->
                draft.checkOutDatetime.ifBlank { draft.datetime }
            draft.main.endsWith(" (Return)") || existing?.main?.endsWith(" (Return)") == true ->
                draft.returnDatetime.ifBlank { draft.datetime }
            else -> draft.datetime
        }
        return TripEvent(
            id = id,
            type = draft.type,
            main = main,
            datetime = datetime,
            from = draft.from,
            to = draft.to,
            address = draft.address,
            workingHours = draft.workingHours,
            notes = draft.notes,
            qrCodes = existing?.qrCodes.orEmpty(),
            done = existing?.done ?: false,
        )
    }

    private fun buildEventsFromDraft(draft: EventFormDraft, existing: TripEvent?): List<TripEvent> {
        val baseId = System.currentTimeMillis()
        val qrCodes = existing?.qrCodes.orEmpty()

        fun event(
            id: String,
            main: String,
            datetime: String,
            from: String = draft.from,
            to: String = draft.to,
        ) = TripEvent(
            id = id,
            type = draft.type,
            main = main,
            datetime = datetime,
            from = from,
            to = to,
            address = draft.address,
            workingHours = draft.workingHours,
            notes = draft.notes,
            qrCodes = qrCodes,
        )

        return when (draft.type) {
            EventType.FLIGHT -> listOf(event(baseId.toString(), draft.main, draft.datetime))
            EventType.HOTEL -> listOf(
                event(baseId.toString(), "${draft.main} (Check-in)", draft.datetime),
                event((baseId + 1).toString(), "${draft.main} (Check-out)", draft.checkOutDatetime),
            )
            EventType.BUS, EventType.TRAIN -> listOf(event(baseId.toString(), draft.main, draft.datetime))
            else -> listOf(event(baseId.toString(), draft.main, draft.datetime))
        }
    }

    fun confirmDeleteEvent(tripId: String, eventId: String) {
        _uiState.update { it.copy(mode = FoldMode.ConfirmDeleteEvent(tripId, eventId)) }
    }

    fun deleteEvent(tripId: String, eventId: String) {
        val trips = _uiState.value.trips.map { trip ->
            if (trip.id != tripId) return@map trip
            trip.copy(events = trip.events.filter { it.id != eventId })
        }
        persist(trips) { state ->
            val trip = trips.firstOrNull { it.id == tripId }
            val days = trip?.let { groupEventsByDay(it.events) }.orEmpty()
            val dayIndex = state.selectedDayIndex.coerceIn(0, (days.size - 1).coerceAtLeast(0))
            state.copy(
                mode = if (days.isEmpty()) FoldMode.TripDetail(tripId) else FoldMode.TripDayDetail(tripId),
                selectedDayIndex = dayIndex,
            )
        }
    }

    fun cancelDeleteEvent(tripId: String) {
        _uiState.update { it.copy(mode = FoldMode.TripDayDetail(tripId)) }
    }

    fun openCopySheet(label: String, text: String) {
        _uiState.update { it.copy(copySheet = CopySheetState(label = label, text = text)) }
    }

    fun closeCopySheet() {
        _uiState.update { it.copy(copySheet = null) }
    }

    fun markCopied() {
        _uiState.update {
            it.copy(
                copySheet = null,
                copiedMessage = "Copied",
            )
        }
    }

    fun dismissCopied() {
        _uiState.update { it.copy(copiedMessage = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorModal = null) }
    }

    fun startScanQr(tripId: String, eventId: String) {
        _uiState.update { it.copy(mode = FoldMode.ScanQr(tripId, eventId)) }
    }

    fun cancelScanQr(tripId: String, eventId: String) {
        _uiState.update {
            it.copy(mode = FoldMode.TripDayDetail(tripId))
        }
    }

    fun startPasteQr(tripId: String, eventId: String) {
        _uiState.update {
            it.copy(
                mode = FoldMode.PasteQr(tripId, eventId),
                textInputSession = it.textInputSession + 1,
            )
        }
    }

    fun cancelPasteQr(tripId: String, eventId: String) {
        _uiState.update {
            it.copy(mode = FoldMode.TripDayDetail(tripId))
        }
    }

    fun submitPasteQr(tripId: String, eventId: String, raw: CharSequence) {
        appendQrCode(tripId, eventId, raw.toString())
    }

    fun appendQrCode(tripId: String, eventId: String, payload: String) {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(mode = FoldMode.TripDayDetail(tripId)) }
            return
        }

        val trips = _uiState.value.trips.map { trip ->
            if (trip.id != tripId) return@map trip
            trip.copy(
                events = trip.events.map { event ->
                    if (event.id != eventId) return@map event
                    if (event.qrCodes.contains(trimmed) || event.qrCodes.size >= 3) return@map event
                    event.copy(qrCodes = event.qrCodes + trimmed)
                },
            )
        }

        persist(trips) {
            it.copy(mode = FoldMode.TripDayDetail(tripId))
        }
    }

    fun openViewQr(tripId: String, eventId: String, index: Int) {
        _uiState.update { it.copy(mode = FoldMode.ViewQr(tripId, eventId, index)) }
    }

    fun closeViewQr(tripId: String, eventId: String) {
        _uiState.update {
            it.copy(mode = FoldMode.TripDayDetail(tripId))
        }
    }

    fun removeQrCode(tripId: String, eventId: String, index: Int) {
        val trips = _uiState.value.trips.map { trip ->
            if (trip.id != tripId) return@map trip
            trip.copy(
                events = trip.events.map { event ->
                    if (event.id != eventId) return@map event
                    event.copy(qrCodes = event.qrCodes.filterIndexed { i, _ -> i != index })
                },
            )
        }
        persist(trips) {
            it.copy(mode = FoldMode.TripDayDetail(tripId))
        }
    }

    fun qrPayload(tripId: String, eventId: String, index: Int): String? {
        return _uiState.value.trips
            .firstOrNull { it.id == tripId }
            ?.events
            ?.firstOrNull { it.id == eventId }
            ?.qrCodes
            ?.getOrNull(index)
    }

    override fun onBackPressed(): Boolean {
        val state = _uiState.value
        return when (val mode = state.mode) {
            is FoldMode.PickEventType -> {
                _uiState.update { it.copy(mode = FoldMode.TripDetail(mode.tripId)) }
                true
            }
            FoldMode.EventForm -> {
                cancelEventForm()
                true
            }
            is FoldMode.TextInput -> {
                cancelTextInput()
                true
            }
            is FoldMode.ConfirmDeleteTrip -> {
                cancelDeleteTrip(mode.tripId, mode.fromList, mode.fromSettings)
                true
            }
            is FoldMode.ConfirmDeleteEvent -> {
                cancelDeleteEvent(mode.tripId)
                true
            }
            is FoldMode.ScanQr -> {
                cancelScanQr(mode.tripId, mode.eventId)
                true
            }
            is FoldMode.PasteQr -> {
                cancelPasteQr(mode.tripId, mode.eventId)
                true
            }
            is FoldMode.ViewPack -> {
                closePackList(mode.tripId)
                true
            }
            is FoldMode.ViewQr -> {
                closeViewQr(mode.tripId, mode.eventId)
                true
            }
            is FoldMode.TripWeekly -> {
                backToTripDay(mode.tripId)
                true
            }
            is FoldMode.TripDayDetail -> {
                backToTripDay(mode.tripId)
                true
            }
            is FoldMode.TripSettings -> {
                backToTripDay(mode.tripId)
                true
            }
            is FoldMode.TripDetail -> {
                backToTripList()
                true
            }
            else -> false
        }
    }

    fun textInputTitle(kind: TextInputKind): String = when (kind) {
        TextInputKind.NewTrip -> "New trip"
        is TextInputKind.RenameTrip -> "Rename trip"
        is TextInputKind.PackList -> "What to pack"
        is TextInputKind.EventField -> eventFieldTitle(kind.field, _uiState.value.eventDraft?.type)
    }

    fun textInputInitialValue(kind: TextInputKind, trips: List<Trip>, draft: EventFormDraft?): String {
        return when (kind) {
            TextInputKind.NewTrip -> ""
            is TextInputKind.RenameTrip -> trips.firstOrNull { it.id == kind.tripId }?.name.orEmpty()
            is TextInputKind.PackList -> trips.firstOrNull { it.id == kind.tripId }?.whatToPack.orEmpty()
            is TextInputKind.EventField -> draft?.let { valueForField(it, kind.field) }.orEmpty()
        }
    }

    private fun valueForField(draft: EventFormDraft, field: EventFormField): String = when (field) {
        EventFormField.MAIN -> draft.main
        EventFormField.DATETIME -> draft.datetime
        EventFormField.CHECK_OUT -> draft.checkOutDatetime
        EventFormField.RETURN_DATETIME -> draft.returnDatetime
        EventFormField.FROM -> draft.from
        EventFormField.TO -> draft.to
        EventFormField.ADDRESS -> draft.address
        EventFormField.WORKING_HOURS -> draft.workingHours
        EventFormField.NOTES -> draft.notes
    }

    private fun eventFieldTitle(field: EventFormField, type: EventType?): String = when (field) {
        EventFormField.MAIN -> when (type) {
            EventType.FLIGHT -> "Flight / airline"
            EventType.HOTEL -> "Hotel name"
            EventType.PLACE -> "Place name"
            else -> "Title"
        }
        EventFormField.DATETIME -> when (type) {
            EventType.FLIGHT -> "Departure"
            EventType.HOTEL -> "Check-in"
            else -> "Date & time"
        }
        EventFormField.CHECK_OUT -> "Check-out"
        EventFormField.RETURN_DATETIME -> "Return"
        EventFormField.FROM -> "From"
        EventFormField.TO -> when (type) {
            EventType.FLIGHT -> "To"
            EventType.BUS, EventType.TRAIN -> "To"
            else -> "To"
        }
        EventFormField.ADDRESS -> "Address"
        EventFormField.WORKING_HOURS -> "Working hours"
        EventFormField.NOTES -> "Notes"
    }

    fun textInputPlaceholder(kind: TextInputKind): String {
        val eventType = _uiState.value.eventDraft?.type
        return when (kind) {
        TextInputKind.NewTrip -> "e.g. Paris summer"
        is TextInputKind.RenameTrip -> "Trip name"
        is TextInputKind.PackList -> "passport, charger, socks..."
        is TextInputKind.EventField -> when (kind.field) {
            EventFormField.MAIN -> when (eventType) {
                EventType.FLIGHT -> "e.g. Ryanair FR123"
                EventType.HOTEL -> "e.g. Hilton"
                EventType.PLACE -> "e.g. Louvre"
                EventType.TAXI, EventType.CAR -> "e.g. Taxi to airport"
                else -> "e.g. FlixBus"
            }
            EventFormField.DATETIME, EventFormField.CHECK_OUT, EventFormField.RETURN_DATETIME ->
                "21.10.2026. or 21.10.2026. 14:30"
            EventFormField.FROM -> "Departure"
            EventFormField.TO -> when (eventType) {
                EventType.FLIGHT -> "Arrival"
                else -> "Destination"
            }
            EventFormField.ADDRESS -> when (eventType) {
                EventType.HOTEL -> "Hotel address"
                else -> "Address"
            }
            EventFormField.WORKING_HOURS -> "e.g. 9-18"
            EventFormField.NOTES -> "Optional"
        }
        }
    }

    fun textInputMultiline(kind: TextInputKind): Boolean = when (kind) {
        is TextInputKind.PackList -> true
        is TextInputKind.EventField ->
            kind.field == EventFormField.NOTES || kind.field == EventFormField.ADDRESS
        else -> false
    }

    private fun packListFallbackMode(kind: TextInputKind.PackList): FoldMode =
        if (kind.returnToView) FoldMode.ViewPack(kind.tripId) else FoldMode.TripSettings(kind.tripId)
}

internal fun foldViewModelFactory(dataStore: DataStore<Preferences>): FoldViewModel {
    return FoldViewModel(TripRepository(dataStore))
}
