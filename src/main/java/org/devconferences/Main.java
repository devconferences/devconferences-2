package org.devconferences;

import net.codestory.http.WebServer;
import net.codestory.http.injection.GuiceAdapter;
import net.codestory.http.templating.ModelAndView;
import org.devconferences.elastic.DeveloppementESNode;
import org.devconferences.events.EventsEndPoint;
import org.devconferences.jobs.ImportCalendarEventsJob;
import org.devconferences.jobs.ImportEventsJob;
import org.devconferences.meetup.MeetupEndPoint;
import org.devconferences.security.Authentication;
import org.devconferences.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    public static final int PORT = 8080;
    public static final ImportCalendarEventsJob importCalendarEventsJob =
            new ImportCalendarEventsJob();
    public static final ImportEventsJob importEventsJob =
            new ImportEventsJob();
    // All options with '-D'
    public static final String PROD_MODE = "PROD_MODE";
    public static final String SKIP_CREATE_ES_DEV_NODE = "SKIP_DEV_NODE";
    public static final String CREATE_INDEX = "CREATE_INDEX";
    public static final String RELOAD_EVENTS = "RELOAD_EVENTS";
    public static final String CHECK_EVENTS = "CHECK_EVENTS";
    public static final String ONLY_CHECK_EVENTS = "ONLY_CHECK_EVENTS";
    public static final String ONLY_RELOAD_CALENDAR = "ONLY_RELOAD_CALENDAR";

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        boolean prodMode = Boolean.parseBoolean(System.getProperty(PROD_MODE, "false"));
        boolean checkEvents = Boolean.parseBoolean(System.getProperty(CHECK_EVENTS, "false"));
        boolean onlyCheckEvents = Boolean.parseBoolean(System.getProperty(ONLY_CHECK_EVENTS, "false"));
        boolean onlyReloadCalendar = Boolean.parseBoolean(System.getProperty(ONLY_RELOAD_CALENDAR, "false"));
        // Vérification des Events dans le dossier ressource "/events/"
        if(prodMode || checkEvents || onlyCheckEvents) {
            LOGGER.info("Checking all Events...");
            try {
                importEventsJob.checkAllData(); // This might throw an RuntimeException
            } catch(RuntimeException e) {
                LOGGER.error(e.getMessage());

                throw e;
            }
            LOGGER.info("All Events are good !");

            if(onlyCheckEvents) {
                return;
            }
        }

        if(onlyReloadCalendar) {
            ImportCalendarEventsJob.reloadMeetupIds();
            importCalendarEventsJob.reloadData();

            return;
        }

        WebServer webServer = new WebServer();

        webServer.configure(routes -> {
                    routes.setIocAdapter(new GuiceAdapter());
                    routes.filter(SecurityFilter.class);
                    routes.add(Authentication.class);
                    routes.add(EventsEndPoint.class);
                    routes.add(MeetupEndPoint.class);
                    routes.get("/ping", (context) -> "pong");
                    routes.get("/city/:id", (context, id) -> ModelAndView.of("index"));
                    routes.get("/search", (context) -> ModelAndView.of("index"));
                    routes.get("/search/:query", (context, query) -> ModelAndView.of("index"));
                    routes.get("/search/:query/:page", (context, query, page) -> ModelAndView.of("index"));
                }
        );
        webServer.start(PORT);

        boolean skipDevNode = Boolean.parseBoolean(System.getProperty(SKIP_CREATE_ES_DEV_NODE, "false"));
        boolean createIndex = Boolean.parseBoolean(System.getProperty(CREATE_INDEX, "false"));
        boolean reloadData = Boolean.parseBoolean(System.getProperty(RELOAD_EVENTS, "false"));
        if (!prodMode && !skipDevNode) {
            LOGGER.info("-D" + SKIP_CREATE_ES_DEV_NODE + "=true To skip ES dev node creation");
            DeveloppementESNode.createDevNode();
        } else if(createIndex) {
            importEventsJob.createIndex();
            importCalendarEventsJob.reloadData();
        } else if(prodMode || reloadData) {
            LOGGER.info("Reload data from resources and services...");
            importEventsJob.reloadData();
            importCalendarEventsJob.reloadData();
        }
    }

}
