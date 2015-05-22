package org.devconferences.v1;

import com.google.inject.AbstractModule;

public class V1DataModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AllConferences.class);
    }

}
