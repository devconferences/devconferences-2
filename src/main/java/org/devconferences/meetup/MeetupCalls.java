package org.devconferences.meetup;

import com.google.gson.Gson;
import net.codestory.http.constants.Headers;
import net.codestory.http.errors.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

class MeetupCalls {
    private static final String MEETUP_API_KEY = "MEETUP_API_KEY";
    private static final String MEETUP_API_BASE_URL = "https://api.meetup.com";
    private static final String MEETUP_API_GROUP_INFO_URL = MEETUP_API_BASE_URL + "/%s?sign=true&key=%s";
    private static final String MEETUP_API_EVENT_INFO_URL = MEETUP_API_BASE_URL + "/2/event/%s?sign=true&key=%s";
    private static final String MEETUP_API_EVENTS_BY_GROUP_URL = MEETUP_API_BASE_URL + "/2/events/?group_urlname=%s&" +
            "status=upcoming&sign=true&key=%s";

    EventSearchResult getUpcomingEvents(String id) {
        return (EventSearchResult) getCastedContent(MEETUP_API_EVENTS_BY_GROUP_URL, id, EventSearchResult.class);
    }

    Group getGroupInfo(String id) {
        return (Group) getCastedContent(MEETUP_API_GROUP_INFO_URL, id, Group.class);
    }

    Event getEventInfo(String id) {
        return (Event) getCastedContent(MEETUP_API_EVENT_INFO_URL, id, Event.class);
    }

    private Object getCastedContent(String urlFormat, String id, Class classType) {
        try {
            HttpResponse githubResponse;

            githubResponse = Request.Get(String.format(urlFormat, id, System.getenv(MEETUP_API_KEY)))
                    .addHeader(new BasicHeader(Headers.ACCEPT, "application/json")).execute().returnResponse();

            // Check if response is OK, otherwise throw a HttpException
            int statusCode = githubResponse.getStatusLine().getStatusCode();
            if(statusCode == 200) {
                // Can't call returnContent() AND returnResponse() (InputStream can be read once...)
                // Use getEntity() instead of returnContent()
                return new Gson().fromJson(EntityUtils.toString(githubResponse.getEntity()), classType);
            } else {
                throw new HttpException(statusCode);
            }
        } catch(IOException e) {
            throw new IllegalStateException("Can't use Meetup API : " + e.getMessage());
        }
    }
}
