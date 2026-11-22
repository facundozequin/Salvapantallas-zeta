package com.facu.zetawall;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Pantalla principal: vista previa + botones para activar fondo y salvapantallas. */
public class MainActivity extends Activity {

    private static final int BG = 0xFF0E0612;
    private static final int PURPLE = 0xFF534AB7;
    private static final int LILAC = 0xFFCECBF6;

    private ZetaView preview;
    private Button modeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(pad, pad, pad, pad);

        preview = new ZetaView(this, false, true);
        root.addView(preview, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView hint = new TextView(this);
        hint.setText("Tocá o arrastrá sobre la animación");
        hint.setTextColor(0xFF888780);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(8), 0, dp(8));
        root.addView(hint);

        modeBtn = makeButton("Ver salvapantallas", false);
        modeBtn.setOnClickListener(v -> {
            boolean s = !preview.isSaver();
            preview.setSaver(s);
            modeBtn.setText(s ? "Ver fondo animado" : "Ver salvapantallas");
        });
        root.addView(modeBtn, btnParams());

        Button wallBtn = makeButton("Poner como fondo de pantalla", true);
        wallBtn.setOnClickListener(v -> setWallpaper());
        root.addView(wallBtn, btnParams());

        Button dreamBtn = makeButton("Configurar salvapantallas", false);
        dreamBtn.setOnClickListener(v -> openDreamSettings());
        root.addView(dreamBtn, btnParams());

        setContentView(root);
    }

    private void setWallpaper() {
        Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(this, ZetaWallpaperService.class));
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(this, "Elegilo desde Ajustes > Fondo de pantalla", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openDreamSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
            Toast.makeText(this, "Elegí \"Zeta\" y activá \"Mientras se carga\"", Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Tu sistema no muestra el menú de salvapantallas", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
        }
    }

    private Button makeButton(String text, boolean primary) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(primary ? Color.WHITE : LILAC);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(primary ? PURPLE : Color.TRANSPARENT);
        bg.setStroke(dp(1), PURPLE);
        b.setBackground(bg);
        return b;
    }

    private LinearLayout.LayoutParams btnParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        p.topMargin = dp(8);
        return p;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
