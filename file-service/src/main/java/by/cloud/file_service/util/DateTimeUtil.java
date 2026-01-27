package by.cloud.file_service.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class DateTimeUtil {
    private final Clock clock;

    public Instant now() {
        return clock.instant();
    }
}

