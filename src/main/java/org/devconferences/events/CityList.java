package org.devconferences.events;

import org.elasticsearch.common.geo.GeoPoint;

import java.util.List;

/**
 * Created by ronan on 24/05/16.
 */
public class CityList {
    public List<CityLocation> all;

    public class CityLocation {
        public String name;
        public String location;
    }
}
