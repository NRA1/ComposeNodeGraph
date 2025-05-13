import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {

    val nodes = remember { mutableStateListOf<NodeState>() }

    val node1 = NodeState(
        text = "Node1",
        inputs = listOf(),
        outputs = listOf(),
        position = DpOffset(x = 100.dp, y = 100.dp),
    )
    val output = OutputState(
        parent = node1,
        label = "Output",
        1
    )
    node1.outputs.add(output)
    nodes.add(node1)

    val node2 =
        NodeState(
            text = "Node2",
            inputs = listOf(),
            outputs = listOf(),
            position = DpOffset(x = 200.dp, y = 200.dp)
        )

    val input = InputState(
        parent = node2,
        label = "Input",
        1
    )
    node2.inputs.add(input)

    val output2 = OutputState(
        parent = node2,
        label = "Output2",
        2,
    )
    node2.outputs.add(output2)
    nodes.add(node2)
    
    val node3 = NodeState(
        text = "Node3",
        inputs = listOf(),
        outputs = listOf(),
        position = DpOffset(x = 100.dp, y = 300.dp)
    )
    val input2 = InputState(
        parent = node3,
        label = "Input2",
        2
    )
    node3.inputs.add(input2)
    nodes.add(node3)

    data class DraggedConnection(
        val source: OutputState,
        val point: Offset
    )
    var draggedConnection by mutableStateOf<DraggedConnection?>(null)

    val density = LocalDensity.current

    var viewportOffset by remember { mutableStateOf(Offset.Zero) }

    val colorScheme = MaterialTheme.colorScheme

    var scale by remember { mutableStateOf(1f) }

    MaterialTheme(darkColorScheme()) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onDrag { offset ->
                        viewportOffset += offset
                    }
                    .onPointerEvent(PointerEventType.Scroll) { event ->
                        val newScale = max(0.1f, min(3f, scale + event.changes[0].scrollDelta.y / 10))
                        if(scale == newScale) return@onPointerEvent

                        val oldScaledWidth = size.width * scale
                        val oldScaledHeight = size.height * scale
                        val newScaledWidth = size.width * newScale
                        val newScaledHeight = size.height * newScale

                        val xDelta = (oldScaledWidth - newScaledWidth) / 2
                        val yDelta = (oldScaledHeight - newScaledHeight) / 2

//                        viewportOffset -= Offset(xDelta, yDelta)

                        scale = newScale
                    }
                    .pointerHoverIcon(
                        if(draggedConnection != null) PointerIcon.Crosshair
                        else PointerIcon.Default
                    )
                    .drawWithCache {
                        onDrawBehind {
                            val scaleOffsetX = (size.width - (size.width * scale)) / 2
                            val scaleOffsetY = (size.height - (size.height * scale)) / 2

                            val dp100 = 500.dp.toPx() * scale

                            val xMod = ((viewportOffset.x + scaleOffsetX) % dp100).roundToInt()
                            val xCount = ceil(size.width / dp100).toInt()

                            for(ix in 0..xCount)
                                drawLine(
                                    color = colorScheme.outline,
                                    start = Offset(ix * dp100 + xMod, 0f),
                                    end = Offset(ix * dp100 + xMod, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )

                            val yMod = ((viewportOffset.y + scaleOffsetY) % dp100).roundToInt()
                            val yCount = ceil(size.height / dp100).toInt()

                            for(iy in 0..yCount)
                                drawLine(
                                    color = colorScheme.outline,
                                    start = Offset(0f, iy * dp100 + yMod),
                                    end = Offset(size.width, iy * dp100 + yMod),
                                    strokeWidth = 1.dp.toPx()
                                )
                        }
                    }
                    .absoluteOffset { viewportOffset.round() }
                    .scale(scale)
            ) {
                Box(
                    modifier = Modifier
                        .drawWithCache {
                            onDrawBehind {
                                nodes.map { node ->
                                    node.outputs.map { output ->
                                        output.targets.map { output to it }
                                    }
                                }.flatten().flatten().forEach { (output, input) ->

                                    val outputPointOffset = output.connectionPointOffset
                                    val inputPointOffset = input.connectionPointOffset
                                    if(outputPointOffset == null || inputPointOffset == null) return@forEach

                                    val startPos = output.parent.getPxOffset(density) + outputPointOffset
                                    val endPos = input.parent.getPxOffset(density) + inputPointOffset

                                    val distance = (startPos - endPos).getDistance()

                                    drawPath(
                                        path = Path().apply {
                                            moveTo(startPos.x, startPos.y)
                                            cubicTo(
                                                x1 = startPos.x + distance / 3,
                                                y1 = startPos.y,
                                                x2 = endPos.x - distance / 3,
                                                y2 = endPos.y,
                                                x3 = endPos.x,
                                                y3 = endPos.y
                                            )
                                        },
                                        color = ColorScheme.forId(output.type),
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                }

                                draggedConnection?.let { connection ->
                                    val outputPointOffset = connection.source.connectionPointOffset

                                    if(outputPointOffset == null) return@let

                                    val startPos = connection.source.parent.getPxOffset(density) + outputPointOffset


                                    drawPath(
                                        path = Path().apply {
                                            moveTo(startPos.x, startPos.y)

                                            cubicTo(
                                                x1 = startPos.x + (startPos - connection.point).getDistance() / 3,
                                                y1 = startPos.y,
                                                x2 = connection.point.x,
                                                y2 = connection.point.y,
                                                x3 = connection.point.x,
                                                y3 = connection.point.y
                                            )
                                        },
                                        color = ColorScheme.forId(connection.source.type),
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                }
                            }
                        }
                ) {
                    for (node in nodes) {
                        Node(
                            state = node,
                            onConnectorDrag = { connector, offset ->
                                draggedConnection.let { connection ->
                                    if(connection == null) {
                                        draggedConnection = when(connector) {
                                            is InputState -> {
                                                val source = connector.source
                                                if(source == null) null
                                                else {
                                                    connector.source = null
                                                    source.targets.remove(connector)
                                                    DraggedConnection(
                                                        source = source,
                                                        point = connector.parent.getPxOffset(density) + connector.connectionPointOffset!! + offset
                                                    )
                                                }
                                            }
                                            is OutputState -> DraggedConnection(
                                                source = connector,
                                                point = connector.parent.getPxOffset(density) + connector.connectionPointOffset!! + offset
                                            )
                                        }
                                    } else {
                                        draggedConnection = connection.copy(
                                            point = connection.point + offset
                                        )
                                    }
                                }
                            },
                            onDragEnd = {
                                val maxDistance = with(density) { Offset(x = 5.dp.toPx(), y = 5.dp.toPx()) }.getDistanceSquared()

                                draggedConnection?.let { connection ->
                                    val target = nodes.map { it.inputs }.flatten().firstOrNull() {
                                        ((it.parent.getPxOffset(density) + it.connectionPointOffset!!) - connection.point)
                                            .getDistanceSquared() <= maxDistance
                                    }

                                    if (target != null && target.type == connection.source.type) {
                                        target.source = connection.source
                                        connection.source.targets.add(target)
                                    }
                                }

                                draggedConnection = null
                            },
                            scale = scale
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Node(state: NodeState, onConnectorDrag: (ConnectorState, Offset) -> Unit, onDragEnd: () -> Unit, scale: Float) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .absoluteOffset(x = state.position.x, y = state.position.y)
    ) {
        var nodeRootPos: Offset? = null

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
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

                    val maxNum = max(state.inputs.size, state.outputs.size)

                    for(i in 0 until maxNum) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
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

                            ConnectorIfExists(state.inputs, i)
                            if(state.inputs.size > i && state.outputs.size > i) Spacer(Modifier.width(10.dp))
                            ConnectorIfExists(state.outputs, i)
                        }
                    }
                }
            }
        }
    }
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
        Text(state.label)
        if(state is OutputState) Point()
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

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}