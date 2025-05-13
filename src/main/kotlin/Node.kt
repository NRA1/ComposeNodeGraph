import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.math.max

class NodeState(
    title: String,
    inputs: List<InputState>,
    outputs: List<OutputState>,
    position: DpOffset = DpOffset.Zero
) {
    var title: String by mutableStateOf(title)
    val inputs: SnapshotStateList<InputState> = mutableStateListOf(*inputs.toTypedArray())
    val outputs: SnapshotStateList<OutputState> = mutableStateListOf(*outputs.toTypedArray())

    var position: DpOffset by mutableStateOf(position)

    private var _pxOffset: Offset? = null
    private var _pxOffsetFor: DpOffset? = null
    fun getPxOffset(density: Density): Offset {
        val pxOffset = _pxOffset
        if(pxOffset == null || _pxOffsetFor != position) {
            val offset = with(density) {
                Offset(
                    x = position.x.toPx(), y = position.y.toPx()
                )
            }
            _pxOffset = offset
            _pxOffsetFor = position
            return offset
        }
        return pxOffset
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Node(state: NodeState, onDelete: () -> Unit, onConnectorDrag: (ConnectorState, Offset) -> Unit, onDragEnd: () -> Unit, scale: Float) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .absoluteOffset(x = state.position.x, y = state.position.y)
    ) {
        var nodeRootPos: Offset? = null

        var interactionSource = remember { MutableInteractionSource() }
        val focused by interactionSource.collectIsFocusedAsState()

        Surface(
            border = if(focused) BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary
            ) else null,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .focusable(interactionSource = interactionSource)
                .clickable(interactionSource = interactionSource, indication = null) {  }
                .onDrag(
                    onDrag = { offset ->
                        state.position += DpOffset(
                            x = with(density) { offset.x.toDp() },
                            y = with(density) { offset.y.toDp() }
                        )
                    }
                )
                .pointerHoverIcon(PointerIcon.Crosshair)
                .onGloballyPositioned { coordinates ->
                    val rootOffset = coordinates.localToRoot(Offset.Zero)
                    nodeRootPos = rootOffset
                }
                .onKeyEvent { event ->
                    if(event.key == Key.Delete) {
                        if(event.type == KeyEventType.KeyDown)
                            onDelete()
                        true
                    } else false
                }
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(5.dp)
                ) {
                    Text(text = state.title, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                Column(
                    modifier = Modifier
                        .padding(start = 5.dp, bottom = 5.dp, end = 5.dp)
                ) {

                    val inputsWoWidgets = state.inputs.filter { it.widget == null }

                    val maxNum = max(inputsWoWidgets.size, state.outputs.size)

                    for(i in 0 until maxNum) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            @Composable
                            fun ConnectorIfExists(list: List<ConnectorState>, i: Int) {
                                if(list.size > i) {
                                    val connector = list[i]

                                    Connector(
                                        state = connector,
                                        onPointPositioned = { offset ->
                                            connector.connectionPointOffset = (offset - nodeRootPos!!) / scale
                                        },
                                        onPointDrag = { offset ->
                                            onConnectorDrag(connector, offset)
                                        },
                                        onPointDragEnd = onDragEnd
                                    )
                                } else {
                                    Spacer(Modifier)
                                }
                            }

                            ConnectorIfExists(inputsWoWidgets, i)
                            if(inputsWoWidgets.size > i && state.outputs.size > i) Spacer(Modifier.width(10.dp))
                            ConnectorIfExists(state.outputs, i)
                        }
                    }

                    val widgetInputs = state.inputs.filter { it.widget != null }
                    for(input in widgetInputs) {
                        WidgetConnector(
                            input,
                            onPointPositioned = { offset ->
                                input.connectionPointOffset = (offset - nodeRootPos!!) / scale
                            },
                            onPointDrag = { offset ->
                                onConnectorDrag(input, offset)
                            },
                            onPointDragEnd = onDragEnd
                        )
                    }
                }
            }
        }
    }
}