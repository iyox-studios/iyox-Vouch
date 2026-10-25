package com.iyox.vouch.feature.onboarding

import android.icu.util.Currency
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyGridSlot
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonTotal
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.feature.person.QuickActionsCard
import com.iyox.vouch.feature.vouch.GroupMemberInput
import com.iyox.vouch.feature.vouch.GroupMemberList
import com.iyox.vouch.feature.vouch.GroupMemberSuggestionsRow
import com.iyox.vouch.feature.vouch.GroupVouchSummaryCard
import com.iyox.vouch.ui.components.CurrencyCard
import com.iyox.vouch.ui.components.CurrencySearchCard
import com.iyox.vouch.ui.components.PageHeader
import com.iyox.vouch.ui.components.VouchBalanceHeroCard
import com.iyox.vouch.ui.components.vouches.SingleVouchItem
import kotlinx.coroutines.launch
import java.util.Locale

private const val PAGE_COUNT = 5

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    currentCurrencyCode: String = CurrencyManager.getDefaultCurrency().currencyCode,
    onCurrencySelected: (String) -> Unit = {},
    onNavigateToCurrencySearch: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Scaffold(
        topBar = {
            PageHeader(
                actions = {
                    AnimatedVisibility(
                        visible = pagerState.currentPage < PAGE_COUNT - 1,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Button(
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(PAGE_COUNT - 1)
                                }
                            }
                        ) {
                            Text(stringResource(R.string.onboarding_skip))
                        }
                    }
                }
            )
        },
        bottomBar = {
            OnboardingBottomNavigation(
                currentPage = pagerState.currentPage,
                onBack = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                onNext = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    if (pagerState.currentPage < PAGE_COUNT - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> WelcomeSlide()
                1 -> VouchesSlide()
                2 -> GroupVouchesSlide()
                3 -> PeopleSlide()
                4 -> CurrencySlide(
                    currentCurrencyCode = currentCurrencyCode,
                    onCurrencySelected = onCurrencySelected,
                    onNavigateToCurrencySearch = onNavigateToCurrencySearch
                )
            }
        }
    }
}

