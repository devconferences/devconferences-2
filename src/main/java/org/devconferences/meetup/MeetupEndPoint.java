package org.devconferences.meetup;

import com.google.inject.Inject;
import net.codestory.http.annotations.AllowOrigin;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.errors.NotFoundException;

import java.io.IOException;

@Prefix("api/v2/")
public class MeetupEndPoint {

    private MeetupApiClient meetupApiClient;

    @Inject
    public MeetupEndPoint(MeetupApiClient meetupApiClient) {
        this.meetupApiClient = meetupApiClient;
    }

    @Get("meetup/:id")
    @AllowOrigin("*")
    public MeetupInfo meetupInfo(String id) throws IOException {
        return NotFoundException.notFoundIfNull(meetupApiClient.getMeetupInfo(id));
    }


}

