package org.devconferences.events;

public class CityLight {

    private final String id;
    private final String name;
    public long count;

    public CityLight(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public CityLight(String id, String name, long count) {
        this(id, name);
        this.count = count;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }
}
