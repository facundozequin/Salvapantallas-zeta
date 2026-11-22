package com.facu.zetawall;

import android.service.dreams.DreamService;

/** Salvapantallas: se muestra mientras el teléfono carga (si está activado en Ajustes). */
public class ZetaDreamService extends DreamService {

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setContentView(new ZetaView(this, true, false));
    }
}
