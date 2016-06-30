package org.devconferences.elastic;

import java.util.List;

public final class SuggestResponse {
    public List<SuggestDataList> suggests;

    public class SuggestDataList {
        public List<SuggestData> options;
    }
}
