package ru.polyakov;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class CacheCleaner {

    private final ScheduledExecutorService executorService;
    private final Map<Cache, Map<MethodStateLock, Future<?>>> tasks;

    CacheCleaner(ScheduledExecutorService executorService, Map<Cache, Map<MethodStateLock, Future<?>>> tasks) {
        this.executorService = executorService;
        this.tasks = tasks;
    }

    void submitCleanTask(Cache cache, MethodStateLock state, int delay, TimeUnit timeUnit) {
        Future<?> task = executorService.schedule(
                () -> {
                    try {
                        state.lock();
                        cache.remove(state);
                    } finally {
                        state.unlock();
                    }
                }, delay, timeUnit);
        tasks.get(cache).put(state, task);
    }

    void cancelTask(Cache cache, MethodStateLock state, Object value) {
        Future<?> task = tasks.get(cache).get(state);
        if(task != null) {
            if(!task.cancel(true)) {
                cache.cacheValue(state, value);
            }
        }
    }

    void cancelAllTasks(Cache cache) {
        tasks.get(cache)
                .values()
                .forEach(f -> f.cancel(true));
    }

}
