package com.m.helpper.DTO;

import com.m.helpper.DTO.config.AndroidConfig;
import com.m.helpper.DTO.config.PushConfig;

public class Push extends Service {

    private String url;
    private PushConfig config;

    public String getUrl() {
        return url;
    }

    public AndroidConfig getConfig() {
        return config.getAndroidConfig();
    }

    public void setConfig(AndroidConfig config) {
        this.config.setAndroidConfig(config);
    }
}
