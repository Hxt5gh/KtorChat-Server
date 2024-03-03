package com.example.RoomControler
import com.example.data.*
import com.example.data.database.DatabaseServiceImp
import com.example.data.exceptions.MemberAlreadyExistException
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.litote.kmongo.json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue


/*here we added user to the map that are online users
 when user send msg , msg hold the receiver id , we use that and send it to the receiver
*/
class RoomController(
   val dbSource : DatabaseServiceImp
) {

    val member = ConcurrentHashMap<String, Member>()

    val roomPeer = ConcurrentLinkedQueue<PeerRoom>()

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



    suspend fun addPeer(user : PeerRoom)
    {
        roomPeer.offer(PeerRoom(userId = user.userId , sessionId = user.sessionId , socket = user.socket))
        CoroutineScope(Dispatchers.Default).launch {
            removePeer(user)
        }
    }
    private suspend fun removePeer(user: PeerRoom) {
        println("new remove start size ${roomPeer.size}")
        delay(5000L) // Delay for  seconds
        roomPeer.remove(user)
        println("new remove agter 5 size ${roomPeer.size}")
    }
    suspend fun getPeer() : PeerRoom
    {
       val otherUserId = roomPeer.poll()
       return otherUserId
    }

    fun isHas(user : PeerRoom) : Boolean {
        return roomPeer.any { it.userId == user.userId }
    }




}


