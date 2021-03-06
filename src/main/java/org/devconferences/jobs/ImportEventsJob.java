package org.devconferences.jobs;

import com.google.gson.Gson;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.GeopointCities;
import org.devconferences.events.data.Event;

import java.io.InputStreamReader;

public class ImportEventsJob extends AbstractImportJSONJob {

    public ImportEventsJob() {
        super();
    }

    public ImportEventsJob(RuntimeJestClient client) {
        super(client);
    }

    @Override
    public int reloadData(boolean noRemoteCall) {
        return importJsonInFolder("events", Event.class, (obj, path) -> {
            if(obj instanceof Event) {
                Event event = (Event) obj;

                // Add City
                String city = path.split("/")[2]; // <null> / events / <cityname> / <eventId>.json
                event.city = city;

                // Add default gps for this city
                if(event.gps == null) {
                    event.gps = GeopointCities.getInstance().getLocation(event.city);
                }

                // Default avatar
                if(event.avatar == null) {
                    event.avatar = "/img/no_logo.png";
                }

                // Add Meetup Id if it exists for Event
                if(event.meetup != null) {
                    ImportCalendarEventsJob.addIdMeetup(event.meetup);
                }
            } else {
                throw new IllegalStateException("Unknown class : " + obj.getClass());
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
        } catch(RuntimeException e) {
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
