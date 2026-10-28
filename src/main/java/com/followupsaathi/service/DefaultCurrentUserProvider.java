package com.followupsaathi.service;

import com.followupsaathi.entity.User;
import com.followupsaathi.exception.ApiException;
import com.followupsaathi.repository.UserRepository;
import org.springframework.stereotype.Component;

/** Default CurrentUserProvider: loads the user, 401 when missing. */
@Component
public class DefaultCurrentUserProvider implements BusinessService.CurrentUserProvider {

    private final UserRepository userRepository;

    public DefaultCurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Not logged in"));
    }
}
