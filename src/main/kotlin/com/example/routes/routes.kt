package com.example.routes

import com.example.RoomControler.PushNotification
import com.example.RoomControler.RoomController
import com.example.callInsertTest
import com.example.data.*
import com.example.data.database.DatabaseServiceImp
import com.example.data.exceptions.MemberAlreadyExistException
import com.example.data.exceptions.MemberNotExistException
import com.example.data.remote.Notification
import com.example.data.remote.NotificationMessage
import com.example.data.remote.OneSignalService
import com.example.data.remote.OneSignalServiceImp
import com.example.plugins.MySession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

fun Route.chatSocket(roomController: RoomController , notification: PushNotification ,databaseService: DatabaseServiceImp){

    webSocket("/chat-socket"){
        val session = call.sessions.get<MySession>()
        if (session == null)
        {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY , "NO session"))
            return@webSocket
        }

        try {
            val data = roomController.getUserById(session.userId)
            roomController.onJoin(
                userId = session.userId,
                userName = data.userName, // update later , get from the database  , on clint side first we need to store user detail while firebase ,
                sessionId = session.sessionId,
                socket = this
            )
            //#testing
            roomController.member.forEach {
                println("MemberInHash : ${it.value.id} -> ${it.value.userName}")
            }

            incoming.consumeEach { frame ->
                if(frame is Frame.Text)
                {
                    // Convert the received JSON string to a UserMessage object
                    val userMessage = Json.decodeFromString<UserMessage>(frame.readText())

                    //saving message detail to databse
                   val detailChatData = ChatDetail(
                       chatId = "${userMessage.senderId}${userMessage.recipientId}",
                       sender = userMessage.senderId,
                       receiver = userMessage.recipientId,
                       receiverName = roomController.getUserById(userMessage.recipientId).displayName,
                       receiverPic = roomController.getUserById(userMessage.recipientId).profileUri,
                       lastMessage = userMessage.message,
                       timeStamp =userMessage.timeStamp
                   )
                    databaseService.insertChatDetail(detailChatData)

                    val detailChatData2 = ChatDetail(
                        chatId = "${userMessage.recipientId}${userMessage.senderId}",
                        sender = userMessage.recipientId,
                        receiver = userMessage.senderId,
                        receiverName = roomController.getUserById(userMessage.senderId).displayName,
                        receiverPic = roomController.getUserById(userMessage.senderId).profileUri,
                        lastMessage = userMessage.message,
                        timeStamp =userMessage.timeStamp
                    )
                    databaseService.insertChatDetail(detailChatData2)

                    checkOnlineOrOffline(roomController , userMessage , notification)
                }
                else if (frame is Frame.Binary){

                }
            }
        }catch(e : MemberAlreadyExistException){
            call.respond(HttpStatusCode.Conflict , e.message.toString())
        }catch (e : MemberNotExistException){
            call.respond(HttpStatusCode.NotFound , e.message.toString())
        }catch (e : Exception){
            println(e.printStackTrace())
        }finally {

            roomController.disconnectUser(session.userId)
            //#testing
            roomController.member.forEach {
                println("MemberInHash : ${it.value.id} -> ${it.value.userName}")
            }
        }
    }
}

suspend fun checkOnlineOrOffline(roomController: RoomController, userMessage: UserMessage , notification: PushNotification) {

    //#testing
    roomController.member.forEach {
        println("MemberInHash : ${it.value.id} -> ${it.value.userName}")
    }

    if (roomController.checkOnlineStatus(userMessage)) {
        //online
        println("debugstatus -> ${userMessage}")
        roomController.saveMessageInDb(userMessage) //save into db
        roomController.sendMessage(userMessage)
    }else
    {
        //offline
        println("debugstatus -> ${userMessage}")
        roomController.saveMessageInDb(userMessage) //save into db
        notification.sendNotification(userMessage)
        println("Notification Send")
        println("New Message : ${userMessage.message} at ${millisecondsToTime(userMessage.timeStamp)}")

    }

}

fun millisecondsToTime(milliseconds: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val calendar = Calendar.getInstance().apply {
        timeInMillis = milliseconds
    }
    return formatter.format(calendar.time)
}

fun Route.getAllMessageById(roomController: RoomController) {
    get("/getMessages") {
        // Retrieve the value of the 'id' parameter from the query string
        val chatId = call.parameters["id"]

        println("getMessage -> ${chatId}")

        // Check if the 'id' parameter is present
        if (!chatId.isNullOrBlank()) {
            // Call the function to get messages for the specified ID
            val messages = roomController.getAllMessages(chatId)

            // Respond with the messages
           // call.respond(HttpStatusCode.OK, Json.encodeToString(messages))
            if (messages != null){
            call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(Chats.serializer(),messages))
            }
            else
            {
                call.respond(HttpStatusCode.OK, emptyList<Chats>())
            }
        } else {
            // If 'id' parameter is missing, respond with an error
            call.respondText("Missing 'id' parameter", status = HttpStatusCode.BadRequest)
        }
    }
}

