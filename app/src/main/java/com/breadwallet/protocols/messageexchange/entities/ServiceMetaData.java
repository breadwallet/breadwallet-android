package com.breadwallet.protocols.messageexchange.entities;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
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
public class ServiceMetaData extends MetaData {
    private String mUrl;
    private String mName;
    private String mHash;
    private String mCreatedTime;
    private String mUpdatedTime;
    private String mLogoUrl;
    private String mDescription;
    private List<String> mDomains;
    private List<Capability> mCapabilities;

    public static final Creator<ServiceMetaData> CREATOR = new Creator<ServiceMetaData>() {
        @Override
        public ServiceMetaData[] newArray(int size) {
            return new ServiceMetaData[size];
        }

        @Override
        public ServiceMetaData createFromParcel(Parcel source) {
            return new ServiceMetaData(source);
        }
    };

    public ServiceMetaData(String url, String name, String hash, String createdTime, String updatedTime,
                           String logoUrl, String description, List<String> domains, List<Capability> capabilities) {
        super(""); // id Not used
        mUrl = url;
        mName = name;
        mHash = hash;
        mCreatedTime = createdTime;
        mUpdatedTime = updatedTime;
        mLogoUrl = logoUrl;
        mDescription = description;
        mDomains = domains;
        mCapabilities = capabilities;
    }

    public ServiceMetaData(Parcel source) {
        super(source);
        mUrl = source.readString();
        mName = source.readString();
        mHash = source.readString();
        mCreatedTime = source.readString();
        mUpdatedTime = source.readString();
        mLogoUrl = source.readString();
        mDescription = source.readString();
        mDomains = new ArrayList<>();
        source.readStringList(mDomains);
        mCapabilities = new ArrayList<>();
        source.readList(mCapabilities, Capability.class.getClassLoader());
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getHash() {
        return mHash;
    }

    public void setHash(String hash) {
        mHash = hash;
    }

    public String getCreatedTime() {
        return mCreatedTime;
    }

    public void setCreatedTime(String createdTime) {
        mCreatedTime = mCreatedTime;
    }

    public String getUpdatedTime() {
        return mUpdatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        mUpdatedTime = mUpdatedTime;
    }

    public String getLogoUrl() {
        return mLogoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        mLogoUrl = mLogoUrl;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = mDescription;
    }

    public List<String> getDomains() {
        return mDomains;
    }

    public void setDomains(List<String> domains) {
        mDomains = mDomains;
    }

    public List<Capability> getCapabilities() {
        return mCapabilities;
    }

    public void setCapabilities(List<Capability> capabilities) {
        mCapabilities = mCapabilities;
    }

    @Override
    public void writeToParcel(Parcel destination) {
        destination.writeString(mUrl);
        destination.writeString(mName);
        destination.writeString(mHash);
        destination.writeString(mCreatedTime);
        destination.writeString(mUpdatedTime);
        destination.writeString(mLogoUrl);
        destination.writeString(mDescription);
        destination.writeStringList(mDomains);
        destination.writeList(mCapabilities);
    }

    public static class Capability implements Parcelable {
        private String mName;
        private HashMap<String, String> mScopes;
        private String mDescription;

        public static final Creator<Capability> CREATOR = new Creator<Capability>() {
            @Override
            public Capability[] newArray(int size) {
                return new Capability[size];
            }

            @Override
            public Capability createFromParcel(Parcel source) {
                return new Capability(source);
            }
        };

        public Capability(String name, HashMap<String, String> scopes, String description) {
            mName = name;
            mScopes = scopes;
            mDescription = description;
        }

        public Capability(Parcel source) {
            mName = source.readString();
            mScopes = new HashMap<>();
            source.readMap(mScopes, String.class.getClassLoader());
            mDescription = source.readString();
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public Map<String, String> getScopes() {
            return mScopes;
        }

        public void setScopes(HashMap<String, String> scopes) {
            mScopes = scopes;
        }

        public String getDescription() {
            return mDescription;
        }

        public void setDescription(String description) {
            mDescription = description;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel destination, int flags) {
            destination.writeString(mName);
            destination.writeMap(mScopes);
            destination.writeString(mDescription);
        }
    }
}
