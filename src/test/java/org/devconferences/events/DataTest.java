package org.devconferences.events;

import org.assertj.core.api.Assertions;
import org.devconferences.meetup.EventsSearch;
import org.devconferences.users.User;
import org.elasticsearch.common.geo.GeoPoint;
import org.junit.Test;

public class DataTest {

    @Test
    public void testEventCopy() {
        Event eventCopy;

        Event event = new Event();
        event.description = "description";
        event.id = "azd135er";
        event.name = "Test Event Copy constructor";
        event.avatar = "/img/no_logo.png";
        event.type = Event.Type.COMMUNITY;
        event.city = "city 1";

        eventCopy = new Event(event);
        Assertions.assertThat(eventCopy.hashCode()).isEqualTo(event.hashCode());
        Assertions.assertThat(eventCopy.equals(event)).isTrue();

        event.youtube = event.new Youtube();
        event.youtube.name = "ABC";
        event.youtube.channel = "zer15z3EQDV646f";

        eventCopy = new Event(event);
        Assertions.assertThat(eventCopy.hashCode()).isEqualTo(event.hashCode());
        Assertions.assertThat(eventCopy.equals(event)).isTrue();
    }

    @Test
    public void testCalendarEventCopy() {
        CalendarEvent calendarEventCopy;
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.id = "az_addsfsdf";
        calendarEvent.name = "Test Événement";
        calendarEvent.date = 132456789000L;
        calendarEvent.description = "Un petit test";

        calendarEventCopy = new CalendarEvent(calendarEvent);
        Assertions.assertThat(calendarEventCopy.hashCode()).isEqualTo(calendarEvent.hashCode());
        Assertions.assertThat(calendarEventCopy.equals(calendarEvent)).isTrue();

        calendarEvent.organizer = calendarEvent.new Group();
        calendarEvent.organizer.name = "Lorem";
        calendarEvent.organizer.url = "http://ips.um";

        calendarEventCopy = new CalendarEvent(calendarEvent);
        Assertions.assertThat(calendarEventCopy.hashCode()).isEqualTo(calendarEvent.hashCode());
        Assertions.assertThat(calendarEventCopy.equals(calendarEvent)).isTrue();

        calendarEvent.location = calendarEvent.new Location();
        calendarEvent.location.name = "Quelque part";
        calendarEvent.location.address = "une adresse";
        calendarEvent.location.city = "Ma ville";
        calendarEvent.location.gps = new GeoPoint(45.00, 00.00);

        calendarEventCopy = new CalendarEvent(calendarEvent);
        Assertions.assertThat(calendarEventCopy.hashCode()).isEqualTo(calendarEvent.hashCode());
        Assertions.assertThat(calendarEventCopy.equals(calendarEvent)).isTrue();

        calendarEvent.cfp = calendarEvent.new CallForPapers();
        calendarEvent.cfp.url = "http://cfp.ips.um";
        calendarEvent.cfp.dateSubmission = 123456789000L;

        calendarEventCopy = new CalendarEvent(calendarEvent);
        Assertions.assertThat(calendarEventCopy.hashCode()).isEqualTo(calendarEvent.hashCode());
        Assertions.assertThat(calendarEventCopy.equals(calendarEvent)).isTrue();
    }

    @Test
    public void testMessageCopy() {
        final User user = new User(null, null, null, null);
        User.Message message = user.new Message();
        message.id = "12345678.90";
        message.text = "ABC DEF GHI JKL MNO PQR STU VWX YZ.";
        message.link = "http://like.ly";
        message.date = 1245987892000L;
        User.Message message2 = user.new Message();
        message2.id = "12345678.90";
        message2.text = "ABC DEF GHI JKL MNO PQR STU VWX YZ.";
        message2.link = "http://like.ly";
        message2.date = 1245987892000L;

        Assertions.assertThat(message.hashCode()).isEqualTo(message2.hashCode());
        Assertions.assertThat(message.equals(message2)).isTrue();
    }
}
