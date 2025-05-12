import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlin.math.max

@Composable
@Preview
fun App() {

    val nodes = remember { mutableStateListOf<NodeState>() }

    val node1 = NodeState(
        text = "Node1",
        inputs = listOf(),
        outputs = listOf(),
        position = DpOffset(x = 100.dp, y = 100.dp),
    );
    val output = OutputState(
        parent = node1,
        label = "Output"
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
        label = "Input"
    )
    node2.inputs.add(input)
    nodes.add(node2)

    input.source = output
    output.target = input

    MaterialTheme {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawBehind {
                        nodes.map { node ->
                            node.outputs.mapNotNull { output ->
                                output.target?.let { output to it }
                            }
                        }.flatten().forEach { (output, input) ->

                            val outputPointOffset = output.connectionPointOffset
                            val inputPointOffset = input.connectionPointOffset
                            if(outputPointOffset == null || inputPointOffset == null) return@forEach

                            drawLine(
                                color = Color.Magenta,
                                start = Offset(
                                    x = output.parent.position.x.toPx(), y = output.parent.position.y.toPx()
                                ) + outputPointOffset,
                                end = Offset(
                                    x = input.parent.position.x.toPx(), y = input.parent.position.y.toPx()
                                ) + inputPointOffset,
                            )
                        }
                    }
                }
        ) {
            for (node in nodes) {
                Node(state = node)
            }
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Node(state: NodeState, onConnectorDrag: (ConnectorState, Offset) -> Unit) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .offset(x = state.position.x, y = state.position.y)
    ) {
        var nodeRootPos: Offset? = null

        Box(
            modifier = Modifier
                .background(Color.Blue)
                .onDrag(
                    onDrag = { offset ->
                        state.position += DpOffset(
                            x = with(density) { offset.x.toDp() },
                            y = with(density) { offset.y.toDp() }
                        )
                    }
                )
                .onGloballyPositioned { coordinates ->
                    val rootOffset = coordinates.localToRoot(Offset.Zero)
                    nodeRootPos = rootOffset
                }
        ) {
            Column {
                Text(state.title)

                val maxNum = max(state.inputs.size, state.outputs.size)

                for(i in 0 until maxNum) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if(state.inputs.size > i) {
                            val input = state.inputs[i]

                            InputConnector(
                                state = input,
                                onPointPositioned = { offset ->
                                    input.connectionPointOffset = offset - nodeRootPos!!
                                }
                            )
                        } else {
                            Spacer(Modifier)
                        }

                        if(state.outputs.size > i) {
                            val output = state.outputs[i]

                            OutputConnector(
                                state = output,
                                onPointPositioned = { offset ->
                                     output.connectionPointOffset = offset - nodeRootPos!!
                                }
                            )
                        } else {
                            Spacer(Modifier)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InputConnector(state: InputState, onPointPositioned: (Offset) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        ConnectionPoint(
            onPositioned = onPointPositioned,
            onDrag = {}
        )
        Text(state.label)
    }
}

@Composable
fun OutputConnector(state: OutputState, onPointPositioned: (Offset) -> Unit, onPointDragged: (Offset) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(state.label)
        ConnectionPoint(
            onPositioned = onPointPositioned,
            onDrag = onPointDragged
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectionPoint(onPositioned: (Offset) -> Unit, onDrag: (Offset) -> Unit, modifier: Modifier = Modifier) {
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
            .onDrag(onDrag = onDrag)
            .drawBehind {
                drawCircle(
                    color = Color.Red
                )
            }
    )
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}