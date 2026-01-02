package com.example.timemanager.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class TagRepository(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("time_manager_tags", Context.MODE_PRIVATE)

    private val PREFS_KEY_TAGS = "saved_tags"

    fun getTags(): List<Tag> {
        val jsonString = sharedPreferences.getString(PREFS_KEY_TAGS, null)
        return if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                val tags = mutableListOf<Tag>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    tags.add(Tag(obj.getString("name"), obj.getInt("color")))
                }
                tags
            } catch (e: Exception) {
                getDefaultTags()
            }
        } else {
            val defaults = getDefaultTags()
            saveTags(defaults)
            defaults
        }
    }

    fun addTag(tag: Tag) {
        val currentTags = getTags().toMutableList()
        if (currentTags.none { it.name == tag.name }) {
            currentTags.add(tag)
            saveTags(currentTags)
        }
    }

    fun removeTag(tag: Tag) {
        val currentTags = getTags().toMutableList()
        currentTags.removeAll { it.name == tag.name }
        saveTags(currentTags)
    }

    fun updateTag(tag: Tag) {
        val currentTags = getTags().toMutableList()
        val index = currentTags.indexOfFirst { it.name == tag.name }
        if (index != -1) {
            currentTags[index] = tag
            saveTags(currentTags)
        }
    }

    private fun saveTags(tags: List<Tag>) {
        val jsonArray = JSONArray()
        tags.forEach { tag ->
            val obj = JSONObject()
            obj.put("name", tag.name)
            obj.put("color", tag.colorArgb)
            jsonArray.put(obj)
        }
        sharedPreferences.edit().putString(PREFS_KEY_TAGS, jsonArray.toString()).apply()
    }

    private fun getDefaultTags(): List<Tag> {
        return listOf(
            Tag.create("Working"),
            Tag.create("Learning"),
            Tag.create("Playing")
        )
    }
}
