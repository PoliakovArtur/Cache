package ru.polyakov;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

class CacheManager implements InvocationHandler {

    private final Object object;
    private final Map<String, Cache> caches;
    private final CacheCleaner cacheCleaner;
    private final MethodStatePool methodStatePool;

    public CacheManager(Object object,
                        Map<String, Cache> caches,
                        CacheCleaner cacheCleaner,
                        MethodStatePool methodStatePool) {
        this.object = object;
        this.caches = caches;
        this.cacheCleaner = cacheCleaner;
        this.methodStatePool = methodStatePool;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        List<Cache> curCaches;
        method = object.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
        method.setAccessible(true);
        if(method.isAnnotationPresent(Cacheable.class)) {
            List<Object> argsList = List.of(args);
            MethodStateLock state = methodStatePool.getOrCreate(method, argsList);
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            curCaches = getCaches(cacheable.caches());
            boolean isNotFound = true;
            Object value = null;
            for(Cache cache : curCaches) {
                if(state.tryLock()) {
                    try {
                        if(cache.contains(state)) {
                            value = cache.getOrNull(state);
                            isNotFound = false;
                            break;
                        }
                    } finally {
                        state.unlock();
                    }
                }
            }
            if(isNotFound) value = method.invoke(object, args);
            int lifetime = cacheable.lifetime();
            TimeUnit timeUnit = cacheable.timeUnit();
            for(Cache cache : curCaches) {
                cache.cacheValue(state, value);
                if(lifetime > 0) {
                    cacheCleaner.cancelTask(cache, state, value);
                    cacheCleaner.submitCleanTask(cache, state, lifetime, timeUnit);
                }
            }
            return value;
        }
        if(method.isAnnotationPresent(Mutator.class)) {
            Mutator mutator = method.getAnnotation(Mutator.class);
            curCaches = getCaches(mutator.caches());
            for(Cache cache : curCaches) {
                cache.clear();
                cacheCleaner.cancelAllTasks(cache);
            }
        }
        return method.invoke(object, args);
    }

    private List<Cache> getCaches(String[] cacheNames) {
        return Arrays.stream(cacheNames)
                .map(caches::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
