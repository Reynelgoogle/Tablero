package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val MesaForest = Color(0xFF1D6B52)
val MesaForestLight = Color(0xFF63C39D)
val MesaForestDark = Color(0xFF175842)
val MesaForestContainer = Color(0xFFDCECE5)
val MesaForestDarkContainer = Color(0xFF1D3A2F)

// Light Theme
val LightBg = Color(0xFFF4F1EA)
val LightSurface = Color(0xFFFDFCFA)
val LightColumn = Color(0xFFEBE7DF)
val LightInk = Color(0xFF1C241F)
val LightInkSecondary = Color(0xFF53625A)
val LightBorder = Color(0x1F1C3026)

// Dark Theme
val DarkBg = Color(0xFF101513)
val DarkSurface = Color(0xFF1A221E)
val DarkColumn = Color(0xFF141B17)
val DarkInk = Color(0xFFE9EFE9)
val DarkInkSecondary = Color(0xFFA7B6AC)
val DarkBorder = Color(0x26D6EEE0)

// Helper to parse hex string safely
fun parseHexColor(hex: String, defaultColor: Color = MesaForest): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(colorInt or 0x00000000FF000000)
        } else if (clean.length == 8) {
            Color(colorInt)
        } else {
            defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}
