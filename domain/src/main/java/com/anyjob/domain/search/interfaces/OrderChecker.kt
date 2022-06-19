package com.anyjob.domain.search.interfaces;

interface OrderChecker {
    fun start(orderId: String, onOrderChanged: (isFinished: Boolean, isCanceled: Boolean) -> Unit)
}
