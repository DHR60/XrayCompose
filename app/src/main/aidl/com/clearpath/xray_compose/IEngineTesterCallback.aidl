// IEngineTesterCallback.aidl
package com.clearpath.xray_compose;

interface IEngineTesterCallback {
    void onTestStarted(String subId);
    void onTestProgress(String profileId, int current, int total);
    void onTestFinished(String subId);
}
