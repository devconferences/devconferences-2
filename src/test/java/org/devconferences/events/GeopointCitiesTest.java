package org.devconferences.events;

import org.assertj.core.api.Assertions;
import org.elasticsearch.common.geo.GeoPoint;
import org.junit.Test;

/**
 * Created by ronan on 24/05/16.
 */
public class GeopointCitiesTest {
    @Test
    public void test() {
        GeopointCities geopointCities = GeopointCities.getInstance();
        Assertions.assertThat(geopointCities).isNotNull();

        Assertions.assertThat(geopointCities.getLocation("the_city"))
                .isEqualToComparingFieldByField(new GeoPoint("49.900,2.3000"));
        Assertions.assertThat(geopointCities.getLocation("the_city2"))
                .isEqualToComparingFieldByField(new GeoPoint("47.4800,-0.5400"));
        Assertions.assertThat(geopointCities.getLocation("the_city3")).isNull();
    }
}
