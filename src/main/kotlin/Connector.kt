import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

sealed class ConnectorState(
    val parent: NodeState,
    label: String,
    val type: Int
) {
    var label: String by mutableStateOf(label)
    var connectionPointOffset: Offset? by mutableStateOf(null)
}

class InputState(
    parent: NodeState,
    label: String,
    type: Int,
    widget: InputWidgetState? = null,
) : ConnectorState(parent, label, type) {
    var source: OutputState? by mutableStateOf(null)

    var widget: InputWidgetState? by mutableStateOf(widget)
}

class OutputState(
    parent: NodeState,
    label: String,
    type: Int
) : ConnectorState(parent, label, type) {
    val targets: SnapshotStateList<InputState> = mutableStateListOf()
}

@Composable
fun Connector(state: ConnectorState, onPointPositioned: (Offset) -> Unit, onPointDrag: (Offset) -> Unit, onPointDragEnd: () -> Unit, modifier: Modifier = Modifier) {

    @Composable
    fun Point()
    {
        ConnectionPoint(
            connectorType = state.type,
            onPositioned = onPointPositioned,
            onDrag = onPointDrag,
            onDragEnd = onPointDragEnd
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if(state is InputState) Point()
        Text(
            text = state.label,
            style = LocalTextStyle.current
        )
        if(state is OutputState) Point()
    }
}

@Composable
fun WidgetConnector(state: InputState, onPointPositioned: (Offset) -> Unit, onPointDrag: (Offset) -> Unit, onPointDragEnd: () -> Unit, modifier: Modifier = Modifier) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        ConnectionPoint(
            connectorType = state.type,
            onPositioned = onPointPositioned,
            onDrag = onPointDrag,
            onDragEnd = onPointDragEnd
        )

        val enabled = state.source == null

        when(val widget = state.widget!!) {
            is TextWidgetState -> {
                TextWidget(
                    enabled = enabled,
                    state = widget,
                )
            }

            is IntWidgetState -> {
                IntWidget(
                    enabled = enabled,
                    label = state.label,
                    state = widget,
                )
            }

            is FloatWidgetState -> {
                FloatWidget(
                    enabled = enabled,
                    label = state.label,
                    state = widget,
                )
            }

            is DropdownWidgetState -> {
                DropdownWidget(
                    enabled = enabled,
                    label = state.label,
                    state = widget
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectionPoint(connectorType: Int, onPositioned: (Offset) -> Unit, onDrag: (Offset) -> Unit, onDragEnd: () -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .size(10.dp)
            .onGloballyPositioned { coordinates ->
                val rootOffset = coordinates.localToRoot(
                    with(density) {
                        Offset(x = 5.dp.toPx(), y = 5.dp.toPx())
                    }
                )
                onPositioned(rootOffset)
            }
            .onDrag(
                onDrag = onDrag,
                onDragEnd = onDragEnd
            )
            .drawBehind {
                drawCircle(
                    color = ColorScheme.forId(connectorType)
                )
            }
    )
}
