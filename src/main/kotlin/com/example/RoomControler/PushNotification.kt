package com.example.RoomControler

import com.example.data.UserMessage
import com.example.data.remote.Notification
import com.example.data.remote.NotificationMessage
import com.example.data.remote.OneSignalService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

class PushNotification(private  val oneSignalService: OneSignalService) {
     suspend fun sendNotification(
         message: UserMessage
     ) : Boolean
     {
        return try {
            val success = oneSignalService.sendNotification(
                Notification(
                    includeExternalUserIds = listOf(message.recipientId),
                    headings = NotificationMessage(en = "New Message"),
                    contents = NotificationMessage(en = message.message),
                    appId = OneSignalService.ONE_SIGNAL_APP_ID
                )
            )
            if (success) true else false
         }catch (e : Exception){
             e.printStackTrace()
             println(e.message)
            return false
         }
     }
}