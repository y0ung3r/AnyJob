package com.anyjob.data.profile

import com.anyjob.data.FirebaseContext
import com.anyjob.data.profile.entities.UserEntity
import com.anyjob.data.profile.interfaces.UserDataSource
import kotlinx.coroutines.tasks.await
import com.google.firebase.database.ktx.getValue

internal class UserDataSourceImplementation(private val context: FirebaseContext) : UserDataSource {
    override suspend fun getUser(id: String) : UserEntity? {
        val snapshot = context.users.child(id).get().await()
        return snapshot.getValue<UserEntity>()
    }

    override fun addUser(userEntity: UserEntity) {
        context.users.child(userEntity.id).setValue(userEntity)
    }
}