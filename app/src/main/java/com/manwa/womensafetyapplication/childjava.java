package com.manwa.womensafetyapplication;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Intent;

public class childjava extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int MAX_EMERGENCY_CONTACTS = 5;
    public static final String PREF_SELECTED_CONTACTS = "selected_contacts";


    private Set<String> selectedEmergencyContacts;

    private ListView selectedContactsListView;
    private ArrayList<String> selectedContactsList;
    private CustomAdapter selectedContactsAdapter;

    TextView textView,servicetext;
    Button btnlogout;
    String pass,name1,accountid;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    FloatingActionButton actionButton;
    private ArrayList<String> selectedContacts = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_childjava);
        textView=findViewById(R.id.textView);
        btnlogout=findViewById(R.id.logoutbtnchild);
        actionButton=findViewById(R.id.addcontactbtn);
        servicetext=findViewById(R.id.servicetxt);
        selectedContactsListView = findViewById(R.id.selectedlist);
        selectedContactsList = new ArrayList<>();
        selectedEmergencyContacts = new HashSet<>();

        // Load saved contacts from SharedPreferences
        SharedPreferences preferences = getSharedPreferences("Information", MODE_PRIVATE);
        String savedContacts = preferences.getString(PREF_SELECTED_CONTACTS, "");

        if (!savedContacts.isEmpty()) {
            String[] savedContactsArray = savedContacts.split(",");
            for (String contact : savedContactsArray) {
                if (!selectedEmergencyContacts.contains(contact)) {
                    selectedContactsList.add(contact);
                    selectedEmergencyContacts.add(contact);
                }
            }
        }
        selectedContactsAdapter = new CustomAdapter(this, selectedContactsList);
        selectedContactsListView.setAdapter(selectedContactsAdapter);


        selectedContactsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedContact = selectedContactsList.get(position);

            if (selectedEmergencyContacts.contains(selectedContact)) {
                selectedEmergencyContacts.remove(selectedContact);
                selectedContactsList.remove(position); // Use position for accuracy
                saveContactsToPreferences();
                Toast.makeText(this, "Removed Contact", Toast.LENGTH_SHORT).show();
            }

            selectedContactsAdapter.notifyDataSetChanged();
        });

        db = FirebaseFirestore.getInstance();

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(childjava.this,contact_selection.class);
                startActivity(i);
            }
        });

        SharedPreferences sharedPreferences=getSharedPreferences("mypref",MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        String login=sharedPreferences.getString("login","0");

        if(login.equals("0")){
            name1=getIntent().getStringExtra("name");
            pass=getIntent().getStringExtra("pass");
            editor.putString("name",name1);
            editor.putString("pass",pass);
            Toast.makeText(this, "asking1"+name1, Toast.LENGTH_SHORT).show();

            editor.apply();
            if(!name1.equals("")){
                Toast.makeText(this, "asking2", Toast.LENGTH_SHORT).show();
                createUserDocument(name1);
            }

        }
        if(login.equals("1")){
             pass=sharedPreferences.getString("pass","");
             name1=sharedPreferences.getString("name","a");
             accountid=sharedPreferences.getString("accountid","");
             if(!name1.equals("") && !accountid.equals("")){
                     textView.setText("Name: "+name1+"\n"+"Account ID: "+accountid);
             }
        }

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String textToCopy =accountid;

                if (!textToCopy.isEmpty()) {
                    // ✅ Copy text to clipboard
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Copied Text", textToCopy);
                    clipboard.setPrimaryClip(clip);
                    // ✅ Show confirmation
                    Toast.makeText(childjava.this, "Copied to Clipboard!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(childjava.this, "Enter text to copy!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnlogout.setOnClickListener(v -> {
            editor.putString("login", "0");
            editor.putString("name", "");
            editor.putString("pass", "");
            editor.putString("accountid","");
            editor.putString("selected_contacts","");
            editor.apply();
            Intent serviceIntent = new Intent(getApplicationContext(), VolumeButtonService.class);
            stopService(serviceIntent);
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show();
            finish();
        });

        Intent serviceIntent = new Intent(this, VolumeButtonService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
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

    }


    private void createUserDocument(String name) {
        SharedPreferences sharedPreferences=getSharedPreferences("mypref",MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        // Prepare data
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("login_time", Timestamp.now());  // Store Firebase Timestamp

        // Create document with auto-generated ID
        db.collection("users")
                .add(userData)
                .addOnSuccessListener(documentReference -> {

                    accountid = documentReference.getId();  // Get generated document ID
                    Log.d("Firestore", "Document created with ID: " + accountid);
                    Toast.makeText(childjava.this, "Document ID: " + accountid, Toast.LENGTH_LONG).show();
                    if(accountid!=null){
                        textView.setText("Name: "+name1+"\n"+"Account ID: "+accountid);
                    }
                    editor.putString("accountid",accountid);
                    editor.putString("login","1");
                    editor.apply();

                    Intent serviceIntent = new Intent(this, VolumeButtonService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error adding document", e);
                    Toast.makeText(childjava.this, "Error saving data, Please try again!", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(childjava.this, MainActivity.class);
                    startActivity(i);
                    finish();
                });
    }

    private static class CustomAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final List<String> contactsList;

        CustomAdapter(Context context, List<String> contactsList) {
            super(context, R.layout.contact_2, contactsList);
            this.context = context;
            this.contactsList = contactsList;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.contact_2, parent, false);
            }
            TextView contactTextView = convertView.findViewById(R.id.textviewOfContact);
            contactTextView.setText(contactsList.get(position));
            return convertView;
        }
    }
    // Refresh contacts when activity resumes
    @Override
    protected void onResume() {
        super.onResume();
        refreshContactList();
    }
    // Function to update the contact list correctly
    private void refreshContactList() {
        SharedPreferences preferences = getSharedPreferences("Information", MODE_PRIVATE);
        String savedContacts = preferences.getString(PREF_SELECTED_CONTACTS, "");

        selectedContactsList.clear();
        selectedEmergencyContacts.clear();

        if (!savedContacts.isEmpty()) {
            String[] savedContactsArray = savedContacts.split(",");
            for (String contact : savedContactsArray) {
                if (!selectedEmergencyContacts.contains(contact)) {
                    selectedContactsList.add(contact);
                    selectedEmergencyContacts.add(contact);
                }
            }
        }

        selectedContactsAdapter.notifyDataSetChanged();
        Log.d("Contacts", "Final List: " + selectedContactsList.toString());
    }

    // Function to save contacts in SharedPreferences
    private void saveContactsToPreferences() {
        SharedPreferences.Editor editor = getSharedPreferences("Information", MODE_PRIVATE).edit();
        editor.putString(PREF_SELECTED_CONTACTS, TextUtils.join(",", selectedEmergencyContacts));
        editor.apply();
    }
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }



    // Ensure no duplicate contacts

}