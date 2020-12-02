package com.atakmap.android.data;


import android.content.Intent;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.List;

public class ClearContentRegistry {


    private static final String TAG = "ClearContentRegistry";


    interface ClearContentListener {
        /**
         * A call to clear content (ZEROIZE) has been made with the option to clear any larger map
         * data.
         * @param clearmaps True to clear map source and cache data
         */
        void onClearContent(boolean clearmaps);
    }

    private static ClearContentRegistry _instance;
    private final List<ClearContentListener> listeners = new ArrayList<>();


    private ClearContentRegistry() {
        // provide for legacy behavior of notification by intent.
        registerListener(new ClearContentListener() {
            @Override
            public void onClearContent(boolean clearmaps) {
                AtakBroadcast.getInstance().sendBroadcast(
                        new Intent(DataMgmtReceiver.ZEROIZE_CONFIRMED_ACTION).putExtra(
                                DataMgmtReceiver.ZEROIZE_CLEAR_MAPS,
                                clearmaps));
            }
        });
    }

    /**
     * Obtain the instance of the registry for the clear content call.
     * @return the registry.
     */
    public synchronized static ClearContentRegistry getInstance() {
        if (_instance == null)
            _instance = new ClearContentRegistry();
        return _instance;
    }

    /**
     * Register a clear content listener with the registry.
     * @param ccl the clear content listener
     */
    public synchronized void registerListener(ClearContentListener ccl) {
        listeners.add(ccl);
    }

    /**
     * Unregister a clear content listener with the registry.
     * @param ccl the clear content listener
     */
    public synchronized void unregisterListener(ClearContentListener ccl) {
        listeners.remove(ccl);
    }

    /**
     * Call kicked off by the ClearContentTask that is part of the CotMapComponent.
     * @param clearMaps True to clear map source and cache data
     */
    void clearContent(boolean clearMaps) {
        List<ClearContentListener> listeners;
        synchronized (this) {
            listeners = new ArrayList<>(this.listeners);
        }
        for (ClearContentListener ccl: listeners) {
            try {
                ccl.onClearContent(clearMaps);
            } catch (Exception e) {
                Log.e(TAG, "error occurred during a clear content with: " + ccl, e);
            }
        }
    }
}
