package com.example.cpfgenerator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;

final class NotificationHelper {
    static final String CHANNEL_ID = "cpf_fake";
    static final String ACTION_COPY = "com.example.cpfgenerator.COPY";
    static final String ACTION_NEW = "com.example.cpfgenerator.NEW";
    static final String EXTRA_TEXT = "text";
    private static final int NOTIFICATION_ID = 1;

    private NotificationHelper() {}

    private static PendingIntent receiverIntent(Context c, String action, int req, String text) {
        Intent i = new Intent(c, NotificationReceiver.class).setAction(action);
        if (text != null) i.putExtra(EXTRA_TEXT, text);
        return PendingIntent.getBroadcast(c, req, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Mostra/atualiza a notificação com o número (já formatado) e os botões. */
    static void show(Context c, String rawCpf, boolean formatted) {
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                        "CPF válido", NotificationManager.IMPORTANCE_LOW);
                ch.setDescription("Mostra um CPF válido com botão de copiar");
                nm.createNotificationChannel(ch);
            }
            String text = CpfFactory.format(rawCpf, formatted);

            Intent open = new Intent(c, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent openPi = PendingIntent.getActivity(c, 0, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(c, CHANNEL_ID)
                    : new Notification.Builder(c);
            b.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(text)
                    .setContentText("CPF válido de teste")
                    .setContentIntent(openPi)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(c, R.drawable.ic_notification), "Copiar",
                            receiverIntent(c, ACTION_COPY, 1, text)).build())
                    .addAction(new Notification.Action.Builder(
                            Icon.createWithResource(c, R.drawable.ic_notification), "Novo",
                            receiverIntent(c, ACTION_NEW, 2, null)).build());
            if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_LOW);
            nm.notify(NOTIFICATION_ID, b.build());
        } catch (SecurityException ignored) {
            // Sem permissão de notificação: simplesmente não mostra.
        }
    }
}
