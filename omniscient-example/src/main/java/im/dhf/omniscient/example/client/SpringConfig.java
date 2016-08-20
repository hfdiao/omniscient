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

package im.dhf.omniscient.example.client;

import com.facebook.swift.service.RuntimeTApplicationException;
import im.dhf.omniscient.example.idl.GreetService;
import im.dhf.omniscient.proxy.JavaServiceProxy;
import im.dhf.omniscient.proxy.thrift.poolable.PoolableThriftServiceProxy;
import im.dhf.omniscient.proxy.util.InvocationExceptionHandler;
import im.dhf.omniscient.proxy.util.ServiceBuilder;
import im.dhf.omniscient.registry.ServiceResolver;
import im.dhf.omniscient.registry.consul.ConsulRegistry;
import org.apache.thrift.TApplicationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author dhf
 */
@Configuration
public class SpringConfig {

    @Bean
    public GreetService greetService(JavaServiceProxy serviceProxy, ServiceResolver serviceResolver, InvocationExceptionHandler invocationExceptionHandler) {
        ServiceBuilder builder = new ServiceBuilder();
        return (GreetService) builder.targetClass(GreetService.class).serviceProxy(serviceProxy).serviceResolver(serviceResolver).invocationExceptionHandler(invocationExceptionHandler).build();
    }

    @Bean
    public JavaServiceProxy serviceProxy() {
        return new PoolableThriftServiceProxy();
    }

    @Bean
    public ServiceResolver serviceResolver() {
        return new ConsulRegistry(consulHost(), consulPort());
    }

    @Bean
    public InvocationExceptionHandler invocationExceptionHandler() {
        return exception -> {
            TApplicationException appException = null;
            if (exception instanceof RuntimeTApplicationException) {
                RuntimeTApplicationException runtimeEx = (RuntimeTApplicationException) exception;
                appException = (TApplicationException) runtimeEx.getCause();
            } else if (exception instanceof TApplicationException) {
                appException = (TApplicationException) exception;
            }

            if (null == appException) {
                throw exception;
            }
            if (appException.getType() != TApplicationException.MISSING_RESULT) {
                throw exception;
            }
            return null;
        };
    }

    public String consulHost() {
        return "127.0.0.1";
    }

    public int consulPort() {
        return 8500;
    }
}
