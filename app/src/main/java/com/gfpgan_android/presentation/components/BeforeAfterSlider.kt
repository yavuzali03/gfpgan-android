package com.gfpgan_android.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlin.math.max

/**
 * Before/After comparison slider
 * Drag to compare original vs processed image
 */
@Composable
fun BeforeAfterSlider(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                viewSize = coordinates.size
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val newPosition = change.position.x / size.width
                        sliderPosition = newPosition.coerceIn(0f, 1f)
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Calculate scale for CenterCrop (Aspect Fill)
            val imageWidth = beforeBitmap.width.toFloat()
            val imageHeight = beforeBitmap.height.toFloat()
            
            val scaleX = canvasWidth / imageWidth
            val scaleY = canvasHeight / imageHeight
            val scale = maxOf(scaleX, scaleY)
            
            val scaledWidth = imageWidth * scale
            val scaledHeight = imageHeight * scale
            
            val leftOffset = (canvasWidth - scaledWidth) / 2
            val topOffset = (canvasHeight - scaledHeight) / 2
            
            // Destination rect for crop/fill drawing
            val dstOffset = IntOffset(leftOffset.toInt(), topOffset.toInt())
            val dstSize = IntSize(scaledWidth.toInt(), scaledHeight.toInt())
            
            // Draw "After" image (full)
            drawImage(
                image = afterBitmap.asImageBitmap(),
                dstOffset = dstOffset,
                dstSize = dstSize
            )
            
            // Draw "Before" image (clipped to slider position)
            clipRect(
                left = 0f,
                top = 0f,
                right = canvasWidth * sliderPosition,
                bottom = canvasHeight
            ) {
                drawImage(
                    image = beforeBitmap.asImageBitmap(),
                    dstOffset = dstOffset,
                    dstSize = dstSize
                )
            }
            
            // Draw divider line
            val dividerX = canvasWidth * sliderPosition
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(dividerX, 0f),
                end = Offset(dividerX, canvasHeight),
                strokeWidth = 8f // Widened line
            )
        }
        
        // --- SLIDER HANDLE ---
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(48.dp)
                .offset { 
                    IntOffset(
                        x = (viewSize.width * sliderPosition - 24.dp.toPx()).toInt(), 
                        y = 0 
                    ) 
                },
            contentAlignment = Alignment.Center
        ) {
            // Background Circles
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, shape = androidx.compose.foundation.shape.CircleShape)
                    .padding(2.dp)
                    .background(Color.Black, shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    FontAwesomeIcon(
                        iconCode = "\uf053", // Chevron Left
                        size = 12.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    FontAwesomeIcon(
                        iconCode = "\uf054", // Chevron Right
                        size = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
        
        // Labels
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Before",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            
            Text(
                text = "After",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
