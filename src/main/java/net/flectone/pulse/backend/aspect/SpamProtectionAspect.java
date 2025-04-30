package net.flectone.pulse.backend.aspect;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.flectone.pulse.backend.util.HttpUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
public class SpamProtectionAspect {

    private final Cache<String, Long> ipCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();


    private final HttpUtils httpUtils;

    @SneakyThrows
    @Around("@annotation(SpamProtect)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String ip = httpUtils.getClientIpAddressIfServletRequestExist();

        Long lastRequestTime = ipCache.getIfPresent(ip);
        if (lastRequestTime != null && System.currentTimeMillis() - lastRequestTime < 3000 * 1000) {
            return null;
        }

        Object object = joinPoint.proceed();
        ipCache.put(ip, System.currentTimeMillis());

        return object;
    }
}
