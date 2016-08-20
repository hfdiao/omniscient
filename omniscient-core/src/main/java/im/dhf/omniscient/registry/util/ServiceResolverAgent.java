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

package im.dhf.omniscient.registry.util;

import im.dhf.omniscient.OmniscientException;
import im.dhf.omniscient.ServiceProvider;
import im.dhf.omniscient.registry.ServiceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author dhf
 */
public class ServiceResolverAgent implements ServiceResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceResolverAgent.class);

    private final ServiceResolver resolver;
    private final long interval;
    private final Object guard = new byte[0];

    private volatile ConcurrentHashMap<String, List<ServiceProvider>> providerMap = new ConcurrentHashMap<>();
    private Thread refresher;

    public ServiceResolverAgent(ServiceResolver resolver, long refreshInterval) {
        Objects.requireNonNull(resolver);

        this.resolver = resolver;
        this.interval = refreshInterval;
    }

    public void init() {
        if (null != refresher) {
            refresher.interrupt();
        }
        refresher = new RefreshThread();
        refresher.start();
    }

    public void destroy() {
        if (null != refresher) {
            refresher.interrupt();
        }
        refresher = null;
    }

    public List<ServiceProvider> resolve(String serviceName) throws OmniscientException {
        if (null == serviceName || serviceName.isEmpty()) {
            return new LinkedList<>();
        }

        List<ServiceProvider> result = providerMap.get(serviceName);
        if (null == result) {
            result = doResolve(serviceName);
        }

        return result;
    }

    private List<ServiceProvider> doResolve(String serviceName) {
        Objects.requireNonNull(serviceName);

        List<ServiceProvider> result = resolver.resolve(serviceName);
        if (null == result) {
            result = new LinkedList<>();
        }
        providerMap.put(serviceName, result);
        return result;
    }

    private class RefreshThread extends Thread {
        public RefreshThread() {
            setDaemon(true);
        }

        @Override
        public void run() {
            LOGGER.info("start refreshing");

            while (true) {
                Collection<String> serviceNames = new LinkedList<>(providerMap.keySet());
                for (String serviceName : serviceNames) {
                    try {
                        doResolve(serviceName);
                    } catch (Exception e) {
                        LOGGER.warn("resolve service provider fail, serviceName: " + serviceName, e);
                    }
                }

                synchronized (guard) {
                    try {
                        guard.wait(interval);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }

            LOGGER.info("stop refreshing");
        }
    }
}
