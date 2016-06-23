package org.devconferences;

import net.codestory.http.WebServer;
import org.assertj.core.api.Assertions;
import org.devconferences.elastic.DeveloppementESNode;
import org.junit.Test;

public class MainMethodsTest {
    @Test
    public void testDefineOptions() {
        DefineOptions booleans = new DefineOptions();

        Assertions.assertThat(booleans.checkFiles).isFalse();
        Assertions.assertThat(booleans.createMappings).isFalse();
        Assertions.assertThat(booleans.dailyJob).isFalse();
        Assertions.assertThat(booleans.noReloadData).isFalse();
        Assertions.assertThat(booleans.prodMode).isFalse();
        Assertions.assertThat(booleans.skipDevNode).isFalse();
    }

    @Test
    public void testWebServer() {
        if(DeveloppementESNode.getPortNode() == null) {
            DeveloppementESNode.setPortNode("9250");
        }
        WebServer webServer = DCSetup.configureWebServer();

        Assertions.assertThat(webServer).isNotNull();
    }
}
