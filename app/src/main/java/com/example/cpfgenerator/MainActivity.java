package com.example.cpfgenerator;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Gerador de CPFs matematicamente válidos para testes de interface/layout.
 * A validade dos dígitos verificadores não confirma existência ou cadastro real.
 *
 * Economia de bateria: fundo preto puro (OLED), nenhuma tarefa em segundo plano,
 * notificação só é atualizada quando o CPF ou a máscara mudam.
 */
public class MainActivity extends Activity {
    private static final int BG = Color.BLACK;
    private static final int SURFACE = Color.rgb(18, 18, 20);
    private static final int SURFACE_2 = Color.rgb(30, 30, 34);
    private static final int OUTLINE = Color.rgb(44, 44, 50);
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(150, 150, 158);
    private static final int ACCENT = Color.rgb(52, 211, 153);
    private static final int ON_ACCENT = Color.rgb(4, 30, 20);
    private static final int RIPPLE = 0x33FFFFFF;

    private static final int[] QUANTITIES = {10, 50, 100, 500};
    private static final String[] REGIONS = {
            "Qualquer", "0 · RS", "1 · DF GO MS MT TO", "2 · AC AM AP PA RO RR", "3 · CE MA PI",
            "4 · AL PB PE RN", "5 · BA SE", "6 · MG", "7 · ES RJ", "8 · SP", "9 · PR SC"};

    private final List<String> samples = new ArrayList<>(); // sempre só dígitos (11)
    private int quantity = 100;
    private int region = -1; // -1 = aleatório
    private boolean formatted = true;
    private String current; // CPF da notificação / destaque

    private SharedPreferences prefs;
    private TextView heroCpf, copyBtn, countText;
    private ListView listView;
    private SamplesAdapter adapter;
    private final List<TextView> qtyViews = new ArrayList<>();
    private final List<TextView> regionViews = new ArrayList<>();
    private final Runnable resetCopyLabel = () -> copyBtn.setText("Copiar");

    // Mantém o destaque em sincronia quando o CPF muda pela notificação (sem polling).
    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (sp, key) -> {
        if (NotificationHelper.KEY_CURRENT.equals(key)) {
            String raw = sp.getString(key, null);
            if (raw != null && !raw.equals(current)) { current = raw; updateHero(); }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = NotificationHelper.prefs(this);
        region = prefs.getInt(NotificationHelper.KEY_REGION, -1);
        formatted = prefs.getBoolean(NotificationHelper.KEY_FORMATTED, true);
        if (savedInstanceState != null) {
            quantity = savedInstanceState.getInt("qty", 100);
            List<String> saved = savedInstanceState.getStringArrayList("samples");
            if (saved != null) samples.addAll(saved);
        }
        current = NotificationHelper.current(this);
        requestNotificationPermission();
        buildUi();
        if (samples.isEmpty()) generate(false); else refreshList();
        updateHero();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String raw = NotificationHelper.current(this);
        if (!raw.equals(current)) { current = raw; updateHero(); }
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        // Repõe a notificação caso o usuário a tenha dispensado.
        NotificationHelper.show(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("qty", quantity);
        out.putStringArrayList("samples", new ArrayList<>(samples));
    }

    // ---------- Geração ----------

    private void generate(boolean feedback) {
        samples.clear();
        HashSet<String> seen = new HashSet<>();
        while (samples.size() < quantity) {
            String s = CpfFactory.create(region);
            if (seen.add(s)) samples.add(s);
        }
        refreshList();
        listView.setSelection(0);
        if (feedback) toast(quantity + " CPFs gerados");
    }

    private void setCurrent(String raw) {
        current = raw;
        prefs.edit().putString(NotificationHelper.KEY_CURRENT, raw).apply();
        updateHero();
        NotificationHelper.show(this);
    }

    private void nextCurrent() {
        current = NotificationHelper.next(this, current);
        updateHero();
        NotificationHelper.show(this);
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
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            NotificationHelper.show(this);
        }
    }

    // ---------- Ações ----------

    private void copy(String label, String text, String message) {
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText(label, text));
        // Android 13+ já mostra a confirmação do sistema ao copiar.
        if (Build.VERSION.SDK_INT < 33) toast(message);
    }

    private void copyCurrent(View v) {
        haptic(v);
        copy("CPF válido de teste", format(current), "Copiado: " + format(current));
        copyBtn.removeCallbacks(resetCopyLabel);
        copyBtn.setText("Copiado ✓");
        copyBtn.postDelayed(resetCopyLabel, 1200);
    }

