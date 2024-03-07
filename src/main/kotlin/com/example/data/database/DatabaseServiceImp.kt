package com.example.data.database

import com.example.data.*
import com.example.data.exceptions.MemberNotExistException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.insertOne

class DatabaseServiceImp(db : CoroutineDatabase) : DatabaseService {

    val userCollection  = db.getCollection<UserData>()

    val messageCollection = db.getCollection<Chats>()

    val chatDetailCollection = db.getCollection<ChatInfo>()

    override suspend fun insertUser(user: UserData) {
        userCollection.findOneAndReplace(
            filter = eq("_id", user.userId),
            replacement = user,
            options = FindOneAndReplaceOptions().upsert(true)
        )
    }

    override suspend fun insertChatDetail(chatDetail: ChatDetail) {

        val chatDetails = ChatInfo(
            userId = chatDetail.sender,
            chatList = listOf(chatDetail)
        )
        val existingInfo = chatDetailCollection.findOne(ChatInfo::userId eq chatDetail.sender)
        if (existingInfo != null){
            //exist
            val chatExist = chatDetailCollection.findOne(Document("chatList._id" ,"${chatDetail.chatId}"))
            if (chatExist != null){
                //chat detail exist want to update
                println("chatDetail -> chat detail exist want to update")
                val updatedChatList = existingInfo.chatList.map { detail ->
                    if (detail.chatId == chatDetail.chatId) {
                        detail.copy(
                            sender = chatDetail.sender,
                            receiver = chatDetail.receiver,
                            receiverPic = chatDetail.receiverPic,
                            receiverName = chatDetail.receiverName,
                            lastMessage = chatDetail.lastMessage, // Example update
                            timeStamp = chatDetail.timeStamp // Example update
                        )
                    } else {
                        detail // Keep other elements unchanged
                    }
                }

                val updatedChatInfo = existingInfo.copy(chatList = updatedChatList)
                chatDetailCollection.save(updatedChatInfo)

            }else
            {
                //chat not exist add inside array
                println("chatDetail -> chat not exist add inside array")
                val updatedChatDetail = existingInfo.copy(chatList = existingInfo.chatList +chatDetail)
                chatDetailCollection.save(updatedChatDetail)
            }

        }else {
            //not exist create
            chatDetailCollection.insertOne(chatDetails)
        }
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
        val chatId2 = "${message.recipientId}${message.senderId}"
        // Find the chat by chatId
        val existingChat = messageCollection.findOne(Chats::chatId eq chatId)
        val existingChat2 = messageCollection.findOne(Chats::chatId eq chatId2)

        // Create a new chat or update the existing one
        val updatedChat = existingChat?.copy(message = existingChat.message + message)
            ?: Chats(chatId = chatId, message = listOf(message))

        val updatedChat2 = existingChat2?.copy(message = existingChat2.message + message)
            ?: Chats(chatId = chatId2, message = listOf(message))

        // Save the chat
        messageCollection.save(updatedChat)
        messageCollection.save(updatedChat2)
    }

    override suspend fun getAllMessageById(chatId: String): Chats? {
       //get all message
        return messageCollection.findOne(Chats::chatId eq chatId)
    }

    override suspend fun findUserByName(userName: String): List<UserData> {

       // val userNameRegexQuery = Document("userName", Document("\$regex", userName).append("\$options", "i"))
        val userNameRegexQuery = Document("userName", Document("\$regex", "^$userName").append("\$options", "i"))
        val matchingUsers = userCollection.find(userNameRegexQuery).toList()

        return matchingUsers
    }

    override suspend fun getUserUChatWith(userId : String): ChatInfo? {
        return chatDetailCollection.findOne(ChatInfo::userId eq userId)
    }

    override suspend fun deleteChatById(userId: String , chatId: String) : Boolean{
        var chats = chatDetailCollection.findOne(ChatInfo::userId eq userId)
        if (chats != null) {
            val chatList = chats.chatList
            val filteredChatList = chatList.filterNot { chatDetail -> chatDetail.chatId == chatId }
            chats.chatList = filteredChatList
            chatDetailCollection.save(chats)

            //deleting chat also
            messageCollection.findOneAndDelete(Chats::chatId eq chatId)
            return true
        }
        return false
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