package com.manwa.womensafetyapplication;

import static com.manwa.womensafetyapplication.childjava.PREF_SELECTED_CONTACTS;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, createNotification());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Log.d("VolumeService", "Service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(volumeReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
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

    private void triggerEmergencyAlert() {
        SharedPreferences preferences = getSharedPreferences("mypref", MODE_PRIVATE);
        String documentId = preferences.getString("accountid", null);

        if (documentId == null) {
            Log.e("VolumeService", "No document ID found in SharedPreferences!");
            return;
        }

        if (checkLocationPermission()) {
            startLocationUpdates();
        } else {
            Log.e("VolumeService", "Location permissions not granted!");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(documentId);

        docRef.update("emergencyFlag", "red")
                .addOnSuccessListener(aVoid -> {
                    Log.d("VolumeService", "✅ Emergency flag set to RED");
//----------------------------------

                    // Ensure fusedLocationClient is initialized
                    SmsManager smsManager = SmsManager.getDefault();

                    try {
                        double latitude = 0; double longitude=0;
                        String mapUrl = "https://www.google.com/maps?q=" + latitude + "," + longitude;
                        String message = "🚨 EMERGENCY! Need Help! Location: " + mapUrl;
                        SharedPreferences contactPrefs = getSharedPreferences("Information", MODE_PRIVATE);
                        String contactsString = contactPrefs.getString(PREF_SELECTED_CONTACTS, "");

                        fusedLocationClient= LocationServices.getFusedLocationProviderClient(this);
                        startLocationUpdates();
                        List<String> savedContacts = new ArrayList<>(Arrays.asList(contactsString.split(",")));

                        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                                .setMinUpdateIntervalMillis(500)
                                .build();

                        LocationCallback locationCallback = new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                                    Location location = locationResult.getLastLocation();

                                    // Convert to String
                                    String longitude = Double.toString(location.getLongitude());
                                    String latitude = Double.toString(location.getLatitude());

                                    // Log the location
                                    Log.d("VolumeService", "Current Location: " + latitude + ", " + longitude);

                                    // Send SMS with location
                                    String message = "Emergency: I had an accident! Here come fast- Location: " +
                                            "https://www.google.com/maps?q=" + latitude + "," + longitude;

                                    for (String contact : savedContacts) {
                                        String phoneNumber = extractPhoneNumber(contact);
                                        if (!phoneNumber.isEmpty()) {
                                            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

                                            Log.d("Service1", "Emergency SMS sent to: " + phoneNumber);
                                        }
                                    }

                                    // Stop location updates to save battery
                                    fusedLocationClient.removeLocationUpdates(this);
                                } else {
                                    Log.e("VolumeService", "⚠️ Failed to get current location!");
                                }
                            }
                        };

// Request location updates
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

                    } catch (SecurityException e) {
                        Log.e("Service1", "Location permission denied: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e("Service1", "Error getting location: " + e.getMessage());
                    }

                    // Reset emergency flag after 5 sec
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        docRef.update("emergencyFlag", "neutral")
                                .addOnSuccessListener(aVoid1 -> Log.d("VolumeService", "✅ Emergency flag reset to NEUTRAL"))
                                .addOnFailureListener(e -> Log.e("VolumeService", "❌ Failed to reset emergency flag", e));
                    }, 10000);

                }).addOnFailureListener(e -> Log.e("VolumeService", "❌ Failed to update emergency flag in Firebase!", e));
    }
    private void sendEmergencySms(double latitude, double longitude) {
        String mapUrl = "https://www.google.com/maps?q=" + latitude + "," + longitude;
        String message = "🚨 EMERGENCY! Need Help! Location: " + mapUrl;
        SharedPreferences contactPrefs = getSharedPreferences("Information", MODE_PRIVATE);
        String contactsString = contactPrefs.getString(PREF_SELECTED_CONTACTS, "");

        if (!contactsString.isEmpty()) {
            List<String> savedContacts = new ArrayList<>(Arrays.asList(contactsString.split(",")));
            SmsManager smsManager = SmsManager.getDefault();
            for (String contact : savedContacts) {
                String phoneNumber = extractPhoneNumber(contact);
                if (!phoneNumber.isEmpty()) {
                    try {
                        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
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