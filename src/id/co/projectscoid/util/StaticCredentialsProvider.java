package id.co.projectscoid.util;

import id.co.projectscoid.util.CredentialsProvider;

public class StaticCredentialsProvider implements CredentialsProvider {

    private final String user;
    private final String password;
    private final String username;
    private final String userpassword;
    private final String userid;
    private final String signalingKey;

    public StaticCredentialsProvider(String user, String password, String username, String userpassword , String userid, String signalingKey) {
        this.user         = user;
        this.password     = password;
        this.username     = username;
        this.userpassword = userpassword;
        this.userid       = userid;
        this.signalingKey = signalingKey;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUserName() {
        return username;
    }

    @Override
    public String getUserPassword() {
        return userpassword;
    }

    @Override
    public String getUserId() {
        return userid;
    }

    @Override
    public String getSignalingKey() {
        return signalingKey;
    }
}

