package com.aistudio.couplebubble.qxztrw.data.remote.mapper

import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema
import com.aistudio.couplebubble.qxztrw.domain.model.GeoPoint
import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import java.time.LocalDate

/** Reads and writes `spaces/{id}/reunion/current`; places live in the map [FirestoreSchema.Reunion.PLACES] keyed by UID. */
internal object ReunionMapper {

    fun fromFields(fields: Fields): Reunion? {
        val start = fields.date(FirestoreSchema.Reunion.START_DATE) ?: return null
        val end = fields.date(FirestoreSchema.Reunion.END_DATE) ?: return null
        if (end.isBefore(start)) return null
        return Reunion(
            startDate = start,
            endDate = end,
            plannedAtDate = fields.date(FirestoreSchema.Reunion.PLANNED_AT) ?: start,
            places = fields.entries(FirestoreSchema.Reunion.PLACES).associate { (uid, place) -> uid to placeFromFields(place) }
        )
    }

    fun placeFromFields(fields: Fields): ReunionPlace {
        val lat = (fields[FirestoreSchema.ReunionPlace.LATITUDE] as? Number)?.toDouble()
        val lng = (fields[FirestoreSchema.ReunionPlace.LONGITUDE] as? Number)?.toDouble()
        return ReunionPlace(
            sharesLocation = fields.bool(FirestoreSchema.ReunionPlace.SHARES_LOCATION) ?: false,
            location = if (lat != null && lng != null) GeoPoint(lat, lng) else null,
            locatedAtMillis = fields.long(FirestoreSchema.ReunionPlace.LOCATED_AT),
            city = fields.string(FirestoreSchema.ReunionPlace.CITY)?.takeIf { it.isNotBlank() },
            cityLocation = (fields[FirestoreSchema.ReunionPlace.CITY_LATITUDE] as? Number)?.toDouble()?.let { lat ->
                (fields[FirestoreSchema.ReunionPlace.CITY_LONGITUDE] as? Number)?.toDouble()?.let { GeoPoint(lat, it) }
            }
        )
    }

    fun datesToFields(start: LocalDate, end: LocalDate, plannedAt: LocalDate, updatedAt: Long): Map<String, Any> = mapOf(
        FirestoreSchema.Reunion.START_DATE to start.toString(),
        FirestoreSchema.Reunion.END_DATE to end.toString(),
        FirestoreSchema.Reunion.PLANNED_AT to plannedAt.toString(),
        FirestoreSchema.Reunion.UPDATED_AT to updatedAt
    )

    /** The whole entry is replaced, so a location switched off leaves no coordinates behind. Rounded again as a guard. */
    fun placeToFields(place: ReunionPlace): Map<String, Any> = buildMap {
        put(FirestoreSchema.ReunionPlace.SHARES_LOCATION, place.sharesLocation)
        val location = place.location
        if (place.sharesLocation && location != null && place.locatedAtMillis != null) {
            val rounded = location.rounded()
            put(FirestoreSchema.ReunionPlace.LATITUDE, rounded.latitude)
            put(FirestoreSchema.ReunionPlace.LONGITUDE, rounded.longitude)
            put(FirestoreSchema.ReunionPlace.LOCATED_AT, place.locatedAtMillis)
        }
        place.city?.trim()?.takeIf { it.isNotEmpty() }?.let { put(FirestoreSchema.ReunionPlace.CITY, it) }
        place.cityLocation?.rounded()?.let {
            put(FirestoreSchema.ReunionPlace.CITY_LATITUDE, it.latitude)
            put(FirestoreSchema.ReunionPlace.CITY_LONGITUDE, it.longitude)
        }
    }

    private fun Fields.date(key: String): LocalDate? = string(key)?.let {
        try {
            LocalDate.parse(it)
        } catch (_: Exception) {
            null
        }
    }
}
