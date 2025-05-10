package com.jobspot.data

import com.google.firebase.database.DatabaseReference

internal class FirebaseContext(database: DatabaseReference) {
    val users = database.child("users")
    val orders = database.child("orders")
    val services = database.child("services")
    val workerFiles = database.child("workerFiles")
}