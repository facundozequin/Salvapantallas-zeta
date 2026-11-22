package com.facu.zetawall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.util.Random;

/**
 * Dibuja el logo animado. Lo usan el fondo animado, el salvapantallas y la vista previa.
 * Modo "fondo": anillo girando + Z hecha de partículas.
 * Modo "salvapantallas": las partículas forman el logo "SOMOS ZETA" y flotan.
 */
public class ZetaRenderer {

    private static final int BG = 0xFF0E0612;
    private static final int Z_GRID = 74;      // resolución de la Z en partículas
    private static final int W_GRID = 112;     // resolución del logo horizontal

    private final Bitmap ring;   // anillo con texto, fondo transparente
    private final Bitmap zSrc;   // recorte de la Z
    private final Bitmap wSrc;   // logo "SOMOS ZETA"

    private float[] zx = new float[0], zy = new float[0];
    private int[] zc = new int[0];
    private float[] wx = new float[0], wy = new float[0];
    private int[] wc = new int[0];

    private float[] px, py, vx, vy, pa;
    private int n;

    private int W, H;
    private float cx, cy, ringR, pr;
    private boolean saver;
    private float ringAlpha = 1f;
    private float touchX, touchY;
    private boolean touching;
    private float driftX, driftY;

    private final float[] sx = new float[60], sy = new float[60], sr = new float[60], sp = new float[60];

    private final Paint paint = new Paint();
    private final Paint ringPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private final Random rnd = new Random();

