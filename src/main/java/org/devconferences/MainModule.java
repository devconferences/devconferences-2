package org.devconferences;

import com.google.inject.AbstractModule;
import org.devconferences.elastic.Repository;
import org.devconferences.v1.BackportV1Data;

public class MainModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(BackportV1Data.class);
        bind(Repository.class);
    }

}
