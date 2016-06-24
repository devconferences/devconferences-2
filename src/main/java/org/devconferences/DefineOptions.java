package org.devconferences;

class DefineOptions {
    /**
     * Production mode (fluent-http uses this).
     */
    static final String PROD_MODE = "PROD_MODE";
    /**
     * If you are in development mode, BUT you have an
     * ES node on localhost:9200, this skip node creation.
     */
    static final String SKIP_CREATE_ES_DEV_NODE = "SKIP_DEV_NODE";
    /**
     * If you have an ES node, and the last modifications are
     * neither on JSON files nor ES mappings, this skip existing data, so launch web server faster.
     */
    static final String NO_RELOAD_DATA = "NO_RELOAD_DATA";
    /**
     * Check Event AND CalendarEvent files, then exit.
     * Useful to check JSON files modifications.
     */
    static final String CHECK_FILES = "CHECK_FILES";
    /**
     * Launch the routine which update ES node (Clever Cloud uses this).
     */
    static final String DAILY_JOB = "DAILY_JOB";
    /**
     * Create Dev Conferences index (if it doesn't exist), and all its types.
     * (IT DELETES ALL DOCUMENTS IN Dev Conferences INDEX !!)
     * Useful if type mappings have changed.
     */
    static final String CREATE_INDEX = "CREATE_INDEX";

    final boolean prodMode;
    final boolean checkFiles;
    final boolean skipDevNode;
    final boolean noReloadData;
    final boolean dailyJob;
    final boolean createIndex;

    DefineOptions() {
        prodMode = getBooleanProperty(PROD_MODE);
        skipDevNode = getBooleanProperty(SKIP_CREATE_ES_DEV_NODE);
        checkFiles = getBooleanProperty(CHECK_FILES);
        noReloadData = getBooleanProperty(NO_RELOAD_DATA);
        dailyJob = getBooleanProperty(DAILY_JOB);
        createIndex = getBooleanProperty(CREATE_INDEX);
    }

    private boolean getBooleanProperty(String property) {
        return Boolean.parseBoolean(System.getProperty(property, "false"));
    }
}
