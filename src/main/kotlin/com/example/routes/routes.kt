package com.example.routes

import com.example.RoomControler.PushNotification
import com.example.RoomControler.RoomController
import com.example.callInsertTest
import com.example.data.ChatInfo
import com.example.data.Chats
import com.example.data.UserData
import com.example.data.UserMessage
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

fun Route.chatSocket(roomController: RoomController , notification: PushNotification){

        val messageQueue = Channel<UserMessage>()


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
                   // messageQueue.send(userMessage)  // into queue
                    checkOnlineOrOffline(roomController , userMessage , notification)

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
        roomController.saveMessageInDb(userMessage) //save into db
        roomController.sendMessage(userMessage)
    }else
    {
        //offline
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

        // Check if the 'id' parameter is present
        if (!chatId.isNullOrBlank()) {
            // Call the function to get messages for the specified ID
            val messages = roomController.getAllMessages(chatId)

            // Respond with the messages
           // call.respond(HttpStatusCode.OK, Json.encodeToString(messages))
            call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(Chats.serializer(),messages!!))
        } else {
            // If 'id' parameter is missing, respond with an error
            call.respondText("Missing 'id' parameter", status = HttpStatusCode.BadRequest)
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
           val name = it.userName
           result.add(UserData(userId = id, userName = name))

       }


       call.respond(HttpStatusCode.OK, Json.encodeToString(result))
      // call.respond(HttpStatusCode.OK, Json.encodeToJsonElement(result))
      // call.respond(HttpStatusCode.OK,result)

   }
}

fun Route.insertUserChatDetails(databaseService: DatabaseServiceImp)
{
    post("save-chats") {
//        val parameters = call.receiveParameters()
//        val sender = parameters["sender"]
//        val receiver = parameters["receiver"]
        val sender = call.parameters["sender"]
        val receiver = call.parameters["receiver"]

        if (sender.isNullOrBlank())
        {
            call.respond(HttpStatusCode.BadRequest ,"sender is missing")
        }
        if (receiver.isNullOrBlank())
        {
            call.respond(HttpStatusCode.BadRequest ,"receiver is missing")
        }
        try {
            if (sender != null && receiver != null)
            {
                val chatId = "${sender}${receiver}"
                databaseService.insertChatDetail(
                    ChatInfo(
                    chatId = chatId,
                    sender = sender,
                    receiver = receiver
                    )
                )
                call.respond(HttpStatusCode.OK)
            }
        }catch (e : Exception)
        {
            call.respond(HttpStatusCode.InternalServerError ,e.message.toString())
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


