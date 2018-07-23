package com.breadwallet.protocol.messageexchange.entities;

import java.util.List;
import java.util.Map;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/18/18.
 * Copyright (c) 2018 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class ServiceObject {
    private String url;
    private String name;
    private String hash;
    private String createdTime;
    private String updatedTime;
    private String logoUrl;
    private String description;
    private List<String> domains;
    private List<Capability> capabilities;

    public ServiceObject(String url, String name, String hash, String createdTime, String updatedTime
            , String logoUrl, String description, List<String> domains, List<Capability> capabilities) {
        this.url = url;
        this.name = name;
        this.hash = hash;
        this.createdTime = createdTime;
        this.updatedTime = updatedTime;
        this.logoUrl = logoUrl;
        this.description = description;
        this.domains = domains;
        this.capabilities = capabilities;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getHash() {
        return hash;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getDomains() {
        return domains;
    }

    public List<Capability> getCapabilities() {
        return capabilities;
    }

    public static class Capability {
        private String name;
        private Map<String, String> scopes;
        private String description;

        public Capability(String name, Map<String, String> scopes, String description) {
            this.name = name;
            this.scopes = scopes;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getScopes() {
            return scopes;
        }

        public String getDescription() {
            return description;
        }
    }
}
