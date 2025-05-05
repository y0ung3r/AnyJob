package com.jobspot.data.search.entities

import com.jobspot.domain.profile.models.MapAddress
import com.jobspot.domain.services.models.Service
import java.io.Serializable

/**
 * Заказ
 */
internal class OrderEntity : Serializable {
    /**
     * Идентификатор заказа
     */
    var id: String? = null

    /**
     * Идентификатор заказчика
     */
    var invokerId: String? = null

    /**
     * Идентификатор исполнителя
     */
    var executorId: String? = null

    /**
     * Географические координаты
     */
    var address: MapAddress? = null

    /**
     * Радиус поиска (в метрах)
     */
    var searchRadius: Double? = null

    /**
     * Отменен ли вызов
     */
    var isCanceled: Boolean = false

    /**
     * Завершен ли вызов
     */
    var isFinished: Boolean = false

    /**
     * Список идентификаторов исполнителей, которые не могут получить данный заказ
     */
    var excludedExecutors: ArrayList<String> = arrayListOf()

    var service: Service? = null
}