package com.facu.agwall;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/** Fondo de pantalla animado. */
public class AgWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new AgEngine();
    }

    private class AgEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable frame = this::drawFrame;
        private AgRenderer renderer;
        private boolean visible;
        private final long start = System.nanoTime();
        private long last = System.nanoTime();
        private long boostUntil;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setTouchEventsEnabled(true);
            renderer = new AgRenderer(AgWallpaperService.this);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            renderer.setSize(width, height);
            drawFrame();
        }

        @Override
        public void onVisibilityChanged(boolean v) {
            visible = v;
            handler.removeCallbacks(frame);
            if (v) {
                last = System.nanoTime();
                drawFrame();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            visible = false;
            handler.removeCallbacks(frame);
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onDestroy() {
            handler.removeCallbacks(frame);
            super.onDestroy();
        }

        @Override
        public void onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    renderer.burst(x, y);
                    renderer.touch(x, y);
                    boostUntil = System.nanoTime() + 3_000_000_000L;
                    break;
                case MotionEvent.ACTION_MOVE:
                    renderer.touch(x, y);
                    boostUntil = System.nanoTime() + 3_000_000_000L;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    renderer.release();
                    break;
                default:
                    break;
            }
            super.onTouchEvent(e);
        }

        private void drawFrame() {
            handler.removeCallbacks(frame);
            long now = System.nanoTime();
            float dt = Math.min(0.05f, (now - last) / 1e9f);
            last = now;
            if (renderer.isReady()) {
                SurfaceHolder holder = getSurfaceHolder();
                Canvas c = null;
                try {
                    c = holder.lockHardwareCanvas();
                    if (c != null) renderer.draw(c, (now - start) / 1e9f, dt);
                } catch (IllegalStateException | IllegalArgumentException ignored) {
                    // la superficie todavía no está lista
                } finally {
                    if (c != null) {
                        try {
                            holder.unlockCanvasAndPost(c);
                        } catch (IllegalStateException ignored) {
                        }
                    }
                }
            }
            // 30 fps en reposo para ahorrar batería, 60 fps cuando lo tocás
            if (visible) handler.postDelayed(frame, now < boostUntil ? 16 : 33);
        }
    }
}
