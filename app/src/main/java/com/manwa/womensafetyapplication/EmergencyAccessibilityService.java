package com.manwa.womensafetyapplication;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.content.Context;
import android.telephony.SmsManager;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class EmergencyAccessibilityService extends AccessibilityService {

    private static final int TIME_INTERVAL = 2000; // 2 seconds to detect rapid presses
    private int volumePressCount = 0;
    private long lastPressTime = 0;
    private Handler handler = new Handler();
    private static final String CHANNEL_ID = "AccessibilityServiceChannel";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            detectVolumeKeyPress();
        }
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Accessibility Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Accessibility Service Running For Parents!")
                .setContentText("This service is running in the background.")
                .setSmallIcon(R.drawable.glossy_card_background)  // Replace with your icon
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(1, notification);
    }

    private void detectVolumeKeyPress() {
        long currentTime = SystemClock.elapsedRealtime();
        Log.d("tag", "Volume button pressed, time since last press: " + (currentTime - lastPressTime));

        if (currentTime - lastPressTime < TIME_INTERVAL) {
            volumePressCount++;
        } else {
            volumePressCount = 1;
        }
        lastPressTime = currentTime;

        Log.d("tag", "Press count: " + volumePressCount);

        if (volumePressCount >= 3) {
            Log.d("tag", "Triggering emergency alert");
            triggerEmergencyAlert();
            volumePressCount = 0;
        }
    }


    private void triggerEmergencyAlert() {
//        Toast.makeText(this, "🚨 Emergency Alert Sent!", Toast.LENGTH_LONG).show();
        sendEmergencySms();
    }

    private void sendEmergencySms() {
        // Get document ID from SharedPreferences
        SharedPreferences preferences = getSharedPreferences("mypref", MODE_PRIVATE);
        String documentId = preferences.getString("accountid", null);

        if (documentId == null) {
            Toast.makeText(this, "Error: No document ID found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reference to Firebase collection
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(documentId);
        Log.d("tag","id="+documentId);


        // Update emergencyFlag in Firebase
        docRef.update("emergencyFlag", "red")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Emergency flag set to RED!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update emergency flag.", Toast.LENGTH_SHORT).show();
                });

        // Optional: Send emergency SMS
//    String emergencyContact = "9876543210"; // Replace with dynamic contact retrieval
//    String message = "🚨 EMERGENCY! Need Help! Location: Unknown";
//    SmsHelper.sendSms(this, emergencyContact, message);
    }

    @Override
    public void onInterrupt() {
        Toast.makeText(this, "Accessibility Service Interrupted!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        setServiceInfo(info);
        startForegroundService();
        Log.d("tag","access started");
    }

    private static class SmsHelper {
        public static void sendSms(Context context, String phoneNumber, String message) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN &&
                (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            detectVolumeKeyPress();
            return true; // Stops event propagation
        }
        return super.onKeyEvent(event);
    }

}
