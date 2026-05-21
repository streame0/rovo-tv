package com.rovo.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.rovo.app.data.model.HubRowItemEntity
import com.rovo.app.domain.HubShape
import com.rovo.app.ui.addons.ImageUploadDialog
import com.rovo.app.ui.addons.VoidButton
import com.rovo.app.ui.addons.VoidDialog
import com.rovo.app.ui.addons.VoidInput
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun HubCategoryManagerDialog(
    initialItems: List<HubRowItemEntity>,
    onDismiss: () -> Unit,
    onSave: (List<HubRowItemEntity>) -> Unit,
    onImageUploaded: (suspend (configUniqueId: String, file: File) -> String?)? = null
) {
    // Local mutable list for reordering/removals
    var currentItems by remember { mutableStateOf(initialItems) }
    var reorderingItem by remember { mutableStateOf<HubRowItemEntity?>(null) }
    var managingItem by remember { mutableStateOf<HubRowItemEntity?>(null) }
    var renameItem by remember { mutableStateOf<HubRowItemEntity?>(null) }
    var newRenameName by remember { mutableStateOf("") }
    var confirmRemoveItem by remember { mutableStateOf<HubRowItemEntity?>(null) }
    var confirmRemoveImageItem by remember { mutableStateOf<HubRowItemEntity?>(null) }
    var showUploadImageDialog by remember { mutableStateOf<HubRowItemEntity?>(null) }

    val focusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }
    val categoryListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    if (managingItem == null && confirmRemoveItem == null && confirmRemoveImageItem == null && renameItem == null) {
        VoidDialog(onDismissRequest = onDismiss, title = if (reorderingItem != null) "Reordering..." else "Manage Categories") {
            if (currentItems.isEmpty()) {
                Text("No categories to manage.", color = Color.White.copy(0.5f))
            } else {
                LazyColumn(
                    state = categoryListState,
                    modifier = Modifier.heightIn(max = 310.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(currentItems, key = { _, it -> it.configUniqueId }) { index, item ->
                        val isReordering = reorderingItem?.configUniqueId == item.configUniqueId

                        HubItemEditorRow(
                            item = item,
                            isReordering = isReordering,
                            onMoveUp = {
                                if (index > 0) {
                                    val mutable = currentItems.toMutableList()
                                    val moved = mutable.removeAt(index)
                                    mutable.add(index - 1, moved)
                                    currentItems = mutable
                                    val target = index - 1
                                    scope.launch { delay(50); categoryListState.animateScrollToItem((target - 1).coerceAtLeast(0)) }
                                }
                            },
                            onMoveDown = {
                                if (index < currentItems.size - 1) {
                                    val mutable = currentItems.toMutableList()
                                    val moved = mutable.removeAt(index)
                                    mutable.add(index + 1, moved)
                                    currentItems = mutable
                                    val target = index + 1
                                    scope.launch { delay(50); categoryListState.animateScrollToItem((target - 1).coerceAtLeast(0)) }
                                }
                            },
                            onClick = {
                                if (reorderingItem != null) {
                                    reorderingItem = null
                                } else {
                                    managingItem = item
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VoidButton("Cancel", onDismiss, Modifier.weight(1f))
                VoidButton("Save Changes", {
                    onSave(currentItems)
                }, Modifier.weight(1f), isPrimary = true, focusRequester = if (currentItems.isEmpty()) focusRequester else saveFocusRequester)
            }
        }
    } else if (managingItem != null && confirmRemoveItem == null && confirmRemoveImageItem == null && renameItem == null) {
        if (showUploadImageDialog != null) {
            ImageUploadDialog(
                onDismissRequest = { showUploadImageDialog = null },
                onImageUploaded = { file ->
                    val item = showUploadImageDialog
                    if (item != null && onImageUploaded != null) {
                        scope.launch {
                            val newPath = onImageUploaded(item.configUniqueId, file)
                            if (newPath != null) {
                                val mutable = currentItems.toMutableList()
                                val idx = mutable.indexOfFirst { it.configUniqueId == item.configUniqueId }
                                if (idx != -1) {
                                    mutable[idx] = mutable[idx].copy(customImageUrl = newPath)
                                    currentItems = mutable
                                }
                            }
                        }
                    }
                    showUploadImageDialog = null
                }
            )
        }

        // Item Actions Dialog (Move, Remove, Remove Image)
        VoidDialog(onDismissRequest = { managingItem = null }, title = managingItem?.title ?: "Item Options") {
             VoidButton("Rename", {
                 renameItem = managingItem
                 newRenameName = managingItem?.title ?: ""
                 managingItem = null
             }, Modifier.fillMaxWidth())

             Spacer(Modifier.height(12.dp))

             VoidButton("Move Item", {
                 reorderingItem = managingItem
                 managingItem = null
             }, Modifier.fillMaxWidth())

             Spacer(Modifier.height(12.dp))

             // Upload Image
             if (onImageUploaded != null) {
                 VoidButton("Upload Custom Image", {
                     showUploadImageDialog = managingItem
                 }, Modifier.fillMaxWidth())

                 Spacer(Modifier.height(12.dp))
             }

             // Remove Image (only shown when item has a custom image)
             if (managingItem?.customImageUrl != null) {
                 VoidButton("Remove Image", {
                     confirmRemoveImageItem = managingItem
                 }, Modifier.fillMaxWidth(), isDestructive = true)

                 Spacer(Modifier.height(12.dp))
             }

             VoidButton("Remove from Hub", {
                 confirmRemoveItem = managingItem
             }, Modifier.fillMaxWidth(), isDestructive = true)

             Spacer(Modifier.height(24.dp))

             VoidButton("Close", { managingItem = null }, Modifier.fillMaxWidth())
        }
    }

    if (confirmRemoveItem != null) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { delay(100); focusRequester.requestFocus() }

        VoidDialog(
            onDismissRequest = { confirmRemoveItem = null },
            title = "Remove from Hub?"
        ) {
            Text(
                "Are you sure you want to remove \"${confirmRemoveItem!!.title}\" from this hub?",
                color = Color.Gray
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VoidButton(
                    "Cancel",
                    { confirmRemoveItem = null },
                    Modifier.weight(1f),
                    focusRequester = focusRequester
                )
                VoidButton(
                    "Remove",
                    {
                        val targetId = confirmRemoveItem!!.configUniqueId
                        currentItems = currentItems.filterNot { it.configUniqueId == targetId }
                        confirmRemoveItem = null
                        managingItem = null
                    },
                    Modifier.weight(1f),
                    isDestructive = true
                )
            }
        }
    }

    // Remove Image Confirmation Dialog
    if (confirmRemoveImageItem != null) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { delay(100); focusRequester.requestFocus() }

        VoidDialog(
            onDismissRequest = { confirmRemoveImageItem = null },
            title = "Remove Image?"
        ) {
            Text(
                "This will remove the custom image for \"${confirmRemoveImageItem!!.title}\".",
                color = Color.Gray
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VoidButton(
                    "Cancel",
                    { confirmRemoveImageItem = null },
                    Modifier.weight(1f),
                    focusRequester = focusRequester
                )
                VoidButton(
                    "Remove",
                    {
                        val targetId = confirmRemoveImageItem!!.configUniqueId
                        val mutable = currentItems.toMutableList()
                        val idx = mutable.indexOfFirst { it.configUniqueId == targetId }
                        if (idx != -1) {
                            mutable[idx] = mutable[idx].copy(customImageUrl = null)
                            currentItems = mutable
                        }
                        confirmRemoveImageItem = null
                        managingItem = null
                    },
                    Modifier.weight(1f),
                    isDestructive = true
                )
            }
        }
    }

    // Rename Dialog
    if (renameItem != null) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { delay(100); focusRequester.requestFocus() }

        VoidDialog(onDismissRequest = { renameItem = null }, title = "Rename Item") {
            VoidInput(
                value = newRenameName,
                onValueChange = { newRenameName = it },
                placeholder = "Item Name",
                modifier = Modifier.focusRequester(focusRequester)
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VoidButton("Cancel", { renameItem = null }, Modifier.weight(1f))
                VoidButton("Save", {
                    // Update item title
                    val mutable = currentItems.toMutableList()
                    val idx = mutable.indexOfFirst { it.configUniqueId == renameItem!!.configUniqueId }
                    if (idx != -1) {
                        mutable[idx] = mutable[idx].copy(title = newRenameName)
                        currentItems = mutable
                    }
                    renameItem = null
                }, Modifier.weight(1f), isPrimary = true)
            }
        }
    }


}
