package com.example.ui.dragdrop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Card
import com.example.model.DefaultData
import com.example.model.Label
import com.example.ui.theme.parseHexColor
import kotlin.math.roundToInt

class KanbanDragState {
    var isDragging by mutableStateOf(false)
        private set

    var draggedCard by mutableStateOf<Card?>(null)
        private set

    var sourceListId by mutableStateOf<String?>(null)
        private set

    var dragPosition by mutableStateOf(Offset.Zero)
        private set

    var cardSize by mutableStateOf(IntSize.Zero)
        private set

    var hoveredListId by mutableStateOf<String?>(null)
        private set

    var hoveredCardIndex by mutableStateOf<Int?>(null)
        private set

    // Column rects in window coordinates
    val columnBounds = mutableStateMapOf<String, Rect>()

    // Card rects in window coordinates (mapped to Pair(listId, cardIndex))
    val cardBounds = mutableStateMapOf<String, Pair<Rect, Pair<String, Int>>>()

    fun registerColumn(listId: String, bounds: Rect) {
        columnBounds[listId] = bounds
    }

    fun unregisterColumn(listId: String) {
        columnBounds.remove(listId)
    }

    fun registerCard(cardId: String, listId: String, index: Int, bounds: Rect) {
        cardBounds[cardId] = Pair(bounds, Pair(listId, index))
    }

    fun unregisterCard(cardId: String) {
        cardBounds.remove(cardId)
    }

    fun startDragging(card: Card, listId: String, initialPosition: Offset, size: IntSize) {
        isDragging = true
        draggedCard = card
        sourceListId = listId
        dragPosition = initialPosition
        cardSize = size
        hoveredListId = listId
        hoveredCardIndex = null
    }

    fun updateDrag(dragDelta: Offset) {
        if (!isDragging) return
        dragPosition += dragDelta

        // Determine which column is being hovered
        var currentHoveredCol: String? = null
        for ((colId, rect) in columnBounds) {
            // Expand rect slightly horizontally for smooth edge transitions
            if (dragPosition.x >= rect.left && dragPosition.x <= rect.right) {
                currentHoveredCol = colId
                break
            }
        }
        hoveredListId = currentHoveredCol

        // Determine target card index within hovered column
        if (currentHoveredCol != null) {
            var targetIdx: Int? = null
            // Check cards in this column
            val cardsInColumn = cardBounds.values
                .filter { it.second.first == currentHoveredCol }
                .sortedBy { it.second.second }

            for ((rect, info) in cardsInColumn) {
                val index = info.second
                if (dragPosition.y < rect.center.y) {
                    targetIdx = index
                    break
                }
            }
            hoveredCardIndex = targetIdx
        } else {
            hoveredCardIndex = null
        }
    }

    fun endDrag(onDrop: (cardId: String, fromListId: String, toListId: String, targetIndex: Int?) -> Unit) {
        if (isDragging) {
            val card = draggedCard
            val fromList = sourceListId
            val toList = hoveredListId ?: sourceListId
            val targetIdx = hoveredCardIndex

            if (card != null && fromList != null && toList != null) {
                onDrop(card.id, fromList, toList, targetIdx)
            }
        }
        reset()
    }

    fun cancelDrag() {
        reset()
    }

    private fun reset() {
        isDragging = false
        draggedCard = null
        sourceListId = null
        dragPosition = Offset.Zero
        cardSize = IntSize.Zero
        hoveredListId = null
        hoveredCardIndex = null
    }
}

@Composable
fun rememberKanbanDragState(): KanbanDragState {
    return remember { KanbanDragState() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingDraggedCardOverlay(
    dragState: KanbanDragState,
    boardLabels: List<Label>
) {
    AnimatedVisibility(
        visible = dragState.isDragging && dragState.draggedCard != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val card = dragState.draggedCard ?: return@AnimatedVisibility
        val activeLabels = card.labelIds.mapNotNull { id -> boardLabels.find { it.id == id } }
        val totalCheck = card.checklist.size
        val doneCheck = card.checklist.count { it.done }

        val today = DefaultData.todayISO()
        val isOverdue = card.due != null && card.due < today
        val isToday = card.due != null && card.due == today

        val offsetX = (dragState.dragPosition.x - (dragState.cardSize.width / 2f)).roundToInt()
        val offsetY = (dragState.dragPosition.y - 40f).roundToInt()

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .width(260.dp)
                    .rotate(3.5f)
                    .shadow(16.dp, RoundedCornerShape(12.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    if (activeLabels.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            activeLabels.forEach { label ->
                                val labelColor = parseHexColor(label.color)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = labelColor.copy(alpha = 0.25f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(labelColor)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = label.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = card.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )

                    val hasBadges = card.due != null || totalCheck > 0 || card.desc.isNotBlank()
                    if (hasBadges) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (card.due != null) {
                                val (dueBg, dueTextCol) = when {
                                    isOverdue -> Pair(Color(0xFFF8E5E2), Color(0xFFB23B32))
                                    isToday -> Pair(Color(0xFFF8EDD8), Color(0xFF8F5E0B))
                                    else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = dueBg
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = dueTextCol,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = card.due,
                                            fontSize = 11.sp,
                                            color = dueTextCol,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            if (totalCheck > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (doneCheck == totalCheck) Color(0xFFDCECE5) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckBox,
                                            contentDescription = null,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "$doneCheck/$totalCheck",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            if (card.desc.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
