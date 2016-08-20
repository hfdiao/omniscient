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
import im.dhf.omniscient.ServiceNotFoundException;
import im.dhf.omniscient.ServiceProvider;
import im.dhf.omniscient.registry.ServiceRegister;
import im.dhf.omniscient.registry.ServiceResolver;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author dhf
 */
public class MemRegistry implements ServiceRegister, ServiceResolver {
    private Map<String, List<ServiceProvider>> providerNameMap = new ConcurrentHashMap<>();
    private Map<String, ServiceProvider> providerIdMap = new ConcurrentHashMap<>();

    public MemRegistry() {
    }

    public MemRegistry(List<ServiceProvider> providers) {
        if (null != providers) {
            providers.forEach(this::register);
        }
    }

    @Override
    public void register(ServiceProvider serviceProvider) throws OmniscientException {
        providerIdMap.put(serviceProvider.getService().getId(), serviceProvider);

        List<ServiceProvider> list = new CopyOnWriteArrayList<>();
        list.add(serviceProvider);
        list = providerNameMap.putIfAbsent(serviceProvider.getService().getName(), list);
        if (null != list) {
            list.add(serviceProvider);
        }
    }

    @Override
    public void deregister(String serviceId) throws OmniscientException {
        ServiceProvider serviceProvider = providerIdMap.get(serviceId);
        if (null == serviceProvider) {
            throw new ServiceNotFoundException(serviceId);
        }

        List<ServiceProvider> list = providerNameMap.get(serviceProvider.getService().getName());
        if (null != list) {
            list.remove(serviceProvider);
        }
        providerIdMap.remove(serviceId);
    }

    @Override
    public void keepAlive(String serviceId) throws OmniscientException {
        // do nothing
    }

    @Override
    public List<ServiceProvider> resolve(String serviceName) throws OmniscientException {
        List<ServiceProvider> result = new LinkedList<>();
        List<ServiceProvider> list = providerNameMap.get(serviceName);
        if (null != list) {
            result.addAll(list);
        }
        return result;
    }
}