    private void share() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, allAsText());
        startActivity(Intent.createChooser(i, "Compartilhar lista"));
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }

    private static void haptic(View v) { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); }

    // ---------- Helpers de UI ----------

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private GradientDrawable shape(int fill, int radius, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    private Drawable ripple(int fill, int radius, int stroke) {
        return new RippleDrawable(ColorStateList.valueOf(RIPPLE),
                shape(fill, radius, stroke), shape(WHITE, radius, 0));
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    private TextView label(String value) {
        TextView t = text(value.toUpperCase(Locale.ROOT), 12, MUTED, true);
        t.setLetterSpacing(0.08f);
        return t;
    }

    /** Botão em pílula: estilo 0 = destaque, 1 = branco, 2 = contorno. */
    private TextView pill(String value, int style, View.OnClickListener click) {
        TextView t = text(value, 15, style == 0 ? ON_ACCENT : style == 1 ? BG : WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(48));
        t.setPadding(dp(12), 0, dp(12), 0);
        t.setSingleLine(true);
        t.setBackground(style == 0 ? ripple(ACCENT, 24, 0)
                : style == 1 ? ripple(WHITE, 24, 0)
                : ripple(BG, 24, OUTLINE));
        t.setOnClickListener(click);
        return t;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, weight);
        p.setMargins(l, t, r, b);
        return p;
    }

    // ---------- Montagem da tela ----------

    private void buildUi() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(0, dp(8), 0, dp(4));

        header.addView(text("Gerador de CPF", 28, WHITE, true));
        header.addView(text("CPFs válidos para testes · funciona offline", 14, MUTED, false),
                lp(-1, -2, 0, 0, dp(2), 0, dp(18)));

        // Cartão de destaque: o mesmo CPF da notificação.
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(18), dp(20), dp(20));
        hero.setBackground(ripple(SURFACE, 24, OUTLINE));
        hero.setOnClickListener(this::copyCurrent);
        TextView heroLabel = label("CPF atual");
        heroLabel.setTextColor(ACCENT);
        hero.addView(heroLabel);
        heroCpf = text("", 28, WHITE, false);
        heroCpf.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        heroCpf.setSingleLine(true);
        hero.addView(heroCpf, lp(-1, -2, 0, 0, dp(10), 0, dp(18)));
        LinearLayout heroActions = row();
        copyBtn = pill("Copiar", 0, this::copyCurrent);
        heroActions.addView(copyBtn, lp(0, -2, 1f, 0, 0, dp(6), 0));
        heroActions.addView(pill("Gerar outro", 2, v -> { haptic(v); nextCurrent(); }),
                lp(0, -2, 1f, dp(6), 0, 0, 0));
        hero.addView(heroActions);
        header.addView(hero);
        header.addView(text("Na notificação: toque para copiar e já gerar o próximo.", 13, MUTED, false),
                lp(-1, -2, 0, dp(4), dp(10), dp(4), dp(24)));

        // Origem (9º dígito)
        header.addView(label("Origem · 9º dígito"), lp(-1, -2, 0, dp(4), 0, 0, dp(10)));
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout chips = row();
        for (int i = 0; i < REGIONS.length; i++) {
            final int r = i - 1;
            TextView chip = text(REGIONS[i], 14, WHITE, true);
            chip.setGravity(Gravity.CENTER);
            chip.setMinHeight(dp(40));
            chip.setPadding(dp(16), 0, dp(16), 0);
            chip.setTag(r);
            chip.setOnClickListener(v -> {
                if (r == region) return;
                haptic(v);
                region = r;
                prefs.edit().putInt(NotificationHelper.KEY_REGION, region).apply();
                updateChips();
                generate(false);
            });
            regionViews.add(chip);
            chips.addView(chip, lp(-2, -2, 0, 0, 0, dp(8), 0));
        }
        scroll.addView(chips);
        header.addView(scroll);
        updateChips();

        // Máscara
        LinearLayout maskRow = row();
        maskRow.setPadding(dp(18), dp(6), dp(12), dp(6));
        maskRow.setBackground(shape(SURFACE, 18, 0));
        LinearLayout maskText = new LinearLayout(this);
        maskText.setOrientation(LinearLayout.VERTICAL);
        maskText.addView(text("Usar máscara", 16, WHITE, true));
        maskText.addView(text("000.000.000-00", 13, MUTED, false));
        maskRow.addView(maskText, lp(0, -2, 1f, 0, 0, 0, 0));
        Switch fmt = new Switch(this);
        fmt.setChecked(formatted);
        fmt.setOnCheckedChangeListener((b, on) -> {
            formatted = on;
            prefs.edit().putBoolean(NotificationHelper.KEY_FORMATTED, on).apply();
            updateHero();
            refreshList();
            NotificationHelper.show(this);
        });
        maskRow.addView(fmt);
        maskRow.setOnClickListener(v -> fmt.toggle());
        header.addView(maskRow, lp(-1, dp(64), 0, 0, dp(14), 0, dp(24)));

        // Lista: quantidade em controle segmentado
        LinearLayout listHead = row();
        listHead.addView(label("Lista"), lp(0, -2, 1f, dp(4), 0, 0, 0));
        countText = text("", 13, MUTED, false);
        listHead.addView(countText);
        header.addView(listHead, lp(-1, -2, 0, 0, 0, dp(4), dp(10)));

        LinearLayout segmented = row();
        segmented.setPadding(dp(4), dp(4), dp(4), dp(4));
        segmented.setBackground(shape(SURFACE, 16, 0));
        for (int q : QUANTITIES) {
            TextView seg = text(String.valueOf(q), 15, MUTED, true);
            seg.setGravity(Gravity.CENTER);
            seg.setTag(q);
            seg.setOnClickListener(v -> {
                if (q == quantity) return;
                haptic(v);
                quantity = q;
                generate(false);
            });
            qtyViews.add(seg);
            segmented.addView(seg, lp(0, dp(40), 1f, 0, 0, 0, 0));
        }
        header.addView(segmented);

        LinearLayout actions = row();
        actions.addView(pill("Gerar lista", 1, v -> { haptic(v); generate(true); }),
                lp(0, -2, 1.2f, 0, 0, dp(4), 0));
        actions.addView(pill("Copiar tudo", 2, v -> {
            haptic(v);
            copy("Lista de CPFs válidos", allAsText(), samples.size() + " CPFs copiados");
        }), lp(0, -2, 1f, dp(4), 0, dp(4), 0));
        actions.addView(pill("Enviar", 2, v -> share()), lp(0, -2, 0.8f, dp(4), 0, 0, 0));
        header.addView(actions, lp(-1, -2, 0, 0, dp(12), 0, dp(6)));

        TextView footer = text("Os dígitos verificadores são válidos, mas isso não confirma cadastro real. " +
                "Use apenas para testes.", 12, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(12), dp(16), dp(12), dp(8));

        // Tudo rola junto: cabeçalho + itens numa única ListView (reciclagem de views).
        listView = new ListView(this);
        listView.setBackgroundColor(BG);
        listView.setDivider(null);
        listView.setSelector(android.R.color.transparent);
        listView.setClipToPadding(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.addHeaderView(header, null, false);
        listView.addFooterView(footer, null, false);
        adapter = new SamplesAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((p, v, pos, id) -> {
            int i = pos - listView.getHeaderViewsCount();
            if (i < 0 || i >= samples.size()) return;
            haptic(v);
            String raw = samples.get(i);
            copy("CPF válido de teste", format(raw), "Copiado: " + format(raw));
            setCurrent(raw);
        });

        // Respeita barras do sistema (edge-to-edge no Android 15+).
        int pad = dp(16);
        listView.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(pad + insets.getSystemWindowInsetLeft(),
                    pad + insets.getSystemWindowInsetTop(),
                    pad + insets.getSystemWindowInsetRight(),
                    pad + insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(listView);
    }

    private void updateHero() {
        if (heroCpf != null && current != null) heroCpf.setText(format(current));
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void updateChips() {
        for (TextView c : regionViews) {
            boolean sel = ((Integer) c.getTag()) == region;
            c.setBackground(sel ? ripple(ACCENT, 20, 0) : ripple(SURFACE, 20, OUTLINE));
            c.setTextColor(sel ? ON_ACCENT : WHITE);
        }
    }

    private void updateQty() {
        for (TextView s : qtyViews) {
            boolean sel = ((Integer) s.getTag()) == quantity;
            s.setBackground(sel ? shape(WHITE, 12, 0) : ripple(SURFACE, 12, 0));
            s.setTextColor(sel ? BG : MUTED);
        }
    }

    private void refreshList() {
        countText.setText(String.format(Locale.getDefault(), "%d CPFs", samples.size()));
        updateQty();
        adapter.notifyDataSetChanged();
    }

    private class SamplesAdapter extends BaseAdapter {
        @Override public int getCount() { return samples.size(); }
        @Override public Object getItem(int i) { return samples.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public View getView(int i, View convert, ViewGroup parent) {
            LinearLayout item = (LinearLayout) convert;
            if (item == null) {
                item = row();
                item.setLayoutParams(new AbsListView.LayoutParams(-1, -2));
                item.setMinimumHeight(dp(56));
                TextView index = text("", 13, MUTED, false);
                index.setTypeface(Typeface.MONOSPACE);
                item.addView(index, lp(dp(44), -2, 0, 0, 0, 0, 0));
                TextView cpf = text("", 18, WHITE, false);
                cpf.setTypeface(Typeface.MONOSPACE);
                cpf.setSingleLine(true);
                item.addView(cpf, lp(0, -2, 1f, 0, 0, 0, 0));
                TextView hint = text("copiar", 13, MUTED, true);
                item.addView(hint);
            }
            String raw = samples.get(i);
            boolean isCurrent = raw.equals(current);
            // Espaço entre itens via margem no fundo (ListView não aceita margens).
            item.setBackground(new android.graphics.drawable.InsetDrawable(
                    ripple(isCurrent ? SURFACE_2 : SURFACE, 16, isCurrent ? ACCENT : 0),
                    0, dp(3), 0, dp(3)));
            item.setPadding(dp(16), dp(4), dp(16), dp(4)); // setBackground substitui o padding
            ((TextView) item.getChildAt(0)).setText(String.format(Locale.ROOT, "%03d", i + 1));
            ((TextView) item.getChildAt(1)).setText(format(raw));
            TextView hint = (TextView) item.getChildAt(2);
            hint.setText(isCurrent ? "atual" : "copiar");
            hint.setTextColor(isCurrent ? ACCENT : MUTED);
            return item;
        }
    }
}
