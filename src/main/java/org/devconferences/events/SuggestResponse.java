package org.devconferences.events;

import java.util.List;

/**
 * Created by ronan on 21/06/16.
 */
final class SuggestResponse {
    public Suggests suggest;

    public class Suggests {
        public List<SuggestDataList> cityEventSuggest;
        public List<SuggestDataList> nameEventSuggest;
        public List<SuggestDataList> tagsEventSuggest;
        public List<SuggestDataList> nameCalendarSuggest;

    }

    public class SuggestDataList {
        public List<SuggestData> options;
    }
}
