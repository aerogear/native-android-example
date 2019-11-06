package com.m.helper.configModel;

import java.util.ArrayList;

public class MobileServiceJSON {
    private String clientId;
    private String namespace;
    private ArrayList<Service> services = new ArrayList<>();



    public Service getService(int pos) {
        return services.get(pos);
    }

    public String getClientId() {
        return clientId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void addService(Service service) {
        this.services.add(service);
    }

    public Service getServiceByType(String type) {
        for (Service service :
                services) {
            if (service.getType().equals(type)) {
                return service;
            }
        }
        return null;
    }
}

