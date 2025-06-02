package com.example.aclass;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_msg";
    private static final String CHANNEL_ID = "FCM_CHANNEL";
    private static final String CHANNEL_NAME = "FCM Notifications";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM Token: " + token);
        // TODO: Optionally send this token to your server or database
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = null;
        String body = null;

        // Check if the message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        // Check if the message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            if (title == null) title = remoteMessage.getData().get("title");
            if (body == null) body = remoteMessage.getData().get("body");
        }

        if (title != null && body != null) {
            showNotification(title, body);
        } else {
            Log.e(TAG, "Notification title or body is null");
        }
    }

    private void showNotification(String title, String message) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // For Android 8.0 and above, create a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel for FCM notifications");
            if (manager != null) {
                manager.createNotificationChannel(channel);
            } else {
                Log.e(TAG, "NotificationManager is null. Channel creation failed.");
                return;
            }
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications) // Ensure this icon exists in res/drawable
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        } else {
            Log.e(TAG, "NotificationManager is null. Notification display failed.");
        }
    }
}





//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Intent;
//import android.os.Build;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.core.app.NotificationCompat;
//import androidx.core.app.NotificationManagerCompat;
//
//import com.google.firebase.messaging.FirebaseMessagingService;
//import com.google.firebase.messaging.RemoteMessage;
//
//import java.util.Map;
//
//public class MyFirebaseMessagingService extends FirebaseMessagingService {
//
//    private static final String TAG = "MyFirebaseMessaging";
//    private static final String CHANNEL_ID = "aclass_channel";
//
//    @Override
//    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
//        super.onMessageReceived(remoteMessage);
//
//        String title = null;
//        String body = null;
//
//        // Check for notification payload
//        if (remoteMessage.getNotification() != null) {
//            title = remoteMessage.getNotification().getTitle();
//            body = remoteMessage.getNotification().getBody();
//        } else {
//            // Fallback for data-only payload
//            Map<String, String> data = remoteMessage.getData();
//            title = data.getOrDefault("title", "Notification");
//            body = data.getOrDefault("body", "New message received");
//        }
//
//        // Log the received message for debugging
//        Log.d(TAG, "Received message: Title=" + title + ", Body=" + body);
//
//        showNotification(title, body);
//    }
//
//    private void showNotification(String title, String message) {
//        int notificationId = (int) System.currentTimeMillis();
//
//        // Create notification channel for Android 8.0+
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID,
//                    "AClass Notifications",
//                    NotificationManager.IMPORTANCE_HIGH
//            );
//            channel.setDescription("Notifications for AClass app updates and alerts");
//            NotificationManager manager = getSystemService(NotificationManager.class);
//            if (manager != null) {
//                manager.createNotificationChannel(channel);
//            } else {
//                Log.e(TAG, "NotificationManager is null, cannot create channel");
//                return;
//            }
//        }
//
//        // Create intent to launch MainActivity
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Bring existing activity to front
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                this, 0, intent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        // Build the notification
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle(title != null ? title : "Notification")
//                .setContentText(message != null ? message : "New message received")
//                .setSmallIcon(R.drawable.ic_notifications)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setContentIntent(pendingIntent)
//                .setAutoCancel(true);
//
//        // Display the notification
//        try {
//            NotificationManagerCompat.from(this).notify(notificationId, builder.build());
//        } catch (SecurityException e) {
//            Log.e(TAG, "Failed to display notification: Permission denied", e);
//        }
//    }
//
//    @Override
//    public void onNewToken(@NonNull String token) {
//        super.onNewToken(token);
//        Log.d(TAG, "New FCM token: " + token);
//        // Optionally, send the token to your server or save to Firestore
//    }
//}