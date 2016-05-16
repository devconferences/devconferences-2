package org.devconferences.jobs;

import com.google.gson.Gson;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.events.CalendarEvent;
import org.devconferences.events.EventsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportCalendarEventsJob extends AbstractImportJSONJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCalendarEventsJob.class);
    // TODO, le temps de ...
    public static final String CALENDAREVENTS_TYPE = "calendarevents";

    public static void main(String[] args) {
        ImportCalendarEventsJob importCalendarEventsJob = new ImportCalendarEventsJob();
        importCalendarEventsJob.createIndex();
    }

    @Override
    public void reloadData() {
        ElasticUtils.deleteData(CALENDAREVENTS_TYPE);

        askMeetupUpcomingEvents();
        importJsonInFolder("calendar");
    }

    @Override
    public void checkAllData() {
        // Check nothing... Yet.
    }

    @Override
    public void checkData(String path) {
        // Check nothing... Yet.
    }

    private void askMeetupUpcomingEvents() {
    }
}
