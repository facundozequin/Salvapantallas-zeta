package com.facu.agwall;

import android.service.dreams.DreamService;

/** Salvapantallas: se muestra mientras el teléfono carga (si está activado en Ajustes). */
public class AgDreamService extends DreamService {

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setContentView(new AgView(this, true, false));
    }
}
