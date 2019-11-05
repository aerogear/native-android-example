package com.m.helper.DTO;

import com.m.helper.DTO.config.KeycloakConfig;

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

