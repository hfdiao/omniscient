/*
 * Copyright (c) 2016, dhf
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package im.dhf.omniscient.proxy.util;

import com.google.gson.GsonBuilder;
import im.dhf.omniscient.OmniscientException;
import im.dhf.omniscient.ServiceProvider;
import im.dhf.omniscient.proxy.JavaService;
import im.dhf.omniscient.proxy.JavaServiceProvider;
import im.dhf.omniscient.proxy.JavaServiceProxy;
import im.dhf.omniscient.registry.ServiceResolver;
import im.dhf.omniscient.selector.ServiceProviderSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author dhf
 */
class ServiceInvocationHandler implements InvocationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInvocationHandler.class);
    private static AtomicLong INDEX = new AtomicLong(0);

    private final Class<?> targetClass;
    private final String serviceName;
    private final ServiceResolver serviceResolver;
    private final JavaServiceProxy serviceProxy;
    private final ServiceProviderSelector serviceProviderSelector;
    private final InvocationExceptionHandler invocationExceptionHandler;

    ServiceInvocationHandler(Class<?> targetClass, String serviceName, JavaServiceProxy serviceProxy, ServiceResolver serviceResolver, ServiceProviderSelector serviceProviderSelector, InvocationExceptionHandler invocationExceptionHandler) {
        this.targetClass = targetClass;
        this.serviceName = serviceName;
        this.serviceProxy = serviceProxy;
        this.serviceResolver = serviceResolver;
        this.serviceProviderSelector = serviceProviderSelector;
        this.invocationExceptionHandler = invocationExceptionHandler;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            switch (method.getName()) {
                case "toString":
                    return targetClass.toString();
                case "equals":
                    return equals(Proxy.getInvocationHandler(args[0]));
                case "hashCode":
                    return hashCode();
                default:
                    throw new UnsupportedOperationException();
            }
        }

        Object result = null;
        if (LOGGER.isDebugEnabled()) {
            String invocationId = generateInvocationId();

            try {
                String className = targetClass.getCanonicalName();
                String methodName = method.getName();
                String argsJSON = toJSON(args);
                // log request
                LOGGER.debug("invoke method: {}.{}, args: {}, invocationId: {}", className, methodName, argsJSON, invocationId);

                result = doInvoke(method, args);

                // log response
                if (result instanceof Future) {
                    LOGGER.debug("method response: {}, invokeId: {}", result, invocationId);
                } else {
                    String resultJSON = toJSON(result);
                    LOGGER.debug("method response: {}, invokeId: {}", resultJSON, invocationId);
                }
            } catch (Throwable t) {
                // log error

                LOGGER.debug("method error, invocationId: {}", invocationId, t);
                throw t;
            }
        } else {
            result = doInvoke(method, args);
        }
        return result;
    }

    private Object doInvoke(Method method, Object[] args) throws Throwable {
        Object target = null;
        Throwable exception = null;
        try {
            List<ServiceProvider> providers = serviceResolver.resolve(serviceName);
            ServiceProvider provider = serviceProviderSelector.select(providers);
            if (null == provider) {
                throw new OmniscientException("no available provider for service: " + serviceName);
            } else {
                LOGGER.debug("service: {}, providers: {}, selected provider: {}", serviceName, providers, provider);
            }
            target = serviceProxy.proxy(convert(provider));
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            exception = getRootException(e);

            if (null != invocationExceptionHandler) {
                try {
                    return invocationExceptionHandler.handle(exception);
                } catch (Throwable t) {
                    exception = t;
                }
            }

            throw exception;
        } finally {
            if (null != target) {
                try {
                    serviceProxy.destroy(targetClass, target, exception);
                } catch (Exception e) {
                    LOGGER.warn("destroy target object fail", e);
                }
            }
        }
    }

    private Throwable getRootException(InvocationTargetException e) {
        Throwable rootEx = e;
        Throwable targetEx = e.getTargetException();
        while (targetEx != null) {
            rootEx = targetEx;

            if (targetEx instanceof InvocationTargetException) {
                targetEx = ((InvocationTargetException) targetEx).getTargetException();
            } else {
                break;
            }
        }
        return rootEx;
    }

    private JavaServiceProvider convert(ServiceProvider provider) {
        if (provider instanceof JavaServiceProvider) {
            return (JavaServiceProvider) provider;
        }

        JavaService javaService = new JavaService(targetClass, provider.getService());
        JavaServiceProvider result = new JavaServiceProvider(javaService, provider.getHost(), provider.getPort());
        return result;
    }

    private static String toJSON(Object obj) {
        return new GsonBuilder().create().toJson(obj);
    }

    private static String generateInvocationId() {
        return System.currentTimeMillis() + "-" + INDEX.getAndIncrement();
    }
}
