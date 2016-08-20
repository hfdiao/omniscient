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

package im.dhf.omniscient.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;
import im.dhf.omniscient.OmniscientException;
import im.dhf.omniscient.Service;
import im.dhf.omniscient.ServiceNotFoundException;
import im.dhf.omniscient.ServiceProvider;
import im.dhf.omniscient.registry.ServiceRegister;
import im.dhf.omniscient.registry.ServiceResolver;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static im.dhf.omniscient.Service.DEFAULT_VERSION;

/**
 *
 * @author dhf
 */
public class ConsulRegistry implements ServiceRegister, ServiceResolver {
    private final ConsulClient consulClient;
    private final long ttl;

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8500;
    private static final long DEFAULT_TTL = 16000L;

    private static final String VERSION_TAG_PREFIX = "version: ";

    public ConsulRegistry() {
        this(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_TTL);
    }

    public ConsulRegistry(String host, int port) {
        this(host, port, DEFAULT_TTL);
    }


    public ConsulRegistry(String host, int port, long ttl) {
        if (null != host) {
            host = host.trim();
        }
        if (null == host || host.isEmpty()) {
            host = DEFAULT_HOST;
        }
        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        if (ttl <= 0) {
            ttl = DEFAULT_TTL;
        }

        this.consulClient = new ConsulClient(host, port);
        this.ttl = ttl;
    }

    @Override
    public void register(ServiceProvider serviceProvider) throws OmniscientException {
        NewService newService = createServiceDef(serviceProvider, ttl);
        try {
            consulClient.agentServiceRegister(newService);
        } catch (Exception e) {
            throw new OmniscientException(e);
        }
    }

    @Override
    public void deregister(String serviceId) throws OmniscientException {
        try {
            consulClient.agentServiceDeregister(serviceId);
        } catch (Exception e) {
            throw new OmniscientException(e);
        }
    }

    @Override
    public void keepAlive(String serviceId) throws OmniscientException {
        try {
            String checkId = getCheckId(serviceId);
            consulClient.agentCheckPass(checkId);
        } catch (OperationException e) {
            throw translate(e);
        }
    }

    @Override
    public List<ServiceProvider> resolve(String serviceName) throws OmniscientException {
        try {
            Response<List<HealthService>> response = consulClient.getHealthServices(serviceName, true, QueryParams.DEFAULT);
            List<HealthService> healthServices = response.getValue();
            List<ServiceProvider> providers = new LinkedList<>();
            for (HealthService healthService : healthServices) {
                HealthService.Service service = healthService.getService();

                String serviceVersion = getVersionFromTags(service.getTags());
                String serviceId = service.getId();
                Service s = new Service(serviceName, serviceVersion, serviceId);
                ServiceProvider provider = new ServiceProvider(s);
                provider.setHost(service.getAddress());
                provider.setPort(service.getPort());
                providers.add(provider);
            }
            return providers;
        } catch (OperationException e) {
            throw translate(e);
        }
    }

    private static NewService createServiceDef(ServiceProvider serviceProvider, long ttl) {
        NewService newService = new NewService();
        newService.setAddress(serviceProvider.getHost());
        newService.setPort(serviceProvider.getPort());
        newService.setId(serviceProvider.getService().getId());
        newService.setName(serviceProvider.getService().getName());
        String version = makeVersionTag(serviceProvider.getService());
        newService.setTags(Collections.singletonList(version));
        // check
        int ttlInSeconds = (int) ttl / 1000;
        NewService.Check check = new NewService.Check();
        check.setTtl(ttlInSeconds + "s");
        newService.setCheck(check);

        return newService;
    }

    private static String makeVersionTag(Service service) {
        String version = service.getVersion();
        return VERSION_TAG_PREFIX + version;
    }

    private static boolean isVersionTag(String tag) {
        return tag != null && tag.startsWith(VERSION_TAG_PREFIX);
    }

    private static String getVersionFromTag(String tag) {
        if (!isVersionTag(tag)) {
            return null;
        }

        return tag.substring(VERSION_TAG_PREFIX.length());
    }

    private static String getVersionFromTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return DEFAULT_VERSION;
        }
        for (String tag : tags) {
            String version = getVersionFromTag(tag);
            if (version != null) {
                return version;
            }
        }
        return DEFAULT_VERSION;
    }

    private static String getCheckId(String serviceId) {
        return "service:" + serviceId;
    }

    private static OmniscientException translate(OperationException e) {
        String serviceNotFoundContent = "CheckID does not have associated TTL";
        if (serviceNotFoundContent.equals(e.getStatusContent())) {
            return new ServiceNotFoundException(e);
        } else {
            return new OmniscientException(e);
        }
    }
}
