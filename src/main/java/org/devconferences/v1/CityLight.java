package org.devconferences.v1;

public class CityLight {

    private final String id;
    private final String name;

    public CityLight(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }
}
