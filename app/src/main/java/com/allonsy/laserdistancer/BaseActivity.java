package com.allonsy.laserdistancer;


import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BaseActivity extends AppCompatActivity {

    public static final String ACTION_USB_PERMISSION = "usbPermmision";
    public static final String EXTRA_USB_DEVICE = "usbDevice";
    public static final String EXTRA_LOGS_FILE_PATH = "logsFilePath";
    public static final String EXTRA_CURRENT_LOGS_FILE_PATH = "currentLogsFilePath";
    public static final String EXTRA_IMAGES_FOLDER_PATH = "imagesFolderPath";
    public static final String EXTRA_NAME = "name";
    public static final String DISTANCE_CALCULATOR_ACTIVITY = "Distance Calculator Activity";
    public static final String SERIAL_MONITOR_ACTIVITY = "Serial Monitor Activity";
    public static final String DISTANCE_CALCULATOR_SERVICE = "Distance Calculator Service";
    public static final String SERIAL_MONITOR_SERVICE = "Serial Monitor Service";


    private HashMap<String,UsbDevice> usbDeviceListHashMap;
    private ArrayList<String> usbDeviceListText;
    UsbManager usbManager;
    UsbDevice device;

    protected void registerUSBPermissionBroadcastReceiver() {

        unregisterUsbPermissionBroadcastReceiver();
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbPermissionBroadcastReceiver, filter);
    }

    protected void unregisterUsbPermissionBroadcastReceiver() {

        try {
            unregisterReceiver(usbPermissionBroadcastReceiver);
        }
        catch(IllegalArgumentException e) {
            //Logger.logError(e.getMessage());
        }
    }

    protected boolean isUSBDeviceConnectedAndPermissionGranted(final String name) {
        usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        HashMap<String,UsbDevice> usbDevices = usbManager.getDeviceList();
        UsbDevice usbDevice;

        if (!usbDevices.isEmpty()) {
            if(device!=null)
            {
                if(!usbManager.hasPermission(device)) { //if no permission
                    requestUSBDevicePermission(name);
                    return false;
                }
                else //if permission already given
                    return true;
            }
            else {
                ArrayList<UsbDevice> usbDeviceList = new ArrayList<UsbDevice>();
                for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                    usbDevice = entry.getValue();
                    usbDeviceList.add(usbDevice);
                }
                showUSBDeviceListDialogue(usbDeviceList,name);
                return false;
            }
        }
        else {
            Toast.makeText(this, "Device not Connected", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /*
    public boolean permissionToUsbDeviceGranted() {
        usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        HashMap<String,UsbDevice> usbDevices = usbManager.getDeviceList();

        if (!usbDevices.isEmpty()) {
            for (Map.Entry<String,UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                //Toast.makeText(this, String.valueOf(deviceVID), Toast.LENGTH_LONG).show();
                if (deviceVID == 9025)//Arduino Vendor ID
                {
                    if(!usbManager.hasPermission(device)) { //if no permission
                        PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                        usbManager.requestPermission(device, pi);
                        return false;
                    }
                    else //if permission already given
                        return true;
                }
                else
                {
                    connection = null;
                    device = null;
                }
            }
        }

        Toast.makeText(this, "Device not Connected", Toast.LENGTH_LONG).show();
        return false;
    }
    */

    protected void requestUSBDevicePermission(String name)
    {
        registerUSBPermissionBroadcastReceiver();
        Intent intent = new Intent(ACTION_USB_PERMISSION);
        intent.putExtra(EXTRA_NAME, name);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        usbManager.requestPermission(device, pi);
    }

    protected final BroadcastReceiver usbPermissionBroadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {

            unregisterUsbPermissionBroadcastReceiver();
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    if (device != null) {
                        //logDebug("Permission Granted, Starting Service");
                        String extraName = intent.getExtras().getString(EXTRA_NAME);
                        if(extraName!=null) {
                            if (extraName.equals(DISTANCE_CALCULATOR_SERVICE)) {
                                startDistanceCalculatorService();
                            }
                            if (extraName.equals(SERIAL_MONITOR_SERVICE)) {
                                startSerialMonitorService();
                            }
                        }
                        else
                            Toast.makeText(BaseActivity.this, "Cant start a null service", Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(BaseActivity.this, "Permission to usb device not granted", Toast.LENGTH_LONG).show();
                }
            }
        };
    };

    protected void showUSBDeviceListDialogue(ArrayList<UsbDevice> usbDeviceList, final String name)
    {
        device = null;
        usbDeviceListText = new  ArrayList<String>();
        usbDeviceListHashMap = new HashMap<String,UsbDevice>();

        for (int i=0;i!=usbDeviceList.size();i++) {
            UsbDevice device = usbDeviceList.get(i);
            String text = device.getDeviceId() + " ("+ device.getProductName() + ", " +
                    device.getManufacturerName() + ")";
            usbDeviceListHashMap.put(text,device);
            usbDeviceListText.add(text);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Arduino Device");

        // add a list
        builder.setItems(usbDeviceListText.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which>=0 && which < usbDeviceListText.size()) {
                    String text = usbDeviceListText.get(which);
                    if(usbDeviceListHashMap.containsKey(text)) {
                        device = usbDeviceListHashMap.get(text);
                        if (!usbManager.hasPermission(device)) { //if no permission
                            requestUSBDevicePermission(name);
                        } else //if permission already given
                        {
                            if (name.equals(DISTANCE_CALCULATOR_SERVICE)) {
                                if (!isMyServiceRunning(DistanceCalculatorService.class)) {
                                    startDistanceCalculatorService();
                                } else
                                    Toast.makeText(BaseActivity.this, DISTANCE_CALCULATOR_SERVICE + "Already Started", Toast.LENGTH_SHORT).show();
                            }
                            else if (name.equals(SERIAL_MONITOR_SERVICE)) {
                                if(!isMyServiceRunning(SerialMonitorService.class)) {
                                    startSerialMonitorService();
                                }
                                else
                                    Toast.makeText(BaseActivity.this, SERIAL_MONITOR_SERVICE + " Already Started", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
                dialog.dismiss();
            }
        });


        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if(device==null)
                {
                    Toast.makeText(BaseActivity.this, "Device not Selected", Toast.LENGTH_LONG).show();
                }
                // dialog dismiss without button press
            }
        });

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }


    protected void startDistanceCalculatorService()
    {

    }

    protected void startSerialMonitorService()
    {

    }

    protected void showStoppingServiceDialogue(final String name) {

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Prompt")
                .setMessage(name + " Running, Do you want to stop it and continue?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if(name.equals(DISTANCE_CALCULATOR_SERVICE)) {
                            DistanceCalculatorActivity.stopDistanceCalculatorService(BaseActivity.this);
                            try {Thread.sleep(3000);} catch (Exception e) {Logger.logStackTrace(e);}
                            if (isUSBDeviceConnectedAndPermissionGranted(SERIAL_MONITOR_SERVICE)) {
                                if (!isMyServiceRunning(SerialMonitorService.class)) {
                                    startSerialMonitorService();
                                } else
                                    Toast.makeText(BaseActivity.this, SERIAL_MONITOR_SERVICE + "Already Started", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else if(name.equals(SERIAL_MONITOR_SERVICE)) {
                            SerialMonitorActivity.stopSerialMonitorService(BaseActivity.this);
                            try {Thread.sleep(3000);} catch (Exception e) {Logger.logStackTrace(e);}
                            if (isUSBDeviceConnectedAndPermissionGranted(DISTANCE_CALCULATOR_SERVICE)) {
                                if(!isMyServiceRunning(DistanceCalculatorService.class)) {
                                    startDistanceCalculatorService();
                                }
                                else
                                    Toast.makeText(BaseActivity.this, DISTANCE_CALCULATOR_SERVICE + " Already Started", Toast.LENGTH_SHORT).show();
                            }
                        }
                        dialog.dismiss();
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }

                })
                .show();
    }

    protected void showBackButtonPressedDialogue(final String name) {
        if(name.equals(DISTANCE_CALCULATOR_SERVICE)) {
            if (isMyServiceRunning(DistanceCalculatorService.class)) {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(name)
                        .setMessage("Do you want to keep " + name + " running in background?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //writeLogsToFile();
                                finish();
                            }

                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                DistanceCalculatorActivity.stopDistanceCalculatorService(BaseActivity.this);
                                //deleteLogsFile();
                                finish();
                            }

                        })
                        .show();
            } else
                finish();
        }
        else if(name.equals(SERIAL_MONITOR_SERVICE)) {
            if (isMyServiceRunning(SerialMonitorService.class)) {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(name)
                        .setMessage("Do you want to keep " + name + " running in background?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //writeLogsToFile();
                                finish();
                            }

                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SerialMonitorActivity.stopSerialMonitorService(BaseActivity.this);
                                //deleteLogsFile();
                                finish();
                            }

                        })
                        .show();
            } else
                finish();
        }
    }

    protected boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

