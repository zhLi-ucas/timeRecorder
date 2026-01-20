package com.example.timemanager.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

class HealthRecordRepository(private val context: Context) {
    private val fileName = "health_records.json"

    fun getAllRecords(): List<HealthRecord> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()

        return try {
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val records = mutableListOf<HealthRecord>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                records.add(
                    HealthRecord(
                        id = obj.getString("id"),
                        type = obj.getString("type"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            records.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun addRecord(record: HealthRecord) {
        val records = getAllRecords().toMutableList()
        records.add(record)
        saveRecords(records)
    }

    private fun saveRecords(records: List<HealthRecord>) {
        val jsonArray = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("type", record.type)
                put("timestamp", record.timestamp)
            }
            jsonArray.put(obj)
        }

        try {
            val file = File(context.filesDir, fileName)
            file.writeText(jsonArray.toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
