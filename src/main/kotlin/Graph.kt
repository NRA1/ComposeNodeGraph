import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun Graph(nodes: List<NodeState>, viewportOffset: Offset, viewportOffsetChange: (Offset) -> Unit,
          onNodeDelete: (NodeState) -> Unit, modifier: Modifier = Modifier) {

    data class DraggedConnection(
        val source: OutputState,
        val point: Offset
    )
    var draggedConnection by remember { mutableStateOf<DraggedConnection?>(null) }

    val density = LocalDensity.current

    val colorScheme = MaterialTheme.colorScheme

    var scale by remember { mutableStateOf(1f) }

    Box(
        modifier = modifier
            .onDrag { offset ->
                viewportOffsetChange(viewportOffset + offset)
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val newScale = max(0.1f, min(3f, scale + event.changes[0].scrollDelta.y / 10))
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

                    val dp100 = 100.dp.toPx() * scale

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
                                color = ColorScheme.forId(output.type.hashCode()),
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
                                color = ColorScheme.forId(connection.source.type.hashCode()),
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
                    scale = scale,
                    onDelete = { onNodeDelete(node) }
                )
            }
        }
    }
}