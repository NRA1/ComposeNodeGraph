import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer

@Serializable
data class NodeInfo(
    val input: NodeInfoInput,
    val inputOrder: NodeInfoInputOrder,
    @Serializable(with = NodeInfoOutputSerializer::class)
    val output: List<List<String>>,
    val outputIsList: List<Boolean>,
    val outputName: List<String>,
    val name: String,
    val displayName: String,
    val description: String,
    val pythonModule: String,
    val category: String,
    val outputNode: Boolean,
    val outputTooltips: List<String> = emptyList(),
    val experimental: Boolean = false,
    val deprecated: Boolean = false,
    val apiNode: Boolean = false,
)

object NodeInfoOutputSerializer : JsonTransformingSerializer<List<List<String>>>(ListSerializer(ListSerializer(String.serializer()))) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        JsonArray(
            (element as JsonArray).map {
                it as? JsonArray ?: JsonArray(listOf(it))
            }
        )
}

@Serializable
data class NodeInfoInput(
    @Serializable(with = NodeInfoInputListSerializer::class)
    val required: Map<String, List<JsonElement>>,
    @Serializable(with = NodeInfoInputListSerializer::class)
    val optional: Map<String, List<JsonElement>> = emptyMap(),
    @Serializable(with = NodeInfoInputListSerializer::class)
    val hidden: Map<String, List<JsonElement>> = emptyMap(),
)

object NodeInfoInputListSerializer : JsonTransformingSerializer<Map<String, List<JsonElement>>>(MapSerializer(String.serializer(),
    ListSerializer(JsonElement.serializer()))) {

    override fun transformDeserialize(element: JsonElement): JsonElement =
        JsonObject(
            (element as JsonObject).map { property ->
                if(property.value !is JsonArray) Pair(property.key, JsonArray(listOf(property.value)))
                else Pair(property.key, property.value)
            }.toMap()
        )
}

@Serializable
data class NodeInfoInputOrder(
    val required: List<String>,
    val optional: List<String> = emptyList(),
    val hidden: List<String> = emptyList(),
)