package by.cloud.person_service.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
/*
    Утилитный класс для получения текущего времени
 */
public class DateTimeUtil {
    private final Clock clock;

    public Instant now() {
        return clock.instant();
    }
}
