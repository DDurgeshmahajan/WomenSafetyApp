package com.manwa.womensafetyapplication;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class parentjava extends AppCompatActivity {
    Button logoutparent;
    TextView nameandid,servicetext;
    String name1, pass, accountid;
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    ListView listView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_parentjava);

        logoutparent = findViewById(R.id.logoutbtnparent);
        nameandid = findViewById(R.id.nameandid);
        servicetext=findViewById(R.id.servicetxtparent);
        listView=findViewById(R.id.listofparents);

        SharedPreferences sharedPreferences = getSharedPreferences("mypref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String login = sharedPreferences.getString("login", "0");

        Intent serviceIntent = new Intent(this, VolumeButtonService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        SharedPreferences prefs = getSharedPreferences("parent_emergencies", MODE_PRIVATE);
        Set<String> emergencySet = prefs.getStringSet("emergencyLogs", new HashSet<>());

        List<String> emergencyList = new ArrayList<>(emergencySet);
        Log.d("TAG", "onCreate: "+emergencyList);
// Sort by latest
        Collections.sort(emergencyList, Collections.reverseOrder());

// Show in ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, emergencyList);
        listView.setAdapter(adapter);




        if (login.equals("0")) {
            name1 = getIntent().getStringExtra("name");
            pass = getIntent().getStringExtra("pass");
            accountid = getIntent().getStringExtra("accountid");

            editor.putString("login", "1");
            editor.putString("name", name1);
            editor.putString("pass", pass);
            editor.putString("accountid", accountid);
            editor.apply();
        }
        if (!isServiceRunning(VolumeButtonService.class)) {
            servicetext.setText("Service is NOT running! Restart APP");
            servicetext.setBackgroundColor(Color.parseColor("#702323"));

            Log.d("ChildActivity", "❌ Service is NOT running!");
        } else {
            servicetext.setText("Service is running! Close APP Now");
            servicetext.setBackgroundColor(Color.parseColor("#486E28"));
            Log.d("ChildActivity", "✅ Service is running!");
        }


        // Retrieve updated values
        login = sharedPreferences.getString("login", "0");
        if (login.equals("1")) {
            pass = sharedPreferences.getString("pass", "");
            name1 = sharedPreferences.getString("name", "a");
            accountid = sharedPreferences.getString("accountid", "");

            Toast.makeText(this, "Retrieved Name: " + name1, Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "Retrieved Account ID: " + accountid, Toast.LENGTH_SHORT).show();

            if (!name1.equals("a") && !accountid.equals("")) {
                nameandid.setText("Your Child Name: " + name1 + "\n" + "Account ID: " + accountid);
            } else {
                nameandid.setText("Error retrieving data");
            }
        }

        logoutparent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("login", "0");
                editor.putString("name", "");
                editor.putString("pass", "");
                editor.putString("accountid", "");
                editor.apply();
                Intent serviceIntent = new Intent(getApplicationContext(), VolumeButtonService.class);
                stopService(serviceIntent);
                // Restart the activity
                finish();

            }
        });
    }
}
