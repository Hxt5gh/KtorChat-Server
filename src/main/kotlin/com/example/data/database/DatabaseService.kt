package com.example.data.database

import com.example.data.Chats
import com.example.data.UserData
import com.example.data.UserMessage

interface DatabaseService {

    suspend fun insertUser(user : UserData)
    suspend fun getUserById(userID  : String) : UserData
    suspend fun insertMessages(message: UserMessage)
    suspend fun getAllMessageById(chatId : String) : Chats?

}