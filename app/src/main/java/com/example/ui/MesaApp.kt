package com.example.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Card
import com.example.ui.components.BackupDialog
import com.example.ui.components.BoardHeader
import com.example.ui.components.BoardOptionsDialog
import com.example.ui.components.CardDetailDialog
import com.example.ui.components.ListColumn
import com.example.ui.components.MesaTopBar
import com.example.ui.components.NewBoardDialog
import com.example.ui.components.NewListDialog
import com.example.ui.dragdrop.FloatingDraggedCardOverlay
import com.example.ui.dragdrop.rememberKanbanDragState
import com.example.ui.theme.MesaTheme

@Composable
fun MesaApp(viewModel: MesaViewModel) {
    val appState by viewModel.appState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dueFilter by viewModel.dueFilter.collectAsState()
    val selectedLabelIds by viewModel.selectedLabelIds.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    val currentBoard = viewModel.currentBoard
    val dragState = rememberKanbanDragState()

    // Dialog states
    var activeDetailCard by remember { mutableStateOf<Pair<Card, String>?>(null) } // Card, listId
    var showNewBoardDialog by remember { mutableStateOf(false) }
    var showBoardOptionsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showNewListDialog by remember { mutableStateOf(false) }

    MesaTheme(darkTheme = appState.isDarkMode) {
        Scaffold(
            topBar = {
                MesaTopBar(
                    boards = appState.boards,
                    currentBoard = currentBoard,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    isDarkMode = appState.isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onSelectBoard = { viewModel.selectBoard(it) },
                    onNewBoardClick = { showNewBoardDialog = true },
                    onOpenBackups = { showBackupDialog = true }
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.testTag("mesa_scaffold")
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (currentBoard != null) {
                        BoardHeader(
                            board = currentBoard,
                            dueFilter = dueFilter,
                            onDueFilterChange = { viewModel.setDueFilter(it) },
                            selectedLabelIds = selectedLabelIds,
                            onToggleLabelFilter = { viewModel.toggleLabelFilter(it) },
                            onClearFilters = { viewModel.clearFilters() },
                            onOptionsClick = { showBoardOptionsDialog = true }
                        )

                        // Horizontal Kanban Board columns
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .horizontalScroll(
                                    state = rememberScrollState(),
                                    enabled = !dragState.isDragging // Lock horizontal scroll while dragging card across columns
                                ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            currentBoard.lists.forEachIndexed { listIndex, list ->
                                ListColumn(
                                    list = list,
                                    boardLabels = currentBoard.labels,
                                    dragState = dragState,
                                    isFirstList = listIndex == 0,
                                    isLastList = listIndex == currentBoard.lists.size - 1,
                                    cardFilter = { viewModel.isCardVisible(it) },
                                    onCardClick = { card ->
                                        if (!dragState.isDragging) {
                                            activeDetailCard = Pair(card, list.id)
                                        }
                                    },
                                    onAddCard = { title ->
                                        viewModel.addCard(list.id, title)
                                    },
                                    onMoveCardPrevList = { card ->
                                        if (listIndex > 0) {
                                            val prevList = currentBoard.lists[listIndex - 1]
                                            viewModel.moveCardToList(card.id, list.id, prevList.id)
                                        }
                                    },
                                    onMoveCardNextList = { card ->
                                        if (listIndex < currentBoard.lists.size - 1) {
                                            val nextList = currentBoard.lists[listIndex + 1]
                                            viewModel.moveCardToList(card.id, list.id, nextList.id)
                                        }
                                    },
                                    onMoveCardUp = { card ->
                                        viewModel.moveCardPosition(list.id, card.id, -1)
                                    },
                                    onMoveCardDown = { card ->
                                        viewModel.moveCardPosition(list.id, card.id, 1)
                                    },
                                    onDuplicateCard = { card ->
                                        viewModel.duplicateCard(list.id, card.id)
                                    },
                                    onDeleteCard = { card ->
                                        viewModel.deleteCard(list.id, card.id)
                                    },
                                    onRenameList = { newTitle ->
                                        viewModel.renameList(list.id, newTitle)
                                    },
                                    onMoveListLeft = {
                                        viewModel.moveList(list.id, -1)
                                    },
                                    onMoveListRight = {
                                        viewModel.moveList(list.id, 1)
                                    },
                                    onSortList = { sortBy ->
                                        viewModel.sortListCards(list.id, sortBy)
                                    },
                                    onDuplicateList = {
                                        viewModel.duplicateList(list.id)
                                    },
                                    onClearList = {
                                        viewModel.clearListCards(list.id)
                                    },
                                    onDeleteList = {
                                        viewModel.deleteList(list.id)
                                    },
                                    onDragStart = { card, listId, pos, size ->
                                        dragState.startDragging(card, listId, pos, size)
                                    },
                                    onDrag = { delta ->
                                        dragState.updateDrag(delta)
                                    },
                                    onDragEnd = {
                                        dragState.endDrag { cardId, fromListId, toListId, targetIndex ->
                                            viewModel.moveCardToPosition(cardId, fromListId, toListId, targetIndex)
                                        }
                                    },
                                    onDragCancel = {
                                        dragState.cancelDrag()
                                    }
                                )
                            }

                            // Add List Column at the end
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .width(240.dp)
                                    .fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Button(
                                        onClick = { showNewListDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("add_new_list_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Añadir lista",
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text("Añadir lista", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // High z-index floating card preview overlay
                if (currentBoard != null) {
                    FloatingDraggedCardOverlay(
                        dragState = dragState,
                        boardLabels = currentBoard.labels
                    )
                }
            }

            // Dialogs
            activeDetailCard?.let { (card, listId) ->
                if (currentBoard != null) {
                    CardDetailDialog(
                        card = card,
                        boardLists = currentBoard.lists,
                        currentListId = listId,
                        boardLabels = currentBoard.labels,
                        onDismiss = { activeDetailCard = null },
                        onSave = { updatedCard ->
                            viewModel.updateCard(updatedCard)
                        },
                        onMoveToList = { targetListId ->
                            viewModel.moveCardToList(card.id, listId, targetListId)
                            activeDetailCard = Pair(card, targetListId)
                        },
                        onDelete = {
                            viewModel.deleteCard(listId, card.id)
                            activeDetailCard = null
                        },
                        onDuplicate = {
                            viewModel.duplicateCard(listId, card.id)
                            activeDetailCard = null
                        }
                    )
                }
            }

            if (showNewBoardDialog) {
                NewBoardDialog(
                    onDismiss = { showNewBoardDialog = false },
                    onCreate = { name, color ->
                        viewModel.createBoard(name, color)
                    }
                )
            }

            if (showBoardOptionsDialog && currentBoard != null) {
                BoardOptionsDialog(
                    board = currentBoard,
                    canDeleteBoard = appState.boards.size > 1,
                    onDismiss = { showBoardOptionsDialog = false },
                    onUpdateBoard = { name, color ->
                        viewModel.updateBoard(currentBoard.id, name, color)
                    },
                    onDuplicateBoard = {
                        viewModel.duplicateBoard(currentBoard.id)
                    },
                    onDeleteBoard = {
                        viewModel.deleteBoard(currentBoard.id)
                    },
                    onAddLabel = { name, color ->
                        viewModel.addLabel(name, color)
                    },
                    onUpdateLabel = { id, name, color ->
                        viewModel.updateLabel(id, name, color)
                    },
                    onDeleteLabel = { id ->
                        viewModel.deleteLabel(id)
                    }
                )
            }

            if (showBackupDialog) {
                BackupDialog(
                    onDismiss = { showBackupDialog = false },
                    onExport = { viewModel.exportBackupJson() },
                    onImport = { json -> viewModel.importBackupJson(json) }
                )
            }

            if (showNewListDialog) {
                NewListDialog(
                    onDismiss = { showNewListDialog = false },
                    onCreate = { listTitle ->
                        viewModel.addList(listTitle)
                    }
                )
            }
        }
    }
}
