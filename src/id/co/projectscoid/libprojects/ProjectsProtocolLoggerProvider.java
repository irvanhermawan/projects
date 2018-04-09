package id.co.projectscoid.libprojects;


public class ProjectsProtocolLoggerProvider {
    private static ProjectsProtocolLogger provider;

    public static ProjectsProtocolLogger getProvider() {
        return provider;
    }

    public static void setProvider(ProjectsProtocolLogger provider) {
        ProjectsProtocolLoggerProvider.provider = provider;
    }
}
