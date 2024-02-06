package com.example.data.remote

interface OneSignalService {
    suspend fun sendNotification(notification: Notification) : Boolean

    companion object{
       const val ONE_SIGNAL_APP_ID = "970f2905-34f2-4905-8d93-5c60b4c5fc55"
//        const val NOTIFICATIONS = "https://api.onesignal.com/notifications"
        const val NOTIFICATIONS = "https://onesignal.com/api/v1/notifications"
    }
}