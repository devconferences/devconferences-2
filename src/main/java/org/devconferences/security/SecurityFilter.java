package org.devconferences.security;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.codestory.http.Context;
import net.codestory.http.filters.Filter;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.io.Resources;
import net.codestory.http.misc.Env;
import net.codestory.http.payload.Payload;
import net.codestory.http.templating.ModelAndView;
import net.codestory.http.templating.Site;

import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class SecurityFilter implements Filter {
    private static final Set<String> resources = getURIResources();

    private static Set<String> getURIResources() {
        Env env = (Boolean.valueOf(System.getProperty("PROD_MODE")) ? Env.prod() : Env.dev());
        Site site = new Site(env, new Resources(env));

        return site.getPages().stream().map(page -> "/" + page.get("path").toString()).collect(Collectors.toSet());
    }

    private Authentication authentication;

    @Inject
    public SecurityFilter(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {
        // Resources and
        // DevConferences API URL (with its own HTML 404)
        if(resources.contains(uri) ||
                uri.startsWith("/auth/") ||
                uri.startsWith("/api/v2/") ||
                uri.equals("/ping")) {
            return nextFilter.get();
        } else {
            // Others : React URL
            // or HTML 404 (React)
            return new Payload(ModelAndView.of("index"));
        }
    }
}
