package com.example.plugins


import com.example.RoomControler.RoomController
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.util.*

fun Application.configureSecurity(roomController: RoomController) {
    install(Sessions) {
        cookie<MySession>("SESSION")
    }

    intercept(ApplicationCallPipeline.Features){
        if(call.sessions.get<MySession>() == null){
            val userId = call.parameters["userId"] ?: "Guest"
            call.sessions.set(MySession(userId , generateNonce()))
        }
    }
}


//we can set up recipient feature here also
data class MySession(
    val userId : String, //senderId
    val sessionId :String
)