package ru.polyakov;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

class MethodStatePool {
    private final Map<Method, Map<List<Object>, MethodStateLock>> methodStatePool;

    MethodStatePool(Map<Method, Map<List<Object>, MethodStateLock>> methodStatePool) {
        this.methodStatePool = methodStatePool;
    }

    MethodStateLock getOrCreate(Method method, List<Object> args) {
        Map<List<Object>, MethodStateLock> curMethodStatePool = methodStatePool.get(method);
        MethodStateLock methodState;
        if(curMethodStatePool.containsKey(args)) {
            methodState = curMethodStatePool.get(args);
        } else {
            methodState = new MethodStateLock(method, args);
            curMethodStatePool.put(args, methodState);
        }
        return methodState;
    }

}
