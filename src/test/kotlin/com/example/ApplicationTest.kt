package com.example

import com.example.RoomControler.PushNotification
import com.example.RoomControler.RoomController
import com.example.data.database.DatabaseServiceImp
import com.example.data.remote.OneSignalServiceImp
import com.example.plugins.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {

    }
}
