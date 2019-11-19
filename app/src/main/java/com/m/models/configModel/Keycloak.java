package com.m.models.configModel;

import com.m.models.configModel.config.KeycloakConfig;

public class Keycloak extends Service {

    private String url;
    private KeycloakConfig config;

    public String getUrl() {
        return url;
    }

    public KeycloakConfig getConfig() {
        return config;
    }
}

