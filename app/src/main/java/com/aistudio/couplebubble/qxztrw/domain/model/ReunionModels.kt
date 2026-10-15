package com.aistudio.couplebubble.qxztrw.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on the map; only ever stored rounded by [rounded]. */
data class GeoPoint(val latitude: Double, val longitude: Double) {

    /** One decimal place, roughly 10 km: enough for a distance, too coarse to show where someone lives. */
    fun rounded(): GeoPoint = GeoPoint(round(latitude * 10) / 10, round(longitude * 10) / 10)

    /** Great-circle distance (haversine) in kilometers. */
    fun distanceKmTo(other: GeoPoint): Double {
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) * sin(dLon / 2).pow(2)
        // Rounding can push `a` slightly above 1 for near-antipodal points; asin of that would be NaN
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    private companion object {
        const val EARTH_RADIUS_KM = 6371.0
    }
}

/**
 * Where one partner is for the reunion distance, written only by that partner (keyed by UID, so a role swap
 * keeps it with the right person). [location] is the rounded device location while [sharesLocation] is on;
 * sharing is optional, without it there is simply no distance.
 */
data class ReunionPlace(
    val sharesLocation: Boolean = false,
    val location: GeoPoint? = null,
    val locatedAtMillis: Long? = null
) {
    /** The location counts while it is younger than [ReunionRules.LOCATION_MAX_AGE_MILLIS]. */
    fun freshLocation(nowMillis: Long): GeoPoint? = location?.takeIf {
        sharesLocation && locatedAtMillis != null && nowMillis - locatedAtMillis <= ReunionRules.LOCATION_MAX_AGE_MILLIS
    }

    val isEmpty: Boolean get() = !sharesLocation && location == null
}

object ReunionRules {
    /** A stored location older than this no longer counts for the distance. */
    const val LOCATION_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000

    /** The location is asked again only once the stored one is older than this. */
    const val LOCATION_REFRESH_MILLIS = 60L * 60 * 1000
}

/** The next time both partners see each other, shared via `spaces/{id}/reunion/current`. */
data class Reunion(
    val startDate: LocalDate,
    val endDate: LocalDate,
    /** When it was planned; the waiting progress runs from here to [startDate]. */
    val plannedAtDate: LocalDate = startDate,
    /** Partner places keyed by auth UID. */
    val places: Map<String, ReunionPlace> = emptyMap()
) {
    val durationDays: Int get() = (ChronoUnit.DAYS.between(startDate, endDate) + 1).toInt()

    fun placeOf(uid: String?): ReunionPlace? = uid?.let { places[it] }

    /** The other partner's place: any entry that is not [uid]'s (a space has at most two members). */
    fun partnerPlaceOf(uid: String?): ReunionPlace? = places.entries.firstOrNull { it.key != uid }?.value

    fun status(today: LocalDate): ReunionStatus = when {
        today.isBefore(startDate) -> {
            val total = ChronoUnit.DAYS.between(plannedAtDate, startDate).coerceAtLeast(1)
            val elapsed = ChronoUnit.DAYS.between(plannedAtDate, today).coerceIn(0, total)
            ReunionStatus.Upcoming(
                daysUntil = ChronoUnit.DAYS.between(today, startDate).toInt(),
                durationDays = durationDays,
                progress = elapsed.toFloat() / total
            )
        }
        !today.isAfter(endDate) -> ReunionStatus.Together(
            day = ChronoUnit.DAYS.between(startDate, today).toInt() + 1,
            durationDays = durationDays
        )
        else -> ReunionStatus.Over
    }

    /** Rounded distance between both partners, or `null` while one of them shares no fresh location. */
    fun distanceKm(uid: String?, nowMillis: Long): Int? {
        val own = placeOf(uid)?.freshLocation(nowMillis) ?: return null
        val partner = partnerPlaceOf(uid)?.freshLocation(nowMillis) ?: return null
        return own.distanceKmTo(partner).roundToInt()
    }

    /** The same distance without knowing who is who (it is symmetric), for the widget; `null` unless both places are fresh. */
    fun distanceKm(nowMillis: Long): Int? {
        val locations = places.values.mapNotNull { it.freshLocation(nowMillis) }
        if (locations.size != 2) return null
        return locations[0].distanceKmTo(locations[1]).roundToInt()
    }
}

sealed interface ReunionStatus {
    data class Upcoming(val daysUntil: Int, val durationDays: Int, val progress: Float) : ReunionStatus
    data class Together(val day: Int, val durationDays: Int) : ReunionStatus
    data object Over : ReunionStatus
}

/** Where a partner's distance value comes from, for the card's source line. */
sealed interface ReunionPlaceSource {
    data class Location(val locatedAtMillis: Long) : ReunionPlaceSource
    data object None : ReunionPlaceSource
}

fun ReunionPlace?.source(nowMillis: Long): ReunionPlaceSource {
    if (this == null) return ReunionPlaceSource.None
    return if (freshLocation(nowMillis) != null && locatedAtMillis != null) {
        ReunionPlaceSource.Location(locatedAtMillis)
    } else {
        ReunionPlaceSource.None
    }
}
