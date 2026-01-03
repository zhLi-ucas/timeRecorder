package com.example.timemanager.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

class TimeRecordRepository(private val context: Context) {
    private val fileName = "time_records.json"

    fun getAllRecords(): List<TimeRecord> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()

        return try {
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val records = mutableListOf<TimeRecord>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                records.add(
                    TimeRecord(
                        id = obj.getString("id"),
                        tag = obj.getString("tag"),
                        startTime = obj.getLong("startTime"),
                        endTime = obj.getLong("endTime"),
                        durationSeconds = obj.getLong("durationSeconds"),
                        description = obj.getString("description")
                    )
                )
            }
            records.sortedByDescending { it.startTime }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun addRecord(record: TimeRecord) {
        val records = getAllRecords().toMutableList()
        records.add(record)
        saveRecords(records)
    }

    fun updateRecord(updatedRecord: TimeRecord) {
        val records = getAllRecords().toMutableList()
        val index = records.indexOfFirst { it.id == updatedRecord.id }
        if (index != -1) {
            records[index] = updatedRecord
            saveRecords(records)
        }
    }

    fun deleteRecord(recordId: String) {
        val records = getAllRecords().toMutableList()
        val index = records.indexOfFirst { it.id == recordId }
        if (index != -1) {
            records.removeAt(index)
            saveRecords(records)
        }
    }

    private fun saveRecords(records: List<TimeRecord>) {
        val jsonArray = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("tag", record.tag)
                put("startTime", record.startTime)
                put("endTime", record.endTime)
                put("durationSeconds", record.durationSeconds)
                put("description", record.description)
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
