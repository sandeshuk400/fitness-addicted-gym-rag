package com.fitnessaddicted.gym.common;

import java.time.LocalDate;
import java.time.ZoneId;

/** Business time zone of the gym. Containers usually run in UTC, so "today" must not use the system zone. */
public final class AppTime {

    public static final ZoneId GYM_ZONE = ZoneId.of("Asia/Kolkata");

    private AppTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(GYM_ZONE);
    }
}
