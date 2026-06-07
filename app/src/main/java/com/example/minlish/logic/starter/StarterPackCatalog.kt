package com.example.minlish.logic.starter

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader

data class StarterPackDefinition(
    val id: String,
    val title: String,
    val description: String,
    val fileName: String,
    val tags: String,
    val autoInstall: Boolean,
)

object StarterPackCatalog {
    private const val MANIFEST_PATH = "starter/packs.json"

    fun loadPacks(context: Context): List<StarterPackDefinition> {
        val jsonText = context.assets.open(MANIFEST_PATH).bufferedReader().use(BufferedReader::readText)
        val array = JSONArray(jsonText)
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    StarterPackDefinition(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        fileName = obj.getString("fileName"),
                        tags = obj.getString("tags"),
                        autoInstall = obj.optBoolean("autoInstall", false),
                    )
                )
            }
        }
    }

    fun loadCsv(context: Context, fileName: String): String {
        return context.assets.open("starter/$fileName").bufferedReader().use(BufferedReader::readText)
    }

    fun recommendedPackIdForGoal(goal: String): String? = when (goal.trim()) {
        "IELTS" -> "ielts_starter"
        "Communication" -> "daily_communication"
        else -> null
    }
}