@Composable
private fun OnboardingBottomNavigation(
    modifier: Modifier = Modifier,
    currentPage: Int,
    pageCount: Int = PAGE_COUNT,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pageCount) { index ->
                    val isSelected = index == currentPage
                    val width = animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        label = "dotWidth"
                    )
                    val color = animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        label = "dotColor"
                    )

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width.value)
                            .clip(CircleShape)
                            .background(color.value)
                    )
                }
            }

            AnimatedVisibility(
                visible = currentPage > 0,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                FilledTonalButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.onboarding_back),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.onboarding_back),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Button(
                onClick = onNext,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(
                    text = if (currentPage == pageCount - 1) {
                        stringResource(R.string.onboarding_get_started)
                    } else {
                        stringResource(R.string.onboarding_next)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (currentPage == pageCount - 1) {
                        Icons.Filled.Check
                    } else {
                        Icons.AutoMirrored.Filled.ArrowForward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomeSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        SlideHeader(
            title = stringResource(R.string.onboarding_welcome_title),
            subtitle = stringResource(R.string.onboarding_welcome_subtitle)
        )

        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WelcomeFeatureRow(
                    icon = Icons.Filled.SwapHoriz,
                    title = stringResource(R.string.onboarding_welcome_feature1_title),
                    description = stringResource(R.string.onboarding_welcome_feature1_desc)
                )
                WelcomeFeatureRow(
                    icon = Icons.Filled.Diversity3,
                    title = stringResource(R.string.onboarding_welcome_feature2_title),
                    description = stringResource(R.string.onboarding_welcome_feature2_desc)
                )
                WelcomeFeatureRow(
                    icon = Icons.Filled.DoneAll,
                    title = stringResource(R.string.onboarding_welcome_feature3_title),
                    description = stringResource(R.string.onboarding_welcome_feature3_desc)
                )
            }
        }

        TipCard(
            icon = Icons.Outlined.Lightbulb,
            text = stringResource(R.string.onboarding_welcome_hint)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomeFeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VouchesSlide() {
    val sampleName = stringResource(R.string.onboarding_slide1_sample_name)
    val sampleDesc = stringResource(R.string.onboarding_slide1_sample_desc)

    var canSettle by remember { mutableStateOf(true) }
    var itemKey by remember { mutableIntStateOf(0) }

    val sampleVouch = remember(sampleName, sampleDesc) {
        VouchWithPerson(
            vouch = Vouch(
                id = 1,
                amount = 2500L,
                description = sampleDesc,
                given = true,
                timestamp = System.currentTimeMillis()
            ),
            persons = listOf(
                PersonWithAmount(
                    person = Person(id = 1, name = sampleName),
                    amount = 2500L,
                    settled = false
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        SlideHeader(
            title = stringResource(R.string.onboarding_slide1_title),
            subtitle = stringResource(R.string.onboarding_slide1_subtitle)
        )

        key(itemKey) {
            SingleVouchItem(
                vouch = sampleVouch,
                canSettle = canSettle,
                onSettle = {
                    canSettle = !canSettle
                    itemKey++
                },
                onDelete = {
                    itemKey++
                },
                onEdit = {},
                onPersonClick = {}
            )
        }

        TipCard(
            icon = Icons.Outlined.Lightbulb,
            text = stringResource(R.string.onboarding_slide1_hint)
        )
    }
}

private const val ONBOARDING_GROUP_VOUCH_TOTAL_MINOR = 21000L

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GroupVouchesSlide() {
    val selfLabel = stringResource(R.string.group_vouch_self_member)
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val vouchTotalMinor = ONBOARDING_GROUP_VOUCH_TOTAL_MINOR
    val totalAmountString = remember(vouchTotalMinor) {
        CurrencyManager.formatAmount(vouchTotalMinor)
    }

    fun recomputeSplits(inputMembers: List<GroupMemberInput>): List<GroupMemberInput> {
        if (inputMembers.isEmpty()) return emptyList()

        val customSum = inputMembers
            .filter { it.isCustom }
            .sumOf { CurrencyManager.parseToMinorUnits(it.amount) ?: 0L }

        val nonCustomCount = inputMembers.count { !it.isCustom }
        if (nonCustomCount == 0) return inputMembers

        val remaining = maxOf(0L, vouchTotalMinor - customSum)
        val defaultAmount = remaining / nonCustomCount
        val defaultAmountStr = CurrencyManager.formatAmount(defaultAmount)

        return inputMembers.map { member ->
            if (member.isCustom) {
                member
            } else {
                member.copy(amount = defaultAmountStr)
            }
        }
    }

    var members by remember {
        mutableStateOf(
            recomputeSplits(
                listOf(
                    GroupMemberInput(personName = selfLabel, amount = "", isCustom = false, isSelf = true)
                )
            )
        )
    }

    val suggestedPersons = remember {
        listOf(
            PersonTotal(person = Person(id = 1, name = "Sarah"), amount = 0L, vouchCount = 10),
            PersonTotal(person = Person(id = 2, name = "Tom"), amount = 0L, vouchCount = 8),
            PersonTotal(person = Person(id = 3, name = "Alex"), amount = 0L, vouchCount = 5),
            PersonTotal(person = Person(id = 4, name = "Emma"), amount = 0L, vouchCount = 4),
            PersonTotal(person = Person(id = 5, name = "Lucas"), amount = 0L, vouchCount = 3)
        )
    }

    val currentMemberNames = remember(members) {
        members.map { it.personName.trim().lowercase() }.toSet()
    }
    val isSelfAdded = remember(members) { members.any { it.isSelf } }
    val availableSuggestions = remember(suggestedPersons, currentMemberNames) {
        suggestedPersons
            .filter { it.person.name.trim().lowercase() !in currentMemberNames }
            .sortedByDescending { it.vouchCount }
            .take(5)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        SlideHeader(
            title = stringResource(R.string.onboarding_slide2_title),
            subtitle = stringResource(R.string.onboarding_slide2_subtitle)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GroupVouchSummaryCard(
                amount = totalAmountString,
                isGiven = true,
                members = members,
                onResetAllToEqual = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    members = recomputeSplits(members.map { it.copy(isCustom = false) })
                }
            )

            GroupMemberSuggestionsRow(
                isSelfAdded = isSelfAdded,
                selfLabel = selfLabel,
                availableSuggestions = availableSuggestions,
                onAddSelf = { selfName ->
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    val newMember = GroupMemberInput(
                        personName = selfName,
                        amount = "",
                        isCustom = false,
                        isSelf = true
                    )
                    members = recomputeSplits(listOf(newMember) + members)
                },
                onPersonSelected = { person ->
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    val newMember = GroupMemberInput(
                        personName = person.name,
                        amount = "",
                        isCustom = false,
                        isSelf = false
                    )
                    members = recomputeSplits(members + newMember)
                },
                focusManager = focusManager
            )

            GroupMemberList(
                members = members,
                totalAmountMinor = vouchTotalMinor,
                onAmountChange = { target, newAmount ->
                    val updated = members.map { member ->
                        if (member.personName == target.personName) {
                            member.copy(amount = newAmount, isCustom = true)
                        } else {
                            member
                        }
                    }
                    members = recomputeSplits(updated)
                },
                onResetMember = { target ->
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    val updated = members.map { member ->
                        if (member.personName == target.personName) {
                            member.copy(isCustom = false)
                        } else {
                            member
                        }
                    }
                    members = recomputeSplits(updated)
                },
                onRemove = { target ->
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    members = recomputeSplits(members.filter { it.personName != target.personName })
                },
                focusManager = focusManager,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 160.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TipCard(
            icon = Icons.Outlined.Lightbulb,
            text = stringResource(R.string.onboarding_slide2_hint)
        )
    }
}

private enum class OnboardingQuickAction(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val icon: ImageVector
) {
    SETTLED_SPACE(
        titleRes = R.string.onboarding_action_settled_space_title,
        descriptionRes = R.string.onboarding_action_settled_space_desc,
        icon = Icons.Outlined.Archive
    ),
    ADD_VOUCH(
        titleRes = R.string.onboarding_action_add_title,
        descriptionRes = R.string.onboarding_action_add_desc,
        icon = Icons.Filled.Add
    ),
    SETTLE(
        titleRes = R.string.onboarding_action_settle_title,
        descriptionRes = R.string.onboarding_action_settle_desc,
        icon = Icons.Filled.DoneAll
    ),
    SHARE(
        titleRes = R.string.onboarding_action_share_title,
        descriptionRes = R.string.onboarding_action_share_desc,
        icon = Icons.Filled.Share
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PeopleSlide() {
    var selectedAction by remember { mutableStateOf<OnboardingQuickAction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        SlideHeader(
            title = stringResource(R.string.onboarding_slide3_title),
            subtitle = stringResource(R.string.onboarding_slide3_subtitle)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VouchBalanceHeroCard(
                amountGiven = 4500L,
                amountOwed = 1500L,
                total = 3000L,
                vouchCount = 3,
                modifier = Modifier.fillMaxWidth()
            )

            QuickActionsCard(
                onSettledSpaceClick = { selectedAction = OnboardingQuickAction.SETTLED_SPACE },
                onAddVouch = { selectedAction = OnboardingQuickAction.ADD_VOUCH },
                onSettleClick = { selectedAction = OnboardingQuickAction.SETTLE },
                onShareClick = { selectedAction = OnboardingQuickAction.SHARE },
                modifier = Modifier.fillMaxWidth()
            )
        }

        TipCard(
            icon = Icons.Outlined.Lightbulb,
            text = stringResource(R.string.onboarding_slide3_hint)
        )
    }

    selectedAction?.let { action ->
        AlertDialog(
            onDismissRequest = { selectedAction = null },
            icon = {
                Surface(
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = stringResource(action.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = stringResource(action.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedAction = null }) {
                    Text(
                        text = stringResource(R.string.onboarding_action_dialog_dismiss),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CurrencySlide(
    currentCurrencyCode: String,
    onCurrencySelected: (String) -> Unit,
    onNavigateToCurrencySearch: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val localeCurrency = remember { CurrencyManager.resolveDefaultCurrency() }
    val slots = remember(localeCurrency) { CurrencyManager.getQuickCurrencySlots(localeCurrency) }

    val isCurrentInSlots = remember(slots, currentCurrencyCode) {
        slots.any { it is CurrencyGridSlot.CurrencyItem && it.currency.currencyCode == currentCurrencyCode }
    }
    val customSelectedCurrency = remember(currentCurrencyCode, isCurrentInSlots) {
        if (!isCurrentInSlots) {
            try { Currency.getInstance(currentCurrencyCode) } catch (_: Exception) { null }
        } else {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        SlideHeader(
            title = stringResource(R.string.onboarding_slide4_title),
            subtitle = stringResource(R.string.onboarding_slide4_subtitle)
        )

        val chunkedSlots = remember(slots) { slots.chunked(3) }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chunkedSlots.forEach { rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowSlots.forEach { slot ->
                        when (slot) {
                            is CurrencyGridSlot.CurrencyItem -> {
                                val isSelected = slot.currency.currencyCode == currentCurrencyCode
                                CurrencyCard(
                                    currency = slot.currency,
                                    isSelected = isSelected,
                                    onSelect = {
                                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        onCurrencySelected(slot.currency.currencyCode)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            is CurrencyGridSlot.SearchButton -> {
                                CurrencySearchCard(
                                    isCustomSelected = !isCurrentInSlots,
                                    selectedCurrency = customSelectedCurrency,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        onNavigateToCurrencySearch()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(1.dp))
    }
}

@Composable
private fun SlideHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun TipCard(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
