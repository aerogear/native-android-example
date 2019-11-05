package com.m.helpper.DTO;

import com.m.helpper.DTO.config.KeycloakConfig;

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

