package org.devconferences.users;

/**
 * Created by ronan on 21/06/16.
 */
public final class NotificationText {
    private final What what;
    private final Why why;
    private final Action action;

    public NotificationText(What what, Why why, Action action) {
        this.what = what;
        this.why = why;
        this.action = action;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", what.getText(), why.getText(what.isFeminine()),
                action.getText(what.isFeminine()));
    }

    public enum What {
        CALENDAR("Un événement", false),
        CONFERENCE("Une conférence", true),
        COMMUNITY("Une communauté", true);

        private final String text;
        private final boolean isFeminine;

        What(String text, boolean isFeminine) {
            this.text = text;
            this.isFeminine = isFeminine;
        }

        public String getText() {
            return this.text;
        }

        public boolean isFeminine() {
            return this.isFeminine;
        }
    }

    public enum Why {
        SEARCH("pouvant vous intéresser"),
        FAVOURITE("favori%s", "te"),
        CITY("dans une ville favorite");

        private final String textMasc;
        private final String textFem;

        Why(String text) {
            this.textMasc = text;
            this.textFem = text;
        }

        Why(String text, String feminineAdd) {
            this.textMasc = String.format(text, "");
            this.textFem = String.format(text, feminineAdd);
        }

        public String getText(boolean isFeminine) {
            return isFeminine ? this.textFem : this.textMasc;
        }
    }

    public enum Action {
        CREATION("a été créé%s", "e"),
        UPDATE("a été mis%s à jour", "e");

        private final String textMasc;
        private final String textFem;

        Action(String text, String feminineAdd) {
            this.textMasc = String.format(text, "");
            this.textFem = String.format(text, feminineAdd);
        }

        public String getText(boolean isFeminine) {
            return isFeminine ? this.textFem : this.textMasc;
        }
    }
}
