// IEngineTesterService.aidl
package com.clearpath.xray_compose;

import com.clearpath.xray_compose.IEngineTesterCallback;

interface IEngineTesterService {
    void startTest(String subId);
    void startTestProfiles(in List<String> profileIds);
    void stopTest();
    boolean isTesting();
    void registerCallback(IEngineTesterCallback callback);
    void unregisterCallback(IEngineTesterCallback callback);
}
