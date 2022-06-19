package com.anyjob.ui.explorer.search.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.anyjob.domain.profile.models.User
import com.anyjob.domain.search.OrderCreationParameters
import com.anyjob.domain.search.models.Order
import com.anyjob.domain.search.useCases.CancelSearchUseCase
import com.anyjob.domain.search.useCases.SearchWorkerUseCase
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchWorkerUseCase: SearchWorkerUseCase,
    private val cancelSearchUseCase: CancelSearchUseCase
) : ViewModel() {
    fun startWorkerSearching(orderParameters: OrderCreationParameters, onWorkerFound: (User) -> Unit): LiveData<Order> = liveData {
        emit(
            searchWorkerUseCase.execute(orderParameters, onWorkerFound)
        )
    }

    fun cancelWorkerSearching(order: Order) {
        viewModelScope.launch {
            cancelSearchUseCase.execute(order.id)
        }
    }
}