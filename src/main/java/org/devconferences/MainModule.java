package org.devconferences;

import com.google.inject.AbstractModule;
import org.devconferences.v1.AllConferences;

public class MainModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AllConferences.class);
    }

}
