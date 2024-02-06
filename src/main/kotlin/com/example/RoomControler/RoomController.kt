package com.example.RoomControler
import com.example.data.Chats
import com.example.data.Member
import com.example.data.UserData
import com.example.data.UserMessage
import com.example.data.database.DatabaseServiceImp
import com.example.data.exceptions.MemberAlreadyExistException
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.litote.kmongo.json
import java.util.concurrent.ConcurrentHashMap


/*here we added user to the map that are online users
 when user send msg , msg hold the receiver id , we use that and send it to the receiver
*/
class RoomController(
   val dbSource : DatabaseServiceImp
) {

    val member = ConcurrentHashMap<String, Member>()

    fun onJoin(
        userId : String, //id
        userName : String,
        sessionId :String,
        socket : WebSocketSession
    )
    {
        println("New Joined Member ->${userId} ${userName}")
        if (member.containsKey(userId))
        {
           throw MemberAlreadyExistException()
        }
        member[userId]  = Member(
            id = userId,
            userName = userName,
            sessionId = sessionId,
            socket = socket
        )
    }



    suspend fun sendMessage(message: UserMessage)
    {
        //send message if user is in online mode or inside hashmap
        val receiver = message.recipientId
        val receiverSocket = member[receiver]
        if (member.containsKey(receiver))
        {
           val msg = Json.encodeToString(message)
           val msgJson = Json.encodeToJsonElement(message)  //not recommended
           receiverSocket?.socket?.send(msg)
        }

        //also add message to database to the specific id // reason to implement push notification
        //dbSource.insertMessages(message)
    }
    suspend fun saveMessageInDb(message: UserMessage)
    {
        //also add message to database to the specific id
        dbSource.insertMessages(message)
    }


    suspend fun getAllMessages(chatId : String): Chats? {
        return dbSource.getAllMessageById(chatId)
    }

    suspend fun getUserById(userId: String) : UserData{
       return dbSource.getUserById(userId)
    }

     suspend fun disconnectUser(userId: String)
     {
         member[userId]?.socket?.close()
         if (member.containsKey(userId)){
             member.remove(userId)
         }
     }

    suspend fun checkOnlineStatus(message: UserMessage) : Boolean{
        val recipientId = message.recipientId
        if (member.containsKey(recipientId))
        {
            return true
        }
        return false
    }
}

