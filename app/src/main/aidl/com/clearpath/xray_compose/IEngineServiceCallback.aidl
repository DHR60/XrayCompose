// IEngineServiceCallback.aidl
package com.clearpath.xray_compose;

interface IEngineServiceCallback {
    void onError(String errorMessage);
    void onStateChanged(int status);
}