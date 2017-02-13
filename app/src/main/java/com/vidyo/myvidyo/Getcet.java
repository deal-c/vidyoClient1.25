package com.vidyo.myvidyo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.esoon.vidyosample.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Administrator on 2017/2/11.
 */

public class Getcet extends Activity{
    private static Context  Context=null;

    public static Context   getContext(){
        return Context;
    }
    private static File file=null;
    public static File  getFile(){
        return file;
    }
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



    }

    public String getAndroidInternalMemDir() throws IOException {
        File fileDir = getFilesDir(); //crashing
        if (fileDir != null) {
            String filedir = fileDir.toString() + "/";

            return filedir;
        } else {

        }
        return null;
    }


    public String writeCaCertificates() {

        try {
            InputStream caCertStream = getResources().openRawResource(R.raw.ca_certificates);
//			File caCertFileName;
//			caCertFileName = getFileStreamPath("ca-certificates.crt");

            File caCertDirectory;
            try {
                String pathDir = getAndroidInternalMemDir();
                caCertDirectory = new File(pathDir);
            } catch (Exception e) {
                caCertDirectory =Context.getDir("marina", 0);
            }
            File cafile = new File(caCertDirectory, "ca-certificates.crt");

            FileOutputStream caCertFile = new FileOutputStream(cafile);
            byte buf[] = new byte[1024];
            int len;
            while ((len = caCertStream.read(buf)) != -1) {
                caCertFile.write(buf, 0, len);
            }
            caCertStream.close();
            caCertFile.close();

            return cafile.getPath();
        } catch (Exception e) {
            return null;
        }
    }
}
