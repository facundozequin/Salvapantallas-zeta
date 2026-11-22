package com.facu.zetawall;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Vista con la animación. La usan el salvapantallas y la vista previa de la app. */
public class ZetaView extends View {

    private final ZetaRenderer renderer;
    private final boolean interactive;
    private final long start = System.nanoTime();
    private long last = System.nanoTime();

    private final Paint clockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SimpleDateFormat hourFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "AR"));
    private String battery = "";
    private long lastBattery;

    public ZetaView(Context ctx, boolean saver, boolean interactive) {
        super(ctx);
        this.interactive = interactive;
        renderer = new ZetaRenderer(ctx);
        renderer.setSaver(saver);
        clockPaint.setColor(0xFFEEEDFE);
        clockPaint.setTextAlign(Paint.Align.CENTER);
        clockPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        datePaint.setColor(0xFFAFA9EC);
        datePaint.setTextAlign(Paint.Align.CENTER);
        setKeepScreenOn(saver && !interactive);
    }

    public void setSaver(boolean saver) {
        renderer.setSaver(saver);
    }

    public boolean isSaver() {
        return renderer.isSaver();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        renderer.setSize(w, h);
        clockPaint.setTextSize(w * 0.17f);
        datePaint.setTextSize(w * 0.042f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - last) / 1e9f);
        last = now;
        renderer.draw(canvas, (now - start) / 1e9f, dt);

        if (renderer.isSaver()) {
            float w = getWidth(), h = getHeight();
            float x = w / 2f + renderer.getDriftX();
            float y = h * 0.72f + renderer.getDriftY();
            Date d = new Date();
            canvas.drawText(hourFmt.format(d), x, y, clockPaint);
            canvas.drawText(dateFmt.format(d), x, y + w * 0.08f, datePaint);
            updateBattery();
            canvas.drawText(battery, x, y + w * 0.16f, datePaint);
        }
        postInvalidateOnAnimation();
    }

    private void updateBattery() {
        long now = System.currentTimeMillis();
        if (now - lastBattery < 30_000 && !battery.isEmpty()) return;
        lastBattery = now;
        Intent i = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int pct = level >= 0 ? Math.round(level * 100f / scale) : 0;
        battery = (plugged != 0 ? "⚡ Cargando · " : "Batería · ") + pct + "%";
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!interactive) return false;
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                renderer.burst(x, y);
                renderer.touch(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                renderer.touch(x, y);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                renderer.release();
                performClick();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
