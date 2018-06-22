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
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ImageServiceService extends Service {
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public int onStartCommand(Intent intent, int flag, int startId) {
        Toast.makeText(this,"Service starting...", Toast.LENGTH_SHORT).show();
        final IntentFilter theFilter = new IntentFilter();
        theFilter.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
        theFilter.addAction("android.net.wifi.STATE_CHANGE");

        String channelId = "Channel";
        CharSequence name = "new channel";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel notificationChannel = new NotificationChannel(channelId, name, importance);

        final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);

        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "Channel");
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
                                    try {
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
                                             //final int PROGRESS_CURRENT = Math.round(100 / PROGRESS_MAX); //the relative part of the progress

                                            int progress_current = 0;

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

                                                    progress_current = (i / PROGRESS_MAX) * 100;

                                                    mBuilder.setProgress(100, progress_current, false);
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

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
        return stream.toByteArray();
    }

    public void onDestroy() {
        Toast.makeText(this,"Service ending...", Toast.LENGTH_SHORT).show();
    }
}
