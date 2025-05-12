import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset

class NodeState(
    text: String,
    inputs: List<InputState>,
    outputs: List<OutputState>,
    position: DpOffset = DpOffset.Zero
) {
    var title: String by mutableStateOf(text)
    val inputs: SnapshotStateList<InputState> = mutableStateListOf(*inputs.toTypedArray())
    val outputs: SnapshotStateList<OutputState> = mutableStateListOf(*outputs.toTypedArray())

    var position: DpOffset by mutableStateOf(position)
}