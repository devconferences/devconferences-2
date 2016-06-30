package org.devconferences.events.data;

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
