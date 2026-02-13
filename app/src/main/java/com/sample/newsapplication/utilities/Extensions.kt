package com.sample.newsapplication.utilities

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.shimmer

@Composable
fun Modifier.shimmerable(
    enabled: Boolean,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    shape: Shape = RoundedCornerShape(8.dp),
): Modifier {
    if (!enabled) return this

    return this
        .shimmer() // 3rd party library call
        .background(color = color, shape = shape)
        .drawWithContent {
            // Do not draw the actual content.
        }
}