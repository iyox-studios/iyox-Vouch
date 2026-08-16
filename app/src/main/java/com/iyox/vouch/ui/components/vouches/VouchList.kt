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
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.model.isSettled
import com.iyox.vouch.core.utils.rememberToastManager

/**
 * Displays a list of vouches with swipe-to-dismiss and edit capabilities.
 *
 * @param displayedSorted The list of vouches to display, sorted appropriately.
 * @param listState The state of the LazyColumn.
 * @param innerPadding The padding to apply to the content.
 * @param sharedVouchActions The sharedVouchActions to use for actions.
 * @param onEdit Callback when a vouch is edited, passing the vouch's ID and whether it is a group vouch.
 * @param isSettled A predicate to determine if a vouch is settled.
 * @param groupSingleContextPersonId Determines if a group vouch is shown for the context of the full group or of one person
 * @param contentPadding The padding applied to the list content.
 */
@Composable
fun VouchList(
    displayedSorted: List<VouchWithPerson>,
    listState: LazyListState,
    innerPadding: PaddingValues,
    sharedVouchActions: SharedVouchActions,
    onEdit: (Int, Boolean) -> Unit,
    isSettled: (VouchWithPerson) -> Boolean = { it.isSettled() },
    groupSingleContextPersonId: Int? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    val toastManager = rememberToastManager()
    val msgVouchUnsettled = stringResource(R.string.msg_vouch_unsettled)
    val msgVouchDeleted = stringResource(R.string.msg_vouch_deleted)
    val msgVouchSettled = stringResource(R.string.msg_vouch_settled)
    val msgSettleFailed = stringResource(R.string.msg_settle_failed)
    val msgDeleteFailed = stringResource(R.string.msg_delete_failed)
    val actionUndo = stringResource(R.string.action_undo)
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding()),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)
    ) {
        itemsIndexed(
            displayedSorted, { _, item -> item.vouch.id }) { _, vouch ->
            val settled = isSettled(vouch)
            val boxModifier = if (settled) {
                Modifier
                    .animateItem()
                    .alpha(0.6f)
            } else {
                Modifier.animateItem()
            }
            val settleMessage = if (settled) msgVouchUnsettled else msgVouchSettled
            val settleAction: suspend () -> Unit = {
                val isGroupSingleContext = groupSingleContextPersonId != null
                when (settled to isGroupSingleContext) {
                    true to true -> sharedVouchActions.setVouchPersonSettlement(
                        vouchId = vouch.vouch.id,
                        personId = groupSingleContextPersonId!!,
                        settled = false
                    )

                    true to false -> sharedVouchActions.unSettleVouch(vouch)
                    false to true -> sharedVouchActions.setVouchPersonSettlement(
                        vouchId = vouch.vouch.id,
                        personId = groupSingleContextPersonId!!,
                        settled = true
                    )

                    false to false -> sharedVouchActions.settleVouch(vouch)
                }
            }
            val undoSettleAction: suspend (Unit) -> Unit = if (settled) {
                { _ -> sharedVouchActions.settleVouch(vouch) }
            } else {
                { _ -> sharedVouchActions.unSettleVouch(vouch) }
            }


            Box(modifier = boxModifier) {
                if (vouch.persons.size > 1) {
                    if (groupSingleContextPersonId != null) {
                        GroupVouchSingleItem(
                            vouch = vouch,
                            person = vouch.persons.find { it.person.id == groupSingleContextPersonId }?.person,
                            canSettle = !settled,
                            onSettle = {
                                toastManager.executeWithUndo(
                                    message = settleMessage,
                                    actionLabel = actionUndo,
                                    errorMessage = msgSettleFailed,
                                    performAction = settleAction,
                                    undoAction = undoSettleAction
                                )
                            },
                            onDelete = {
                                toastManager.executeWithUndo(
                                    message = msgVouchDeleted,
                                    actionLabel = actionUndo,
                                    errorMessage = msgDeleteFailed,
                                    performAction = {
                                        sharedVouchActions.deletePersonFromVouch(
                                            vouchId = vouch.vouch.id,
                                            personId = groupSingleContextPersonId!!
                                        )
                                    },
                                    undoAction = { _: Unit -> sharedVouchActions.restoreVouch(vouch) }
                                )
                            },
                            onEdit = { onEdit(vouch.vouch.id, true) }
                        )
                    } else {
                        GroupVouchFullItem(
                            vouch = vouch,
                            canSettle = !settled,
                            onSettle = {
                                toastManager.executeWithUndo(
                                    message = settleMessage,
                                    actionLabel = actionUndo,
                                    errorMessage = msgSettleFailed,
                                    performAction = settleAction,
                                    undoAction = undoSettleAction
                                )
                            },
                            onDelete = {
                                toastManager.executeWithUndo(
                                    message = msgVouchDeleted,
                                    actionLabel = actionUndo,
                                    errorMessage = msgDeleteFailed,
                                    performAction = { sharedVouchActions.deleteVouch(vouch.vouch.id) },
                                    undoAction = { _: Unit -> sharedVouchActions.restoreVouch(vouch) }
                                )
                            },
                            onEdit = { onEdit(vouch.vouch.id, true) }
                        )
                    }
                } else { // TODO: maybe interface
                    SingleVouchItem(
                        vouch = vouch,
                        canSettle = !settled,
                        onSettle = {
                            toastManager.executeWithUndo(
                                message = settleMessage,
                                actionLabel = actionUndo,
                                errorMessage = msgSettleFailed,
                                performAction = settleAction,
                                undoAction = undoSettleAction
                            )
                        },
                        onDelete = {
                            toastManager.executeWithUndo(
                                message = msgVouchDeleted,
                                actionLabel = actionUndo,
                                errorMessage = msgDeleteFailed,
                                performAction = {
                                    if (groupSingleContextPersonId != null) {
                                        sharedVouchActions.deletePersonFromVouch(
                                            vouchId = vouch.vouch.id,
                                            personId = groupSingleContextPersonId
                                        )
                                    } else {
                                        sharedVouchActions.deleteVouch(vouch.vouch.id)
                                    }
                                },
                                undoAction = { _: Unit -> sharedVouchActions.restoreVouch(vouch) }
                            )
                        },
                        onEdit = { onEdit(vouch.vouch.id, false) }
                    )
                }
            }
        }
    }
}
