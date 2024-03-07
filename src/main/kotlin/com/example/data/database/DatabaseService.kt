package com.example.data.database

import com.example.data.*

interface DatabaseService {

    suspend fun insertUser(user : UserData)
    suspend fun insertChatDetail(chatDetail : ChatDetail)
    suspend fun getUserById(userID  : String) : UserData
    suspend fun insertMessages(message: UserMessage)
    suspend fun getAllMessageById(chatId : String) : Chats?
    suspend fun findUserByName(userName : String) : List<UserData>
    suspend fun getUserUChatWith(userId : String) : ChatInfo?
    suspend fun deleteChatById(userId: String ,chatId: String) : Boolean

}