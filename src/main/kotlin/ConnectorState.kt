import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

sealed class ConnectorState(
    val parent: NodeState,
    label: String
) {
    var label: String by mutableStateOf(label)
    var connectionPointOffset: Offset? by mutableStateOf(null)
}

class InputState(
    parent: NodeState,
    label: String
) : ConnectorState(parent, label) {
    var source: OutputState? by mutableStateOf(null)
}

class OutputState(
    parent: NodeState,
    label: String
) : ConnectorState(parent, label) {
    var target: InputState? by mutableStateOf(null)
}