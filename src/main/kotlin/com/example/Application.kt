package com.example
import com.example.RoomControler.PushNotification
import com.example.RoomControler.RoomController
import com.example.data.ChatInfo
import com.example.data.UserData
import com.example.data.UserMessage
import com.example.data.database.DatabaseServiceImp
import com.example.data.remote.OneSignalServiceImp
import com.example.plugins.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.server.application.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo


//dont
//970f2905-34f2-4905-8d93-5c60b4c5fc55

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

//    startKoin{
//        modules(mainModule)
//    }

    val client = HttpClient(CIO)

    val db = KMongo.createClient(
        connectionString = environment.config.property("onesignal.db_key").getString()
    ).coroutine.getDatabase("chat_app")
    val dataSource = DatabaseServiceImp(db)
    val roomController = RoomController(dataSource)

    val api = environment.config.property("onesignal.api_key").getString()
    val serviceNotify = OneSignalServiceImp(client , api)


    //
    val notification = PushNotification(serviceNotify)



    GlobalScope.launch {

//        dataSource.insertUser(
//            UserData(
//            userId = "james",
//            userName = "james",
//            displayName = "JD2",
//            profileUri = "some uri"
//        ))
        //Test insertcaht detail
//        dataSource.insertChatDetail(ChatInfo(
//            chatId = "NayanNayan",
//            sender = "Nayan",
//            receiver = "Nayan"
//        ))

       // callInsertTest(inset)
//       val data =  dataSource.getAllMessageById("12")
//        println(data.toString())
//        val data = dataSource.getUserById("Ldu9N8DTIjfUApMlLUhuwzxZszt1")
//        println("User by id ->   ${data.userId} ${data.userName}")
    }


    configureSockets()
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureRouting(roomController , dataSource , serviceNotify , notification)
}

suspend fun callInsertTest(inset: DatabaseServiceImp) {
    inset.insertMessages(
        UserMessage(
            senderId = "Harry",
            recipientId = "James" ,
            message = "Test one",
            timeStamp = System.currentTimeMillis()
        )
    )
    inset.insertMessages(
        UserMessage(
            senderId = "Harry",
            recipientId = "James" ,
            message = "Test two",
            timeStamp = System.currentTimeMillis()
        )
    )

    inset.insertMessages(
        UserMessage(
            senderId = "Harry",
            recipientId = "James" ,
            message = "Test three",
            timeStamp = System.currentTimeMillis()
        )
    )
    inset.insertMessages(
        UserMessage(
            senderId = "Harry",
            recipientId = "James" ,
            message = "Test four",
            timeStamp = System.currentTimeMillis()
        )
    )
    inset.insertMessages(
        UserMessage(
            senderId = "Harry",
            recipientId = "James" ,
            message = "Test five",
            timeStamp = System.currentTimeMillis()
        )
    )

    inset.insertMessages(
        UserMessage(
            senderId = "Harry",
            recipientId = "James" ,
            message = "Test six",
            timeStamp = System.currentTimeMillis()
        )
    )
}
