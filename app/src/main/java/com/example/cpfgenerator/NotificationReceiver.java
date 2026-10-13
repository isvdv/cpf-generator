package com.example.cpfgenerator;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (NotificationHelper.ACTION_COPY.equals(action)) {
            String text = intent.getStringExtra(NotificationHelper.EXTRA_TEXT);
            if (text == null) return;
            ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("CPF válido de teste", text));
            Toast.makeText(context, "Copiado: " + text, Toast.LENGTH_SHORT).show();
        } else if (NotificationHelper.ACTION_NEW.equals(action)) {
            SharedPreferences sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
            String raw = CpfFactory.create(sp.getInt("region", -1));
            NotificationHelper.show(context, raw, sp.getBoolean("formatted", true));
        }
    }
}
