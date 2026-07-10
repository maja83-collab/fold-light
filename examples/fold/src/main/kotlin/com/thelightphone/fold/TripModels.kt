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

internal fun parseDateKey(datetime: String): String {
    if (datetime.isBlank()) return "No date"
    return datetime.trim().substringBefore(' ')
}

internal fun isDateOnly(datetime: String): Boolean {
    return datetime.isNotBlank() && !datetime.contains(' ')
}

internal fun parseDateTime(datetime: String): Long {
    if (datetime.isBlank()) return 0L
    val trimmed = datetime.trim()
    return runCatching {
        if (isDateOnly(trimmed)) {
            java.time.LocalDate.parse(trimmed)
                .atStartOfDay()
                .toEpochSecond(java.time.ZoneOffset.UTC)
        } else {
            java.time.LocalDateTime.parse(trimmed.replace(' ', 'T'))
                .toEpochSecond(java.time.ZoneOffset.UTC)
        }
    }.getOrElse {
        runCatching { java.time.Instant.parse(trimmed).epochSecond }.getOrDefault(0L)
    }
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
    if (isDateOnly(datetime)) return formatDayTitle(datetime)
    val timePart = datetime.trim().substringAfter(' ', "").trim()
    val dayTitle = formatDayTitle(parseDateKey(datetime))
    return if (timePart.isNotBlank()) "$dayTitle $timePart" else dayTitle
}

internal fun sortEventsWithinDay(events: List<TripEvent>): List<TripEvent> {
    return events.sortedWith(
        compareBy(
            { if (isDateOnly(it.datetime)) 0 else 1 },
            { parseDateTime(it.datetime) },
            { it.main.lowercase() },
        ),
    )
}

internal fun groupEventsByDay(events: List<TripEvent>): List<Pair<String, List<TripEvent>>> {
    return events
        .groupBy { parseDateKey(it.datetime) }
        .toList()
        .sortedBy { (day, _) -> if (day == "No date") Long.MAX_VALUE else parseDateTime("$day 00:00") }
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
    if (event.datetime.isNotBlank() && !isDateOnly(event.datetime)) {
        parts += event.datetime.substringAfter(' ', "")
    }
    return parts.filter { it.isNotBlank() }.joinToString(" · ")
}
