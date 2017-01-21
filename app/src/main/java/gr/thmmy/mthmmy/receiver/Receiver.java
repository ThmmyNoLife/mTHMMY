package gr.thmmy.mthmmy.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.webkit.MimeTypeMap;

import java.io.File;

import gr.thmmy.mthmmy.R;
import mthmmy.utils.Report;

import static gr.thmmy.mthmmy.services.DownloadService.ACTION_DOWNLOAD;
import static gr.thmmy.mthmmy.services.DownloadService.COMPLETED;
import static gr.thmmy.mthmmy.services.DownloadService.EXTRA_DOWNLOAD_ID;
import static gr.thmmy.mthmmy.services.DownloadService.EXTRA_DOWNLOAD_STATE;
import static gr.thmmy.mthmmy.services.DownloadService.EXTRA_FILE_EXTENSION;
import static gr.thmmy.mthmmy.services.DownloadService.EXTRA_FILE_NAME;
import static gr.thmmy.mthmmy.services.DownloadService.EXTRA_NOTIFICATION_TEXT;
import static gr.thmmy.mthmmy.services.DownloadService.EXTRA_NOTIFICATION_TICKER;
import static gr.thmmy.mthmmy.services.DownloadService.EXTRA_NOTIFICATION_TITLE;
import static gr.thmmy.mthmmy.services.DownloadService.SAVE_DIR;
import static gr.thmmy.mthmmy.services.DownloadService.STARTED;

public class Receiver extends BroadcastReceiver {
    private static final String TAG = "BroadcastReceiver";

    public Receiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (intent.getAction().equals(ACTION_DOWNLOAD)) {
            Bundle extras = intent.getExtras();
            int id = extras.getInt(EXTRA_DOWNLOAD_ID);
            String state = extras.getString(EXTRA_DOWNLOAD_STATE, "NONE");
            String title = extras.getString(EXTRA_NOTIFICATION_TITLE);
            String text = extras.getString(EXTRA_NOTIFICATION_TEXT);
            String ticker = extras.getString(EXTRA_NOTIFICATION_TICKER);

            builder.setContentTitle(title)
                    .setContentText(text)
                    .setTicker(ticker)
                    .setAutoCancel(true)    //???
                    .setSmallIcon(R.mipmap.ic_launcher);

            if (state.equals(STARTED))
                builder.setOngoing(true);
            else if (state.equals(COMPLETED)) {
                String fileName = extras.getString(EXTRA_FILE_NAME, "NONE");
                String extension = extras.getString(EXTRA_FILE_EXTENSION, "extension");

                File file = new File(SAVE_DIR, fileName);
                if (file.exists()) {
//                    String type = "application/" + extension;
                    String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

                    Uri pathUri = Uri.fromFile(file);
                    Intent chooserIntent = new Intent(Intent.ACTION_VIEW);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Intent chooser = Intent.createChooser(chooserIntent, "Open With...");
                    intent.setDataAndType(pathUri, type);

                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, chooser, PendingIntent.FLAG_CANCEL_CURRENT);
                    builder.setContentIntent(pendingIntent);
                }
                else
                    Report.w(TAG,"File doesn't exist.");


            }
            Notification notification = builder.build();
            notificationManager.notify(id, notification);
        }
    }

}
