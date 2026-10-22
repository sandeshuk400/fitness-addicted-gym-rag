package com.fitnessaddicted.gym.auth.service;

import com.fitnessaddicted.gym.auth.domain.AppUser;
import com.fitnessaddicted.gym.auth.domain.Role;
import com.fitnessaddicted.gym.auth.repository.AppUserRepository;
import com.fitnessaddicted.gym.common.BootstrapAdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN at startup when BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD are set
 * and no admin exists yet. Without it nobody could ever create staff accounts.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final BootstrapAdminProperties props;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(BootstrapAdminProperties props, AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.props = props;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean configured = props.email() != null && !props.email().isBlank()
                && props.password() != null && !props.password().isBlank();
        if (!configured || users.existsByRole(Role.ADMIN)) {
            return;
        }
        if (props.password().length() < 8) {
            log.error("BOOTSTRAP_ADMIN_PASSWORD must be at least 8 characters; admin not created");
            return;
        }
        String email = AuthService.normalize(props.email());
        users.save(new AppUser(email, passwordEncoder.encode(props.password()), Role.ADMIN));
        log.info("Bootstrap admin account created for {}", email);
    }
}
