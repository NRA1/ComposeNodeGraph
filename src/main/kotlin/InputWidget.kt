import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

sealed class InputWidgetState {

}

class TextWidgetState(

) : InputWidgetState() {
    var text: String by mutableStateOf("")
}

class IntWidgetState(

) : InputWidgetState() {
    var value: Int by mutableStateOf(0)
}

class FloatWidgetState(

) : InputWidgetState() {
    var value: Float by mutableStateOf(0.0f)
}

class DropdownWidgetState(
    val options: List<String>
) : InputWidgetState() {
    var selectedIndex: Int by mutableStateOf(0)

    val selected
        get() = options[selectedIndex]
}

@Composable
fun TextWidget(enabled: Boolean, state: TextWidgetState, modifier: Modifier = Modifier) {
    val colors = TextFieldDefaults.colors()

    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = state.text,
        onValueChange = { state.text = it },
        cursorBrush = SolidColor(colors.cursorColor),
        textStyle = TextStyle(color =
            if(enabled) {
                if(focused) colors.focusedTextColor
                else colors.unfocusedTextColor
            } else colors.disabledTextColor
        ),
        interactionSource = interactionSource,
        enabled = enabled,
        decorationBox = { innerTextField ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                ) {
                    innerTextField()
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun IntWidget(enabled: Boolean, label: String, state: IntWidgetState, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(state.value.toString()) }

    NumberField(
        value = text,
        onValueChange = {
            if(it.isBlank()) {
                state.value = 0
                text = ""
            }
            else {
                val int = it.toIntOrNull()
                if(int != null) {
                    state.value = int
                    text = it
                }
            }
        },
        enabled = enabled,
        label = label,
        modifier = modifier
    )
}


@Composable
fun FloatWidget(enabled: Boolean, label: String, state: FloatWidgetState, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(state.value.toString()) }

    NumberField(
        value = text,
        onValueChange = {
            if(it.isBlank()) {
                state.value = 0f
                text = ""
            }
            else {
                val float = it.toFloatOrNull()
                if(float != null) {
                    state.value = float
                    text = it
                }
            }
        },
        enabled = enabled,
        label = label,
        modifier = modifier
            .onFocusEvent { event ->
                if(!event.isFocused) text = state.value.toString()
            }
    )
}

@Composable
fun NumberField(value: String, onValueChange: (String) -> Unit, enabled: Boolean, label: String, modifier: Modifier = Modifier) {
    val colors = TextFieldDefaults.colors()

    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()


    val textStyle = LocalTextStyle.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        cursorBrush = SolidColor(colors.cursorColor),
        textStyle = textStyle.copy(color =
            if(enabled) {
                if(focused) colors.focusedTextColor
                else colors.unfocusedTextColor
            } else colors.disabledTextColor,
            textAlign = TextAlign.End,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        interactionSource = interactionSource,
        enabled = enabled,
        decorationBox = { innerTextField ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(2.dp)
                ) {
                    Text(
                        text = label,
                        color = colors.unfocusedPlaceholderColor,
                        style = textStyle,
                    )
                    Spacer(Modifier.width(2.dp))
                    innerTextField()
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
    )
}

@Composable
fun DropdownWidget(state: DropdownWidgetState, label: String, enabled: Boolean, modifier: Modifier = Modifier) {
    val textStyle = LocalTextStyle.current
    val colors = TextFieldDefaults.colors()

    var focused by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = modifier
                .let {
                    if(enabled) it
                        .onFocusEvent { event ->
                            focused = event.hasFocus
                        }
                        .clickable { focused = true  }
                        .focusable()
                    else it
                }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(2.dp)
        ) {
            Text(
                text = label,
                color = colors.unfocusedPlaceholderColor,
                style = textStyle
            )

            Text(
                text = state.selected,
                style = textStyle.copy(
                    color = if (enabled) colors.focusedTextColor else colors.disabledTextColor,
                    textAlign = TextAlign.End,
                ),
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = if (enabled) colors.focusedTextColor else colors.disabledTextColor,
                modifier = Modifier
                    .size(15.dp)
            )

        }
        if(focused) {
            Popup(
                popupPositionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset = anchorBounds.topRight

                }
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column {
                        for((idx, option) in state.options.withIndex()) {
                            Box(
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable {
                                        state.selectedIndex = idx
                                        focused = false
                                    }
                            ) {
                                Text(
                                    text = option,
                                    style = textStyle,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}