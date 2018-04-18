package id.co.projectscoid.util;

public interface CredentialsProvider {

    public String getUser();
    public String getPassword();
    public String getUserName();
    public String getUserPassword();
    public String getUserId();
    public String getSignalingKey();
}