package com.cashier.api.support;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Lightweight Context proxy for controller and middleware unit tests. */
public final class TestContext {
    private final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private HandlerType method = HandlerType.GET;
    private String path = "/";

    public HttpStatus status;
    public Object json;
    public boolean skipped;
    public final Context context;

    public TestContext() {
        context = (Context) Proxy.newProxyInstance(
            Context.class.getClassLoader(),
            new Class<?>[]{Context.class},
            (proxy, reflectedMethod, args) -> {
                String name = reflectedMethod.getName();
                if (name.equals("attribute")) {
                    if (args.length == 2) {
                        attributes.put((String) args[0], args[1]);
                        return null;
                    }
                    return attributes.get(args[0]);
                }
                if (name.equals("header") && args.length == 1) {
                    return headers.get(args[0]);
                }
                if (name.equals("method") || name.equals("handlerType")) {
                    return method;
                }
                if (name.equals("path")) {
                    return path;
                }
                if (name.equals("status") && args != null && args.length == 1) {
                    status = args[0] instanceof HttpStatus httpStatus
                        ? httpStatus
                        : HttpStatus.forStatus((Integer) args[0]);
                    return proxy;
                }
                if (name.equals("json")) {
                    json = args[0];
                    return proxy;
                }
                if (name.equals("skipRemainingHandlers")) {
                    skipped = true;
                    return proxy;
                }
                if (name.equals("toString")) {
                    return "TestContext";
                }
                if (name.equals("hashCode")) {
                    return System.identityHashCode(proxy);
                }
                if (name.equals("equals")) {
                    return proxy == args[0];
                }
                return defaultValue(reflectedMethod.getReturnType());
            }
        );
    }

    public TestContext withAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    public TestContext withHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public TestContext withRequest(HandlerType method, String path) {
        this.method = method;
        this.path = path;
        return this;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }
}
