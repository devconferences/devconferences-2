package org.devconferences.events;

import java.util.List;

/**
 * Created by ronan on 21/06/16.
 */
final class SuggestResponse {
    public List<SuggestDataList> suggests;

    public class SuggestDataList {
        public List<SuggestData> options;
    }
}
