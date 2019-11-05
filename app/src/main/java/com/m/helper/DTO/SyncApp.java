package com.m.helper.DTO;

import com.m.helper.DTO.config.SyncConfig;

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

