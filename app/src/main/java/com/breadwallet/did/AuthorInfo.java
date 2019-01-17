package com.breadwallet.did;

public class AuthorInfo {
    private String nickName;
    private String did;
    private String PK;
    private long authorTime;
    private String appName;
    private String appIcon;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getDid() {
        return did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String getPK() {
        return PK;
    }

    public void setPK(String PK) {
        this.PK = PK;
    }

    public long getAuthorTime() {
        return authorTime;
    }

    public void setAuthorTime(long authorTime) {
        this.authorTime = authorTime;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(String appIcon) {
        this.appIcon = appIcon;
    }
}
