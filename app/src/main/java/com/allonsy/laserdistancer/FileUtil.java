package com.allonsy.laserdistancer;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

public class FileUtil {

    public static void moveFile(String inputPath, String outputPath) {
        File file1 = new File(inputPath);
        File file2 = new File(outputPath);
        String inputParentPath = file1.getParent();
        String outputParentPath = file2.getParent();

        if(inputParentPath==null)
            inputParentPath="";
        if(outputParentPath==null)
            outputParentPath="";

            moveFile(inputParentPath, file1.getName(), outputParentPath, file2.getName());
    }


    public static void moveFile(String inputPath, String inputFile, String outputPath, String outputFile) {

        if(!inputPath.equals("") && inputPath.charAt(inputPath.length()-1)!=File.separatorChar)
            inputPath += File.separator;

        if(!outputPath.equals("") && outputPath.charAt(outputPath.length()-1)!=File.separatorChar)
            outputPath += File.separator;

        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            boolean isOutDirExists=true;
            File dir = new File (outputPath);
            if (!dir.exists())
            {
                isOutDirExists=dir.mkdirs();
            }

            if(isOutDirExists) {
                in = new FileInputStream(inputPath + inputFile);
                out = new FileOutputStream(outputPath + outputFile);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                in = null;

                // write the output file
                out.flush();
                out.close();
                out = null;

                // delete the original file
                if(!new File(inputPath + inputFile).delete())
                    Logger.logError(inputPath + inputFile + " not deleted after moving");

            }
            else
                Logger.logError(outputPath + " does not exist, move failed");
        }
        catch (Exception e) {
            Logger.logError(e.getMessage());
        }


    }

    public static void deleteFile(String inputPath) {
        File file = new File(inputPath);

        String inputParentPath = file.getParent();

        if(inputParentPath==null)
            inputParentPath="";

        deleteFile(inputParentPath, file.getName());
    }



    public static void deleteFile(String inputPath, String inputFile) {
        try {
            if(!inputPath.equals("") && inputPath.charAt(inputPath.length()-1)!=File.separatorChar)
                inputPath += File.separator;

            File file = new File(inputPath + inputFile);
            if(!file.exists()) {
                Logger.logError(inputPath + inputFile + " does not exist, delete failed");
                return;
            }
            // delete the original file
            if(!file.delete())
                Logger.logError(inputPath + inputFile + " not deleted, delete failed");
        }
        catch (Exception e) {
            Logger.logError(e.getMessage());
        }
    }

    public static void copyFile(String inputPath, String outputPath) {
        File file1 = new File(inputPath);
        File file2 = new File(outputPath);
        String inputParentPath = file1.getParent();
        String outputParentPath = file2.getParent();

        if(inputParentPath==null)
            inputParentPath="";
        if(outputParentPath==null)
            outputParentPath="";

        copyFile(inputParentPath, file1.getName(), outputParentPath, file2.getName());
    }


    public static void copyFile(String inputPath, String inputFile, String outputPath,  String outputFile) {

        InputStream in = null;
        OutputStream out = null;
        try {

            if(!inputPath.equals("") && inputPath.charAt(inputPath.length()-1)!=File.separatorChar)
                inputPath += File.separator;

            if(!outputPath.equals("") && outputPath.charAt(outputPath.length()-1)!=File.separatorChar)
                outputPath += File.separator;

            boolean isOutDirExists=true;
            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists())
            {
                isOutDirExists=dir.mkdirs();
            }

            if(isOutDirExists) {
                in = new FileInputStream(inputPath + inputFile);
                out = new FileOutputStream(outputPath + outputFile);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                in = null;

                // write the output file (You have now copied the file)
                out.flush();
                out.close();
                out = null;
            }
            else
                Logger.logError(outputPath + " does not exist, copy failed");
        }
        catch (Exception e) {
            Logger.logError(e.getMessage());
        }

    }

    public static boolean writeStringToTextFile(String filePath, String data, boolean append)
    {

        try {
            PrintWriter out = new PrintWriter(new FileOutputStream(new File(filePath), append));
            out.println(data);
            out.close();

        } catch (IOException e) {
            Logger.logError(e.getMessage());
            return false;
        }

        return true;
    }

    public static String readStringFromTextFile(String filePath, Charset charset)
    {
        try {
            if(!Charset.isSupported(charset.name())) {
                Logger.logError("Charset not supported");
                return null;
            }
        } catch ( IllegalArgumentException e) {
            Logger.logError("Invalid Charset: " + e.getMessage());
            return null;
        }

        String data = "";
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(filePath);
            inputStreamReader = new InputStreamReader(fileInputStream, charset);
            bufferedReader = new BufferedReader(inputStreamReader);

            String receiveString = "";
            StringBuilder stringBuilder = new StringBuilder();

            while ( (receiveString = bufferedReader.readLine()) != null ) {
                stringBuilder.append(receiveString).append("\n");
            }

            data = stringBuilder.toString();

        }
        catch (FileNotFoundException e) {
            Logger.logError("File not found: " + e.getMessage());
            return null;
        } catch (IOException e) {
            Logger.logError("Failed to write to file : " + e.getMessage());
            return null;
        }finally {
            try {
                if (fileInputStream != null)
                    fileInputStream.close();
                if (bufferedReader != null)
                    bufferedReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return data;
    }
}
