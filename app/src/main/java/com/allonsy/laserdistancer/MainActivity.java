package com.allonsy.laserdistancer;

import android.Manifest;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button mDistanceCalculatorButton;
    private Button mSerialMonitorButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            checkPermissions();


        setContentView(R.layout.activity_main);


        mDistanceCalculatorButton = (Button) findViewById(R.id.start_distance_calculator_button);
        mDistanceCalculatorButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startDistanceCalculatorActivity();
            }
        });



        mSerialMonitorButton = (Button) findViewById(R.id.start_serial_monitor_button);
        mSerialMonitorButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startSerialMonitorActivity();
            }
        });


    }

    private void startDistanceCalculatorActivity()
    {
        Intent distanceCalculatorActivity = new Intent(MainActivity.this, DistanceCalculatorActivity.class);
        startActivity(distanceCalculatorActivity);
    }

    private void startSerialMonitorActivity()
    {
        Intent serialMonitorActivity = new Intent(MainActivity.this, SerialMonitorActivity.class);
        startActivity(serialMonitorActivity);
    }


    public void checkPermissions() {
        int result;
        String[] permissions = new String[]{
				Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,

        };

        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{p},
                        0);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissions Missing, Exiting", Toast.LENGTH_LONG).show();
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}
