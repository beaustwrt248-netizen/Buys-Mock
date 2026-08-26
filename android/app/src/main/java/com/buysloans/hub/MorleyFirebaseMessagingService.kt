package com.buysloans.hub

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MorleyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (AuthManager.isSignedIn(this)) DeviceRegistrar.register(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "B&L Morley"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "You have a new message."
        NotificationInboxStore.add(this, title, body, message.data["type"] ?: "admin")
        NotificationHelper.showRemoteMessage(this, title, body)
    }
}
