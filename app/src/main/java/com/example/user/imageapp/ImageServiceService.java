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
import java.util.ArrayList;
import java.util.List;

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

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "default");
        mBuilder.setContentTitle("Picture Download")
                .setContentText("Download in progress")
                .setPriority(NotificationCompat.PRIORITY_LOW);

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
                                            //  startTransfer();

                                            //here you must put your computer's IP address.
                                            InetAddress serverAddr = InetAddress.getByName("10.0.2.2");
                                            //create a socket to make the connection with the server
                                            socket = new Socket(serverAddr, 1234);
                                            try {
                                                //getting the camera folder
                                                File dcim = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
                                                if (dcim == null) {
                                                    return;
                                                }

                                                //getting pictures list
                                                final List<File> pics = new ArrayList<File>();
                                                searhSubFolders(dcim.toString(), pics);

                                                // Issue the initial notification with zero progress
                                                final int PROGRESS_MAX = pics.size();
                                                final int PROGRESS_CURRENT = (1/PROGRESS_MAX) * 100; //the relative part of the progress

                                                //sending pictures
                                                for (int i = 0; i < pics.size(); i++) {
                                                    OutputStream oStream = socket.getOutputStream();
                                                    FileInputStream fis = new FileInputStream(pics.get(i));
                                                    Bitmap bm = BitmapFactory.decodeStream(fis);
                                                    byte[] imgBytes = getBytesFromBitmap(bm);

                                                    oStream.write(ByteBuffer.allocate(4).putInt(imgBytes.length).array());
                                                    oStream.write(imgBytes);
                                                    oStream.write(ByteBuffer.allocate(4).putInt(pics.get(i).getName().getBytes().length).array());
                                                    oStream.write(pics.get(i).getName().getBytes());

                                                    oStream.flush();

                                                    mBuilder.setProgress(100, PROGRESS_CURRENT, false);
                                                    notificationManager.notify(1, mBuilder.build());
                                            }

                                                // When done, update the notification one more time to remove the progress bar
                                                mBuilder.setContentText("Download complete")
                                                        .setProgress(0,0,false);
                                                notificationManager.notify(1, mBuilder.build());
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
        //File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File dcim = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if (dcim == null) {
            return;
        }

//        File[] pics = dcim.listFiles();

        final List<File> pics = new ArrayList<File>();
        searhSubFolders(dcim.toString(), pics);

   //     int count =0;

        if (pics != null) {
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "default");
            mBuilder.setContentTitle("Picture Download")
                    .setContentText("Download in progress")
                    .setPriority(NotificationCompat.PRIORITY_LOW);

            // Issue the initial notification with zero progress
            final int PROGRESS_MAX = pics.size();
            final int PROGRESS_CURRENT = (1/PROGRESS_MAX) * 100; //the relative part of the progress
//            mBuilder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
//            notificationManager.notify(1, mBuilder.build());

            Thread thread = new Thread() {
                public void run() {
                    for (File pic : pics)

                    {
                        //transfer pic
                        FileInputStream fis = null;
                        try {
                            fis = new FileInputStream(pic);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                        sendPic(pic);
                        mBuilder.setProgress(100, PROGRESS_CURRENT, false);
                        notificationManager.notify(1, mBuilder.build());
                    }
                }
            };

            thread.start();

            // When done, update the notification one more time to remove the progress bar
            mBuilder.setContentText("Download complete")
                    .setProgress(0,0,false);
            notificationManager.notify(1, mBuilder.build());

        }
    }

    public void searhSubFolders(String directoryName, List<File> files) {
        File directory = new File(directoryName);

        // Get all the files from a directory.
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                searhSubFolders(file.getAbsolutePath(), files);
            }
        }
    }

    public void sendPic(File pic) {
           try {
                //sends the message to the server
                OutputStream output = socket.getOutputStream();
                FileInputStream fis = new FileInputStream(pic);

               Bitmap bm = BitmapFactory.decodeStream(fis);
               byte[] imgbyte = getBytesFromBitmap(bm);

                //sending the size of the picture
                output.write(ByteBuffer.allocate(4).putInt(imgbyte.length).array());
               output.flush();

                //sending the picture
                output.write(imgbyte);
               output.flush();

                //sendind the picture's name
               output.write(ByteBuffer.allocate(4).putInt(pic.getName().getBytes().length).array());
               output.flush();

               output.write(pic.getName().getBytes());
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
