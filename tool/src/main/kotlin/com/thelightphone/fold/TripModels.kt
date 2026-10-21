package com.thelightphone.fold

import kotlinx.serialization.Serializable

@Serializable
enum class EventType {
    FLIGHT,
    HOTEL,
    BUS,
    TRAIN,
    TAXI,
    CAR,
    PLACE,
}

@Serializable
data class TripEvent(
    val id: String,
    val type: EventType,
    val main: String,
    val datetime: String = "",
    val from: String = "",
    val to: String = "",
    val address: String = "",
    val workingHours: String = "",
    val notes: String = "",
    val qrCodes: List<String> = emptyList(),
    val done: Boolean = false,
)

@Serializable
data class Trip(
    val id: String,
    val name: String,
    val whatToPack: String = "",
    val events: List<TripEvent> = emptyList(),
)

@Serializable
data class TripStore(
    val trips: List<Trip> = emptyList(),
)

internal fun EventType.label(): String = when (this) {
    EventType.FLIGHT -> "Flight"
    EventType.HOTEL -> "Hotel"
    EventType.BUS -> "Bus"
    EventType.TRAIN -> "Train"
    EventType.TAXI -> "Taxi"
    EventType.CAR -> "Car rental"
    EventType.PLACE -> "Place"
}

internal fun EventType.icon() = when (this) {
    EventType.FLIGHT -> com.thelightphone.sdk.ui.LightIcons.AIRPLANE
    EventType.HOTEL -> com.thelightphone.sdk.ui.LightIcons.MAP
    EventType.BUS -> com.thelightphone.sdk.ui.LightIcons.DIRECTIONS_BUS
    EventType.TRAIN -> com.thelightphone.sdk.ui.LightIcons.DIRECTIONS_TRAIN
    EventType.TAXI -> com.thelightphone.sdk.ui.LightIcons.DIRECTIONS_ARRIVAL
    EventType.CAR -> com.thelightphone.sdk.ui.LightIcons.DIRECTIONS_STRAIGHT
    EventType.PLACE -> com.thelightphone.sdk.ui.LightIcons.CROSSHAIR
}

internal fun TripEvent.copyTargets(): List<Pair<String, String>> {
    val items = mutableListOf<Pair<String, String>>()
    if (main.isNotBlank()) items += "Title" to main
    if (address.isNotBlank()) items += "Address" to address
    val route = listOf(from, to).filter { it.isNotBlank() }.joinToString(" → ")
    if (route.isNotBlank()) items += "Route" to route
    if (workingHours.isNotBlank()) items += "Hours" to workingHours
    return items
}

/**
 * Canonical forms: `YYYY-MM-DD` or `YYYY-MM-DD HH:MM`.
 * Also accepts `DD.MM.YYYY`, `DD.MM.YYYY.` (točka nakon godine), `DD/MM/YYYY`, and optional time.
 */
internal fun normalizeDateTimeInput(raw: String): String {
    val trimmed = raw.trim().replace('\u00A0', ' ').replace(Regex("\\s+"), " ")
    if (trimmed.isEmpty()) return ""

    parseFlexibleDateTime(trimmed)?.let { (date, time) ->
        return if (time != null) "$date $time" else date
    }
    return trimmed
}

private fun parseFlexibleDateTime(trimmed: String): Pair<String, String?>? {
    val parts = trimmed.split(' ', limit = 2)
    val datePart = parts[0]
    val timePart = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }

    val date = parseFlexibleDate(datePart) ?: return null
    val time = timePart?.let { parseFlexibleTime(it) }
    if (timePart != null && time == null) return null
    return date to time
}

private fun parseFlexibleDate(raw: String): String? {
    val iso = runCatching { java.time.LocalDate.parse(raw) }.getOrNull()
    if (iso != null) return iso.toString()

    // Allow trailing period after year: 21.10.2026. (common when typing)
    val dotted = Regex("""^(\d{1,2})[./](\d{1,2})[./](\d{4})\.?$""").matchEntire(raw) ?: return null
    val day = dotted.groupValues[1].toInt()
    val month = dotted.groupValues[2].toInt()
    val year = dotted.groupValues[3].toInt()
    return runCatching {
        java.time.LocalDate.of(year, month, day).toString()
    }.getOrNull()
}

