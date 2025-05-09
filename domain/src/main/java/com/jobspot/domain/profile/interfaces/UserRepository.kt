package com.jobspot.domain.profile.interfaces

import com.jobspot.domain.authorization.ProfileCreationParameters
import com.jobspot.domain.profile.models.MapAddress
import com.jobspot.domain.profile.models.User

/**
 * Определяет репозиторий пользователя
 */
interface UserRepository {
    /**
     * Создает личный кабинет для пользователя
     * @param parameters Личные данные пользователя
     */
    suspend fun createProfile(parameters: ProfileCreationParameters)

    /**
     * Возвращает список сотрудников, доступных для получения заказа
     */
    suspend fun getAvailableWorkers(): List<User>

    suspend fun addRateToUser(userId: String, rate: Float)

    suspend fun updateGeolocation(userId: String, geolocation: MapAddress)

    suspend fun getUser(id: String): User?
}