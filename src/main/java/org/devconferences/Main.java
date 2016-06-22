package org.devconferences;

import net.codestory.http.WebServer;
import net.codestory.http.injection.GuiceAdapter;
import net.codestory.http.templating.ModelAndView;
import org.devconferences.elastic.DeveloppementESNode;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.events.EventsEndPoint;
import org.devconferences.jobs.AbstractImportJSONJob;
import org.devconferences.jobs.DailyJob;
import org.devconferences.jobs.ImportCalendarEventsJob;
import org.devconferences.jobs.ImportEventsJob;
import org.devconferences.meetup.MeetupEndPoint;
import org.devconferences.security.Authentication;
import org.devconferences.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    // Store all boolean properties passed when launch DevCOnferences
    static class BooleanProperties {
        final boolean prodMode;
        final boolean checkEvents;
        final boolean onlyCheckEvents;
        final boolean checkCalendar;
        final boolean onlyCheckCalendar;
        final boolean onlyReloadCalendar;
        final boolean skipDevNode;
        final boolean createIndex;
        final boolean reloadData;
        final boolean noReloadData;
        final boolean dailyJob;
        public boolean createMappings;

        BooleanProperties() {
            prodMode = getBooleanProperty(PROD_MODE);
            checkEvents = getBooleanProperty(CHECK_EVENTS);
            onlyCheckEvents = getBooleanProperty(ONLY_CHECK_EVENTS);
            checkCalendar = getBooleanProperty(CHECK_CALENDAR);
            onlyCheckCalendar = getBooleanProperty(ONLY_CHECK_CALENDAR);
            onlyReloadCalendar = getBooleanProperty(ONLY_RELOAD_CALENDAR);
            skipDevNode = getBooleanProperty(SKIP_CREATE_ES_DEV_NODE);
            createIndex = getBooleanProperty(CREATE_INDEX);
            reloadData = getBooleanProperty(RELOAD_EVENTS);
            noReloadData = getBooleanProperty(NO_RELOAD_DATA);
            dailyJob = getBooleanProperty(DAILY_JOB);
            createMappings = getBooleanProperty(CREATE_MAPPINGS);
        }

        private boolean getBooleanProperty(String property) {
            return Boolean.parseBoolean(System.getProperty(property, "false"));
        }
    }

    public static final int PORT = 8080;
    public static final ImportCalendarEventsJob importCalendarEventsJob =
            new ImportCalendarEventsJob();
    public static final ImportEventsJob importEventsJob =
            new ImportEventsJob();
    public static final DailyJob dailyJob = new DailyJob();
    // All options with '-D'
    public static final String PROD_MODE = "PROD_MODE";
    public static final String SKIP_CREATE_ES_DEV_NODE = "SKIP_DEV_NODE";
    public static final String CREATE_INDEX = "CREATE_INDEX";
    public static final String RELOAD_EVENTS = "RELOAD_EVENTS";
    public static final String NO_RELOAD_DATA = "NO_RELOAD_DATA";
    public static final String CHECK_EVENTS = "CHECK_EVENTS";
    public static final String ONLY_CHECK_EVENTS = "ONLY_CHECK_EVENTS";
    public static final String ONLY_RELOAD_CALENDAR = "ONLY_RELOAD_CALENDAR";
    public static final String CHECK_CALENDAR = "CHECK_CALENDAR";
    public static final String ONLY_CHECK_CALENDAR = "ONLY_CHECK_CALENDAR";
    public static final String DAILY_JOB = "DAILY_JOB";
    public static final String CREATE_MAPPINGS = "CREATE_MAPPINGS";

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    static void checkData(boolean noReloadData, boolean prodMode, boolean checkData, boolean onlyCheckData, AbstractImportJSONJob job) {
        if(!noReloadData && (prodMode || checkData || onlyCheckData)) {
            LOGGER.info(job.getClass().getSimpleName() + " : Checking data...");
            try {
                job.checkAllData(); // This might throw an RuntimeException
            } catch(RuntimeException e) {
                LOGGER.error(job.getClass().getSimpleName() + " : " + e.getMessage());
                throw e;
            }
            LOGGER.info(job.getClass().getSimpleName() + " : Data is OK !");

            if(onlyCheckData) {
                System.exit(0);
            }
        }
    }

    static void ifCheckEvents(BooleanProperties booleans) {
        boolean noReloadData = booleans.noReloadData;
        boolean prodMode = booleans.prodMode;
        boolean checkEvents = booleans.checkEvents;
        boolean onlyCheckEvents = booleans.onlyCheckEvents;

        checkData(noReloadData, prodMode, checkEvents, onlyCheckEvents, importEventsJob);
    }

    static void ifCheckCalendarEvents(BooleanProperties booleans) {
        boolean noReloadData = booleans.noReloadData;
        boolean prodMode = booleans.prodMode;
        boolean checkCalendar = booleans.checkCalendar;
        boolean onlyCheckCalendar = booleans.onlyCheckCalendar;

        checkData(noReloadData, prodMode, checkCalendar, onlyCheckCalendar, importCalendarEventsJob);
    }

    static void ifOnlyReloadCalendar(boolean onlyReloadCalendar) {
        if(onlyReloadCalendar) {
            ImportCalendarEventsJob.reloadMeetupIds();
            importCalendarEventsJob.reloadData(false);

            System.exit(0);
        }
    }

    static void manageESNode(BooleanProperties booleans) {
        boolean noReloadData = booleans.noReloadData;
        boolean prodMode = booleans.prodMode;
        boolean skipDevNode = booleans.skipDevNode;
        boolean createIndex = booleans.createIndex;
        boolean reloadData = booleans.reloadData;
        boolean createMappings = booleans.createMappings;

        if (!noReloadData && !prodMode && !skipDevNode) {
            LOGGER.info("-D" + SKIP_CREATE_ES_DEV_NODE + "=true To skip ES dev node creation");
            DeveloppementESNode.createDevNode();
        }
        if(!noReloadData && ((!prodMode && !skipDevNode) || createIndex)) {
            ElasticUtils.createIndex();
        } else if(createMappings) { // createIndex() calls createAllTypes()
            ElasticUtils.createAllTypes(true);
        }
        if(!noReloadData && ((!prodMode && !skipDevNode) || createIndex || prodMode || reloadData)) {
            LOGGER.info("Reload data from resources and online services...");
            importEventsJob.reloadData(false);
            importCalendarEventsJob.reloadData(false);
        }
    }


    static void ifDailyJob(BooleanProperties booleans) {
        if(booleans.dailyJob) {
            dailyJob.doJob();

            System.exit(0);
        }
    }

    static WebServer configureWebServer() {
        WebServer webServer = new WebServer();

        webServer.configure(routes -> routes
                .setIocAdapter(new GuiceAdapter())
                .filter(SecurityFilter.class)
                .add(Authentication.class)
                .add(EventsEndPoint.class)
                .add(MeetupEndPoint.class)
                .get("/ping", (context) -> "pong")
                .get("/calendar/:id", (context, id) -> ModelAndView.of("index"))
                .get("/city/:id", (context, id) -> ModelAndView.of("index"))
                .get("/city/:id/:query", (context, id, query) -> ModelAndView.of("index"))
                .get("/event/:id", (context, id) -> ModelAndView.of("index"))
                .get("/favourites/:type", (context, type) -> ModelAndView.of("index"))
                .get("/search", (context) -> ModelAndView.of("index"))
                .get("/search/:query", (context, query) -> ModelAndView.of("index"))
                .get("/search/:query/:page", (context, query, page) -> ModelAndView.of("index"))
        );
        return webServer;
    }

    static void ifNoReloadData(BooleanProperties booleans) {
        if(booleans.noReloadData) {
            LOGGER.info("No data reload");
        }
    }

    public static void main(String[] args) {
        BooleanProperties booleans = new BooleanProperties();

        ifCheckEvents(booleans);
        ifCheckCalendarEvents(booleans);
        ifDailyJob(booleans);
        ifOnlyReloadCalendar(booleans.onlyReloadCalendar);
        ifNoReloadData(booleans);

        WebServer webServer = configureWebServer();
        webServer.start(PORT);

        manageESNode(booleans);
    }
}
