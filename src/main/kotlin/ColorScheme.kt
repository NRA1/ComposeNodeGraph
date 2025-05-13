import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

object ColorScheme {
    private val colorsList: List<Color> = listOf(
        darkColorScheme().primary,
        darkColorScheme().secondary,
        darkColorScheme().tertiary,
        darkColorScheme().error,
    )

    fun forId(id: Int): Color = colorsList[abs(id % colorsList.size)]
}