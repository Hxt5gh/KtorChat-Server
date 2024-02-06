package com.example.data.remote

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OneSignalServiceImp(
   private val httpClient: HttpClient,
    private val apiKey : String
) : OneSignalService {
    override suspend fun sendNotification(notification: Notification): Boolean {
        return try {
            httpClient.request(OneSignalService.NOTIFICATIONS){
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Basic ${apiKey}")
                }
//                parameter("include_player_ids", "cbf8d0cf-c701-46c1-866c-6cc2b5b90ce3")
                setBody(Json.encodeToString(Notification.serializer() , notification))
            }
            true
        } catch(e: Exception) {
            e.printStackTrace()
            false
        }
    }
}