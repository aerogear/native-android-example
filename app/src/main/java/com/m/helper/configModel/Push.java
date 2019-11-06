package com.m.helper.configModel;

import com.m.helper.configModel.config.AndroidConfig;
import com.m.helper.configModel.config.PushConfig;

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
