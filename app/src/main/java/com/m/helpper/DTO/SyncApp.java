package com.m.helpper.DTO;

public class SyncApp extends Service {
    private String url;
    private Config config;

    public String getUrl() {
        return url;
    }

    public Config getConfig() {
        return config;
    }
}

class Config {
    private String websocketUrl;

    public String getWebsocketUrl() {
        return websocketUrl;
    }
}
