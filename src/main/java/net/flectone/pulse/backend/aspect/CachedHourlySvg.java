package net.flectone.pulse.backend.aspect;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CachedHourlySvg {
    String key() default "";
}

