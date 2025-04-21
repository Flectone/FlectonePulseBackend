package net.flectone.pulse.backend.aspect;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class SpamProtectionAspect {

    private final Cache<String, Long> ipCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();


    private final HttpServletRequest request;

    public SpamProtectionAspect(HttpServletRequest request) {
        this.request = request;
    }

    @SneakyThrows
    @Around("@annotation(SpamProtect)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String ip = getClientIp();

        Long lastRequestTime = ipCache.getIfPresent(ip);
        if (lastRequestTime != null && System.currentTimeMillis() - lastRequestTime < 3000 * 1000) {
            return null;
        }

        Object object = joinPoint.proceed();
        ipCache.put(ip, System.currentTimeMillis());

        return object;
    }

    private String getClientIp() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
