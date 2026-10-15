package com.example.cpfgenerator;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

/** Recebe os toques na notificação. Trabalho curto e síncrono: não mantém o processo vivo. */
public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String raw = NotificationHelper.current(context);
        if (NotificationHelper.ACTION_COPY_NEXT.equals(action)) {
            boolean formatted = NotificationHelper.prefs(context)
                    .getBoolean(NotificationHelper.KEY_FORMATTED, true);
            String text = CpfFactory.format(raw, formatted);
            ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) cb.setPrimaryClip(ClipData.newPlainText("CPF válido de teste", text));
            // Android 13+ já mostra a confirmação do sistema ao copiar.
            if (Build.VERSION.SDK_INT < 33) {
                Toast.makeText(context, "Copiado: " + text, Toast.LENGTH_SHORT).show();
            }
            NotificationHelper.next(context, raw);
            NotificationHelper.show(context, text);
        } else if (NotificationHelper.ACTION_SKIP.equals(action)) {
            NotificationHelper.next(context, raw);
            NotificationHelper.show(context);
        }
    }
}
