package `in`.getdownfoundation.sahusales.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF1565C0)
val PrimaryDark = Color(0xFF0D47A1)
val Background = Color(0xFFFFFFFF)
val Surface = Color(0xFFF8FAFC)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val Border = Color(0xFFE2E8F0)

val StatusUpcoming = Color(0xFF22C55E)
val StatusSnoozed = Color(0xFFEAB308)
val StatusOverdue = Color(0xFFEF4444)
val StatusArchived = Color(0xFF94A3B8)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    background = Background,
    surface = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Border,
    error = Color(0xFFEF4444)
)

@Composable
fun SahuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
