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

import im.dhf.omniscient.Service;
import im.dhf.omniscient.ServiceProvider;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 *
 * @author dhf
 */
public class MemRegistryTest {
    private MemRegistry registry = new MemRegistry();

    private String serviceName = "MemRegistryTest";
    private String host = "localhost";
    private int port = 10000;

    @Test
    public void test() throws Exception {
        List<ServiceProvider> providers = registry.resolve(serviceName);
        for (ServiceProvider p : providers) {
            registry.deregister(p.getService().getId());
        }
        providers = registry.resolve(serviceName);
        Assert.assertTrue(providers.isEmpty());

        // register
        ServiceProvider provider = new ServiceProvider(new Service(serviceName), host, port);
        registry.register(provider);
        registry.keepAlive(provider.getService().getId());
        providers = registry.resolve(serviceName);
        Assert.assertTrue(providers.size() == 1);

        // deregister
        registry.deregister(provider.getService().getId());
        providers = registry.resolve(serviceName);
        Assert.assertTrue(providers.isEmpty());
    }
}
