package com.anyjob.domain.search.useCases

import com.anyjob.domain.search.interfaces.OrderRepository

class FinishOrderUseCase(private val orderRepository: OrderRepository) {
    suspend fun execute(orderId: String) {
        orderRepository.finishOrder(orderId)
    }
}