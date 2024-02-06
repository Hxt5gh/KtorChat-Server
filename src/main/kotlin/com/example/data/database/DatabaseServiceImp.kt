package com.example.data.database

import com.example.data.Chats
import com.example.data.UserData
import com.example.data.UserMessage
import com.example.data.exceptions.MemberNotExistException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.addToSet
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq

class DatabaseServiceImp(db : CoroutineDatabase) : DatabaseService {

    val userCollection  = db.getCollection<UserData>()

    val messageCollection = db.getCollection<Chats>()

    override suspend fun insertUser(user: UserData) {
       userCollection.insertOne(user)
    }

    override suspend fun getUserById(userID: String): UserData {
        val user = userCollection.findOne(UserData::userId eq userID)
        if(user == null)
        {
            throw MemberNotExistException()
        }
        return user
    }


    override suspend fun insertMessages(message: UserMessage) {

        //insert message
        val chatId = "${message.senderId}${message.recipientId}"
        // Find the chat by chatId
        val existingChat = messageCollection.findOne(Chats::chatId eq chatId)

        // Create a new chat or update the existing one
        val updatedChat = existingChat?.copy(message = existingChat.message + message)
            ?: Chats(chatId = chatId, message = listOf(message))

        // Save the chat
        messageCollection.save(updatedChat)
    }

    override suspend fun getAllMessageById(chatId: String): Chats? {
       //get all message
        return messageCollection.findOne(Chats::chatId eq chatId)
    }

    suspend fun insertMessageOptimized(message: UserMessage) {
        // Create the chat ID by combining sender and receiver IDs
        val chatId = "${message.senderId}${message.recipientId}"

        // Create the filter to find the chat by chatId
        val filter = Filters.eq("chatId", chatId)

        // Check if the chat exists
        val existingChat = messageCollection.findOne(filter)

        if (existingChat != null) {
            // If the chat exists, add the message to the 'messages' array
            val update = Updates.push("messages", message)
            messageCollection.updateOne(filter, update)
        } else {
            // If the chat does not exist, create a new chat document with the message
            val newChat = Chats(chatId = chatId, message = listOf(message))
            messageCollection.insertOne(newChat)
        }
    }

}