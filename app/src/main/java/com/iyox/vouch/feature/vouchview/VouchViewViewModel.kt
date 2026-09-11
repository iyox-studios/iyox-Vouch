package com.iyox.vouch.feature.vouchview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.domain.SharedVouchActions
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.model.isSettled
import com.iyox.vouch.navigation.VouchViewRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class VouchViewUiState(
    val vouch: VouchWithPerson? = null,
    val isLoading: Boolean = false,
    val isSettled: Boolean = false
)

@HiltViewModel
class VouchViewViewModel @Inject constructor(
    private val vouchRepository: VouchRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val route: VouchViewRoute? = runCatching { savedStateHandle.toRoute<VouchViewRoute>() }.getOrNull()
    val vouchId: Long = route?.vouchId ?: savedStateHandle.get<Long>("vouchId") ?: 0L

    val sharedActions = SharedVouchActions(vouchRepository)

    private val initialVouch: VouchWithPerson? =
        vouchRepository.vouchesFlow.value.firstOrNull { it.vouch.id == vouchId }

    val uiState: StateFlow<VouchViewUiState> = vouchRepository.vouchesFlow
        .map { vouches ->
            val vouch = vouches.firstOrNull { it.vouch.id == vouchId }
            VouchViewUiState(
                vouch = vouch,
                isLoading = false,
                isSettled = vouch?.isSettled() ?: false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VouchViewUiState(
                vouch = initialVouch,
                isLoading = initialVouch == null && vouchRepository.vouchesFlow.value.isEmpty(),
                isSettled = initialVouch?.isSettled() ?: false
            )
        )
}
