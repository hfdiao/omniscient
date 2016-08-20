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

import im.dhf.omniscient.proxy.JavaServiceProxy;
import im.dhf.omniscient.registry.ServiceResolver;
import im.dhf.omniscient.selector.ServiceProviderRandomSelector;
import im.dhf.omniscient.selector.ServiceProviderSelector;

import java.io.Closeable;
import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 *
 * @author dhf
 */
public class ServiceBuilder {
    private /* @NonNull */ Class<?> targetClass;
    private /* @NonNull */ JavaServiceProxy serviceProxy;
    private /* @NonNull */ ServiceResolver serviceResolver;
    private ServiceProviderSelector serviceProviderSelector;
    private InvocationExceptionHandler invocationExceptionHandler;
    private String serviceName;
    private boolean closeable = false;

    public Object build() {
        Objects.requireNonNull(targetClass, "targetClass");
        Objects.requireNonNull(serviceProxy, "serviceProxy");
        Objects.requireNonNull(serviceResolver, "serviceResolver");

        Class<?> targetClass = this.targetClass;
        String serviceName = this.serviceName;
        if (null == serviceName || serviceName.trim().isEmpty()) {
            serviceName = targetClass.getCanonicalName();
        }
        if (null == serviceProviderSelector) {
            serviceProviderSelector = ServiceProviderRandomSelector.get();
        }

        ServiceInvocationHandler handler = new ServiceInvocationHandler(targetClass, serviceName, serviceProxy, serviceResolver, serviceProviderSelector, invocationExceptionHandler);
        Class<?>[] interfaces = null;
        if (closeable) {
            interfaces = new Class[]{targetClass, Closeable.class};
        } else {
            interfaces = new Class[]{targetClass};
        }

        return targetClass.cast(Proxy.newProxyInstance(targetClass.getClassLoader(), interfaces, handler));
    }

    public /* @NonNull */ Class<?> targetClass() {
        return targetClass;
    }

    public ServiceBuilder targetClass(/* @NonNull */ Class<?> targetClass) {
        this.targetClass = targetClass;
        return this;
    }


    public /* @NonNull */ JavaServiceProxy serviceProxy() {
        return serviceProxy;
    }

    public ServiceBuilder serviceProxy(/* @NonNull */ JavaServiceProxy serviceProxy) {
        this.serviceProxy = serviceProxy;
        return this;
    }

    public /* @NonNull */ ServiceResolver serviceResolver() {
        return serviceResolver;
    }

    public ServiceBuilder serviceResolver(/* @NonNull */ ServiceResolver serviceResolver) {
        this.serviceResolver = serviceResolver;
        return this;
    }

    public ServiceProviderSelector serviceProviderSelector() {
        return serviceProviderSelector;
    }

    public ServiceBuilder serviceProviderSelector(ServiceProviderSelector serviceProviderSelector) {
        this.serviceProviderSelector = serviceProviderSelector;
        return this;
    }

    public InvocationExceptionHandler invocationExceptionHandler() {
        return invocationExceptionHandler;
    }

    public ServiceBuilder invocationExceptionHandler(InvocationExceptionHandler invocationExceptionHandler) {
        this.invocationExceptionHandler = invocationExceptionHandler;
        return this;
    }

    public String serviceName() {
        return serviceName;
    }

    public ServiceBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public boolean closeable() {
        return closeable;
    }

    public ServiceBuilder closeable(boolean closeable) {
        this.closeable = closeable;
        return this;
    }
}
