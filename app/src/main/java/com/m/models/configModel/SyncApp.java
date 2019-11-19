package com.m.models.configModel;

import com.m.models.configModel.config.SyncConfig;

public class SyncApp extends Service {
    private String url;
    private SyncConfig syncConfig;

    public String getUrl() {
        return url;
    }

    public SyncConfig getSyncConfig() {
        return syncConfig;
    }
}

