package com.aistudio.couplebubble.qxztrw.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.data.LocalImageStorage
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ProfilePhotoCropDialog(
    imageUri: Uri?,
    preloadedBitmap: Bitmap? = null,
    onDismiss: () -> Unit,
    onCropConfirmed: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf(preloadedBitmap) }
    var isLoading by remember { mutableStateOf(preloadedBitmap == null) }
    var isProcessingCrop by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(imageUri) {
        if (preloadedBitmap != null) {
            sourceBitmap = preloadedBitmap
            isLoading = false
            return@LaunchedEffect
        }
        if (imageUri != null) {
            isLoading = true
            val loaded = LocalImageStorage.loadSampledBitmapFromUri(context, imageUri, maxDimension = 1200)
            sourceBitmap = loaded
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isProcessingCrop) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.crop_photo_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isProcessingCrop
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.crop_photo_instruction),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Viewfinder / Crop Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .onGloballyPositioned { coordinates ->
                            viewportSize = coordinates.size
                        }
                        .pointerInput(sourceBitmap) {
                            if (sourceBitmap != null && !isProcessingCrop) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1.0f, 4.0f)
                                    panOffset = Offset(
                                        x = panOffset.x + pan.x,
                                        y = panOffset.y + pan.y
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = sourceBitmap
                    if (bitmap != null && viewportSize.width > 0 && viewportSize.height > 0) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            // Draw original image scaled & panned
                            val baseScale = max(
                                canvasWidth / bitmap.width.toFloat(),
                                canvasHeight / bitmap.height.toFloat()
                            )
                            val finalScale = baseScale * scale
                            val scaledWidth = bitmap.width * finalScale
                            val scaledHeight = bitmap.height * finalScale

                            val centerOffsetX = (canvasWidth - scaledWidth) / 2f + panOffset.x
                            val centerOffsetY = (canvasHeight - scaledHeight) / 2f + panOffset.y

                            // Viewfinder circle in the center
                            val radius = min(canvasWidth, canvasHeight) * 0.45f
                            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

                            // Draw Image inside clipping path
                            val circlePath = Path().apply {
                                addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
                            }

                            // 1. Draw image across full area
                            val srcRect = androidx.compose.ui.unit.IntRect(0, 0, bitmap.width, bitmap.height)
                            val dstRect = androidx.compose.ui.unit.IntRect(
                                centerOffsetX.roundToInt(),
                                centerOffsetY.roundToInt(),
                                (centerOffsetX + scaledWidth).roundToInt(),
                                (centerOffsetY + scaledHeight).roundToInt()
                            )

                            drawImage(
                                image = bitmap.asImageBitmap(),
                                srcOffset = IntOffset(srcRect.left, srcRect.top),
                                srcSize = IntSize(srcRect.width, srcRect.height),
                                dstOffset = IntOffset(dstRect.left, dstRect.top),
                                dstSize = IntSize(dstRect.width, dstRect.height)
                            )

                            // 2. Dim outside circle
                            val dimPath = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
                                addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
                            }
                            drawPath(dimPath, color = Color.Black.copy(alpha = 0.65f))

                            // 3. Crisp circular border
                            drawCircle(
                                color = Color(0xFFE65D2E), // Sunset Terracotta accent
                                radius = radius,
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                            )
                        }
                    } else if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.secondary,
                            strokeWidth = 3.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Zoom Slider & Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 1.0f..4.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            scale = 1.0f
                            panOffset = Offset.Zero
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isProcessingCrop
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val bitmap = sourceBitmap ?: return@Button
                            if (viewportSize.width <= 0 || viewportSize.height <= 0) return@Button

                            isProcessingCrop = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            scope.launch(Dispatchers.Default) {
                                val croppedBytes = renderCroppedBitmapBytes(
                                    bitmap = bitmap,
                                    scale = scale,
                                    panOffset = panOffset,
                                    viewportSize = viewportSize
                                )
                                withContext(Dispatchers.Main) {
                                    isProcessingCrop = false
                                    if (croppedBytes != null) {
                                        onCropConfirmed(croppedBytes)
                                    } else {
                                        onDismiss()
                                    }
                                }
                            }
                        },
                        enabled = sourceBitmap != null && !isProcessingCrop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (isProcessingCrop) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.crop_photo_confirm))
                    }
                }
            }
        }
    }
}

/**
 * Computes and renders the circular viewport area into a crisp, high-resolution square Bitmap
 * and compresses it to JPEG ByteArray (< 200 KB).
 */
private fun renderCroppedBitmapBytes(
    bitmap: Bitmap,
    scale: Float,
    panOffset: Offset,
    viewportSize: IntSize,
    targetDimension: Int = 512
): ByteArray? {
    return try {
        val canvasWidth = viewportSize.width.toFloat()
        val canvasHeight = viewportSize.height.toFloat()

        val baseScale = max(
            canvasWidth / bitmap.width.toFloat(),
            canvasHeight / bitmap.height.toFloat()
        )
        val finalScale = baseScale * scale
        val scaledWidth = bitmap.width * finalScale
        val scaledHeight = bitmap.height * finalScale

        val centerOffsetX = (canvasWidth - scaledWidth) / 2f + panOffset.x
        val centerOffsetY = (canvasHeight - scaledHeight) / 2f + panOffset.y

        val radius = min(canvasWidth, canvasHeight) * 0.45f
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f

        // Bounding box of the circle on viewport coordinates:
        val circleLeft = centerX - radius
        val circleTop = centerY - radius
        val circleDiameter = radius * 2f

        // Map back to original bitmap coordinates
        val cropXInBitmap = ((circleLeft - centerOffsetX) / finalScale).coerceIn(0f, bitmap.width.toFloat())
        val cropYInBitmap = ((circleTop - centerOffsetY) / finalScale).coerceIn(0f, bitmap.height.toFloat())
        val cropWidthInBitmap = (circleDiameter / finalScale).coerceAtMost(bitmap.width - cropXInBitmap)
        val cropHeightInBitmap = (circleDiameter / finalScale).coerceAtMost(bitmap.height - cropYInBitmap)

        val finalCropSize = min(cropWidthInBitmap, cropHeightInBitmap).roundToInt().coerceAtLeast(1)
        val finalX = cropXInBitmap.roundToInt().coerceIn(0, bitmap.width - finalCropSize)
        val finalY = cropYInBitmap.roundToInt().coerceIn(0, bitmap.height - finalCropSize)

        val cropped = Bitmap.createBitmap(bitmap, finalX, finalY, finalCropSize, finalCropSize)
        val scaledResult = if (finalCropSize != targetDimension) {
            Bitmap.createScaledBitmap(cropped, targetDimension, targetDimension, true).also {
                if (it != cropped) cropped.recycle()
            }
        } else {
            cropped
        }

        val output = ByteArrayOutputStream()
        scaledResult.compress(Bitmap.CompressFormat.JPEG, 85, output)
        scaledResult.recycle()
        output.toByteArray()
    } catch (_: Exception) {
        null
    }
}

@Preview(name = "Crop Dialog Preview")
@Composable
private fun ProfilePhotoCropDialogPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        val sampleBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.DKGRAY)
        }
        ProfilePhotoCropDialog(
            imageUri = null,
            preloadedBitmap = sampleBitmap,
            onDismiss = {},
            onCropConfirmed = {}
        )
    }
}
