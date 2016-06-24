package org.devconferences;

import net.codestory.http.WebServer;
import net.codestory.http.injection.GuiceAdapter;
import org.devconferences.elastic.DeveloppementESNode;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.events.EventsEndPoint;
import org.devconferences.jobs.DailyJob;
import org.devconferences.jobs.ImportCalendarEventsJob;
import org.devconferences.jobs.ImportEventsJob;
import org.devconferences.meetup.MeetupEndPoint;
import org.devconferences.security.Authentication;
import org.devconferences.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.devconferences.DefineOptions.SKIP_CREATE_ES_DEV_NODE;

class DCSetup {
    private static final ImportCalendarEventsJob importCalendarEventsJob =
            new ImportCalendarEventsJob();
    private static final ImportEventsJob importEventsJob =
            new ImportEventsJob();
    private static final DailyJob dailyJob = new DailyJob();
    private static DefineOptions defineOptions = new DefineOptions();
    // All options with '-D'
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    static void checkJSONFiles() {
        boolean noReloadData = defineOptions.noReloadData;
        boolean prodMode = defineOptions.prodMode;
        boolean checkFiles = defineOptions.checkFiles;

        if(!noReloadData && (prodMode || checkFiles)) {
            LOGGER.info("Checking JSON files...");
            try {
                importEventsJob.checkAllData();
                importCalendarEventsJob.checkAllData();
            } catch(RuntimeException e) {
                LOGGER.error(e.getMessage());
                throw e;
            }
            LOGGER.info("All JSON files are good !");
        }

        if(checkFiles) {
            System.exit(0);
        }
    }

    static void manageESNode() {
        boolean noReloadData = defineOptions.noReloadData;
        boolean prodMode = defineOptions.prodMode;
        boolean skipDevNode = defineOptions.skipDevNode;
        boolean createIndex = defineOptions.createIndex;

        if(noReloadData) {
            LOGGER.info("Skip reload JSON files.");
        } else {
            if(!prodMode && !skipDevNode) {
                LOGGER.info("-D" + SKIP_CREATE_ES_DEV_NODE + "=true to skip ES dev node creation");
                DeveloppementESNode.createDevNode();
            }
            if(!prodMode && !skipDevNode || createIndex) {
                ElasticUtils.createIndex();
            }
            if(!prodMode && !skipDevNode || createIndex || prodMode) {
                LOGGER.info("Update index with JSON files and online services...");
                importEventsJob.reloadData(false);
                importCalendarEventsJob.reloadData(false);
            }
        }
    }

    static void dailyJob() {
        if(defineOptions.dailyJob) {
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
        );
        return webServer;
    }

    static void setDefineOptions(DefineOptions defineOptions) {
        DCSetup.defineOptions = defineOptions;
    }
}
