package org.devconferences.jobs;

import org.assertj.core.api.Assertions;
import org.devconferences.elastic.MockJestClient;
import org.devconferences.elastic.RuntimeJestClientAdapter;
import org.devconferences.events.EventsRepository;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by ronan on 18/05/16.
 */
public class ImportCalendarEventsJobTest {
    private ImportCalendarEventsJob importCalendarEventsJob;
    private RuntimeJestClientAdapter mockClient;

    @Before
    public void setUp() {
        mockClient = MockJestClient.createMock(EventsRepository.EVENTS_TYPE);
        importCalendarEventsJob = new ImportCalendarEventsJob(mockClient);
    }

    @Test
    public void testReloadData() {
        int totalImportedFiles = importCalendarEventsJob.reloadData(true);
        Assertions.assertThat(totalImportedFiles).isEqualTo(1);
    }

    @Test
    public void testMeetupIdsList() {
        // When reload Events
        ImportCalendarEventsJob.idMeetupList.clear();

        ImportEventsJob importEventsJob = new ImportEventsJob(mockClient);
        importEventsJob.reloadData(true);
        importCalendarEventsJob.reloadData(true);

        Assertions.assertThat(ImportCalendarEventsJob.idMeetupList).hasSize(1);
        Assertions.assertThat(ImportCalendarEventsJob.idMeetupList.contains("lskfpfs1265")).isTrue();

        // When read file .meetupIdList
        ImportCalendarEventsJob.idMeetupList.clear();
        ImportCalendarEventsJob.reloadMeetupIds();

        Assertions.assertThat(ImportCalendarEventsJob.idMeetupList).hasSize(1);
        Assertions.assertThat(ImportCalendarEventsJob.idMeetupList.contains("lskfpfs1265")).isTrue();
    }
}
