package com.example.parkingapp;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.parkingapp.listener.LocationService;
import com.example.parkingapp.util.GpsUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
/*
authentication ->uid
        user->uid

        feedback->owner/uid->user/uid  feedback

        booking->user/uid->push1
                           push2

        parking->owner/uid->push1 (this code is not applicable)

        parking->cityname->owneruid->   (because one owner has one parking only)
*/

public class SingleTask extends Application {
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private GoogleSignInClient mGoogleSignInClient;
    private StorageReference mStorageRef;
    private BroadcastReceiver broadcastReceiver;
    private double lat, lon;
    private Handler myhaHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        myhaHandler = new Handler();

        myhaHandler.post(new Runnable() {
            @Override
            public void run() {
                mAuth = FirebaseAuth.getInstance();
                database = FirebaseDatabase.getInstance();
                mStorageRef = FirebaseStorage.getInstance().getReference();
                GoogleSignInOptions gso = new GoogleSignInOptions.
                        Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

                mGoogleSignInClient = GoogleSignIn.getClient(SingleTask.this, gso);
            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                lat = intent.getExtras().getDouble("lat");
                lon = intent.getExtras().getDouble("lon");
                Toast.makeText(context, "Current Location : " + lat + "\n" + lon, Toast.LENGTH_SHORT).show();
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("com.example.parkingapp.listener.cutomlocation"));


    }

    public BroadcastReceiver getLocationBrodcast() {
        return broadcastReceiver;
    }

    public LatLng getCurrentLocation() {
        return new LatLng(lat, lon);
    }

    public GoogleSignInClient getmGoogleSignInClient() {
        return mGoogleSignInClient;
    }

    public StorageReference getDocumentStorageIdProof() {
        return mStorageRef.child("idproofs/");
    }

    public DatabaseReference getUserDatabaseReference() {
        return database.getReference("users");
    }

    public DatabaseReference getProfileDatabaseReference() {
        return database.getReference("profile");
    }

    public DatabaseReference getFeedbackDatabaseReference() {
        return database.getReference("feedbacks");
    }

    public DatabaseReference getBookingDatabaseReference() {
        return database.getReference("bookings");
    }

    public DatabaseReference getParkingDatabaseReference() {
        return database.getReference("parkings");
    }

    public FirebaseAuth getFirebaseAuth() {
        return mAuth;
    }


}
