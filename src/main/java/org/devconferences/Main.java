package org.devconferences;

import net.codestory.http.WebServer;

public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        // This 2 functions might exit, depending of -D options
        DCSetup.dailyJob();
        DCSetup.checkJSONFiles();

        WebServer webServer = DCSetup.configureWebServer();
        webServer.start(PORT);

        DCSetup.manageESNode();
    }
}
