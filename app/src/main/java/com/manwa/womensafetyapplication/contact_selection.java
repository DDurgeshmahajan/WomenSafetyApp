package com.manwa.womensafetyapplication;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class contact_selection extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int MAX_EMERGENCY_CONTACTS = 5;
    public static final String PREF_SELECTED_CONTACTS = "selected_contacts";

    private ListView contactsListView;
    private ArrayList<String> contactsList;
    private CustomAdapter adapter;
    private Set<String> selectedEmergencyContacts;

    private ListView selectedContactsListView;
    private ArrayList<String> selectedContactsList;
    private CustomAdapter selectedContactsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contact_selection);

        contactsListView = findViewById(R.id.contactlistview);
        contactsList = new ArrayList<>();
        adapter = new CustomAdapter(this, contactsList);
        contactsListView.setAdapter(adapter);

        // ✅ Initialize Sets and Lists
        selectedEmergencyContacts = new HashSet<>();
        selectedContactsList = new ArrayList<>();
        selectedContactsAdapter = new CustomAdapter(this, selectedContactsList);

        SharedPreferences preferences = getSharedPreferences("Information", MODE_PRIVATE);

        // ✅ Permission Check & Request
        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            readContacts();
        }

        // ✅ OnClickListener for ListView
        contactsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedContact = contactsList.get(position);

            if (selectedEmergencyContacts.contains(selectedContact)) {
                selectedEmergencyContacts.remove(selectedContact);
                selectedContactsList.remove(selectedContact);
            } else {
                if (selectedEmergencyContacts.size() < MAX_EMERGENCY_CONTACTS) {
                    Toast.makeText(this, "added"+selectedContact, Toast.LENGTH_SHORT).show();
                    selectedEmergencyContacts.add(selectedContact);
                    selectedContactsList.add(selectedContact);

                } else {
                    Toast.makeText(contact_selection.this, "You Can ADD 5 Contacts At Most!!!", Toast.LENGTH_SHORT).show();
                }
            }
            saveSelectedContactsToPreferences();
            adapter.notifyDataSetChanged();
            selectedContactsAdapter.notifyDataSetChanged();
        });
    }

    // ✅ Custom Adapter
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

    // ✅ Handle Permission Results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readContacts();
            } else {
                Toast.makeText(this, "Permission denied! Cannot display contacts.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ✅ Read Contacts Function
    private void readContacts() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                contactsList.add(displayName + " - " + phoneNumber);
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    // ✅ Save Selected Contacts
    public void saveSelectedContactsToPreferences() {
        SharedPreferences preferences = getSharedPreferences("Information", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_SELECTED_CONTACTS, TextUtils.join(",", selectedEmergencyContacts));
        editor.apply();
    }
}