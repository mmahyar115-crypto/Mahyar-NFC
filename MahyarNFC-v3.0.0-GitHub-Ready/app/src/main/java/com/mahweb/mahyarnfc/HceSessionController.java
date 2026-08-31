package com.mahweb.mahyarnfc;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;

/** Owns foreground HCE routing while the user is actively sharing. */
public final class HceSessionController {
    private final NfcAdapter adapter;
    private final CardEmulation cardEmulation;
    private final ComponentName service;

    public HceSessionController(Context context) {
        adapter = NfcAdapter.getDefaultAdapter(context);
        CardEmulation ce = null;
        if (adapter != null) {
            try {
                ce = CardEmulation.getInstance(adapter);
            } catch (RuntimeException ignored) {
            }
        }
        cardEmulation = ce;
        service = new ComponentName(context, NfcCardService.class);
    }

    public boolean activate(Activity activity) {
        if (adapter == null || !adapter.isEnabled() || cardEmulation == null) return false;
        try {
            boolean preferred = cardEmulation.setPreferredService(activity, service);
            if (preferred && Build.VERSION.SDK_INT >= 35) {
                try {
                    if (adapter.isObserveModeSupported()) {
                        adapter.setObserveModeEnabled(false);
                    }
                } catch (RuntimeException ignored) {
                }
            }
            return preferred;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void deactivate(Activity activity) {
        if (cardEmulation == null) return;
        try {
            cardEmulation.unsetPreferredService(activity);
        } catch (RuntimeException ignored) {
        }
    }
}
