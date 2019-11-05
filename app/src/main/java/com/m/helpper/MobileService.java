package com.m.helpper;

import android.content.Context;
import android.content.res.AssetManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.m.helpper.DTO.MobileServiceJSON;
import com.m.helpper.DTO.Service;
import com.m.helpper.DTO.SyncApp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class MobileService implements MobileServices {

    private static MobileService INSTANCE = null;
    private MobileServiceJSON config = new MobileServiceJSON();

    private AssetManager assetManager;

    private Gson gson = new Gson();

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
        SyncApp service = (SyncApp) config.getServiceByType("sync-app");
        return service.getUrl();
    }

    private void readMobileServiceJSON(){
        try {
            InputStream IS = assetManager.open("config/mobile-services.json");
            InputStreamReader ISR = new InputStreamReader(IS);
            BufferedReader BR = new BufferedReader(ISR);

            JsonParser parser = new JsonParser();
            JsonObject array = parser.parse(BR).getAsJsonObject();

            config.setClientId(array.get("clientId").getAsString());
            config.setNamespace(array.get("namespace").getAsString());

            JsonArray services = array.get("services").getAsJsonArray();

            for (int i = 0; i < services.size(); i++){
                Service newService = buildService(services.get(i).getAsJsonObject());
                if (newService != null){
                    config.addService(newService);
                } else {
                    System.out.println("APP: Service was null");
                }
            }

            System.out.println("APP: Service Type: " + config.getService(0).getType());

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
            case "sync-app":
                result = gson.fromJson(service, SyncApp.class);
                break;
            default:
                System.out.println("APP: No type found");

        }

        return result;
    }

}
