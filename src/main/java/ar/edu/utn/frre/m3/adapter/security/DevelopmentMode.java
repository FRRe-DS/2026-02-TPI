package ar.edu.utn.frre.m3.adapter.security;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
@Component
public class DevelopmentMode {
    private final boolean enabled;
    public DevelopmentMode(Environment environment) {
        enabled=environment.acceptsProfiles(Profiles.of("local","demo","test")) && !environment.acceptsProfiles(Profiles.of("production","docker"));
    }
    public boolean enabled() { return enabled; }
}
