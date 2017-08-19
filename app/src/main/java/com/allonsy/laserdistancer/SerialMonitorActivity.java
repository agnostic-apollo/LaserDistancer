package com.allonsy.laserdistancer;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

public class SerialMonitorActivity extends BaseActivity {

    private Button mSendCommand;
    private Button mSendQuickCommand;
    private Spinner mQuickCommandList;
    private EditText mCommand;
    private TextView mLogText;
    private Button mClearLogsButton;

    UIUpdateReceiver uiUpdateReceiver;
    private static final String ACTION_USB_PERMISSION = "usbPermmision";
    public static final String EXTRA_USB_DEVICE = "usbDevice";
    public static final String ACTION_UPDATE_LOG = "updateLog";
    public static final String EXTRA_LOG = "log";
    public static final String EXTRA_LOG_TYPE = "logType";
    public static final String EXTRA_LOG_TYPE_DEBUG = "logType";
    public static final String EXTRA_LOG_TYPE_ERROR = "logError";
    public static final String SAVED_LOG_TEXT = "savedLogText";

    private String logsFolderPath = "";
    private String logsFilePath = "";
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    UsbDeviceConnection connection;
    int imageViewWidth=0;
    int imageViewHeight=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_serial_monitor);
        handleFiles();

        mLogText = (TextView) findViewById(R.id.log_text);
        updateLogs();

        mCommand = (EditText) findViewById(R.id.command);


        mSendCommand = (Button) findViewById(R.id.send_command);
        mSendCommand.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {

                if(isMyServiceRunning(SerialMonitorService.class)) {
                    String command = mCommand.getText().toString();
                    if (!command.equals("")) {
                        sendCommandToSerialMonitorService(command);
                    } else
                        Toast.makeText(SerialMonitorActivity.this, "enter command first", Toast.LENGTH_LONG).show();
                }
                else
                    Toast.makeText(SerialMonitorActivity.this, SERIAL_MONITOR_SERVICE + " not Started", Toast.LENGTH_SHORT).show();

            }
        });

        mSendQuickCommand = (Button) findViewById(R.id.send_quick_command);
        mSendQuickCommand.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(isMyServiceRunning(SerialMonitorService.class)) {
                    String command = String.valueOf(mQuickCommandList.getSelectedItem());
                    if(command!=null && !command.equals("")) {
                        sendCommandToSerialMonitorService(command);
                    }
                    else
                        Toast.makeText(SerialMonitorActivity.this, "no quick command selected", Toast.LENGTH_LONG).show();
                }
                else
                    Toast.makeText(SerialMonitorActivity.this, SERIAL_MONITOR_SERVICE + " not Started", Toast.LENGTH_SHORT).show();
            }
        });

        mQuickCommandList = (Spinner) findViewById(R.id.quick_command_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getQuickCommandArray());
        mQuickCommandList.setAdapter(adapter);

        mClearLogsButton = (Button) findViewById(R.id.clear_logs_button);
        mClearLogsButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(mLogText!=null)
                    mLogText.setText("");
                deleteLogFile();
            }
        });

    }
    @Override
    protected void onResume() {
        super.onResume();
        registerUpdateUIReceivers();
        updateLogs();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unRegisterUIUpdateReceivers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!isMyServiceRunning(SerialMonitorService.class))
            deleteLogFile();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_serial_monitor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_start:
                if(!isMyServiceRunning(SerialMonitorService.class)) {
                    if (isMyServiceRunning(DistanceCalculatorService.class))
                        showStoppingServiceDialogue(DISTANCE_CALCULATOR_SERVICE);
                    else {
                        if (isUSBDeviceConnectedAndPermissionGranted(SERIAL_MONITOR_SERVICE)) {
                            if (!isMyServiceRunning(SerialMonitorService.class)) {
                                startSerialMonitorService();
                            } else
                                Toast.makeText(this, SERIAL_MONITOR_SERVICE + " Already Started", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else
                    Toast.makeText(this, SERIAL_MONITOR_SERVICE + " Already Started", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_item_stop:
                if(isMyServiceRunning(SerialMonitorService.class)) {
                    stopSerialMonitorService(this);
                }
                else
                    Toast.makeText(this,SERIAL_MONITOR_SERVICE + " not Started", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onBackPressed() {
        showBackButtonPressedDialogue(SERIAL_MONITOR_SERVICE);
    }

    private String[] getQuickCommandArray()
    {
        ArrayList<String> quickCommandList = new  ArrayList<String>();
        quickCommandList.add("laserTurretStart");
        quickCommandList.add("laserTurretStop");
        quickCommandList.add("turnOnBothLasers");
        quickCommandList.add("turnOffBothLasers");
        quickCommandList.add("getAngle");
        quickCommandList.add("reset");

        return quickCommandList.toArray(new String[0]);
    }

    protected void startSerialMonitorService()
    {
        if(device!=null) {
        Toast.makeText(this, "Starting " + SERIAL_MONITOR_SERVICE , Toast.LENGTH_SHORT).show();
        Intent serialMonitorService = new Intent(SerialMonitorActivity.this, SerialMonitorService.class);
        serialMonitorService.setAction(SerialMonitorService.ACTION_SERVICE_START);
        serialMonitorService.putExtra(EXTRA_USB_DEVICE, device);
        serialMonitorService.putExtra(EXTRA_LOGS_FILE_PATH, logsFilePath);
        startService(serialMonitorService);
        }
        else
            Toast.makeText(this,"Cant start " + SERIAL_MONITOR_SERVICE + ", device is null", Toast.LENGTH_SHORT).show();
    }

    private void sendCommandToSerialMonitorService(String command)
    {
        Intent serialMonitorService = new Intent(SerialMonitorActivity.this, SerialMonitorService.class);
        serialMonitorService.setAction(SerialMonitorService.ACTION_SEND_COMMAND);
        serialMonitorService.putExtra(SerialMonitorService.EXTRA_COMMAND,command);
        startService(serialMonitorService);
    }

    public static void stopSerialMonitorService(Context context)
    {
        Toast.makeText(context,"Stopping " + SERIAL_MONITOR_SERVICE , Toast.LENGTH_SHORT).show();
        Intent serialMonitorService = new Intent(context, SerialMonitorService.class);
        serialMonitorService.setAction(SerialMonitorService.ACTION_SERVICE_STOP);
        context.startService(serialMonitorService);
    }


    public void registerUpdateUIReceivers() {

        unRegisterUIUpdateReceivers();
        uiUpdateReceiver = new UIUpdateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_LOG);
        registerReceiver(uiUpdateReceiver, filter);
    }

    public void unRegisterUIUpdateReceivers() {

        try {
            unregisterReceiver(uiUpdateReceiver);
        }
        catch(IllegalArgumentException e) {
            //Logger.logError(e.getMessage());
        }
    }




    private class UIUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals((ACTION_UPDATE_LOG))) {

                String message = intent.getStringExtra(EXTRA_LOG);
                String type = intent.getStringExtra(EXTRA_LOG_TYPE);
                if(type.equals(EXTRA_LOG_TYPE_DEBUG))
                    logDebug(message);
                else if(type.equals(EXTRA_LOG_TYPE_ERROR))
                    logError(message);
            }
        }
    }

    private void logDebug(String message)
    {
        if(mLogText!=null) {
            Spannable word = new SpannableString(message);
            word.setSpan(new ForegroundColorSpan(Color.BLACK), 0, word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mLogText.append("\n" + word);
        }
        Logger.logDebug(message);
    }

    private void logError(String message)
    {
        if(mLogText!=null) {
            Spannable word = new SpannableString(message);
            word.setSpan(new ForegroundColorSpan(Color.RED), 0, word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mLogText.append("\n" + word);
        }
    }

    public void updateLogs()
    {
        String data = FileUtil.readStringFromTextFile(logsFilePath, StandardCharsets.UTF_8);
        if(data!=null && mLogText != null) {
            mLogText.setText(data);
        }
        else
        {
            if(mLogText!=null)
                mLogText.setText("");
        }
    }

    public void deleteLogFile()
    {
        if(logsFilePath!=null)
            FileUtil.deleteFile(logsFilePath);
    }


    private void handleFiles() {
        logsFolderPath = null;
        logsFilePath = null;
        File dir = this.getExternalFilesDir(null);
        String outDirPath = "";
        boolean success1 = true;
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            logError("Output Directory to store files does not exists");
            return;
        }

        File logsDirectory = new File(dir.getAbsolutePath() + File.separator + "serialMonitorLogs");
        if (!logsDirectory.exists()) {
            success1 = logsDirectory.mkdir();
        } else if (logsDirectory.exists() && !logsDirectory.isDirectory()) {
            logError("Failed to create serialMonitorLogs directory because of already existing file");
            success1 = false;
        }

        if (success1) {
            logsFolderPath = logsDirectory.getAbsolutePath() + File.separator;
            logsFilePath = logsFolderPath + "log.txt";
        }
    }
}
