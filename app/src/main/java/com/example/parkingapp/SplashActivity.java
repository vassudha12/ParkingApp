package com.example.parkingapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.example.parkingapp.util.GpsUtils;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    private TextView ttile;
    private GpsUtils gpsUtils;
    private boolean status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        //create gps util object
        gpsUtils = new GpsUtils(this);
        //initialize all view here
        initViews();
        //load animation file here
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        ttile.setAnimation(animation);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                if (gpsEnable()) {
                    FirebaseUser currentuser = ((SingleTask) getApplication()).getFirebaseAuth().getCurrentUser();
                    if (currentuser != null) {
                        goToHomePage();
                    } else {
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        finish();
                    }
                } else {
                    Toast.makeText(SplashActivity.this, "Please Enable Gps", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void initViews() {
        ttile = findViewById(R.id.splashtitle);
    }

    private void goToHomePage() {
        startActivity(new Intent(SplashActivity.this, HomePage.class));
        finish();
    }

    public boolean gpsEnable() {
        gpsUtils.turnGPSOn(new GpsUtils.OnGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
                status = isGPSEnable;
            }
        });
        return status;
    }

    public void checkGps(View view) {
        if (gpsEnable()) {
            FirebaseUser currentuser = ((SingleTask) getApplication()).getFirebaseAuth().getCurrentUser();
            if (currentuser != null) {
                goToHomePage();
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        } else {

        }
    }
}
