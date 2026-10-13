package com.example.cpfgenerator;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gerador de CPFs matematicamente válidos para testes de interface/layout.
 * A validade dos dígitos verificadores não confirma existência ou cadastro real.
 */
public class MainActivity extends Activity {
    private static final int BLACK = Color.BLACK;
    private static final int PANEL = Color.rgb(24, 24, 24);
    private static final int WHITE = Color.WHITE;
    private static final int GRAY = Color.rgb(175, 175, 175);
    private static final int[] QUANTITIES = {10, 50, 100, 500};

    private final List<String> samples = new ArrayList<>(); // sempre só dígitos (11)
    private int quantity = 100;
    private int region = -1; // -1 = aleatório
    private boolean formatted = true;
    private String shown; // número exibido na notificação

    private TextView countText;
    private ListView listView;
    private SamplesAdapter adapter;
    private final List<Button> qtyButtons = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            quantity = savedInstanceState.getInt("qty", 100);
            region = savedInstanceState.getInt("region", -1);
            formatted = savedInstanceState.getBoolean("fmt", true);
            List<String> saved = savedInstanceState.getStringArrayList("samples");
            if (saved != null) samples.addAll(saved);
        }
        if (savedInstanceState == null) {
            SharedPreferences sp = getSharedPreferences("settings", MODE_PRIVATE);
            region = sp.getInt("region", -1);
            formatted = sp.getBoolean("formatted", true);
        }
        requestNotificationPermission();
        buildUi();
        if (samples.isEmpty()) generate(false); else refresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("qty", quantity);
        out.putInt("region", region);
        out.putBoolean("fmt", formatted);
        out.putStringArrayList("samples", new ArrayList<>(samples));
    }

    // ---------- Geração ----------

    private void generate(boolean toast) {
        samples.clear();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        while (samples.size() < quantity) {
            String s = CpfFactory.create(region);
            if (seen.add(s)) samples.add(s);
        }
        shown = samples.get(0);
        refresh();
        listView.setSelection(0);
        if (toast) toast(quantity + " CPFs válidos gerados");
    }

    private String format(String raw) { return CpfFactory.format(raw, formatted); }

    private String allAsText() {
        StringBuilder sb = new StringBuilder();
        for (String s : samples) sb.append(format(s)).append('\n');
        return sb.toString().trim();
    }

    // ---------- Notificação ----------

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED && shown != null) {
            NotificationHelper.show(this, shown, formatted);
        }
    }

    // ---------- Ações ----------

    private void copy(String label, String text) {
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText(label, text));
        toast("Copiado");
    }

    private void share() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, allAsText());
        startActivity(Intent.createChooser(i, "Compartilhar lista"));
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }

    // ---------- UI ----------

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String label, int bg, int fg, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(fg);
        b.setAllCaps(false);
        b.setBackgroundTintList(ColorStateList.valueOf(bg));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, weight);
        p.setMargins(l, t, r, b);
        return p;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BLACK);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        // Respeita barras do sistema (edge-to-edge no Android 15+).
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(pad + insets.getSystemWindowInsetLeft(),
                    pad + insets.getSystemWindowInsetTop(),
                    pad + insets.getSystemWindowInsetRight(),
                    pad + insets.getSystemWindowInsetBottom());
            return insets;
        });

        root.addView(text("CPF Generator", 26, WHITE, true));
        root.addView(text("CPFs válidos para testes", 14, GRAY, false),
                lp(-1, -2, 0, 0, dp(2), 0, 0));

        countText = text("", 16, WHITE, true);
        root.addView(countText, lp(-1, -2, 0, 0, dp(14), 0, dp(4)));

        // Quantidade
        LinearLayout qtyRow = new LinearLayout(this);
        qtyRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int q : QUANTITIES) {
            Button b = button(String.valueOf(q), Color.rgb(55, 55, 55), WHITE, () -> {
                quantity = q;
                updateQtyButtons();
                generate(false);
            });
            b.setTag(q);
            qtyButtons.add(b);
            qtyRow.addView(b, lp(0, dp(44), 1f, dp(2), 0, dp(2), 0));
        }
        root.addView(qtyRow);
        updateQtyButtons();

        // Região (9º dígito) + formatação
        LinearLayout opts = new LinearLayout(this);
        opts.setOrientation(LinearLayout.HORIZONTAL);
        opts.setGravity(Gravity.CENTER_VERTICAL);
        TextView regionLabel = text("9º dígito:", 14, GRAY, false);
        opts.addView(regionLabel);
        Spinner sp = new Spinner(this);
        String[] items = {"Aleatório", "0", "1", "2", "3", "4", "5", "6", "7", "8 (SP)", "9"};
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
        sp.setSelection(region + 1);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                int newRegion = pos - 1;
                if (first) { first = false; return; }
                if (newRegion != region) { region = newRegion; generate(false); }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        opts.addView(sp, lp(0, -2, 1f, dp(8), 0, dp(8), 0));
        Switch fmt = new Switch(this);
        fmt.setText("Máscara ");
        fmt.setTextColor(GRAY);
        fmt.setChecked(formatted);
        fmt.setOnCheckedChangeListener((b, on) -> { formatted = on; refresh(); });
        opts.addView(fmt);
        root.addView(opts, lp(-1, -2, 0, 0, dp(8), 0, dp(4)));

        // Ações
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(button("Gerar novos", WHITE, BLACK, () -> generate(true)),
                lp(0, -2, 1.3f, dp(2), 0, dp(2), 0));
        actions.addView(button("Copiar tudo", Color.rgb(55, 55, 55), WHITE,
                () -> copy("Lista de CPFs válidos", allAsText())),
                lp(0, -2, 1f, dp(2), 0, dp(2), 0));
        actions.addView(button("Enviar", Color.rgb(55, 55, 55), WHITE, this::share),
                lp(0, -2, 0.8f, dp(2), 0, dp(2), 0));
        root.addView(actions);

        root.addView(text("Os dígitos verificadores são válidos, mas isso não confirma cadastro real. " +
                        "Toque em um item para copiá-lo.", 12, GRAY, false),
                lp(-1, -2, 0, 0, dp(8), 0, dp(8)));

        listView = new ListView(this);
        listView.setDivider(null);
        listView.setSelector(android.R.color.transparent);
        adapter = new SamplesAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((p, v, pos, id) -> {
            shown = samples.get(pos);
            copy("CPF válido de teste", format(shown));
            NotificationHelper.show(this, shown, formatted);
        });
        root.addView(listView, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
    }

    private void updateQtyButtons() {
        for (Button b : qtyButtons) {
            boolean sel = ((Integer) b.getTag()) == quantity;
            b.setBackgroundTintList(ColorStateList.valueOf(sel ? WHITE : Color.rgb(55, 55, 55)));
            b.setTextColor(sel ? BLACK : WHITE);
        }
    }

    private void refresh() {
        countText.setText(String.format(Locale.getDefault(), "%d registros de teste", samples.size()));
        updateQtyButtons();
        if (adapter != null) adapter.notifyDataSetChanged();
        getSharedPreferences("settings", MODE_PRIVATE).edit()
                .putInt("region", region).putBoolean("formatted", formatted).apply();
        if (shown == null && !samples.isEmpty()) shown = samples.get(0);
        if (shown != null) NotificationHelper.show(this, shown, formatted);
    }

    private class SamplesAdapter extends BaseAdapter {
        @Override public int getCount() { return samples.size(); }
        @Override public Object getItem(int i) { return samples.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public View getView(int i, View convert, ViewGroup parent) {
            TextView t = (TextView) convert;
            if (t == null) {
                t = new TextView(MainActivity.this);
                t.setTextSize(17);
                t.setTextColor(WHITE);
                t.setTypeface(Typeface.MONOSPACE);
                t.setBackgroundColor(PANEL);
                t.setGravity(Gravity.CENTER_VERTICAL);
                t.setMinHeight(dp(48));
                t.setPadding(dp(14), dp(6), dp(14), dp(6));
                AbsListViewLayout.apply(t, dp(6));
            }
            t.setText(String.format(Locale.getDefault(), "%03d   %s", i + 1, format(samples.get(i))));
            return t;
        }
    }

    /** Helper para margem inferior entre itens da ListView. */
    private static final class AbsListViewLayout {
        static void apply(TextView t, int gap) {
            t.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, -2));
            t.setBackground(new android.graphics.drawable.InsetDrawable(
                    new android.graphics.drawable.ColorDrawable(PANEL), 0, 0, 0, gap));
        }
    }
}
