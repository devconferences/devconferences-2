package org.devconferences;

import net.codestory.http.WebServer;
import org.assertj.core.api.Assertions;
import org.devconferences.elastic.DeveloppementESNode;
import org.junit.Test;

public class MainMethodsTest {
    @Test
    public void testBooleanProperties() {
        Main.BooleanProperties booleans = new Main.BooleanProperties();

        Assertions.assertThat(booleans.checkCalendar).isFalse();
        Assertions.assertThat(booleans.checkEvents).isFalse();
        Assertions.assertThat(booleans.createIndex).isFalse();
        Assertions.assertThat(booleans.onlyCheckCalendar).isFalse();
        Assertions.assertThat(booleans.onlyCheckEvents).isFalse();
        Assertions.assertThat(booleans.onlyReloadCalendar).isFalse();
        Assertions.assertThat(booleans.prodMode).isFalse();
        Assertions.assertThat(booleans.reloadData).isFalse();
        Assertions.assertThat(booleans.skipDevNode).isFalse();
    }

    @Test
    public void testCheckData() {
        System.setProperty(Main.CHECK_EVENTS, "true");
        System.setProperty(Main.CHECK_CALENDAR, "true");

        Main.BooleanProperties booleans = new Main.BooleanProperties();
        try {
            Main.ifCheckCalendarEvents(booleans);
            Main.ifCheckEvents(booleans);
        } catch(RuntimeException e) {
            Assertions.fail("THis should not throw an exception !", e);
        }

        System.setProperty(Main.CHECK_EVENTS, "");
        System.setProperty(Main.CHECK_CALENDAR, "");
    }

    @Test
    public void testWebServer() {
        if(DeveloppementESNode.getPortNode() == null) {
            DeveloppementESNode.setPortNode("9250");
        }
        WebServer webServer = Main.configureWebServer();

        Assertions.assertThat(webServer).isNotNull();
    }
}
