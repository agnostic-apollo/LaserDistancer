package com.allonsy.laserdistancer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class DistanceCalculatorActivity extends BaseActivity {

    private Button mClearLogsButton;
    private Button mSaveLogsButton;
    private ImageView mCameraImage;
    private TextView mDistanceText;
    private TextView mLogText;
    private boolean imageReceived = false;
    private boolean distanceReceived = false;

    UIUpdateReceiver uiUpdateReceiver;

    public static final String ACTION_UPDATE_DISTANCE = "updateDistance";
    public static final String ACTION_UPDATE_IMAGE = "updateImageView";
    public static final String ACTION_UPDATE_LOG = "updateLog";

    public static final String EXTRA_IMAGE_PATH = "imagePath";
    public static final String EXTRA_LOG = "log";
    public static final String EXTRA_LOG_TYPE = "logType";
    public static final String EXTRA_LOG_TYPE_DEBUG = "logType";
    public static final String EXTRA_LOG_TYPE_ERROR = "logError";

    private HashMap<String,UsbDevice> usbDeviceListHashMap;
    private ArrayList<String> usbDeviceListText;
    int imageViewWidth=0;
    int imageViewHeight=0;
    private String logsFolderPath = "";
    private String logsFilePath = "";
    private String currentLogsFilePath = "";
    private String imagesFolderPath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_distance_calculator);
        handleFiles();


        mCameraImage = (ImageView) findViewById(R.id.camera_image_view);
        mDistanceText = (TextView) findViewById(R.id.distance_text);
        mLogText = (TextView) findViewById(R.id.log_text);



        final ViewTreeObserver vto = mCameraImage.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                imageViewWidth = mCameraImage.getWidth();
                imageViewHeight = mCameraImage.getHeight();
                //Then remove layoutChange Listener
                ViewTreeObserver vto = mCameraImage.getViewTreeObserver();
                vto.removeOnGlobalLayoutListener(this);
            }
        });

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
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unRegisterUIUpdateReceivers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!isMyServiceRunning(DistanceCalculatorService.class))
            deleteLogFile();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_distance_calculator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_start:
                if(!isMyServiceRunning(DistanceCalculatorService.class)) {
                    if (isMyServiceRunning(SerialMonitorService.class))
                        showStoppingServiceDialogue(SERIAL_MONITOR_SERVICE);
                    else {
                        if (isUSBDeviceConnectedAndPermissionGranted(DISTANCE_CALCULATOR_SERVICE)) {
                            if (!isMyServiceRunning(DistanceCalculatorService.class)) {
                                startDistanceCalculatorService();
                            } else
                                Toast.makeText(this, DISTANCE_CALCULATOR_SERVICE + " Already Started", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else
                    Toast.makeText(this, DISTANCE_CALCULATOR_SERVICE + " Already Started", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_item_stop:
                if(isMyServiceRunning(DistanceCalculatorService.class)) {
                    stopDistanceCalculatorService(this);
                }
                else
                    Toast.makeText(this,DISTANCE_CALCULATOR_SERVICE + " not Started", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void startDistanceCalculatorService()
    {
        if(device!=null) {
            Toast.makeText(this,"Starting " + DISTANCE_CALCULATOR_SERVICE , Toast.LENGTH_SHORT).show();
            resetUI();
            if(logsFolderPath!=null)
                currentLogsFilePath = logsFolderPath + File.separator + ("Log_" + new Date(System.currentTimeMillis())).replaceAll(" ", "_").replaceAll(":", "_") + ".txt";
            Intent distanceCalculatorService = new Intent(DistanceCalculatorActivity.this, DistanceCalculatorService.class);
            distanceCalculatorService.setAction(DistanceCalculatorService.ACTION_SERVICE_START);
            distanceCalculatorService.putExtra(EXTRA_USB_DEVICE, device);
            distanceCalculatorService.putExtra(EXTRA_LOGS_FILE_PATH, logsFilePath);
            distanceCalculatorService.putExtra(EXTRA_CURRENT_LOGS_FILE_PATH, currentLogsFilePath);
            distanceCalculatorService.putExtra(EXTRA_IMAGES_FOLDER_PATH, imagesFolderPath);
            startService(distanceCalculatorService);
        }
        else
            Toast.makeText(this,"Cant start " + DISTANCE_CALCULATOR_SERVICE + ", device is null", Toast.LENGTH_SHORT).show();
    }

    public static void stopDistanceCalculatorService(Context context)
    {
        Toast.makeText(context,"Stopping " + DISTANCE_CALCULATOR_SERVICE , Toast.LENGTH_SHORT).show();
        Intent distanceCalculatorService = new Intent(context, DistanceCalculatorService.class);
        distanceCalculatorService.setAction(DistanceCalculatorService.ACTION_SERVICE_STOP);
        context.startService(distanceCalculatorService);
    }

    public void updateUI() {
        updateDistanceText();
        updateImageView();
        updateLogs();
    }

    public void resetUI() {
        distanceReceived=false;
        imageReceived=false;
        mDistanceText.setText("Distance = - ");
        mCameraImage.setImageDrawable(null);
    }

    public void updateDistanceText() {
        if(distanceReceived)
            mDistanceText.setText("Distance = " + String.valueOf(QueryPreferences.getDistance(this)) + " ft");
        else
            mDistanceText.setText("Distance = - ");
    }

    public void updateImageView() {
        if(imageReceived) {
            String filePath = QueryPreferences.getImagePath(this);
            if (!filePath.equals("")) {
                File image = new File(filePath);
                if (image.exists() && image.isFile()) {
                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(), bmOptions);
                    //Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if(bitmap!=null)
                         mCameraImage.setImageBitmap(Bitmap.createScaledBitmap(bitmap, imageViewWidth, imageViewHeight, true));
                }
            }
        }
        else
            mCameraImage.setImageDrawable(null);
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

    public void registerUpdateUIReceivers() {

        unRegisterUIUpdateReceivers();
        uiUpdateReceiver = new UIUpdateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_DISTANCE);
        filter.addAction(ACTION_UPDATE_IMAGE);
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

    public void onBackPressed() {
        showBackButtonPressedDialogue(DISTANCE_CALCULATOR_SERVICE);
    }


    private class UIUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals((ACTION_UPDATE_DISTANCE))) {
                distanceReceived = true;
                updateDistanceText();
            }

            else if (intent.getAction().equals((ACTION_UPDATE_IMAGE))) {
                imageReceived = true;
                updateImageView();
            }

            else if (intent.getAction().equals((ACTION_UPDATE_LOG))) {

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



    public void deleteLogFile()
    {
        if(logsFilePath!=null)
            FileUtil.deleteFile(logsFilePath);
    }

    private void handleFiles() {
        logsFolderPath = null;
        logsFilePath = null;
        currentLogsFilePath = null;
        imagesFolderPath = null;
        boolean success1 = true;
        boolean success2 = true;

        File dir =  this.getExternalFilesDir(null);
        String outDirPath="";
        if(dir==null || !dir.exists() || !dir.isDirectory()){
            logError("Output Directory to store files does not exists");
            return;
        }

        File imagesDir = new File(dir.getAbsolutePath() + File.separator + "images");
        if (!imagesDir.exists() ) {
            success1 = imagesDir.mkdir();
        }
        else if(imagesDir.exists() && !imagesDir.isDirectory())
        {
            logError("Failed to create images directory because of already existing file");
            success1 = false;
        }

        File logsDirectory = new File(dir.getAbsolutePath() + File.separator + "distanceCalculatorLogs");
        if (!logsDirectory.exists() ) {
            success2 = logsDirectory.mkdir();
        }
        else if(logsDirectory.exists() && !logsDirectory.isDirectory())
        {
            logError("Failed to create distanceCalculatorLogs directory because of already existing file");
            success2 = false;
        }

        if(success2)
             imagesFolderPath = imagesDir.getAbsolutePath();
        if(success2) {
            logsFolderPath = logsDirectory.getAbsolutePath() ;
            logsFilePath = logsFolderPath + File.separator + "log.txt";
        }
    }
}
