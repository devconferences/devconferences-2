package org.devconferences.events;

import com.google.gson.Gson;
import org.devconferences.jobs.AbstractImportJSONJob;
import org.elasticsearch.common.geo.GeoPoint;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ronan on 24/05/16.
 */
public class GeopointCities {
    private static GeopointCities instance = null;

    private HashMap<String,GeoPoint> citiesLoc;

    private GeopointCities() {
        citiesLoc = new HashMap<>();

        CityList cities = new Gson().fromJson(new InputStreamReader(GeopointCities.class.getResourceAsStream("/cities/cities.json")), CityList.class);

        cities.all.forEach(city -> {
            citiesLoc.put(city.name, new GeoPoint(city.location));
        });
    }

    public static GeopointCities getInstance() {
        if(instance == null) {
            instance = new GeopointCities();
        }

        return instance;
    }

    public GeoPoint getLocation(String city) {
        return citiesLoc.get(city);
    }

    public HashMap<String,GeoPoint> getAllLocations() {
        return (HashMap<String, GeoPoint>) citiesLoc.clone();
    }

}
