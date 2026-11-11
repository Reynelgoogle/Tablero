package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CheckItem(
    val id: String = "chk_" + UUID.randomUUID().toString().take(8),
    val text: String,
    val done: Boolean = false
)

data class Label(
    val id: String = "lbl_" + UUID.randomUUID().toString().take(8),
    val name: String,
    val color: String
)

data class Card(
    val id: String = "crd_" + UUID.randomUUID().toString().take(8),
    val title: String,
    val desc: String = "",
    val labelIds: List<String> = emptyList(),
    val due: String? = null, // Format: YYYY-MM-DD
    val checklist: List<CheckItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BoardList(
    val id: String = "lst_" + UUID.randomUUID().toString().take(8),
    val name: String,
    val cards: List<Card> = emptyList()
)

data class Board(
    val id: String = "brd_" + UUID.randomUUID().toString().take(8),
    val name: String,
    val color: String = "#1d6b52",
    val labels: List<Label> = DefaultData.defaultLabels(),
    val lists: List<BoardList> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AppState(
    val boards: List<Board>,
    val selectedBoardId: String,
    val isDarkMode: Boolean = false
)

object DefaultData {
    val BOARD_COLORS = listOf(
        "#1d6b52", "#2f6f9e", "#8a5a2b", "#8e3b46",
        "#5b4a8a", "#2f6f63", "#3f5b7a", "#6d5b2f"
    )

    fun defaultLabels(): List<Label> = listOf(
        Label(name = "Urgente", color = "#c7433b"),
        Label(name = "Importante", color = "#c08a2e"),
        Label(name = "En curso", color = "#3d7fbf"),
        Label(name = "Listo", color = "#37845f"),
        Label(name = "Idea", color = "#8267bd"),
        Label(name = "Baja prioridad", color = "#78858e")
    )

    fun todayISO(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun addDaysISO(days: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, days)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun seedState(): AppState {
        val labels = defaultLabels()
        val ideaId = labels.find { it.name == "Idea" }?.id ?: labels[4].id
        val importanteId = labels.find { it.name == "Importante" }?.id ?: labels[1].id
        val enCursoId = labels.find { it.name == "En curso" }?.id ?: labels[2].id
        val listoId = labels.find { it.name == "Listo" }?.id ?: labels[3].id

        val welcomeCard = Card(
            title = "Bienvenido a Mesa 👋",
            desc = "Este tablero vive de forma 100% offline en tu dispositivo. No necesitas cuentas ni servidores.\n\nToca una tarjeta para editarla, añadir listas de comprobación o cambiar sus etiquetas.",
            labelIds = listOf(ideaId),
            checklist = listOf(
                CheckItem(text = "Crear una tarjeta", done = true),
                CheckItem(text = "Mover entre listas", done = false),
                CheckItem(text = "Exportar un respaldo", done = false)
            )
        )

        val moveCard = Card(
            title = "Muéveme a otra lista",
            desc = "Puedes mover tarjetas entre listas usando los botones de navegación o el menú de la tarjeta.\n\nEl botón Deshacer (↶) restaura cualquier cambio previo.",
            labelIds = listOf(importanteId),
            due = addDaysISO(1)
        )

        val customizeCard = Card(
            title = "Personaliza tu espacio",
            desc = "Cambia el nombre del tablero, su color y gestiona las etiquetas desde «Opciones». Todo es configurable a tu gusto.",
            labelIds = listOf(enCursoId),
            checklist = listOf(
                CheckItem(text = "Crear un nuevo tablero", done = true),
                CheckItem(text = "Añadir etiquetas personalizadas", done = true),
                CheckItem(text = "Programar un vencimiento", done = false),
                CheckItem(text = "Probar la búsqueda instantánea", done = false)
            )
        )

        val savedCard = Card(
            title = "Todo se guarda solo",
            desc = "Cada cambio se guarda al instante en tu almacenamiento local. Puedes generar un respaldo en JSON en cualquier momento desde el menú.",
            labelIds = listOf(listoId)
        )

        val initialLists = listOf(
            BoardList(name = "Por hacer", cards = listOf(welcomeCard, moveCard)),
            BoardList(name = "En curso", cards = listOf(customizeCard)),
            BoardList(name = "Hecho", cards = listOf(savedCard))
        )

        val initialBoard = Board(
            name = "Mi tablero",
            color = BOARD_COLORS[0],
            labels = labels,
            lists = initialLists
        )

        return AppState(
            boards = listOf(initialBoard),
            selectedBoardId = initialBoard.id,
            isDarkMode = false
        )
    }
}
