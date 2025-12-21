package by.cloud.api.aspect;

import by.cloud.api.metric.LoginCountTotalMetric;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class LoginMetricAspect {
    private final LoginCountTotalMetric loginCountTotalMetric;

    @AfterReturning("execution(public * by.cloud.api.service.TokenService.login(..))")
    public void afterLogin() {
        loginCountTotalMetric.incrementLoginCount();
    }
}
