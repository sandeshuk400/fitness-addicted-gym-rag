package com.fitnessaddicted.gym.attendance.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fitnessaddicted.gym.attendance.service.DeviceEventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Receives fingerprint scans from the door device or a bridge. Uses a shared key, not a user login. */
@RestController
@RequestMapping("/api/v1/device")
public class DeviceEventController {

    public record DeviceEventRequest(@NotBlank String deviceSerial, @NotNull Integer deviceUserId,
                                     @NotNull Instant scannedAt) {
    }

    public record DeviceEventResponse(String result) {
    }

    private final DeviceEventService service;
    private final byte[] apiKey;

    public DeviceEventController(DeviceEventService service, @Value("${app.device.api-key:}") String apiKey) {
        this.service = service;
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/events")
    public DeviceEventResponse event(@RequestHeader(value = "X-Device-Key", required = false) String key,
                                     @Valid @RequestBody DeviceEventRequest request) {
        // Fail closed: with no key configured, nobody gets in.
        if (apiKey.length < 32 || key == null
                || !MessageDigest.isEqual(apiKey, key.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid device key");
        }
        return new DeviceEventResponse(
                service.record(request.deviceSerial(), request.deviceUserId(), request.scannedAt()));
    }
}