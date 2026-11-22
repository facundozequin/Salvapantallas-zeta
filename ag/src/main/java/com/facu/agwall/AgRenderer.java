package com.facu.agwall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Random;

/**
 * Logo AG hecho de partículas, con los colores originales (negro y grises sobre blanco).
 * Modo "fondo": logo grande al centro con un brillo que lo recorre cada tanto.
 * Modo "salvapantallas": logo más chico arriba, flotando, con reloj debajo.
 */
public class AgRenderer {

    private static final int BG = 0xFFFFFFFF;
    private static final int GRID = 130;  // resolución horizontal del logo en partículas

    private final Bitmap logo;
    private float[] lx = new float[0], ly = new float[0];
    private int[] lc = new int[0];
    private int gw, gh;

    private float[] px, py, vx, vy, pa;
    private int n;

    private int W, H;
    private float pr;
    private boolean saver;
    private float scale = 1f;   // 1 = tamaño fondo, menor = tamaño salvapantallas
    private float offY;         // desplazamiento vertical según el modo
    private float touchX, touchY;
    private boolean touching;
    private float driftX, driftY;

    private final float[] sx = new float[45], sy = new float[45], sr = new float[45], sp = new float[45];

    private final Paint paint = new Paint();
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();

    public AgRenderer(Context ctx) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        logo = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.logo, o);
        for (int i = 0; i < sx.length; i++) {
            sx[i] = rnd.nextFloat();
            sy[i] = rnd.nextFloat();
            sr[i] = rnd.nextFloat();
            sp[i] = rnd.nextFloat() * 6f;
        }
        dotPaint.setColor(0xFF9E9E9E);
    }

    public boolean isReady() {
        return W > 0 && H > 0;
    }

    public void setSize(int w, int h) {
        if (w <= 0 || h <= 0 || (w == W && h == H)) return;
        W = w;
        H = h;

        gw = GRID;
        gh = Math.round(GRID * logo.getHeight() / (float) logo.getWidth());
        Bitmap s = Bitmap.createScaledBitmap(logo, gw, gh, true);
        int[] pix = new int[gw * gh];
        s.getPixels(pix, 0, gw, 0, 0, gw, gh);

        int count = 0;
        for (int p : pix) if (isInk(p)) count++;
        lx = new float[count];
        ly = new float[count];
        lc = new int[count];
        int k = 0;
        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                int p = pix[y * gw + x];
                if (isInk(p)) {
                    // coordenadas normalizadas: centro del logo = (0,0), ancho = 1
                    lx[k] = (x - gw / 2f) / gw;
                    ly[k] = (y - gh / 2f) / gw;
                    lc[k] = p | 0xFF000000;
                    k++;
                }
            }
        }

        float logoW = W * 0.9f;
        pr = logoW / gw * 0.47f;

        n = count;
        px = new float[n]; py = new float[n]; vx = new float[n]; vy = new float[n]; pa = new float[n];
        for (int i = 0; i < n; i++) {
            px[i] = rnd.nextFloat() * W;
            py[i] = rnd.nextFloat() * H;
            pa[i] = 0f;
        }
    }

    /** Un píxel es parte del logo si no es blanco (incluye los grises del brillo). */
    private static boolean isInk(int p) {
        return Color.red(p) + Color.green(p) + Color.blue(p) < 600;
    }

    public void setSaver(boolean s) {
        saver = s;
    }

    public boolean isSaver() {
        return saver;
    }

    public float getDriftX() {
        return driftX;
    }

    public float getDriftY() {
        return driftY;
    }

    public void burst(float x, float y) {
        if (n == 0) return;
        float cap = W * 0.07f;
        for (int i = 0; i < n; i++) {
            float dx = px[i] - x, dy = py[i] - y;
            float d = (float) Math.hypot(dx, dy) + 1f;
            float f = Math.min(W * 5.5f / d, cap);
            vx[i] += dx / d * f;
            vy[i] += dy / d * f;
        }
    }

    public void touch(float x, float y) {
        touchX = x;
        touchY = y;
        touching = true;
    }

    public void release() {
        touching = false;
    }

    public void draw(Canvas c, float t, float dt) {
        c.drawColor(BG);
        if (n == 0) return;
        float k = Math.max(0.2f, Math.min(3f, dt * 60f));

        // puntitos grises flotando de fondo
        float base = W * 0.0018f;
        for (int i = 0; i < sx.length; i++) {
            float yy = (sy[i] - t * 0.004f * (0.5f + sr[i])) % 1f;
            if (yy < 0) yy += 1f;
            float a = 0.15f + 0.15f * (float) Math.sin(t * 1.5f + sp[i]);
            dotPaint.setAlpha((int) (a * 255));
            c.drawCircle(sx[i] * W, yy * H, base * (0.5f + sr[i]), dotPaint);
        }

        // transición de tamaño y posición entre modos
        float tScale = saver ? 0.72f : 1f;
        float tOff = saver ? H * 0.34f : H * 0.5f;
        scale += (tScale - scale) * 0.04f * k;
        offY = offY == 0 ? tOff : offY + (tOff - offY) * 0.04f * k;

        // desplazamiento lento en el salvapantallas para cuidar la pantalla AMOLED
        float tdx = saver ? (float) Math.sin(t * 0.05f) * W * 0.05f : 0f;
        float tdy = saver ? (float) Math.sin(t * 0.037f) * H * 0.05f : 0f;
        driftX += (tdx - driftX) * 0.02f * k;
        driftY += (tdy - driftY) * 0.02f * k;

        float logoW = W * 0.9f * scale;
        float cx = W / 2f + driftX;
        float fl = saver ? (float) Math.sin(t * 0.6f) * W * 0.03f : (float) Math.sin(t * 1.2f) * W * 0.008f;
        float cy = offY + driftY + fl;
        float size = pr * scale;
        float jit = size * 0.7f;
        float rep = W * 0.15f, rep2 = rep * rep;
        float damp = (float) Math.pow(0.87, k);

        // brillo diagonal que recorre el logo cada 6 segundos
        float cycle = (t % 6f) / 6f;
        float sweep = -0.9f + cycle * 3.0f;

        for (int i = 0; i < n; i++) {
            pa[i] += (1f - pa[i]) * 0.05f * k;

            float gx = cx + lx[i] * logoW + (float) Math.sin(t * 1.5f + i * 0.13f) * jit;
            float gy = cy + ly[i] * logoW + (float) Math.cos(t * 1.3f + i * 0.11f) * jit;
            vx[i] += (gx - px[i]) * 0.015f * k;
            vy[i] += (gy - py[i]) * 0.015f * k;

            if (touching) {
                float dx = px[i] - touchX, dy = py[i] - touchY;
                if (dx * dx + dy * dy < rep2) {
                    vx[i] += dx * 0.05f * k;
                    vy[i] += dy * 0.05f * k;
                }
            }
            vx[i] *= damp;
            vy[i] *= damp;
            px[i] += vx[i] * k;
            py[i] += vy[i] * k;

            int col = lc[i];
            float dd = Math.abs(lx[i] + ly[i] - sweep);
            if (dd < 0.08f) {
                float m = (1f - dd / 0.08f) * 0.55f;
                int r = Color.red(col), g = Color.green(col), b = Color.blue(col);
                col = Color.rgb(r + (int) ((210 - r) * m), g + (int) ((210 - g) * m), b + (int) ((210 - b) * m));
            }
            paint.setColor(col);
            paint.setAlpha((int) (pa[i] * 255));
            c.drawRect(px[i] - size, py[i] - size, px[i] + size, py[i] + size, paint);
        }
    }
}
