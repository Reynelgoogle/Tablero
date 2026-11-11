package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.MesaRepository
import com.example.model.AppState
import com.example.model.Board
import com.example.model.BoardList
import com.example.model.Card
import com.example.model.CheckItem
import com.example.model.DefaultData
import com.example.model.Label
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MesaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MesaRepository(application)

    private val _appState = MutableStateFlow(repository.loadState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _dueFilter = MutableStateFlow("all") // "all", "overdue", "today", "week", "none"
    val dueFilter: StateFlow<String> = _dueFilter.asStateFlow()

    private val _selectedLabelIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedLabelIds: StateFlow<Set<String>> = _selectedLabelIds.asStateFlow()

    private val undoStack = mutableListOf<List<Board>>()
    private val redoStack = mutableListOf<List<Board>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    val currentBoard: Board?
        get() {
            val state = _appState.value
            return state.boards.find { it.id == state.selectedBoardId } ?: state.boards.firstOrNull()
        }

    private fun pushHistory(boards: List<Board>) {
        if (undoStack.size >= 50) {
            undoStack.removeAt(0)
        }
        undoStack.add(boards)
        redoStack.clear()
        updateHistoryFlags()
    }

    private fun updateHistoryFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun updateBoards(newBoards: List<Board>, saveHistory: Boolean = true) {
        if (saveHistory) {
            pushHistory(_appState.value.boards)
        }
        val currentSelectedId = _appState.value.selectedBoardId
        val validSelectedId = if (newBoards.any { it.id == currentSelectedId }) {
            currentSelectedId
        } else {
            newBoards.firstOrNull()?.id ?: ""
        }
        val newState = _appState.value.copy(
            boards = newBoards,
            selectedBoardId = validSelectedId
        )
        _appState.value = newState
        repository.saveState(newState)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previousBoards = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(_appState.value.boards)
        updateHistoryFlags()

        val currentSelectedId = _appState.value.selectedBoardId
        val validSelectedId = if (previousBoards.any { it.id == currentSelectedId }) {
            currentSelectedId
        } else {
            previousBoards.firstOrNull()?.id ?: ""
        }
        val newState = _appState.value.copy(
            boards = previousBoards,
            selectedBoardId = validSelectedId
        )
        _appState.value = newState
        repository.saveState(newState)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val nextBoards = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(_appState.value.boards)
        updateHistoryFlags()

        val currentSelectedId = _appState.value.selectedBoardId
        val validSelectedId = if (nextBoards.any { it.id == currentSelectedId }) {
            currentSelectedId
        } else {
            nextBoards.firstOrNull()?.id ?: ""
        }
        val newState = _appState.value.copy(
            boards = nextBoards,
            selectedBoardId = validSelectedId
        )
        _appState.value = newState
        repository.saveState(newState)
    }

    fun toggleDarkMode() {
        val newState = _appState.value.copy(isDarkMode = !_appState.value.isDarkMode)
        _appState.value = newState
        repository.saveState(newState)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDueFilter(filter: String) {
        _dueFilter.value = filter
    }

    fun toggleLabelFilter(labelId: String) {
        val current = _selectedLabelIds.value.toMutableSet()
        if (current.contains(labelId)) {
            current.remove(labelId)
        } else {
            current.add(labelId)
        }
        _selectedLabelIds.value = current
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _dueFilter.value = "all"
        _selectedLabelIds.value = emptySet()
    }

    // --- Board operations ---

    fun selectBoard(boardId: String) {
        val newState = _appState.value.copy(selectedBoardId = boardId)
        _appState.value = newState
        repository.saveState(newState)
        clearFilters()
    }

    fun createBoard(name: String, color: String = DefaultData.BOARD_COLORS[0]) {
        val newBoard = Board(
            name = name.ifBlank { "Tablero" },
            color = color,
            labels = DefaultData.defaultLabels(),
            lists = listOf(
                BoardList(name = "Por hacer"),
                BoardList(name = "En curso"),
                BoardList(name = "Hecho")
            )
        )
        val newBoards = _appState.value.boards + newBoard
        val newState = _appState.value.copy(
            boards = newBoards,
            selectedBoardId = newBoard.id
        )
        pushHistory(_appState.value.boards)
        _appState.value = newState
        repository.saveState(newState)
    }

    fun updateBoard(boardId: String, name: String, color: String) {
        val updated = _appState.value.boards.map { board ->
            if (board.id == boardId) {
                board.copy(
                    name = name.ifBlank { board.name },
                    color = color,
                    updatedAt = System.currentTimeMillis()
                )
            } else board
        }
        updateBoards(updated)
    }

    fun duplicateBoard(boardId: String) {
        val source = _appState.value.boards.find { it.id == boardId } ?: return
        val duplicated = source.copy(
            id = "brd_" + UUID.randomUUID().toString().take(8),
            name = "${source.name} (Copia)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newBoards = _appState.value.boards + duplicated
        val newState = _appState.value.copy(
            boards = newBoards,
            selectedBoardId = duplicated.id
        )
        pushHistory(_appState.value.boards)
        _appState.value = newState
        repository.saveState(newState)
    }

    fun deleteBoard(boardId: String) {
        if (_appState.value.boards.size <= 1) return
        val remaining = _appState.value.boards.filter { it.id != boardId }
        val newSelectedId = if (_appState.value.selectedBoardId == boardId) {
            remaining.first().id
        } else {
            _appState.value.selectedBoardId
        }
        val newState = _appState.value.copy(
            boards = remaining,
            selectedBoardId = newSelectedId
        )
        pushHistory(_appState.value.boards)
        _appState.value = newState
        repository.saveState(newState)
    }

    // --- Label operations for current board ---

    fun addLabel(name: String, color: String) {
        val board = currentBoard ?: return
        val newLabel = Label(name = name.ifBlank { "Etiqueta" }, color = color)
        val updatedBoard = board.copy(
            labels = board.labels + newLabel,
            updatedAt = System.currentTimeMillis()
        )
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun updateLabel(labelId: String, name: String, color: String) {
        val board = currentBoard ?: return
        val updatedLabels = board.labels.map {
            if (it.id == labelId) it.copy(name = name.ifBlank { it.name }, color = color) else it
        }
        val updatedBoard = board.copy(labels = updatedLabels, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun deleteLabel(labelId: String) {
        val board = currentBoard ?: return
        val updatedLabels = board.labels.filter { it.id != labelId }
        // Also remove labelId from cards in the board
        val updatedLists = board.lists.map { list ->
            list.copy(cards = list.cards.map { card ->
                card.copy(labelIds = card.labelIds.filter { it != labelId })
            })
        }
        val updatedBoard = board.copy(
            labels = updatedLabels,
            lists = updatedLists,
            updatedAt = System.currentTimeMillis()
        )
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    // --- List operations ---

    fun addList(name: String) {
        val board = currentBoard ?: return
        val newList = BoardList(name = name.ifBlank { "Lista" })
        val updatedBoard = board.copy(
            lists = board.lists + newList,
            updatedAt = System.currentTimeMillis()
        )
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun renameList(listId: String, name: String) {
        val board = currentBoard ?: return
        val updatedLists = board.lists.map {
            if (it.id == listId) it.copy(name = name.ifBlank { it.name }) else it
        }
        val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun moveList(listId: String, direction: Int) {
        val board = currentBoard ?: return
        val index = board.lists.indexOfFirst { it.id == listId }
        val targetIndex = index + direction
        if (index < 0 || targetIndex < 0 || targetIndex >= board.lists.size) return

        val lists = board.lists.toMutableList()
        val item = lists.removeAt(index)
        lists.add(targetIndex, item)

        val updatedBoard = board.copy(lists = lists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun duplicateList(listId: String) {
        val board = currentBoard ?: return
        val list = board.lists.find { it.id == listId } ?: return
        val duplicatedCards = list.cards.map { card ->
            card.copy(
                id = "crd_" + UUID.randomUUID().toString().take(8),
                checklist = card.checklist.map { it.copy(id = "chk_" + UUID.randomUUID().toString().take(8)) }
            )
        }
        val duplicatedList = list.copy(
            id = "lst_" + UUID.randomUUID().toString().take(8),
            name = "${list.name} (Copia)",
            cards = duplicatedCards
        )
        val index = board.lists.indexOfFirst { it.id == listId }
        val lists = board.lists.toMutableList()
        lists.add(index + 1, duplicatedList)

        val updatedBoard = board.copy(lists = lists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun clearListCards(listId: String) {
        val board = currentBoard ?: return
        val updatedLists = board.lists.map {
            if (it.id == listId) it.copy(cards = emptyList()) else it
        }
        val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun deleteList(listId: String) {
        val board = currentBoard ?: return
        val updatedLists = board.lists.filter { it.id != listId }
        val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun sortListCards(listId: String, sortBy: String) {
        val board = currentBoard ?: return
        val list = board.lists.find { it.id == listId } ?: return
        val sortedCards = when (sortBy) {
            "title" -> list.cards.sortedBy { it.title.lowercase() }
            "due" -> list.cards.sortedWith(compareBy(nullsLast()) { it.due })
            "created" -> list.cards.sortedByDescending { it.createdAt }
            else -> list.cards
        }
        val updatedLists = board.lists.map {
            if (it.id == listId) it.copy(cards = sortedCards) else it
        }
        val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    // --- Card operations ---

    fun addCard(listId: String, title: String) {
        val board = currentBoard ?: return
        val newCard = Card(title = title.ifBlank { "Sin título" })
        val updatedLists = board.lists.map { list ->
            if (list.id == listId) list.copy(cards = list.cards + newCard) else list
        }
        val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun updateCard(card: Card) {
        val board = currentBoard ?: return
        val updatedLists = board.lists.map { list ->
            list.copy(cards = list.cards.map { if (it.id == card.id) card.copy(updatedAt = System.currentTimeMillis()) else it })
        }
        val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun deleteCard(listId: String, cardId: String) {
        val board = currentBoard ?: return
        val updatedLists = board.lists.map { list ->
            if (list.id == listId) list.copy(cards = list.cards.filter { it.id != cardId }) else list
        }
        val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun duplicateCard(listId: String, cardId: String) {
        val board = currentBoard ?: return
        val list = board.lists.find { it.id == listId } ?: return
        val card = list.cards.find { it.id == cardId } ?: return
        val duplicated = card.copy(
            id = "crd_" + UUID.randomUUID().toString().take(8),
            title = "${card.title} (Copia)",
            checklist = card.checklist.map { it.copy(id = "chk_" + UUID.randomUUID().toString().take(8)) },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val index = list.cards.indexOfFirst { it.id == cardId }
        val cards = list.cards.toMutableList()
        cards.add(index + 1, duplicated)

        val updatedLists = board.lists.map { if (it.id == listId) it.copy(cards = cards) else it }
        val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun moveCardToList(cardId: String, fromListId: String, toListId: String) {
        if (fromListId == toListId) return
        val board = currentBoard ?: return
        val fromList = board.lists.find { it.id == fromListId } ?: return
        val card = fromList.cards.find { it.id == cardId } ?: return

        val updatedLists = board.lists.map { list ->
            when (list.id) {
                fromListId -> list.copy(cards = list.cards.filter { it.id != cardId })
                toListId -> list.copy(cards = list.cards + card)
                else -> list
            }
        }
        val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun moveCardPosition(listId: String, cardId: String, direction: Int) {
        val board = currentBoard ?: return
        val list = board.lists.find { it.id == listId } ?: return
        val index = list.cards.indexOfFirst { it.id == cardId }
        val targetIndex = index + direction
        if (index < 0 || targetIndex < 0 || targetIndex >= list.cards.size) return

        val cards = list.cards.toMutableList()
        val card = cards.removeAt(index)
        cards.add(targetIndex, card)

        val updatedLists = board.lists.map { if (it.id == listId) it.copy(cards = cards) else it }
        val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    fun moveCardToPosition(cardId: String, fromListId: String, toListId: String, targetIndex: Int? = null) {
        val board = currentBoard ?: return
        val fromList = board.lists.find { it.id == fromListId } ?: return
        val card = fromList.cards.find { it.id == cardId } ?: return

        if (fromListId == toListId) {
            val currentIndex = fromList.cards.indexOfFirst { it.id == cardId }
            if (currentIndex < 0) return
            val validTargetIndex = (targetIndex ?: fromList.cards.lastIndex).coerceIn(0, fromList.cards.lastIndex)
            if (currentIndex == validTargetIndex) return

            val cards = fromList.cards.toMutableList()
            cards.removeAt(currentIndex)
            cards.add(validTargetIndex, card)

            val updatedLists = board.lists.map { if (it.id == fromListId) it.copy(cards = cards) else it }
            val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
            val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
            updateBoards(updatedBoards)
        } else {
            val toList = board.lists.find { it.id == toListId } ?: return
            val targetCards = toList.cards.toMutableList()
            val validIndex = if (targetIndex != null && targetIndex in 0..targetCards.size) {
                targetIndex
            } else {
                targetCards.size
            }
            targetCards.add(validIndex, card)

            val updatedLists = board.lists.map { list ->
                when (list.id) {
                    fromListId -> list.copy(cards = list.cards.filter { it.id != cardId })
                    toListId -> list.copy(cards = targetCards)
                    else -> list
                }
            }
            val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
            val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
            updateBoards(updatedBoards)
        }
    }

    fun toggleCheckItem(cardId: String, checkItemId: String) {
        val board = currentBoard ?: return
        val updatedLists = board.lists.map { list ->
            list.copy(cards = list.cards.map { card ->
                if (card.id == cardId) {
                    val updatedChecklist = card.checklist.map { item ->
                        if (item.id == checkItemId) item.copy(done = !item.done) else item
                    }
                    card.copy(checklist = updatedChecklist, updatedAt = System.currentTimeMillis())
                } else card
            })
        }
        val updatedBoard = board.copy(lists = updatedLists, updatedAt = System.currentTimeMillis())
        val updatedBoards = _appState.value.boards.map { if (it.id == board.id) updatedBoard else it }
        updateBoards(updatedBoards)
    }

    // --- Backup & Restore ---

    fun exportBackupJson(): String {
        return repository.exportToJson(_appState.value)
    }

    fun importBackupJson(json: String): Boolean {
        val result = repository.importFromJson(json)
        return if (result.isSuccess) {
            val newState = result.getOrThrow()
            pushHistory(_appState.value.boards)
            _appState.value = newState
            clearFilters()
            true
        } else {
            false
        }
    }

    // --- Filter logic ---

    fun isCardVisible(card: Card): Boolean {
        val query = _searchQuery.value.trim().lowercase()
        if (query.isNotEmpty()) {
            val inTitle = card.title.lowercase().contains(query)
            val inDesc = card.desc.lowercase().contains(query)
            val inChecks = card.checklist.any { it.text.lowercase().contains(query) }
            if (!inTitle && !inDesc && !inChecks) return false
        }

        val labelFilters = _selectedLabelIds.value
        if (labelFilters.isNotEmpty()) {
            if (!card.labelIds.any { labelFilters.contains(it) }) return false
        }

        val dueFilterVal = _dueFilter.value
        if (dueFilterVal != "all") {
            val today = DefaultData.todayISO()
            val week = DefaultData.addDaysISO(7)
            when (dueFilterVal) {
                "none" -> if (card.due != null) return false
                "overdue" -> if (card.due == null || card.due >= today) return false
                "today" -> if (card.due != today) return false
                "week" -> if (card.due == null || card.due < today || card.due > week) return false
            }
        }

        return true
    }
}
