package com.allonsy.laserdistancer;


import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static android.content.ContentValues.TAG;

public class Logger {

    static private boolean errorLogsEnabled= true;
    static private boolean debugLogsEnabled= true;

    static public void logError(String message)
    {
        if(errorLogsEnabled)
            Log.e(TAG,message);
    }
    static public void logDebug(String message)
    {
        if(debugLogsEnabled)
            Log.d(TAG,message);
    }

    static public void logStackTrace(Exception e)
    {
        if(errorLogsEnabled)
        {
            try {
                StringWriter errors = new StringWriter();
                PrintWriter pw = new PrintWriter(errors);
                e.printStackTrace(pw);
                pw.close();
                logError(errors.toString());
                errors.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }
}