private fun parseFlexibleTime(raw: String): String? {
    val match = Regex("""^(\d{1,2}):(\d{2})(?::(\d{2}))?$""").matchEntire(raw.trim()) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    if (hour !in 0..23 || minute !in 0..59) return null
    return "%02d:%02d".format(hour, minute)
}

internal fun parseDateKey(datetime: String): String {
    if (datetime.isBlank()) return "No date"
    val normalized = normalizeDateTimeInput(datetime)
    if (normalized.isBlank()) return "No date"
    val key = normalized.substringBefore(' ')
    return if (parseFlexibleDate(key) != null) key else "No date"
}

internal fun isDateOnly(datetime: String): Boolean {
    val normalized = normalizeDateTimeInput(datetime)
    return normalized.isNotBlank() && !normalized.contains(' ')
}

internal fun parseDateTime(datetime: String): Long? {
    if (datetime.isBlank()) return null
    val normalized = normalizeDateTimeInput(datetime)
    if (normalized.isBlank()) return null
    return runCatching {
        if (isDateOnly(normalized)) {
            java.time.LocalDate.parse(normalized)
                .atStartOfDay()
                .toEpochSecond(java.time.ZoneOffset.UTC)
        } else {
            java.time.LocalDateTime.parse(normalized.replace(' ', 'T'))
                .toEpochSecond(java.time.ZoneOffset.UTC)
        }
    }.getOrNull()
}

internal fun formatDayTitle(dateKey: String): String {
    if (dateKey == "No date") return "No date"
    return runCatching {
        val date = java.time.LocalDate.parse(dateKey)
        date.format(
            java.time.format.DateTimeFormatter.ofPattern("EEE, MMMM d", java.util.Locale.ENGLISH),
        )
    }.getOrDefault(dateKey)
}

internal fun formatDateTimeDisplay(datetime: String): String {
    if (datetime.isBlank()) return datetime
    val normalized = normalizeDateTimeInput(datetime)
    if (isDateOnly(normalized)) return formatDayTitle(normalized)
    val timePart = normalized.substringAfter(' ', "").trim()
    val dayTitle = formatDayTitle(parseDateKey(normalized))
    return if (timePart.isNotBlank()) "$dayTitle $timePart" else dayTitle
}

internal fun sortEventsWithinDay(events: List<TripEvent>): List<TripEvent> {
    return events.sortedWith(
        compareBy<TripEvent> { event ->
            val epoch = parseDateTime(event.datetime)
            when {
                epoch == null -> 2
                isDateOnly(event.datetime) -> 0
                else -> 1
            }
        }
            .thenBy { parseDateTime(it.datetime) ?: Long.MAX_VALUE }
            .thenBy { it.main.lowercase() },
    )
}

internal fun groupEventsByDay(events: List<TripEvent>): List<Pair<String, List<TripEvent>>> {
    return events
        .groupBy { parseDateKey(it.datetime) }
        .toList()
        .sortedWith(
            compareBy { (day, _) ->
                if (day == "No date") Long.MAX_VALUE else parseDateTime("$day 00:00") ?: Long.MAX_VALUE - 1
            },
        )
        .map { (day, dayEvents) -> day to sortEventsWithinDay(dayEvents) }
}

internal fun defaultDayIndex(events: List<TripEvent>): Int {
    val days = groupEventsByDay(events)
    if (days.isEmpty()) return 0
    val today = java.time.LocalDate.now().toString()
    days.indexOfFirst { it.first == today }.takeIf { it >= 0 }?.let { return it }
    days.indexOfFirst { it.first >= today }.takeIf { it >= 0 }?.let { return it }
    return 0
}

internal fun dayIndexForDate(events: List<TripEvent>, dateKey: String): Int {
    val days = groupEventsByDay(events)
    return days.indexOfFirst { it.first == dateKey }.coerceAtLeast(0)
}

internal fun eventSummaryLine(event: TripEvent): String {
    val parts = mutableListOf(event.main)
    val normalized = normalizeDateTimeInput(event.datetime)
    if (normalized.isNotBlank() && !isDateOnly(normalized)) {
        parts += normalized.substringAfter(' ', "")
    }
    return parts.filter { it.isNotBlank() }.joinToString(" · ")
}
