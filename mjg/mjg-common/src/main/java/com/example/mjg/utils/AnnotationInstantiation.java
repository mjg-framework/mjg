package com.example.mjg.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import com.example.mjg.config.ErrorResolution;

public class AnnotationInstantiation {
    public static ErrorResolution createErrorResolution(int retryTimes, int retryDelayInSeconds) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "retryTimes": return retryTimes;
                case "retryDelayInSeconds": return retryDelayInSeconds;
                case "annotationType": return ErrorResolution.class;
                default: throw new UnsupportedOperationException("Unsupported method: " + method.getName());
            }
        };

        return (ErrorResolution) Proxy.newProxyInstance(
                ErrorResolution.class.getClassLoader(),
                new Class[]{ErrorResolution.class},
                handler
        );
    }
}
