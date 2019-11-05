package com.m.helpper.DTO;

import com.m.helpper.DTO.config.SyncConfig;

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

