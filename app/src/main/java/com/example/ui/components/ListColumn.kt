package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BoardList
import com.example.model.Card
import com.example.model.Label
import com.example.ui.dragdrop.KanbanDragState

@Composable
fun ListColumn(
    list: BoardList,
    boardLabels: List<Label>,
    dragState: KanbanDragState,
    isFirstList: Boolean,
    isLastList: Boolean,
    cardFilter: (Card) -> Boolean,
    onCardClick: (Card) -> Unit,
    onAddCard: (String) -> Unit,
    onMoveCardPrevList: (Card) -> Unit,
    onMoveCardNextList: (Card) -> Unit,
    onMoveCardUp: (Card) -> Unit,
    onMoveCardDown: (Card) -> Unit,
    onDuplicateCard: (Card) -> Unit,
    onDeleteCard: (Card) -> Unit,
    onRenameList: (String) -> Unit,
    onMoveListLeft: () -> Unit,
    onMoveListRight: () -> Unit,
    onSortList: (String) -> Unit,
    onDuplicateList: () -> Unit,
    onClearList: () -> Unit,
    onDeleteList: () -> Unit,
    onDragStart: (card: Card, listId: String, position: Offset, size: IntSize) -> Unit,
    onDrag: (dragDelta: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var isAddingCard by remember { mutableStateOf(false) }
    var newCardTitle by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var isEditingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(list.name) }

    val visibleCards = list.cards.filter(cardFilter)
    val isHovered = dragState.isDragging && dragState.hoveredListId == list.id

    DisposableEffect(list.id) {
        onDispose {
            dragState.unregisterColumn(list.id)
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHovered) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            }
        ),
        border = if (isHovered) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .width(290.dp)
            .fillMaxHeight()
            .onGloballyPositioned { coordinates ->
                dragState.registerColumn(list.id, coordinates.boundsInWindow())
            }
            .testTag("list_column_${list.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Column Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isEditingTitle) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        IconButton(onClick = {
                            onRenameList(editedTitle)
                            isEditingTitle = false
                        }) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Guardar")
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                editedTitle = list.name
                                isEditingTitle = true
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = list.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${visibleCards.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // List options menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones de lista",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cambiar nombre") },
                            onClick = {
                                menuExpanded = false
                                editedTitle = list.name
                                isEditingTitle = true
                            }
                        )
                        if (!isFirstList) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                },
                                text = { Text("Mover a la izquierda") },
                                onClick = {
                                    menuExpanded = false
                                    onMoveListLeft()
                                }
                            )
                        }
                        if (!isLastList) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                },
                                text = { Text("Mover a la derecha") },
                                onClick = {
                                    menuExpanded = false
                                    onMoveListRight()
                                }
                            )
                        }
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                            },
                            text = { Text("Ordenar por título") },
                            onClick = {
                                menuExpanded = false
                                onSortList("title")
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                            },
                            text = { Text("Ordenar por vencimiento") },
                            onClick = {
                                menuExpanded = false
                                onSortList("due")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicar lista") },
                            onClick = {
                                menuExpanded = false
                                onDuplicateList()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Vaciar lista") },
                            onClick = {
                                menuExpanded = false
                                onClearList()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar lista", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDeleteList()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cards Scrollable List with Drag Target Indicators
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(visibleCards, key = { _, card -> card.id }) { index, card ->
                    // Insertion drop indicator if dragging over this index
                    if (isHovered && dragState.hoveredCardIndex == index && dragState.draggedCard?.id != card.id) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    CardItem(
                        card = card,
                        boardLabels = boardLabels,
                        isBeingDragged = dragState.isDragging && dragState.draggedCard?.id == card.id,
                        canMovePrevList = !isFirstList,
                        canMoveNextList = !isLastList,
                        canMoveUp = index > 0,
                        canMoveDown = index < visibleCards.size - 1,
                        onClick = { onCardClick(card) },
                        onMovePrevList = { onMoveCardPrevList(card) },
                        onMoveNextList = { onMoveCardNextList(card) },
                        onMoveUp = { onMoveCardUp(card) },
                        onMoveDown = { onMoveCardDown(card) },
                        onDuplicate = { onDuplicateCard(card) },
                        onDelete = { onDeleteCard(card) },
                        onPositionReport = { bounds ->
                            dragState.registerCard(card.id, list.id, index, bounds)
                        },
                        onDragStart = { pos, size ->
                            onDragStart(card, list.id, pos, size)
                        },
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel
                    )
                }

                // If hovered at the end of list
                if (isHovered && (dragState.hoveredCardIndex == null || dragState.hoveredCardIndex!! >= visibleCards.size)) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Soltar aquí",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else if (visibleCards.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sin tarjetas",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Card Composer at bottom of list
            AnimatedVisibility(visible = isAddingCard) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        OutlinedTextField(
                            value = newCardTitle,
                            onValueChange = { newCardTitle = it },
                            placeholder = { Text("Título de la tarjeta…", fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_card_title_input"),
                            singleLine = false,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                isAddingCard = false
                                newCardTitle = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = {
                                    if (newCardTitle.isNotBlank()) {
                                        onAddCard(newCardTitle.trim())
                                        newCardTitle = ""
                                        isAddingCard = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("save_quick_card_button")
                            ) {
                                Text("Añadir", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (!isAddingCard) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isAddingCard = true }
                        .testTag("add_card_in_list_${list.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Añadir tarjeta",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Añadir tarjeta",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
