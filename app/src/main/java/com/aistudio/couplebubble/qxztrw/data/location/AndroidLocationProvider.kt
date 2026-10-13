package com.aistudio.couplebubble.qxztrw.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import com.aistudio.couplebubble.qxztrw.domain.model.GeoPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Fused location at balanced-power accuracy, which is all `ACCESS_COARSE_LOCATION` allows anyway. */
class AndroidLocationProvider(private val context: Context) : LocationProvider {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Checked by hasPermission() right below; lint can't follow the call
    @SuppressLint("MissingPermission")
    override suspend fun currentApproximateLocation(): GeoPoint? {
        if (!hasPermission()) return null
        val cancellation = CancellationTokenSource()
        return try {
            withTimeoutOrNull(LOCATION_TIMEOUT) {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token).await()
            }?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            // SecurityException if the permission was revoked meanwhile, ApiException without Play services
            null
        } finally {
            cancellation.cancel()
        }
    }

    private companion object {
        val LOCATION_TIMEOUT = 10.seconds
    }
}
