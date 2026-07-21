package com.devpulse.ai.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.ui.theme.BackgroundDark
import com.devpulse.ai.ui.theme.BorderDark
import com.devpulse.ai.ui.theme.Primary
import com.devpulse.ai.ui.theme.Secondary
import com.devpulse.ai.ui.theme.SurfaceDark

@Composable
fun BrandBackground(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundDark,
                        Color(0xFF060709)
                    )
                )
            )
    ) {
        // Soft glowing mesh orbs inspired by Linear App
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x18BB86FC), // Soft neon purple glow
                            Color.Transparent
                        ),
                        center = Offset(200f, 0f),
                        radius = 1000f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x1003DAC6), // Soft neon cyan glow
                            Color.Transparent
                        ),
                        center = Offset(800f, 200f),
                        radius = 900f
                    )
                )
        )
        content()
    }
}

@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark.copy(alpha = 0.85f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    BorderDark,
                    BorderDark.copy(alpha = 0.3f)
                )
            )
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif
        ),
        placeholder = {
            Text(
                text = placeholder,
                color = Color(0xFF64748B),
                fontSize = 16.sp
            )
        },
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0xFF090A0E),
            unfocusedContainerColor = Color(0xFF090A0E),
            focusedBorderColor = Primary,
            unfocusedBorderColor = BorderDark,
            cursorColor = Primary
        )
    )
}

@Composable
fun BrandButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val gradientBrush = if (enabled) {
        Brush.linearGradient(
            colors = listOf(Primary, Secondary)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF1E212E), Color(0xFF1E212E))
        )
    }

    val textColor = if (enabled) BackgroundDark else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(gradientBrush)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        )
    }
}
