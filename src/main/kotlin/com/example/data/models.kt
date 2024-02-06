package com.example.data

import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId


@Serializable
data class UserData(
    @BsonId val userId : String ,//google id
    val userName : String,
)

@Serializable
data class Member(
    val id : String,
    val userName : String,
    val sessionId : String,
    val socket : WebSocketSession
)


@Serializable
data class UserMessage(
    @BsonId
    val id: String = ObjectId().toString(),
    val senderId : String,
    val recipientId: String,
    val message : String ,
    val timeStamp : Long
)

@Serializable
data class Chats(
     @BsonId val chatId : String,
    val message: List<UserMessage>
)

