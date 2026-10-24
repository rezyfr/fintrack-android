package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.domain.model.LlmTool
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking

// Wraps a domain tool in the library's OpenApiTool. The library calls execute on its own thread.
internal class LlmToolAdapter(private val tool: LlmTool) : OpenApiTool {
    override fun getToolDescriptionJsonString(): String {
        val props = JsonObject()
        tool.params.forEach { p ->
            props.add(p.name, JsonObject().apply {
                addProperty("type", "string")
                addProperty("description", p.description)
            })
        }
        val required = com.google.gson.JsonArray().also { arr -> tool.params.filter { it.required }.forEach { arr.add(it.name) } }
        return JsonObject().apply {
            addProperty("name", tool.name)
            addProperty("description", tool.description)
            add("parameters", JsonObject().apply {
                addProperty("type", "object")
                add("properties", props)
                add("required", required)
            })
        }.toString()
    }

    override fun execute(paramsJsonString: String): String {
        val args = runCatching { JsonParser.parseString(paramsJsonString).asJsonObject }.getOrNull()
        val params = args?.entrySet()?.associate { (k, v) -> k to (if (v.isJsonPrimitive) v.asString else v.toString()) }.orEmpty()
        val text = runBlocking { runCatching { tool.run(params) }.getOrElse { "Error: ${it.message}" } }
        return JsonObject().apply { addProperty("result", text) }.toString()
    }
}
