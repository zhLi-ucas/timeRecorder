package com.example.timemanager.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class DurationRepository(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("time_manager_durations", Context.MODE_PRIVATE)

    private val PREFS_KEY_DURATIONS = "saved_durations"

    fun getDurations(): List<Int> {
        val jsonString = sharedPreferences.getString(PREFS_KEY_DURATIONS, null)
        return if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                val durations = mutableListOf<Int>()
                for (i in 0 until jsonArray.length()) {
                    durations.add(jsonArray.getInt(i))
                }
                durations.sorted()
            } catch (e: Exception) {
                getDefaultDurations()
            }
        } else {
            val defaults = getDefaultDurations()
            saveDurations(defaults)
            defaults
        }
    }

    fun addDuration(minutes: Int) {
        val currentDurations = getDurations().toMutableList()
        if (!currentDurations.contains(minutes)) {
            currentDurations.add(minutes)
            saveDurations(currentDurations.sorted())
        }
    }

    fun removeDuration(minutes: Int) {
        val currentDurations = getDurations().toMutableList()
        currentDurations.remove(minutes)
        saveDurations(currentDurations)
    }

    private fun saveDurations(durations: List<Int>) {
        val jsonArray = JSONArray()
        durations.forEach {
            jsonArray.put(it)
        }
        sharedPreferences.edit().putString(PREFS_KEY_DURATIONS, jsonArray.toString()).apply()
    }

    private fun getDefaultDurations(): List<Int> {
        return listOf(15, 60)
    }
}
