package com.m.models.configModel.config;

import com.google.gson.annotations.SerializedName;

public class KeycloakConfig {

    @SerializedName("auth-server-url")
    private String auth_server_url;

    @SerializedName("confidential-port")
    private int confidential_port;

    @SerializedName("public-client")
    private boolean public_client;

    private String realm;

    private String resource;

    @SerializedName("ssl-required")
    private String ssl_required;

    public String getAuth_server_url() {
        return auth_server_url;
    }

    public int getConfidential_port() {
        return confidential_port;
    }

    public boolean isPublic_client() {
        return public_client;
    }

    public String getRealm() {
        return realm;
    }

    public String getResource() {
        return resource;
    }

    public String getSsl_required() {
        return ssl_required;
    }
}
