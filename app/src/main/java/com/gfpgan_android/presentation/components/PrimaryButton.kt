package com.gfpgan_android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    gradientColors: List<Color> = listOf(Color(0xFF054C76), Color(0xFF471C3A))
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(4.dp) // Space for glow
            .drawBehind {
                if (enabled) {
                    this.drawIntoCanvas { canvas ->
                        val paint = Paint()
                        val frameworkPaint = paint.asFrameworkPaint()
                        
                        // Create Gradient Shader for the Glow
                        frameworkPaint.shader = android.graphics.LinearGradient(
                            0f, 0f, size.width, 0f,
                            gradientColors.map { it.toArgb() }.toIntArray(),
                            null,
                            android.graphics.Shader.TileMode.CLAMP
                        )
                        
                        // Static Blur for Neon Effect
                        frameworkPaint.maskFilter = android.graphics.BlurMaskFilter(
                            30f, // Static blur radius
                            android.graphics.BlurMaskFilter.Blur.NORMAL
                        )
                        
                        // Draw glow
                        canvas.drawRoundRect(
                            left = -4f, // Slight spread
                            top = -4f,
                            right = size.width + 4f,
                            bottom = size.height + 4f,
                            radiusX = 16.dp.toPx(),
                            radiusY = 16.dp.toPx(),
                            paint = paint
                        )
                    }
                }
            }
            .background(
                brush = if (enabled) Brush.horizontalGradient(
                    colors = gradientColors
                ) else SolidColor(Color.Gray),
                shape = RoundedCornerShape(16.dp)
            ),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
