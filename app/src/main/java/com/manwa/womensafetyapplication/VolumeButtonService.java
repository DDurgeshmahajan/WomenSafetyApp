package com.manwa.womensafetyapplication;

import static com.manwa.womensafetyapplication.childjava.PREF_SELECTED_CONTACTS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.BoolRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;

public class VolumeButtonService extends Service {
    private int volumePressCount = 0;
    private long lastPressTime = 0;
    private static final long TIME_INTERVAL = 2000;
    private static final long COOLDOWN_INTERVAL = 500;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private final BroadcastReceiver volumeReceiver = new VolumeReceiver();
    private static final String PREF_SELECTED_CONTACTS = "selected_contacts";

    private Boolean locationReceived=false;
    private MediaPlayer mediaPlayer;
    private boolean isSirenPlaying = false;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, createNotification());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Log.d("VolumeService", "Service started");

    }

    private void startParentMode() {
        SharedPreferences sharedPreferences=getSharedPreferences("mypref", MODE_PRIVATE);
        String childAccountId = sharedPreferences.getString("accountid", null); // ID of the girl's account
        if (childAccountId == null) {
            Log.e("VolumeService", "No child account ID found for parent!");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference childDocRef = db.collection("users").document(childAccountId);

        childDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e("VolumeService", "Listen failed.", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                String emergencyFlag = snapshot.getString("emergencyFlag");

                if ("red".equals(emergencyFlag)) {
                    Log.d("VolumeService", "🚨 Emergency detected from child!");

// Get current date & time
                    String timestamp = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());

                    // Save to SharedPreferences (append to existing list)
                    SharedPreferences prefs = getSharedPreferences("parent_emergencies", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();

                    Set<String> emergencySet = prefs.getStringSet("emergencyLogs", new HashSet<>());
                    emergencySet.add(timestamp); // Add new timestamp

                    editor.putStringSet("emergencyLogs", emergencySet);
                    editor.apply();

                    sendParentAlert();

                    // Optional: reset flag to avoid multiple triggers
//                    childDocRef.update("emergencyFlag", "acknowledged")
//                            .addOnSuccessListener(aVoid -> Log.d("VolumeService", "Flag acknowledged"))
//                            .addOnFailureListener(err -> Log.e("VolumeService", "Failed to update flag", err));
                }
            }
        });
    }

    private void sendParentAlert() {
        if (isSirenPlaying) {
            Log.d("VolumeService", "Siren already playing. Ignoring...");
            return; // Do not re-trigger if already playing
        }
        isSirenPlaying = true;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "volume_service_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🚨 Emergency Alert!")
                .setContentText("Your child has triggered an emergency alert.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("volume_service_channel", "Emergency Alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(99, builder.build());
        notificationManager.notify(99, builder.build());

        mediaPlayer = MediaPlayer.create(this, R.raw.siren); // Your siren file in res/raw
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true); // You can loop it if needed
            mediaPlayer.start();

            // Optional: Stop after 30 seconds
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    isSirenPlaying = false;
                }
            }, 10000); // Stop after 30 sec
        }

        // Optional: You can also start a loud alarm sound here if needed.
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // 1️⃣ Check user type from SharedPreferences (can also check intent if passed)
        SharedPreferences preferences = getSharedPreferences("mypref", MODE_PRIVATE);
        String userType = preferences.getString("pass", "child"); // default to child

        if (userType.equals("parent")) {
            // Parent-specific logic
            startParentMode();
            Log.d("VolumeService", "👨‍👧 Running as Parent");
            // (Optional) You can start another parent-specific service or task here
            // e.g., monitor location of child or receive flags
        } else {
            // Child-specific logic
            Log.d("VolumeService", "🧒 Running as Child");

            // Register for volume button detection
            registerReceiver(volumeReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
        }

        return START_STICKY;
    }

    private class VolumeReceiver extends BroadcastReceiver {
        private long lastPressTime = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !"android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) return;
            long currentTime = SystemClock.elapsedRealtime();
            if (currentTime - lastPressTime < COOLDOWN_INTERVAL) return;
            lastPressTime = currentTime;
            detectVolumeKeyPress();
        }
    }

    private void detectVolumeKeyPress() {
        long currentTime = SystemClock.elapsedRealtime();
        volumePressCount = (currentTime - lastPressTime < TIME_INTERVAL) ? volumePressCount + 1 : 1;
        lastPressTime = currentTime;

        Log.d("VolumeService", "Volume button pressed");

        if (volumePressCount >= 3) {
            Log.d("VolumeService", "Triggering emergency alert");
            triggerEmergencyAlert();
            volumePressCount = 0;
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(5000)
                .setFastestInterval(3000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    Log.d("LocationUpdate", "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

 @SuppressLint("UnspecifiedRegisterReceiverFlag")
 @RequiresPermission(Manifest.permission.VIBRATE)
 private void triggerEmergencyAlert() {

     SharedPreferences preferences = getSharedPreferences("mypref", MODE_PRIVATE);
     String documentId = preferences.getString("accountid", null);

     if (documentId == null) {
         Log.e("VolumeService", "No document ID found in SharedPreferences!");
         return;
     }

     if (!checkLocationPermission()) {
         Log.e("VolumeService", "Location permissions not granted!");
         return;
     }

     FirebaseFirestore db = FirebaseFirestore.getInstance();
     DocumentReference docRef = db.collection("users").document(documentId);

// Set emergency flag in Firestore
     docRef.update("emergencyFlag", "red")
             .addOnSuccessListener(aVoid -> {
                         Log.d("VolumeService", "✅ Emergency flag set to RED");

                         SmsManager smsManager = SmsManager.getDefault();
//                 smsManager.sendTextMessage(phoneNumber, null, message, sentPI, null);
                         SharedPreferences contactPrefs = getSharedPreferences("Information", MODE_PRIVATE);
                         String contactsString = contactPrefs.getString(PREF_SELECTED_CONTACTS, "");
                         List<String> savedContacts = new ArrayList<>(Arrays.asList(contactsString.split(",")));

                 FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

                 LocationRequest locationRequest = LocationRequest.create();
                 locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                 locationRequest.setInterval(1000); // 1 second

                 String message = "I need Help!!!";
                         if (!contactsString.isEmpty()) {
                             for (String contact : savedContacts) {
                                 String phoneNumber = extractPhoneNumber(contact);
                                 if (!phoneNumber.isEmpty()) {
                                     try {

                                         LocationCallback locationCallback = new LocationCallback() {
                                             @Override
                                             public void onLocationResult(LocationResult locationResult) {
                                                 if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                                                     Location location = locationResult.getLastLocation();
                                                     Log.d("TAG", "📍Location fetched: " + location);
                                                     String mapUrl = "https://www.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude();;

                                                     String message2 = "I need Help, here=!!!"+mapUrl;
                                                     try {
                                                         smsManager.sendTextMessage(phoneNumber, null, message2, null, null);
                                                         Log.d("VolumeService", "📩 second SMS sent to " + phoneNumber);
                                                     } catch (Exception e) {
                                                         Log.e("VolumeService", "❌ Failed to send second SMS to " + phoneNumber, e);
                                                     }
                                                     // Optionally stop location updates after first result
                                                     fusedLocationClient.removeLocationUpdates(this);
                                                 } else {
                                                     Log.d("TAG", "📍 LocationResult is empty");
                                                 }
                                             }
                                         };

                                         try {
                                             smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                                             Log.d("VolumeService", "📩 First SMS sent to " + phoneNumber);

                                             // Now actually request location
                                             if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                                     || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                                 fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                                             } else {
                                                 Log.e("VolumeService", "❌ Location permission not granted");
                                             }

                                         } catch (Exception e) {
                                             Log.e("VolumeService", "❌ Failed to send SMS to " + phoneNumber, e);
                                         }



                                     } catch (Exception e) {
                                         Log.e("VolumeService", "❌ Failed to send SMS to " + phoneNumber, e);
                                     }
                                 }
                             }
                         }


//                 fallbackHandler.postDelayed(() -> {
//                     if (!locationReceived) {
//                         triggeredFallback[0] = true;
//                         Log.w("VolumeService", "⚠️ Live location not received, using last known location...");
//                         fusedLocationClient.getLastLocation()
//                                 .addOnSuccessListener(lastLocation -> {
//                                     if (lastLocation != null) {
//
//                                         if (!contactsString.isEmpty()) {
//                                             for (String contact : savedContacts) {
//                                                 String phoneNumber = extractPhoneNumber(contact);
//                                                 if (!phoneNumber.isEmpty()) {
//                                                     try {
//                                                         String message = "🚨 EMERGENCY! Need Help! Location: ";
//                                                         smsManager.sendTextMessage(phoneNumber, null, message, sentPI, null);
//                                                         Log.d("VolumeService", "📩 SMS sent to " + phoneNumber);
//                                                     } catch (Exception e) {
//                                                         Log.e("VolumeService", "❌ Failed to send SMS to " + phoneNumber, e);
//                                                     }
//                                                 }
//                                             }
//                                         }
////                                         sendEmergencySms(lastLocation.getLatitude(), lastLocation.getLongitude());
//                                     } else {
//                                         Log.e("VolumeService", "❌ No last known location available!");
//                                     }
//                                 })
//                                 .addOnFailureListener(e -> Log.e("VolumeService", "❌ Failed to get last known location", e));
//                     }
//                 }, 5000); // 5 seconds timeout

                 // Begin location updates
//                 fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

                 // Trigger alert feedback
                 triggerSoundAndVibration();

                 // Reset flag after 10s
                 new Handler(Looper.getMainLooper()).postDelayed(() -> {
                     docRef.update("emergencyFlag", "neutral")
                             .addOnSuccessListener(aVoid1 -> Log.d("VolumeService", "✅ Emergency flag reset to NEUTRAL"))
                             .addOnFailureListener(e -> Log.e("VolumeService", "❌ Failed to reset emergency flag", e));
                 }, 10000);

             }).addOnFailureListener(e -> Log.e("VolumeService", "❌ Failed to update emergency flag in Firebase!", e));

 }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private void triggerSoundAndVibration() {
        // Vibration
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            VibrationEffect effect = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                effect = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(effect);
            }
        }
        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700);
