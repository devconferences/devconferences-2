package org.devconferences.security;

import com.google.common.io.Files;
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
import net.codestory.http.types.ContentTypes;
import org.devconferences.users.User;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by chris on 07/06/15.
 */
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
        // DevConferences API URL
        if(uri.startsWith("/auth/") ||
                uri.startsWith("/api/v2/") ||
                uri.equals("/ping")) {
            if(authentication.isAuthenticated(context)) {
                User user = authentication.getUser(context);
                context.setCurrentUser(user);
            }
            return nextFilter.get();

            // Resource File URL
        } else if(resources.contains(uri)) {
            return new Payload(ContentTypes.get(uri), Files.asByteSource(new File(SecurityFilter.class.getResource("/app" + uri).toURI())).read());

            // Others : React URL
            // or HTML 404 (React)
        } else {
            return new Payload(ModelAndView.of("index"));
        }
    }
}
