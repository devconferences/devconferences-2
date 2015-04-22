package org.devconferences;

import net.codestory.http.WebServer;

public class Main {

    public static final int PORT = 8080;

    public static void main(String[] args) {
        WebServer webServer = new WebServer();
        webServer.start(PORT);
    }

}
