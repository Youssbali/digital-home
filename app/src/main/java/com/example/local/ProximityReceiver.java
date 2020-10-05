package com.example.local;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProximityReceiver extends BroadcastReceiver {


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(final Context context, Intent intent) {

        //Variable  Key pour déterminer si l'utilisateur quitte ou entre
        String key = LocationManager.KEY_PROXIMITY_ENTERING;

        //Variable booléenne pour savoir si l'utilisateur entre ou sort
        boolean state = intent.getBooleanExtra(key, false);

        if(state){

            Log.i("TAG", "Risque de proximité");

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder myPopup = new AlertDialog.Builder(context);
            myPopup.setTitle("Risque de proximité");
            myPopup.setMessage("Fais attention, une voiture se rapproche de toi");
            myPopup.setPositiveButton("OK, j'ai compris", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(context, "Le danger a été reconnu", Toast.LENGTH_SHORT).show();
                }
            });
            myPopup.show();
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "0")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentTitle("Risque de proximité")
                    .setContentText("Fais attention, une voiture se rapproche de toi");

            // Creates the intent needed to show the notification
            Intent notificationIntent = new Intent(context, MapLocationActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(contentIntent);
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(0, builder.build());
            // Create the AlertDialog object and return it

            //Faire une sonnerie de message ou vibreur (suivant le mode par défaut)
            //lors de l'entré de l'utilisateur
            AudioManager audiomanager = (AudioManager)
                    context.getSystemService(Context.AUDIO_SERVICE);

            switch (audiomanager.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT:
                    Log.i("Mode","mode silencieux");
                    break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    Log.i("Mode"," mode vibreur");
                    break;
                case AudioManager.RINGER_MODE_NORMAL:
                    Log.i("Mode"," mode normale");
                    break;
            }
            //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, null, 0);
        }else{

            Log.i("TagTest", "Pas de risque pour l'instant");

            AlertDialog.Builder Popup = new AlertDialog.Builder(context);
            Popup.setTitle("Alerte de proximité");
            Popup.setMessage("Plus de véhicule à proximité");
            Popup.setPositiveButton("OK, j'ai compris", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(context, "Danger écarté", Toast.LENGTH_SHORT).show();
                }
            });
            Popup.show();
            //Faire une sonnerie de message ou vibreur (suivant le mode par défaut)
            //lors de la sortie de l'utilisateur
            AudioManager audiomanager = (AudioManager)
                    context.getSystemService(Context.AUDIO_SERVICE);

            switch (audiomanager.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT:
                    Log.i("Mode","Silent mode");
                    break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    Log.i("Mode","Vibrate mode");
                    break;
                case AudioManager.RINGER_MODE_NORMAL:
                    Log.i("Mode","Normal mode");
                    break;
            }
        }
    }

    public void addNotification(Context context) {
        // Builds your notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "0")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Risque de proximité")
                .setContentText("Fais attention, une voiture se rapproche de toi");

        // Creates the intent needed to show the notification
        Intent notificationIntent = new Intent(context, MapLocationActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Add as notification
        /*NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());*/
    }



}