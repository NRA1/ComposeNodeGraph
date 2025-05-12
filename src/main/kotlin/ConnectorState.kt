import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset

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
    type: Int
) : ConnectorState(parent, label, type) {
    var source: OutputState? by mutableStateOf(null)
}

class OutputState(
    parent: NodeState,
    label: String,
    type: Int
) : ConnectorState(parent, label, type) {
    val targets: SnapshotStateList<InputState> = mutableStateListOf()
}