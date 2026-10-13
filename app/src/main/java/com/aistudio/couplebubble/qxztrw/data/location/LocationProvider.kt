package com.aistudio.couplebubble.qxztrw.data.location

import com.aistudio.couplebubble.qxztrw.domain.model.GeoPoint

/** The device's approximate location, asked only while the app is open; never in the background. */
interface LocationProvider {
    /** Whether the user granted (at least approximate) location access. */
    fun hasPermission(): Boolean

    /** One approximate fix, or `null` without permission, with location turned off or after a timeout. */
    suspend fun currentApproximateLocation(): GeoPoint?
}
