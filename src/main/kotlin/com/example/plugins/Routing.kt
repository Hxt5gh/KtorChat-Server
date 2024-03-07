package com.example.plugins

import com.example.RoomControler.PushNotification
import com.example.RoomControler.RoomController
import com.example.data.database.DatabaseServiceImp
import com.example.data.remote.OneSignalServiceImp
import com.example.routes.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    roomController: RoomController,
    databaseServiceImp: DatabaseServiceImp,
    serviceNotify: OneSignalServiceImp,
    notification: PushNotification

) {
    routing {

        get("/") {
            call.respond("Working")
        }

        chatSocket(roomController, notification , databaseServiceImp)
        getAllMessageById(roomController)
        saveUserDetailToDatabase(databaseServiceImp)
        sendNotification(serviceNotify)
        searchUserByUserName(databaseServiceImp)
        insertUserChatDetails(databaseServiceImp)
        getUserById(roomController)
        getUserYouHaveChatWith(databaseServiceImp)
        findRandomPeer(roomController)
        deleteChatWithChatId(databaseServiceImp)
    }
}
