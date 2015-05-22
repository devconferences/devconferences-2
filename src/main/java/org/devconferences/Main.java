package org.devconferences;

import net.codestory.http.WebServer;
import net.codestory.http.injection.GuiceAdapter;
import org.devconferences.v1.BackportV1Resource;

public class Main {

    public static final int PORT = 8080;

    public static void main(String[] args) {
        WebServer webServer = new WebServer();
        webServer.configure(routes -> {
                    routes.setIocAdapter(new GuiceAdapter(new MainModule()));
                    routes.add(BackportV1Resource.class);
                }
        );
        webServer.start(PORT);
    }

}
