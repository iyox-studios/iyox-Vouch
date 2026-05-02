package com.iyox.vouch.ui.components.vouches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iyox.vouch.R
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.utils.rememberUndoCoordinator

/**
 * Displays a list of vouches with swipe-to-dismiss and edit capabilities.
 *
 * @param displayedSorted The list of vouches to display, sorted appropriately.
 * @param listState The state of the LazyColumn.
 * @param innerPadding The padding to apply to the content.
 * @param sharedVouchActions The sharedVouchActions to use for actions.
 * @param onEdit Callback when a vouch is edited, passing the vouch's ID.
 */
@Composable
fun VouchList(
    displayedSorted: List<Vouch>,
    listState: LazyListState,
    innerPadding: PaddingValues,
    sharedVouchActions: SharedVouchActions,
    onEdit: (Int) -> Unit
) {
    val undoCoordinator = rememberUndoCoordinator()
    val msgVouchUnsettled = stringResource(R.string.msg_vouch_unsettled)
    val msgVouchDeleted = stringResource(R.string.msg_vouch_deleted)
    val msgVouchSettled = stringResource(R.string.msg_vouch_settled)
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding()),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Top)
    ) {
        itemsIndexed(
            displayedSorted, { _, item -> item.id }) { _, vouch ->
            if (vouch.settled) {
                Box(
                    modifier = Modifier
                        .animateItem()
                        .alpha(0.6f)
                ) {
                    VouchItem(
                        vouch = vouch,
                        canSettle = false,
                        onSettle = {
                            undoCoordinator.executeWithUndo(
                                message = msgVouchUnsettled,
                                performAction = { sharedVouchActions.unSettleVouch(vouch) },
                                undoAction = { sharedVouchActions.settleVouch(vouch) }
                            )
                        },
                        onDelete = {
                            undoCoordinator.executeWithUndo(
                                message = msgVouchDeleted,
                                performAction = { sharedVouchActions.deleteVouch(vouch) },
                                undoAction = { sharedVouchActions.addVouch(vouch) }
                            )
                        },
                        onEdit = { onEdit(vouch.id) }
                    )
                }
            } else {
                Box(modifier = Modifier.animateItem()) {
                    VouchItem(
                        vouch = vouch,
                        onSettle = {
                            undoCoordinator.executeWithUndo(
                                message = msgVouchSettled,
                                performAction = { sharedVouchActions.settleVouch(vouch) },
                                undoAction = { sharedVouchActions.unSettleVouch(vouch) }
                            )
                        },
                        onDelete = {
                            undoCoordinator.executeWithUndo(
                                message = msgVouchDeleted,
                                performAction = { sharedVouchActions.deleteVouch(vouch) },
                                undoAction = { sharedVouchActions.addVouch(vouch) }
                            )
                        },
                        onEdit = { onEdit(vouch.id) }
                    )
                }
            }
        }
    }
}
