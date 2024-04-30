package ru.polyakov;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static java.util.concurrent.Executors.newScheduledThreadPool;

public final class CacheSupport {

    private CacheSupport() {}

    @SuppressWarnings("unchecked")
    public static <T> T getCacheSupport(T obj) {
        Class<?> clazz = obj.getClass();
        checkClass(clazz);
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                clazz.getInterfaces(),
                createCacheManager(clazz, obj));
    }

    private static void checkClass(Class<?> clazz) {
        if(clazz.getInterfaces().length == 0) {
            throw new IllegalForCacheClassException("Class not implemented interfaces");
        }
    }

    private static CacheManager createCacheManager(Class<?> clazz, Object obj) {
        Map<Method, Map<List<Object>, MethodStateLock>> methodStatePool = new HashMap<>();
        Map<String, Cache> caches = new HashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            boolean isCacheable = method.isAnnotationPresent(Cacheable.class);
            boolean isMutator = method.isAnnotationPresent(Mutator.class);

            if (isCacheable && isMutator) {
                throw new IllegalForCacheClassException("@Cacheable and @Mutator cannot be at one method");
            }

            if(isCacheable) {
                Cacheable cacheable = method.getAnnotation(Cacheable.class);
                String[] cacheNames = cacheable.caches();
                for (String cacheName : cacheNames) {
                    if(!caches.containsKey(cacheName)) {
                        caches.put(cacheName, new Cache(cacheName, new ConcurrentHashMap<>()));
                    }
                }
                methodStatePool.put(method, new WeakHashMap<>());
            }
        }

        Map<Cache, Map<MethodStateLock, Future<?>>> tasks = new HashMap<>();
        caches.values().forEach(cache -> tasks.put(cache, new HashMap<>()));

        return new CacheManager(obj, caches,
                new CacheCleaner(newScheduledThreadPool(caches.size()), tasks),
                new MethodStatePool(methodStatePool));
    }

}
