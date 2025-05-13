import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

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
        type = 1,
        widget = DropdownWidgetState(
            options = listOf(
                "Option1",
                "Option2",
                "Option3"
            )
        )
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


    MaterialTheme(darkColorScheme()) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
                .clickable(
                    interactionSource = null,
                    indication = null,
                ) {}
        ) {
            Graph(
                nodes = nodes,
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}