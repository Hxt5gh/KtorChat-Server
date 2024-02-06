package com.example.di

import com.example.RoomControler.RoomController
import com.example.data.database.DatabaseService
import com.example.data.database.DatabaseServiceImp
import org.koin.dsl.module
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo


val mainModule = module {
    single {
        KMongo.createClient(
            connectionString = "mongodb+srv://laptopharpreet:root@cluster0.r0egyzt.mongodb.net/"
        ).coroutine.getDatabase("chat_app")
    }
    single<DatabaseService> {
        DatabaseServiceImp(get())
    }
    single {
        RoomController(get())
    }
}