package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.couplebubble.qxztrw.R
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlinx.coroutines.launch

/**
 * Fullscreen photo with pinch zoom (1x–4x), double-tap zoom, bounded pan and swipe-to-dismiss at normal zoom.
 * [isSaving] shows a spinner on the gallery button while an export runs.
 */
@Composable
internal fun FullscreenImageViewer(
    imageUrl: String,
    isSaving: Boolean,
    onSaveToGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
    var dragOffsetY by remember(imageUrl) { mutableFloatStateOf(0f) }
    var isDismissing by remember(imageUrl) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val backdropAlpha = (1f - (dragOffsetY.absoluteValue / 600f)).coerceIn(0.2f, 1f)
                    drawRect(Color.Black.copy(alpha = 0.95f * backdropAlpha))
                }
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val fullscreenRequest = remember(imageUrl, context) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .allowHardware(false)
                    .build()
            }
            AsyncImage(
                model = fullscreenRequest,
                placeholder = painterResource(R.drawable.ic_image_placeholder),
                error = painterResource(R.drawable.ic_image_error),
                contentDescription = stringResource(R.string.fullscreen_view),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y + dragOffsetY
                    }
                    .pointerInput(imageUrl) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = if (scale > 1f) 1f else 2.5f
                                offset = Offset.Zero
                            }
                        )
                    }
                    .pointerInput(imageUrl) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            offset = if (newScale > 1f) {
                                offset + pan
                            } else {
                                Offset.Zero
                            }
                        }
                    }
                    // Swipe-to-dismiss: only at normal zoom with a single finger. Placed inside the
                    // transform detector so it sees moves first; once it consumes them, the transform
                    // gesture cancels, while pinches (2+ pointers) are left untouched.
                    .pointerInput(imageUrl) {
                        val dismissThresholdPx = 150.dp.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (scale > 1.05f || isDismissing) return@awaitEachGesture
                            var isDragging = false
                            var slopY = 0f
                            while (true) {
                                val event = awaitPointerEvent()
                                if (!isDragging && event.changes.count { it.pressed } > 1) {
                                    return@awaitEachGesture
                                }
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) break
                                val deltaY = change.positionChange().y
                                if (!isDragging) {
                                    slopY += deltaY
                                    isDragging = slopY.absoluteValue > viewConfiguration.touchSlop
                                }
                                if (isDragging) {
                                    dragOffsetY += deltaY
                                    change.consume()
                                }
                            }
                            if (!isDragging) return@awaitEachGesture
                            if (dragOffsetY.absoluteValue > dismissThresholdPx) {
                                isDismissing = true
                                val exitTarget = size.height * dragOffsetY.sign
                                scope.launch {
                                    animate(
                                        initialValue = dragOffsetY,
                                        targetValue = exitTarget,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ) { value, _ -> dragOffsetY = value }
                                    onDismiss()
                                }
                            } else {
                                scope.launch {
                                    animate(
                                        initialValue = dragOffsetY,
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) { value, _ -> dragOffsetY = value }
                                }
                            }
                        }
                    }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (!isSaving) onSaveToGallery() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = stringResource(R.string.save_to_gallery),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
