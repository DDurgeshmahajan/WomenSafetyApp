package com.manwa.womensafetyapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import javax.annotation.Nullable;

public class ParentAlertService extends Service {
    private static final String TAG = "ParentAlertService";
    private static final String CHANNEL_ID = "ParentAlertChannel";
    private MediaPlayer mediaPlayer;
    private FirebaseFirestore firestore;
    private DocumentReference docRef;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created");

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

        // Get the registered Document ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("mypref", MODE_PRIVATE);
        String documentId = sharedPreferences.getString("accountid", "");

        if (documentId.isEmpty()) {
            Log.e(TAG, "No account ID found! Stopping service.");
            stopSelf();
            return;
        }

        // Get reference to the Firestore document
        docRef = firestore.collection("users").document(documentId);

        // Start Foreground Service with Notification
        startForegroundServiceWithNotification();

        // Listen for real-time changes in Firestore
        listenForSignalChanges();
    }

    private void listenForSignalChanges() {
        docRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Firestore Listener Error", error);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                String signal = documentSnapshot.getString("emergencyFlag"); // Fetch "signal" field

                Log.d(TAG, "Signal Changed: " + signal);
                Toast.makeText(this, "Signal Changed: " + signal, Toast.LENGTH_SHORT).show();

                if ("red".equalsIgnoreCase(signal)) {
                    playSiren();  // Play siren if signal is red
//                    sendNotification("🚨 Emergency Alert!", "The signal is RED! Immediate attention required.");
                } else {
                    stopSiren();  // Stop siren if signal is not red
                }
            }
        });
    }

    private void playSiren() {

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.siren); // Replace with your siren file
            mediaPlayer.setLooping(true);
        }
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            Log.d(TAG, "Siren Playing...");
        }
    }

    private void stopSiren() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "Siren Stopped.");
        }
    }

    private void startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Parent Alert Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Parent Alert Service Running")
                .setContentText("Monitoring safety signals...")
                .setSmallIcon(R.drawable.ic_notification) // Replace with your app's icon
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(1, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not binding to any activity
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSiren();
        Log.d(TAG, "Service Destroyed");
    }
}
