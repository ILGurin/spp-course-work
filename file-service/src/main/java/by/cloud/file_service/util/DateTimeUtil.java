package by.cloud.file_service.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DateTimeUtil {
    private final Clock clock;

    public Instant now() {
        return clock.instant();
    }
}