//
        // Sound
//        MediaPlayer mediaPlayer = MediaPlayer.create(this, ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD); // ensure alert_tone.mp3 exists in res/raw
//        if (mediaPlayer != null) {
//            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
//            mediaPlayer.start();
//        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void sendEmergencySms(double latitude, double longitude) {
        String mapUrl = "https://www.google.com/maps?q=" + latitude + "," + longitude;
        String message = "🚨 EMERGENCY! Need Help! Location: " + mapUrl;
        SharedPreferences contactPrefs = getSharedPreferences("Information", MODE_PRIVATE);
        String contactsString = contactPrefs.getString(PREF_SELECTED_CONTACTS, "");

        String SENT = "SMS_SENT";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), PendingIntent.FLAG_IMMUTABLE);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Log.d("SMS", "SMS sent successfully!");
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Log.e("SMS", "❌ Generic failure!");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Log.e("SMS", "❌ No service!");
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Log.e("SMS", "❌ Null PDU!");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Log.e("SMS", "❌ Radio off!");
                        break;
                }
            }
        }, new IntentFilter(SENT));


        if (!contactsString.isEmpty()) {
            List<String> savedContacts = new ArrayList<>(Arrays.asList(contactsString.split(",")));
            SmsManager smsManager = SmsManager.getDefault();
            for (String contact : savedContacts) {
                String phoneNumber = extractPhoneNumber(contact);
                if (!phoneNumber.isEmpty()) {
                    try {
                        smsManager.sendTextMessage(phoneNumber, null, message, sentPI, null);
                        Log.d("VolumeService", "📩 SMS sent to " + phoneNumber);
                    } catch (Exception e) {
                        Log.e("VolumeService", "❌ Failed to send SMS to " + phoneNumber, e);
                    }
                }
            }
        } else {
            Log.e("VolumeService", "⚠️ No emergency contacts found!");
        }
    }

    private String extractPhoneNumber(String contact) {
        String[] parts = contact.split(" - ");
        return parts.length > 1 ? parts[1].trim() : "";
    }

    private Notification createNotification() {
        String channelId = "volume_service_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Volume Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Volume Button Service")
                .setContentText("Listening for volume key presses...")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .build();
    }

//    private void triggerSoundAndVibration() {
//        // 🔊 Play alert tone
//        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
//        toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700);
//
//        // 📳 Vibrate
//        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//        if (vibrator != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
//            vibrator.vibrate(800);
//        }
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(volumeReceiver);
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}