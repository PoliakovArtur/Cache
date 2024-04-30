package ru.polyakov;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

class MethodStateLock extends ReentrantLock {

    private final Method method;
    private final List<Object> args;

    public MethodStateLock(Method method, List<Object> args) {
        this.method = method;
        this.args = args;
    }
}
