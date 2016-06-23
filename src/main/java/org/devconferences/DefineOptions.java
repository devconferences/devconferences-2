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
     * Re-create 'dev-conferences' types. (BUT IT DELETES ALL DOCUMENTS IN dev-conferences INDEX !!)
     * Useful if mappings have changed, and a PUT _mapping might fail.
     */
    static final String CREATE_MAPPINGS = "CREATE_MAPPINGS";

    final boolean prodMode;
    final boolean checkFiles;
    final boolean skipDevNode;
    final boolean noReloadData;
    final boolean dailyJob;
    final boolean createMappings;

    DefineOptions() {
        prodMode = getBooleanProperty(PROD_MODE);
        skipDevNode = getBooleanProperty(SKIP_CREATE_ES_DEV_NODE);
        checkFiles = getBooleanProperty(CHECK_FILES);
        noReloadData = getBooleanProperty(NO_RELOAD_DATA);
        dailyJob = getBooleanProperty(DAILY_JOB);
        createMappings = getBooleanProperty(CREATE_MAPPINGS);
    }

    private boolean getBooleanProperty(String property) {
        return Boolean.parseBoolean(System.getProperty(property, "false"));
    }
}
