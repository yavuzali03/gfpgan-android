package com.gfpgan_android.presentation.components

import android.graphics.Typeface
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun FontAwesomeIcon(
    iconCode: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    size: TextUnit = 24.sp
) {
    val context = LocalContext.current
    
    // Remember the FontFamily to avoid reloading it on every recomposition
    val fontAwesomeFamily = remember {
        try {
            val typeface = Typeface.createFromAsset(context.assets, "icons/Font Awesome 7 Free-Solid-900.otf")
            FontFamily(androidx.compose.ui.text.font.Typeface(typeface))
        } catch (e: Exception) {
            e.printStackTrace()
            FontFamily.Default
        }
    }

    Text(
        text = iconCode,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontFamily = fontAwesomeFamily
    )
}
