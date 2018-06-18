package com.example.user.imageapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ImageServiceService extends Service {
    public ImageServiceService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flag, int startId) {
        Toast.makeText(this,"Service starting...", Toast.LENGTH_SHORT).show();
        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName("10.0.0.2");
            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, 1234);

//            try {
//                //sends the message to the server
//                OutputStream output = socket.getOutputStream();
//                FileInputStream fis = new FileInputStream(pic);
//                output.write(imgbyte);
//                output.flush();
//
//            } catch (Exception e) {
//                Log.e("TCP", "S: Error", e);
//            } finally {
//                socket.close();
//            }
        } catch (Exception e) {
            Log.e("TCP", "C: Error", e);
        }

        return START_STICKY;
    }

    public void onDestroy() {
        Toast.makeText(this,"Service ending...", Toast.LENGTH_SHORT).show();
    }
}
