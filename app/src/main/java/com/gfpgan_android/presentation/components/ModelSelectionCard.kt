package com.gfpgan_android.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gfpgan_android.presentation.components.FontAwesomeIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionCard(
    title: String,
    subtitle: String,
    iconCode: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedGradientColors: List<androidx.compose.ui.graphics.Color>? = null
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected && selectedGradientColors != null) {
                androidx.compose.ui.graphics.Brush.linearGradient(selectedGradientColors)
            } else if (isSelected) {
                androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            } else {
                 androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
            }
        ),
        modifier = modifier.height(100.dp),
        enabled = enabled
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FontAwesomeIcon(
                iconCode = iconCode,
                size = 28.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
