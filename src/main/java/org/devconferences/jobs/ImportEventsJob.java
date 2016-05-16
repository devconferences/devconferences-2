package org.devconferences.jobs;

import com.google.gson.Gson;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.events.CalendarEvent;
import org.devconferences.events.Event;
import org.devconferences.events.EventsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportEventsJob extends AbstractImportJSONJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportEventsJob.class);
    // TODO, le temps de ...
    public static final String EVENTS_TYPE = "events";

    public static void main(String[] args) {
        ImportEventsJob importEventsJob = new ImportEventsJob();
        importEventsJob.createIndex();
    }

    @Override
    public void reloadData() {
        ElasticUtils.deleteData(EVENTS_TYPE);

        importJsonInFolder("events", Event.class, (obj, path) -> {

            if(obj instanceof Event) {
                Event event = (Event) obj;

                // Add City
                String city = path.split("/")[2]; // <null> / events / <cityname> / <eventId>.json
                event.city = city;

                // Add Meetup Id if it exists for Event
                if(event.meetup != null) {
                    ImportCalendarEventsJob.addIdMeetup(event.meetup);
                }
            } else {
                System.out.println("Not an Event... Issue ? " + obj.getClass());
            }

            return obj;
        });
    }

    @Override
    public void checkAllData() {
        checkAllDataInFolder("events");
    }

    @Override
    public void checkData(String path) {
        Event event = new Gson().fromJson(new InputStreamReader(ImportEventsJob.class.getResourceAsStream(path)), Event.class);
        try {
            checkEvent(event, path); // This line might throw an exception
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage() + " - file path : " + path);
        }
    }

    public static void checkEvent(Event event, String path) {
        if(event.id == null) {
            throw new RuntimeException("Invalid Event : no 'id' field");
        }
        if(!(event.id + ".json").equals(path.split("/")[3])) {
            throw new RuntimeException("Invalid Event : filename and 'id' field mismatch");
        }
        if(event.type == null) {
            throw new RuntimeException("Invalid Event : no 'type' field");
        }
        if(event.name == null) {
            throw new RuntimeException("Invalid Event : no 'name' field");
        }
        if(event.description == null) {
            throw new RuntimeException("Invalid Event : no 'description' field");
        }
    }
}
