package com.m.helper;

import android.content.Context;
import android.content.res.AssetManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.m.helper.DTO.Keycloak;
import com.m.helper.DTO.MobileServiceJSON;
import com.m.helper.DTO.Push;
import com.m.helper.DTO.Service;
import com.m.helper.DTO.SyncApp;
import com.m.helper.DTO.config.AndroidConfig;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MobileService implements IMobileService {

    private static MobileService INSTANCE = null;
    private MobileServiceJSON config = new MobileServiceJSON();
    private AssetManager assetManager;
    private Gson gson = new Gson();

    private final String SYNC_APP = "sync-app";
    private final String KEYCLOAK = "keycloak";
    private final String PUSH = "push";

    private MobileService(Context context){
        System.out.println("APP : Setting up Mobile Services");
        assetManager = context.getAssets();
        readMobileServiceJSON();
    }

    private MobileService(){}

    public static MobileService getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new MobileService(context);
        }
        return INSTANCE;
    }

    public static MobileService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MobileService();
        }
        return INSTANCE;
    }

    @Override
    public String getGraphqlServer() {
        SyncApp service = (SyncApp) config.getServiceByType(SYNC_APP);
        return service.getUrl();
    }

    @Override
    public String getKIssuer() {
        Keycloak service = (Keycloak) config.getServiceByType(KEYCLOAK);
        String server = service.getConfig().getAuth_server_url();
        String realm = service.getConfig().getRealm();
        return server + "/realms/" + realm + "/";
    }

    @Override
    public String getKClientId() {
        Keycloak service = (Keycloak) config.getServiceByType(KEYCLOAK);
        return service.getConfig().getResource();
    }

    @Override
    public String getPushUrl() {
        Push service = (Push) config.getServiceByType(PUSH);
        return service.getUrl();
    }

    @Override
    public String getPushVariantId() {
        Push service = (Push) config.getServiceByType(PUSH);
        return service.getConfig().getVariantId();
    }

    @Override
    public String getPushVariantSecret() {
        Push service = (Push) config.getServiceByType(PUSH);
        return service.getConfig().getVariantSecret();
    }

    private void readMobileServiceJSON(){
        try {
            BufferedReader bufferedReader = getBufferReader();
            JsonObject array = getJsonObject(bufferedReader);

            config.setClientId(array.get("clientId").getAsString());
            config.setNamespace(array.get("namespace").getAsString());

            JsonArray services = array.get("services").getAsJsonArray();
            configureServices(services);

        } catch (FileNotFoundException e) {
            System.out.println("APP: FileNotFoundException " + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("APP: IOException " + e.toString());

        }
    }

    private Service buildService(JsonObject service){
        Service result = null;
        switch (service.get("type").getAsString()) {
            case SYNC_APP:
                result = gson.fromJson(service, SyncApp.class);
                break;
            case KEYCLOAK:
                result = gson.fromJson(service, Keycloak.class);
                break;
            case PUSH:
                Push r = gson.fromJson(service, Push.class);
                JsonElement jsonConfig = service.get("config");
                JsonElement jsonAndroidConfig = jsonConfig.getAsJsonObject().get("android");
                AndroidConfig androidConfig = gson.fromJson(jsonAndroidConfig, AndroidConfig.class);
                r.setConfig(androidConfig);
                result = r;
                break;
            default:
                System.out.println("APP: No type found");

        }
        return result;
    }

    private BufferedReader getBufferReader() throws IOException {
        InputStream IS = assetManager.open("config/mobile-services.json");
        InputStreamReader ISR = new InputStreamReader(IS);
        return new BufferedReader(ISR);
    }

    private JsonObject getJsonObject(BufferedReader bufferedReader){
        JsonParser parser = new JsonParser();
        return parser.parse(bufferedReader).getAsJsonObject();
    }

    private void configureServices(JsonArray services) {
        for (int i = 0; i < services.size(); i++){
            Service newService = buildService(services.get(i).getAsJsonObject());
            if (newService != null){
                config.addService(newService);
            } else {
                System.out.println("APP: Service was null");
            }
        }

    }
}
