package org.devconferences;

import net.codestory.http.WebServer;
import net.codestory.http.injection.GuiceAdapter;
import net.codestory.http.templating.ModelAndView;
import org.devconferences.events.EventsEndPoint;
import org.devconferences.meetup.MeetupEndPoint;
import org.devconferences.security.Authentication;
import org.devconferences.security.SecurityFilter;

public class Main {

    public static final int PORT = 8080;

    public static void main(String[] args) {
        WebServer webServer = new WebServer();

        webServer.configure(routes -> {
                    routes.setIocAdapter(new GuiceAdapter());
                    routes.filter(SecurityFilter.class);
                    routes.add(Authentication.class);
                    routes.add(EventsEndPoint.class);
                    routes.add(MeetupEndPoint.class);
                    routes.get("/city/:id", (context, id) -> ModelAndView.of("index"));
                }
        );
        webServer.start(PORT);
    }

}
