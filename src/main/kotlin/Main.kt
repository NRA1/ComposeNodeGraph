import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import java.io.File

data class NodeType(
    val type: String?,
    val widget: InputWidgetState?
)

fun parseNodeInput(input: List<JsonElement>): NodeType {
    val type = input[0]

    if(type is JsonPrimitive && type.isString) {
        val typeName = type.content

        if(typeName == "INT") {
            return NodeType(
                typeName,
                IntWidgetState()
            )
        } else if(typeName == "FLOAT") {
            return NodeType(
                typeName,
                FloatWidgetState()
            )
        } else if(typeName == "STRING") {
            return NodeType(
                typeName,
                TextWidgetState()
            )
        } else {
            return NodeType(
                typeName,
                null
            )
        }
    } else if(type is JsonArray) {
        val options = type.map { item -> item.jsonPrimitive.content }

        return NodeType(
            null,
            DropdownWidgetState(
                options
            )
        )
    } else throw NotImplementedError()
}

fun createNode(info: NodeInfo, position: DpOffset): NodeState {
    val node = NodeState(
        title = info.displayName,
        inputs = listOf(),
        outputs = listOf(),
        position = position
    )

    fun addInput(list: List<JsonElement>, name: String) {
        val input = parseNodeInput(list)

        val connector = InputState(
            parent = node,
            label = name,
            type = input.type ?: "___INVALID___", //TODO
            widget = input.widget,
        )
        node.inputs.add(connector)
    }

    for(inputName in info.inputOrder.required) {
        val list = info.input.required[inputName]!!
        addInput(list, inputName)
    }

    for(inputName in info.inputOrder.optional) {
        val list = info.input.optional[inputName]!!
        addInput(list, inputName)
    }

    for((idx, outputsList) in info.output.withIndex()) {
        for(output in outputsList) {
            val name = info.outputName[idx]

            val connector = OutputState(
                parent = node,
                label = name,
                type = output,
            )
            node.outputs.add(connector)
        }
    }

    return node
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App(nodeCatalog: Map<String, NodeInfo>) {

    val nodes = remember { mutableStateListOf<NodeState>() }

    var searchBoxAt by remember { mutableStateOf<DpOffset?>(null) }

    val density = LocalDensity.current

    var lastPointerPosition by remember { mutableStateOf(Offset.Zero) }

    var viewportOffset by remember { mutableStateOf(Offset.Zero) }

    MaterialTheme(darkColorScheme()) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
                .onClick(
                    onDoubleClick = {
                        searchBoxAt = with(density) {
                            DpOffset(
                                x = 100.dp,
                                y = 100.dp
                            )
                        }
                    }
                ) {  }
                .onPointerEvent(eventType = PointerEventType.Move) { event ->
                    lastPointerPosition = event.changes[0].position
                }
                .focusable()
//                .clickable(
//                    interactionSource = null,
//                    indication = null,
//                ) {}
        ) {
            Graph(
                nodes = nodes,
                viewportOffset = viewportOffset,
                viewportOffsetChange = { viewportOffset = it },
                onNodeDelete = { node ->
                    nodes.remove(node)
                },
                modifier = Modifier
                    .fillMaxSize()
            )

            searchBoxAt?.let { position ->
                SearchBox(
                    options = nodeCatalog.values,
                    onSelected = { info ->
                        val pos = -viewportOffset + lastPointerPosition

                        nodes.add(
                            createNode(
                                info,
                                with(density) { DpOffset(pos.x.toDp(), pos.y.toDp()) }
                            )
                        )
                        searchBoxAt = null
                    },
                    onDismissed = { searchBoxAt = null }
                )
            }
        }
    }
}

@Composable
fun SearchBox(options: Collection<NodeInfo>, onSelected: (NodeInfo) -> Unit, onDismissed: () -> Unit) {

    var filter by remember { mutableStateOf("") }

    val filteredOptions = options.stream().filter { it.displayName.startsWith(filter) }.limit(10).toList()

    val textStyle = LocalTextStyle.current
    val colors = TextFieldDefaults.colors()

    val focusRequester = remember { FocusRequester() }

    Popup(
        onDismissRequest = onDismissed,
        properties = PopupProperties(
            focusable = true
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .focusable()
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
            ){
                BasicTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    textStyle = textStyle.copy(color = colors.focusedTextColor),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.cursorColor),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                )

                for (option in filteredOptions) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clickable {
                                onSelected(option)
                            }
                    ) {
                        Text(text = option.displayName, style = textStyle)
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun main() {

    val file = File("/home/alpha/Documents/Projects/ComposeNodeGraph/src/main/resources/config.json")
    val text = file.readText()

    val list = JSON.decodeFromString<Map<String, NodeInfo>>(text)

    application {
        Window(onCloseRequest = ::exitApplication) {
            App(list)
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
val JSON = Json {
        namingStrategy = JsonNamingStrategy.SnakeCase
    }