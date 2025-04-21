package net.flectone.pulse.backend.aspect;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Aspect
@Component
public class CachedHourlySvgAspect {

    private final Cache<String, Object> cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build();

    @Around("@annotation(CachedHourlySvg)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        CachedHourlySvg annotation = method.getAnnotation(CachedHourlySvg.class);

        String hourKey = Instant.now()
                .truncatedTo(ChronoUnit.HOURS)
                .toString();

        String baseKey = annotation.key().isEmpty()
                ? method.getName()
                : annotation.key();

        String key = baseKey + ":" + hourKey;

        Object cached = cache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }

        Object result = joinPoint.proceed();
        cache.put(key, result);
        return result;
    }
}

