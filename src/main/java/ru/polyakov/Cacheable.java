package ru.polyakov;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cacheable {
    String[] caches() default "default";
    int lifetime() default 0;
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