    public ZetaRenderer(Context ctx) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap ringRaw = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ring, o);
        int s = ringRaw.getWidth();
        int zHalf = Math.round(s * 0.31f);
        zSrc = Bitmap.createBitmap(ringRaw, s / 2 - zHalf, s / 2 - zHalf, zHalf * 2, zHalf * 2);
        ring = makeTransparentRing(ringRaw);
        wSrc = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.word, o);
        for (int i = 0; i < sx.length; i++) {
            sx[i] = rnd.nextFloat();
            sy[i] = rnd.nextFloat();
            sr[i] = rnd.nextFloat();
            sp[i] = rnd.nextFloat() * 6f;
        }
        starPaint.setColor(0xFFCECBF6);
    }

    /** Quita el fondo oscuro y el centro (la Z) del anillo para que quede sólo el texto. */
    private static Bitmap makeTransparentRing(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        int[] px = new int[w * h];
        src.getPixels(px, 0, w, 0, 0, w, h);
        float c = w / 2f, hole = w * 0.5f * 0.62f;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int col = px[i];
                int r = Color.red(col), g = Color.green(col), b = Color.blue(col);
                float dx = x - c, dy = y - c;
                int a;
                if (dx * dx + dy * dy < hole * hole) {
                    a = 0;
                } else {
                    a = Math.max(0, Math.min(255, (r + g + b - 90) * 2));
                }
                px[i] = Color.argb(a, r, g, b);
            }
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }

    public boolean isReady() {
        return W > 0 && H > 0;
    }

    public void setSize(int w, int h) {
        if (w <= 0 || h <= 0 || (w == W && h == H)) return;
        W = w;
        H = h;
        cx = W / 2f;
        cy = H * 0.56f;
        ringR = W * 0.41f;

        // Z: se reduce la imagen a una grilla y cada píxel claro es una partícula
        float zSize = 2f * ringR * 0.62f;
        float zStep = zSize / Z_GRID;
        Bitmap zs = Bitmap.createScaledBitmap(zSrc, Z_GRID, Z_GRID, true);
        int[][] zr = samplePoints(zs, cx - zSize / 2f, cy - zSize / 2f, zStep);
        zx = toF(zr[0]); zy = toF(zr[1]); zc = zr[2];

        // Logo horizontal
        int gh = Math.round(W_GRID * wSrc.getHeight() / (float) wSrc.getWidth());
        float ww = W * 0.86f;
        float wStep = ww / W_GRID;
        Bitmap ws = Bitmap.createScaledBitmap(wSrc, W_GRID, gh, true);
        int[][] wr = samplePoints(ws, cx - ww / 2f, H * 0.30f - gh * wStep / 2f, wStep);
        wx = toF(wr[0]); wy = toF(wr[1]); wc = wr[2];

        pr = (zStep + wStep) * 0.5f * 0.45f;

        n = Math.max(zx.length, wx.length);
        px = new float[n]; py = new float[n]; vx = new float[n]; vy = new float[n]; pa = new float[n];
        for (int i = 0; i < n; i++) {
            px[i] = rnd.nextFloat() * W;
            py[i] = rnd.nextFloat() * H;
            pa[i] = 0f;
        }
    }

    /** Devuelve {x*1000, y*1000, color} de los píxeles claros (enteros para no crear objetos). */
    private static int[][] samplePoints(Bitmap b, float ox, float oy, float step) {
        int w = b.getWidth(), h = b.getHeight();
        int[] pix = new int[w * h];
        b.getPixels(pix, 0, w, 0, 0, w, h);
        int count = 0;
        for (int p : pix) if (Color.red(p) + Color.green(p) + Color.blue(p) > 200) count++;
        int[] xs = new int[count], ys = new int[count], cs = new int[count];
        int k = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = pix[y * w + x];
                if (Color.red(p) + Color.green(p) + Color.blue(p) > 200) {
                    xs[k] = Math.round((ox + x * step) * 1000f);
                    ys[k] = Math.round((oy + y * step) * 1000f);
                    cs[k] = p | 0xFF000000;
                    k++;
                }
            }
        }
        return new int[][]{xs, ys, cs};
    }

    private static float[] toF(int[] a) {
        float[] f = new float[a.length];
        for (int i = 0; i < a.length; i++) f[i] = a[i] / 1000f;
        return f;
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

    /** Toque: dispersa las partículas desde ese punto. */
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

        // estrellas
        float starBase = W * 0.0012f;
        for (int i = 0; i < sx.length; i++) {
            float a = 0.25f + 0.25f * (float) Math.sin(t * 2f + sp[i]);
            starPaint.setAlpha((int) (a * 255));
            c.drawCircle(sx[i] * W, sy[i] * H, starBase * (0.4f + sr[i]), starPaint);
        }

        // desplazamiento lento en el salvapantallas para cuidar la pantalla AMOLED
        float tdx = saver ? (float) Math.sin(t * 0.05f) * W * 0.05f : 0f;
        float tdy = saver ? (float) Math.sin(t * 0.037f) * H * 0.05f : 0f;
        driftX += (tdx - driftX) * 0.02f * k;
        driftY += (tdy - driftY) * 0.02f * k;

        // anillo
        ringAlpha += ((saver ? 0f : 1f) - ringAlpha) * 0.05f * k;
        if (ringAlpha > 0.01f) {
            float s = (2f * ringR) / ring.getWidth();
            matrix.reset();
            matrix.postTranslate(-ring.getWidth() / 2f, -ring.getHeight() / 2f);
            matrix.postScale(s, s);
            matrix.postRotate(t * 8.6f);
            matrix.postTranslate(cx, cy);
            ringPaint.setAlpha((int) (255 * ringAlpha));
            c.drawBitmap(ring, matrix, ringPaint);
        }

        // partículas
        float[] tx = saver ? wx : zx, ty = saver ? wy : zy;
        int[] tc = saver ? wc : zc;
        int len = tx.length;
        if (len == 0) return;
        float fl = saver ? (float) Math.sin(t * 0.6f) * W * 0.03f : (float) Math.sin(t * 1.2f) * W * 0.008f;
        float jit = pr * 0.8f;
        float rep = W * 0.15f, rep2 = rep * rep;
        float damp = (float) Math.pow(0.87, k);

        for (int i = 0; i < n; i++) {
            int j = i % len;
            float target = i < len ? 1f : 0f;
            pa[i] += (target - pa[i]) * 0.05f * k;

            float gx = tx[j] + driftX + (float) Math.sin(t * 1.5f + i * 0.13f) * jit;
            float gy = ty[j] + driftY + fl + (float) Math.cos(t * 1.3f + i * 0.11f) * jit;
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

            if (pa[i] < 0.02f) continue;
            paint.setColor(tc[j]);
            paint.setAlpha((int) (pa[i] * 255));
            c.drawRect(px[i] - pr, py[i] - pr, px[i] + pr, py[i] + pr, paint);
        }
    }
}