fun Route.getUserById(roomController: RoomController) {
    get("/get-user") {
        // Retrieve the value of the 'userid' parameter from the query string
        val userId = call.parameters["userid"]

        // Check if the 'userId' parameter is present
        if (!userId.isNullOrBlank()) {
            // Call the function to get user for the specified userId
            val user = roomController.getUserById(userId)

            // Respond with the messages
            // call.respond(HttpStatusCode.OK, Json.encodeToString(messages))
            call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(UserData.serializer(),user))
        } else {
            // If 'id' parameter is missing, respond with an error
            call.respondText("Missing 'userID' parameter", status = HttpStatusCode.BadRequest)
        }
    }
}

fun Route.saveUserDetailToDatabase(databaseService: DatabaseServiceImp) {
    post("save-user") {
        try {
            val data = call.receive<UserData>()
            // Check if the received data is of type UserData
            if (data != null) {
                // Insert the data into the database
                databaseService.insertUser(data)
                // Respond with a success message
                call.respondText("User data saved successfully", status = HttpStatusCode.OK)
            } else {
                // If the received data is not of type UserData, return an error response
                call.respondText("Data is not of type UserData", status = HttpStatusCode.BadRequest)
            }
        } catch (e: Exception) {
            // Handle any exceptions that occur during data processing
            call.respondText("Error processing request", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun  Route.searchUserByUserName(databaseService: DatabaseServiceImp){
   get("search-user") {
       val userName = call.parameters["userName"] ?: return@get call.respondText(
           "Missing provide  \"userName\" to search",
           status = HttpStatusCode.BadRequest
       )
       val list = databaseService.findUserByName(userName)
       println("SearchedUser -> ${userName} ${list}")
       if (list.isEmpty()) call.respond(HttpStatusCode.OK , "No User Exist With That Name")

      // list.map { UserData(it.userId, it.userName) }
       val result : MutableList<UserData> = mutableListOf()
       list.forEach {
           val id = it.userId
           val usersName = it.userName
           val displayName = it.displayName
           val imageUrl = it.profileUri
           result.add(UserData(userId = id, userName = usersName , displayName =  displayName , profileUri = imageUrl))

       }


       call.respond(HttpStatusCode.OK, Json.encodeToString(result))
      // call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(result))
      // call.respond(HttpStatusCode.OK,result)

   }
}

fun Route.insertUserChatDetails(databaseService: DatabaseServiceImp) {
    post("save-chats") {

        val data = call.receive<ChatDetail>()

        if (data != null){
            databaseService.insertChatDetail(data)
            call.respond(HttpStatusCode.OK ,"Saved")

        }else
        {
            call.respond(HttpStatusCode.BadRequest ,"Body missing")
        }

    }
}


fun Route.getUserYouHaveChatWith(databaseService: DatabaseServiceImp){
    get("/get-chat-with") {
        // Retrieve the value of the 'userid' parameter from the query string
        val userId = call.parameters["userid"]

        // Check if the 'userId' parameter is present
        if (!userId.isNullOrBlank()) {
            // Call the function to get chat detail for the specified userId
            val userChat = databaseService.getUserUChatWith(userId)

            if (userChat == null){
                call.respondText("No Chat detail Exist with that 'userID'", status = HttpStatusCode.BadRequest)
            }
            // Respond with the messages
            call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(ChatInfo.serializer() , userChat!!))
        } else {
            // If 'id' parameter is missing, respond with an error
            call.respondText("Missing 'userID' parameter", status = HttpStatusCode.BadRequest)
        }
    }
}

fun Route.findRandomPeer(roomController: RoomController){
   webSocket("/get-peer") {
       val session = call.sessions.get<MySession>()
       println("new peer size ${roomController.roomPeer.size} with session ${session?.userId}  ${session?.sessionId}")
       if (session == null)
       {
           close(CloseReason(CloseReason.Codes.VIOLATED_POLICY , "NO session"))
           return@webSocket
       }
       try {
           if (roomController.roomPeer.isEmpty())
           {
               println("new peer ${session.userId}")
               roomController.addPeer(
                   PeerRoom(
                       userId = session.userId,
                       sessionId = session.sessionId,
                       socket = this
                   )
               )
//               val msg = Json.encodeToString(PeerRes("waiting for match ${roomController.roomPeer.size}"))
//               this.send(msg)
              
           }
           else
           {
               if (!roomController.isHas(
                       PeerRoom(
                           userId = session.userId,
                           sessionId = session.sessionId,
                           socket = this
                       )
                   )
               ) {

                   //user exist waiting for peer
                   val firstPeer = roomController.getPeer()
                   //send to first
                   val msgOne = Json.encodeToString(PeerRes(session.userId))
                   firstPeer.socket.send(msgOne)


                   //send to second
                   val msgTwo = Json.encodeToString(PeerRes(firstPeer.userId))
                   this.send(msgTwo)
               }else
               {
                   return@webSocket
                   this.close()
               }

           }

           incoming.consumeEach { frame ->
               if(frame is Frame.Text)
               {
               }
           }
       }catch(e : MemberAlreadyExistException){
           call.respond(HttpStatusCode.Conflict , e.message.toString())
       }catch (e : MemberNotExistException){
           call.respond(HttpStatusCode.NotFound , e.message.toString())
       }catch (e : Exception){
           println(e.printStackTrace())
       }finally {
            this.close()
       }
   }
}

fun Route.deleteChatWithChatId(databaseService: DatabaseServiceImp){
    get("delete-chat") {
        val userId = call.parameters["userId"] ?: return@get call.respondText( "userId is missing" ,status = HttpStatusCode.BadRequest)
        val chatId = call.parameters["chatId"] ?: return@get call.respondText("chatId is missing",status = HttpStatusCode.BadRequest)

        if (chatId != null && userId != null){
            if (databaseService.deleteChatById(userId , chatId))
                 call.respond(HttpStatusCode.OK, message = "deleted")
            else
                call.respond(HttpStatusCode.NotFound , "not exist")
        }
    }
}

fun Route.findRtcPeer(roomController: RoomController){
    webSocket("/get-rtc-peer") {
        val session = call.sessions.get<MySession>()
//        println("new peer size ${roomController.callQueue.size} hash ${roomController.callMember.size} with session ${session?.userId}  ${session?.sessionId}")
        if (session == null)
        {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY , "NO session"))
            return@webSocket
        }
        try {
            //adding user to hash
            roomController.addMemberToHash(PeerRoom(userId = session.userId , sessionId = session.sessionId , socket = this))
            println("RTCDEBUG  -> member in hash : ${roomController.callMember.size}")
            roomController.callMember.forEach { t, u ->
                println("RTCDEBUG  -> member in hash : ${t} ${u}")
            }

            incoming.consumeEach { frame ->
                if(frame is Frame.Text)
                {
                    val message = Json.decodeFromString<RtcObject>(frame.readText())

                    println("rtcdebug ${message}")
                    when(message.type){
                        "type_offer" ->{
                            if (roomController.callQueue.isEmpty()) {
                                roomController.addMemberToQueue(message)
                            } else {

                                //member exist get member from the queue

                                //getting member from hash
                                val firstMember = roomController.getMemberFromQueue()
                                val offerToSecond = Json.encodeToString(
                                    RtcObject(
                                        type = firstMember!!.type,
                                        sender = firstMember.sender, //who sending offer
                                        target = session.userId,
                                        sdp = firstMember.sdp,
                                        offer = firstMember.offer,
                                        iceCandidateModel = firstMember.iceCandidateModel
                                    )
                                )


                                //sending offer to second  client (by passsing first's sdp)
                                roomController.callMember[message.sender]?.socket?.send(offerToSecond)

                                /*
                                val offerToFirst = Json.encodeToString(
                                    RtcObject(
                                        type = message.type,
                                        sender = message.sender, //who sending offer
                                        target = firstMember.sender,
                                        sdp = message.sdp,
                                        offer = message.offer
                                    )
                                )
                                println("RTCDEBUG sending to first ${offerToFirst}")
                                //also sending offer to first (by passing current client's)
                                roomController.callMember[firstMember.sender]?.socket?.send(offerToFirst)
                                */

                            }
                        }
                        "type_answer" ->{
                            //getting answer from second user and sending back to user one
                            val answer = Json.encodeToString(
                                RtcObject(
                                    type = message.type, //answer from clint side
                                    sender = message.sender, //who sending offer
                                    target = message.target, //to send
                                    sdp = message.sdp,
                                    offer = message.offer
                                )
                            )
                            roomController.callMember[message.target]?.socket?.send(answer)
                        }
                        "ice_candidate" ->{
                        }
                    }
                }
            }
        }catch(e : MemberAlreadyExistException){
            call.respond(HttpStatusCode.Conflict , e.message.toString())
        }catch (e : MemberNotExistException){
            call.respond(HttpStatusCode.NotFound , e.message.toString())
        }catch (e : Exception){
            println(e.printStackTrace())
        }finally {
            roomController.callMember.remove(session.userId)
            this.close()

        }
    }
}
//Testing
fun Route.sendNotification(oneSignalService: OneSignalService){
    post("/sendNotification") {
        val message = call.parameters["message"] ?: "New Message "

        println("Notification -> ${message}")



        try {
           val success =  oneSignalService.sendNotification(
                Notification(
                    includeExternalUserIds = listOf( "Ldu9N8DTIjfUApMlLUhuwzxZszt1" ,"8UDX0ydCIsYohnnSqrwd5oM74232"),
                    headings = NotificationMessage(en = "New Message"),
                    contents = NotificationMessage(en = message),
                    appId = OneSignalService.ONE_SIGNAL_APP_ID
                )
            )
            if (success)
            {
                call.respond(HttpStatusCode.OK)
            }else
            {
                call.respond(HttpStatusCode.Conflict)
            }
        }catch (e : Exception){
            e.printStackTrace()
            println(e.message)
        }
    }
}
