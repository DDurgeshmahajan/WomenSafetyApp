package com.manwa.womensafetyapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class parentjava extends AppCompatActivity {
    Button logoutparent;
    TextView nameandid;
    String name1, pass, accountid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_parentjava);

        logoutparent = findViewById(R.id.logoutbtnparent);
        nameandid = findViewById(R.id.nameandid);

        SharedPreferences sharedPreferences = getSharedPreferences("mypref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String login = sharedPreferences.getString("login", "0");

        Intent serviceIntent = new Intent(this, VolumeButtonService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }


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
                Intent serviceIntent = new Intent(getApplicationContext(), ParentAlertService.class);
                stopService(serviceIntent);

                // Restart the activity
                finish();

            }
        });
    }
}
