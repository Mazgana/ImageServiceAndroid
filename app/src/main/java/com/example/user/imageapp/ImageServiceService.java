package com.example.user.imageapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ImageServiceService extends Service {
    InetAddress serverAddr;
    Socket socket;
    BroadcastReceiver yourReceiver;

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
        final IntentFilter theFilter = new IntentFilter();
        theFilter.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
        theFilter.addAction("android.net.wifi.STATE_CHANGE");

        this.yourReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (networkInfo != null) {
                        if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                            //get the different network states
                            if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                                Thread t = new Thread( new Runnable() {
                                    public void run() {
                                        //   startTransfer();            // Starting the Transfer
                                        try {
                                            //here you must put your computer's IP address.
                                            InetAddress serverAddr = InetAddress.getByName("10.0.2.2");
                                            //create a socket to make the connection with the server
                                            Socket socket = new Socket(serverAddr, 1234);
                                            try {
                                            /*
                                            for (int i = 0; i < imageList.size(); i++) {
                                                OutputStream output = socket.getOutputStream();
                                                FileInputStream fis = new FileInputStream(imageList.get(i));
                                                Bitmap bm = BitmapFactory.decodeStream(fis);
                                                byte[] imgbyte = getBytesFromBitmap(bm);
                                                output.write(ByteBuffer.allocate(4).putInt(imgbyte.length).array());
                                                output.write(imgbyte);
                                                output.write(ByteBuffer.allocate(4).putInt(imageList.get(i).getName().getBytes().length).array());
                                                output.write(imageList.get(i).getName().getBytes());
                                                output.flush();
                                            }*/
                                            } catch (Exception e) {
                                                Log.e("TCP", "S: Error", e);
                                            } finally {
                                                socket.close();
                                            }
                                        } catch (Exception e) {
                                            Log.e("TCP", "C: Error", e);
                                        }
                                    }
                                });
                                t.start();
                            }
                        }
                    }
                }
            };
            // Registers the receiver so that your service will listen for broadcasts
            this.registerReceiver(this.yourReceiver, theFilter);

        return START_STICKY;
    }

    public void startTransfer() {
        // Getting the Camera Folder
        File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        if (dcim == null) {
            return;
        }

        File[] pics = dcim.listFiles();

        int count =0;

        if (pics != null) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "default");
            mBuilder.setContentTitle("Picture Download")
                    .setContentText("Download in progress")
                    .setPriority(NotificationCompat.PRIORITY_LOW);

            // Issue the initial notification with zero progress
            int PROGRESS_MAX = 100;
            int PROGRESS_CURRENT = 0;
            mBuilder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
            notificationManager.notify(1, mBuilder.build());

            for (File pic : pics) {
	            //transfer pic
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(pic);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Bitmap bm = BitmapFactory.decodeStream(fis);
                byte[] imgbyte = getBytesFromBitmap(bm);
                sendPic(pic, imgbyte);

            }

            // When done, update the notification one more time to remove the progress bar
            mBuilder.setContentText("Download complete")
                    .setProgress(0,0,false);
            notificationManager.notify(1, mBuilder.build());
        }
    }

    public void sendPic(File pic, byte[] imgbyte) {
           try {
                //sends the message to the server
                OutputStream output = socket.getOutputStream();
                FileInputStream fis = new FileInputStream(pic);
                output.write(imgbyte);
                output.flush();

            } catch (Exception e) {
                Log.e("TCP", "S: Error", e);
            } finally {
               try {
                   this.socket.close();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
        return stream.toByteArray();
    }


    public void onDestroy() {
        Toast.makeText(this,"Service ending...", Toast.LENGTH_SHORT).show();
    }
}
