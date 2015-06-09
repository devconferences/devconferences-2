package org.devconferences.security;

import com.google.inject.Inject;
import net.codestory.http.Context;
import net.codestory.http.filters.Filter;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;
import org.devconferences.users.User;

/**
 * Created by chris on 07/06/15.
 */
public class SecurityFilter implements Filter {

    private Authentication authentication;

    @Inject
    public SecurityFilter(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {
        if (authentication.isAuthenticated(context)) {
            User user = authentication.getUser(context);
            context.setCurrentUser(user);
        }
        return nextFilter.get();
    }

    @Override
    public boolean matches(String uri, Context context) {
        return true;
    }
}
