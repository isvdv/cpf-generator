package com.example.cpfgenerator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;

/**
 * Notificação fixa com o CPF atual. Tocar nela copia o número e já troca por outro.
 * Nada roda em segundo plano: só reage a toques (sem serviço, alarme ou wakelock).
 */
final class NotificationHelper {
    static final String CHANNEL_ID = "cpf_fake";
    static final String ACTION_COPY_NEXT = "com.example.cpfgenerator.COPY_NEXT";
    static final String ACTION_SKIP = "com.example.cpfgenerator.SKIP";

    static final String PREFS = "settings";
    static final String KEY_REGION = "region";
    static final String KEY_FORMATTED = "formatted";
    static final String KEY_CURRENT = "current";

    private static final int NOTIFICATION_ID = 1;
    private static boolean channelReady;

    private NotificationHelper() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** CPF atual (só dígitos); cria e salva um se ainda não existir. */
    static String current(Context c) {
        SharedPreferences sp = prefs(c);
        String raw = sp.getString(KEY_CURRENT, null);
        if (raw == null || raw.length() != 11) raw = next(c, null);
        return raw;
    }

    /** Gera um novo CPF diferente de {@code previous}, salva como atual e retorna. */
    static String next(Context c, String previous) {
        SharedPreferences sp = prefs(c);
        int region = sp.getInt(KEY_REGION, -1);
        String raw;
        do raw = CpfFactory.create(region); while (raw.equals(previous));
        sp.edit().putString(KEY_CURRENT, raw).apply();
        return raw;
    }

    private static PendingIntent receiverIntent(Context c, String action, int req) {
        Intent i = new Intent(c, NotificationReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(c, req, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void show(Context c) { show(c, null); }

    /**
     * Mostra/atualiza a notificação com o CPF atual.
     * @param justCopied número recém-copiado (formatado) para dar retorno visual, ou null.
     */
    static void show(Context c, String justCopied) {
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= 26 && !channelReady) {
                NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                        "CPF válido", NotificationManager.IMPORTANCE_LOW);
                ch.setDescription("Toque para copiar o CPF e gerar outro");
                ch.setShowBadge(false);
                ch.enableVibration(false);
                ch.enableLights(false);
                ch.setSound(null, null);
                nm.createNotificationChannel(ch);
                channelReady = true;
            }
            boolean formatted = prefs(c).getBoolean(KEY_FORMATTED, true);
            String text = CpfFactory.format(current(c), formatted);
            String hint = justCopied != null
                    ? "✓ Copiado " + justCopied + " · toque para o próximo"
                    : "Toque para copiar e gerar outro";

            Intent open = new Intent(c, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent openPi = PendingIntent.getActivity(c, 0, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Icon icon = Icon.createWithResource(c, R.drawable.ic_notification);

            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(c, CHANNEL_ID)
                    : new Notification.Builder(c);
            b.setSmallIcon(R.drawable.ic_notification)
                    .setColor(0xFF34D399)
                    .setContentTitle(text)
                    .setContentText(hint)
                    // Toque na notificação = copiar + gerar outro (sem abrir o app).
                    .setContentIntent(receiverIntent(c, ACTION_COPY_NEXT, 1))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .setLocalOnly(true)
                    .setCategory(Notification.CATEGORY_STATUS)
                    .addAction(new Notification.Action.Builder(icon, "Pular",
                            receiverIntent(c, ACTION_SKIP, 2)).build())
                    .addAction(new Notification.Action.Builder(icon, "Abrir app", openPi).build());
            if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_LOW);
            nm.notify(NOTIFICATION_ID, b.build());
        } catch (SecurityException ignored) {
            // Sem permissão de notificação: simplesmente não mostra.
        }
    }
}
