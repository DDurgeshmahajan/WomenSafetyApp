package com.manwa.womensafetyapplication;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.content.Intent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button child,parent,btncancel,btnname,submitparent,parentcancel;
    EditText nameedittext,parentid;
    CardView cardofname;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int PERMISSION_REQUEST_CODE = 100;
    LinearLayout layoutchild,layoutparent;
    ProgressBar progressBar;


    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Add all required permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CALL_PHONE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE);
            }
        }

        // Request permissions if not granted
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    // Handle the result of permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            Map<String, Integer> perms = new HashMap<>();
            perms.put(Manifest.permission.SEND_SMS, PackageManager.PERMISSION_GRANTED);
            perms.put(Manifest.permission.CALL_PHONE, PackageManager.PERMISSION_GRANTED);
            perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
            perms.put(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                perms.put(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE, PackageManager.PERMISSION_GRANTED);
            }

            // Check if all permissions are granted
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                perms.put(permissions[i], grantResults[i]);
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                }
            }

            if (allGranted) {
                Log.d("Permissions", "All permissions granted!");
            } else {
                Toast.makeText(this, "All permissions are required for full functionality!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        child=findViewById(R.id.childbtn);
        parent=findViewById(R.id.parentbtn);
        btncancel=findViewById(R.id.btn_maincancel);
        btnname=findViewById(R.id.btn_mainname);
        cardofname=findViewById(R.id.cardofname);
        nameedittext=findViewById(R.id.nameedittext);
        layoutchild=findViewById(R.id.cardofchildlayout);
        layoutparent=findViewById(R.id.cardofparentlayout);
        submitparent=findViewById(R.id.btn_parentsubmit);
        parentid=findViewById(R.id.parentID);
        parentcancel=findViewById(R.id.btn_parentcancel);
        progressBar=findViewById(R.id.progressbar);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        FirebaseApp.initializeApp(this);
//     Check location permissions
        checkAndRequestPermissions();
        SharedPreferences sharedPreferences=getSharedPreferences("mypref",MODE_PRIVATE);
        String pass=sharedPreferences.getString("pass","");
        String login=sharedPreferences.getString("login","");
        if(login.equals("1")){
            if(pass.equals("child")) {
                Intent i = new Intent(MainActivity.this, childjava.class);
                i.putExtra("pass", "child");
                startActivity(i);
                finish();
            }
            if(pass.equals("parent")){
                Intent i = new Intent(MainActivity.this, parentjava.class);
                i.putExtra("pass", "parent");
                startActivity(i);
                finish();
            }
        }

//        SmsManager smsManager = SmsManager.getDefault();
//        smsManager.sendTextMessage("9623146203", null, "hi", null, null);
//        Log.d("MainActivity", "SMS sent successfully!");

        child.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cardofname.setVisibility(View.VISIBLE);
                layoutchild.setVisibility(View.VISIBLE);
                parent.setVisibility(View.GONE);
                child.setVisibility(View.GONE);

                btncancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        child.setVisibility(View.VISIBLE);
                        cardofname.setVisibility(View.GONE);
                        parent.setVisibility(View.VISIBLE);
                        layoutchild.setVisibility(View.GONE);
                    }
                });
                btnname.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                      if(!nameedittext.getText().toString().isEmpty()){
                          Intent i = new Intent(MainActivity.this,childjava.class);
                          i.putExtra("pass","child");
                          i.putExtra("name",nameedittext.getText().toString());
                          startActivity(i);
                          finish();
                        }
                    }
                });

            }
        });

        parentcancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardofname.setVisibility(View.GONE);
                parent.setVisibility(View.VISIBLE);
                child.setVisibility(View.VISIBLE);
                layoutparent.setVisibility(View.GONE);
            }
        });

        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cardofname.setVisibility(View.VISIBLE);
                parent.setVisibility(View.GONE);
                child.setVisibility(View.GONE);
                layoutparent.setVisibility(View.VISIBLE);
//                Intent i = new Intent(MainActivity.this,parentjava.class);
//                i.putExtra("pass","parent");
//                startActivity(i);
            }
        });

        submitparent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredID = parentid.getText().toString().trim();

                submitparent.setVisibility(View.GONE);
                parentcancel.setVisibility(View.GONE);
                if (!enteredID.isEmpty()) {
                    progressBar.setVisibility(View.VISIBLE); // Show ProgressBar

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("users") // Adjust the collection name as per your Firestore
                            .document(enteredID)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String name = documentSnapshot.getString("name"); // Adjust field name
                                    // Save in SharedPreferences
                                    SharedPreferences sharedPreferences = getSharedPreferences("mypref", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("accountid", enteredID);
                                    editor.putString("name", name);
                                    editor.putString("pass","parent");
                                    editor.apply();

                                    progressBar.setVisibility(View.GONE); // Hide ProgressBar

                                    // Move to Next Activity
                                    Intent intent = new Intent(MainActivity.this, parentjava.class);
                                    intent.putExtra("name", name);
                                    intent.putExtra("pass","parent");
                                    intent.putExtra("accountid",enteredID);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    submitparent.setVisibility(View.VISIBLE);
                                    parentcancel.setVisibility(View.VISIBLE);
                                    Toast.makeText(MainActivity.this, "ID not found!", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {

                                progressBar.setVisibility(View.GONE);
                                submitparent.setVisibility(View.VISIBLE);
                                parentcancel.setVisibility(View.VISIBLE);
                                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(MainActivity.this, "Please enter an ID!", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }
}