// IEngineServiceControl.aidl
package com.clearpath.xray_compose;

import com.clearpath.xray_compose.IEngineServiceCallback;
import com.clearpath.xray_compose.service.engine.model.TrafficSummary;

interface IEngineServiceControl {
    void start();
    void stop();
    int getState();
    long measureHttpDelay();
    long measureHttpDelayWithUrl(String url);
    TrafficSummary getTrafficSummary();
    void startTrafficStatMonitor();
    void stopTrafficStatMonitor();
    void registerCallback(IEngineServiceCallback callback);
    void unregisterCallback(IEngineServiceCallback callback);
}