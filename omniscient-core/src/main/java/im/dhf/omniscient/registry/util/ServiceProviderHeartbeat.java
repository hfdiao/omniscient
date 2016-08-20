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

import im.dhf.omniscient.ServiceNotFoundException;
import im.dhf.omniscient.ServiceProvider;
import im.dhf.omniscient.registry.ServiceRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 *
 * @author dhf
 */
public class ServiceProviderHeartbeat implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderHeartbeat.class);

    private final ServiceRegister register;
    private final ServiceProvider serviceProvider;
    private final long interval;
    private final Object guard = new byte[0];
    private boolean registered = false;

    public ServiceProviderHeartbeat(ServiceRegister register, ServiceProvider serviceProvider, long interval) {
        Objects.requireNonNull(register);
        Objects.requireNonNull(serviceProvider);

        this.register = register;
        this.serviceProvider = serviceProvider;
        this.interval = interval;
    }

    @Override
    public void run() {
        while (true) {
            if (!registered) {
                registerProvider();
            }
            if (registered) {
                keepAlive();
            }

            synchronized (guard) {
                try {
                    guard.wait(interval);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        deregisterProvider();
    }

    private void keepAlive() {
        String serviceId = null;
        try {
            serviceId = serviceProvider.getService().getId();
            register.keepAlive(serviceId);
            LOGGER.debug("keep alive success, serviceId: {}", serviceId);
        } catch (Exception e) {
            LOGGER.warn("keep alive fail, serviceId: {}", serviceId, e);

            if (e instanceof ServiceNotFoundException) {
                registered = false;
            }
        }
    }

    private void registerProvider() {
        try {
            register.register(serviceProvider);
            registered = true;
            LOGGER.info("register service success. provider: {}", serviceProvider);
        } catch (Exception e) {
            LOGGER.warn("register service failed. provider: {}", serviceProvider, e);
        }
    }

    private void deregisterProvider() {
        try {
            String serviceId = serviceProvider.getService().getId();
            register.deregister(serviceId);
            registered = false;
            LOGGER.info("deregister service success. provider: {}", serviceProvider);
        } catch (Exception e) {
            LOGGER.warn("deregister service failed. provider: {}", serviceProvider, e);
        }
    }
}
