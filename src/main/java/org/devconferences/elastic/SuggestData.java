package org.devconferences.elastic;

public class SuggestData implements Comparable<SuggestData> {
    public String text;
    public Double score;

    public SuggestData(String text, Double score) {
        this.text = text;
        this.score = score;
    }

    // Sort order : high score, alphabetical text
    @Override

    public int compareTo(SuggestData suggestData) {
        if(suggestData != null) {
            if(this.score.compareTo(suggestData.score) != 0) {
                return -1 * this.score.compareTo(suggestData.score); // Desc sort
            } else {
                return this.text.compareTo(suggestData.text);
            }
        } else {
            return -1;
        }
    }
}
