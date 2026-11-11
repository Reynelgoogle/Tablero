package com.example.data

import android.content.Context
import com.example.model.AppState
import com.example.model.DefaultData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class MesaRepository(private val context: Context) {
    private val fileName = "mesa_state.json"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun loadState(): AppState {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            val freshState = DefaultData.seedState()
            saveState(freshState)
            return freshState
        }

        return try {
            val json = file.readText()
            val state = gson.fromJson(json, AppState::class.java)
            if (state != null && state.boards.isNotEmpty()) {
                state
            } else {
                val seed = DefaultData.seedState()
                saveState(seed)
                seed
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val seed = DefaultData.seedState()
            saveState(seed)
            seed
        }
    }

    fun saveState(state: AppState) {
        try {
            val file = File(context.filesDir, fileName)
            val json = gson.toJson(state)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToJson(state: AppState): String {
        return gson.toJson(state)
    }

    fun importFromJson(json: String): Result<AppState> {
        return try {
            val parsed = gson.fromJson(json, AppState::class.java)
            if (parsed != null && parsed.boards.isNotEmpty()) {
                val selectedId = if (parsed.boards.any { it.id == parsed.selectedBoardId }) {
                    parsed.selectedBoardId
                } else {
                    parsed.boards.first().id
                }
                val validState = parsed.copy(selectedBoardId = selectedId)
                saveState(validState)
                Result.success(validState)
            } else {
                Result.failure(IllegalArgumentException("El archivo JSON no contiene tableros válidos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
