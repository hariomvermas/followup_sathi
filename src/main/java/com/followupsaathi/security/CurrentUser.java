package com.followupsaathi.security;

import com.followupsaathi.entity.User;
import com.followupsaathi.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Gives services the authenticated user/business id. Ids always come from the
 * security context — never from a request parameter — so one business can
 * never touch another's data.
 */
@Component
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Returns the authenticated user, or throws 401-equivalent state. */
    public User require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AppUserDetails details
                && details.getUser() != null) {
            return details.getUser();
        }
        throw new IllegalStateException("No authenticated user in security context");
    }

    /** Re-reads the user: entity from the session may be detached/stale. */
    public User requireFresh() {
        return userRepository.findById(require().getId())
                .orElseThrow(() -> new IllegalStateException("Account no longer exists"));
    }

    public Long requireUserId() {
        return require().getId();
    }
}
