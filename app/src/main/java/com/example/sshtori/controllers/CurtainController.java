package com.example.sshtori.controllers;

import com.example.sshtori.models.CurtainState;

public interface CurtainController {

    void openCurtain(CurtainCallback callback);
    void closeCurtain(CurtainCallback callback);
    void stopCurtain(CurtainCallback callback);
    void setPosition(int position, CurtainCallback callback);
    void getState(CurtainCallback callback);
    void checkConnection(CurtainCallback callback);
    void shutdown();

    interface CurtainCallback {
        void onSuccess(Object result);
        void onError(String error);
    }
}