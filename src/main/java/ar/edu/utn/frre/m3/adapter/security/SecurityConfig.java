package ar.edu.utn.frre.m3.adapter.security;
import ar.edu.utn.frre.m3.application.usecase.DriverAccessService;
import ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.user.*;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Value("${app.security.issuer:${OIDC_ISSUER:}}") private String issuer;
    @Value("${app.security.jwks-uri:${OIDC_JWKS_URI:}}") private String jwks;
    @Value("${app.security.audience:${OIDC_AUDIENCE:account}}") private String audience;
    @Value("${OIDC_WEB_CLIENT_ID:m3-web-client}") private String webClient;
    @Bean @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(DevelopmentMode mode) {
        if(issuer.isBlank() || jwks.isBlank()) {
            if(mode.enabled()) return token->{throw new BadJwtException("M1 JWT no está configurado en este entorno local.");};
            throw new IllegalStateException("Configurar OIDC_ISSUER y OIDC_JWKS_URI para autenticar tokens de M1.");
        }
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withJwkSetUri(jwks).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),jwt->{
            boolean valid=jwt.getAudience().contains(audience) && jwt.getSubject()!=null && !jwt.getSubject().isBlank()
                    && jwt.getExpiresAt()!=null && !"ID".equalsIgnoreCase(jwt.getClaimAsString("typ"));
            return valid?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Audiencia o tipo de token inválido",null));
        }));return decoder;
    }
    private Collection<GrantedAuthority> authorities(Map<String,Object> claims,String clientId) {
        Set<GrantedAuthority> result=new HashSet<>();
        Object scope=claims.get("scope");if(scope instanceof String text) for(String s:text.split(" ")) if(!s.isBlank())result.add(new SimpleGrantedAuthority("SCOPE_"+s));
        addRoles(result,claims.get("roles"));
        addRoles(result,claims.get("realm_access"));
        if(claims.get("resource_access") instanceof Map<?,?> resources) addRoles(result,resources.get(clientId));
        return result;
    }
    private void addRoles(Set<GrantedAuthority> result,Object value) {
        if(value instanceof Map<?,?> map) {
            addRoles(result,map.get("roles"));
        } else if(value instanceof Collection<?> roles) {
            for(Object role:roles) addRoles(result,role);
        } else if(value instanceof String role && !role.isBlank()) {
            result.add(new SimpleGrantedAuthority("ROLE_"+role.replaceFirst("^ROLE_", "").toUpperCase(Locale.ROOT)));
        }
    }
    @Bean @Order(1)
    public SecurityFilterChain api(HttpSecurity http,DevelopmentMode mode,JwtDecoder decoder,DriverAccessService access,CurrentPrincipalPort principal) throws Exception {
        JwtAuthenticationConverter converter=new JwtAuthenticationConverter();converter.setJwtGrantedAuthoritiesConverter(jwt->authorities(jwt.getClaims(),audience));
        http.securityMatcher("/api/**").csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->{if(mode.enabled())a.anyRequest().permitAll();else a.anyRequest().authenticated();})
            .oauth2ResourceServer(o->o.jwt(j->j.decoder(decoder).jwtAuthenticationConverter(converter)))
            .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->writeProblem(s,401)).accessDeniedHandler((r,s,x)->writeProblem(s,403)))
            .addFilterAfter(new DriverAuthorizationFilter(access,principal,mode),AuthorizationFilter.class);
        return http.build();
    }
    @Bean @Order(2)
    public SecurityFilterChain web(HttpSecurity http,DevelopmentMode mode,ClientRegistrationRepository registrations,
            DriverAccessService access,CurrentPrincipalPort principal, DemoAccessAuthenticationFilter demoAccess) throws Exception {
        http.authorizeHttpRequests(a->{
            a.requestMatchers("/","/health/**","/actuator/health/**","/css/**","/js/**","/images/**","/vendor/**","/login/**","/oauth2/**","/demo/access","/demo/operator","/error").permitAll();
            if(mode.enabled()) a.anyRequest().permitAll();
            else a.requestMatchers("/ui/operator/**").hasAnyRole("OPERATOR","OPERADOR","ADMIN","ADMINISTRADOR")
                .requestMatchers("/ui/**","/").authenticated().anyRequest().denyAll();
        });
        if(!mode.enabled()) {
            var resolver=new DefaultOAuth2AuthorizationRequestResolver(registrations,"/oauth2/authorization");
            resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
            var oidc=new OidcUserService();
            http.oauth2Login(o->o.authorizationEndpoint(e->e.authorizationRequestResolver(resolver)).userInfoEndpoint(e->e.oidcUserService(req->{
                OidcUser user=oidc.loadUser(req);var roles=new HashSet<GrantedAuthority>(user.getAuthorities());roles.addAll(authorities(user.getIdToken().getClaims(),webClient));
                return new DefaultOidcUser(roles,user.getIdToken(),user.getUserInfo(),"sub");
            })).successHandler((request,response,authentication)->{
                boolean operator=authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                    .anyMatch(role->role.equals("ROLE_OPERATOR") || role.equals("ROLE_OPERADOR") || role.equals("ROLE_ADMIN") || role.equals("ROLE_ADMINISTRADOR"));
                response.sendRedirect(operator ? "/ui/operator/drivers" : "/ui");
            }));
            var logout=new OidcClientInitiatedLogoutSuccessHandler(registrations);logout.setPostLogoutRedirectUri("{baseUrl}/");
            http.logout(l->l.logoutSuccessHandler(logout).invalidateHttpSession(true).deleteCookies("JSESSIONID"));
        }
        else http.logout(l->l.logoutSuccessUrl("/ui").invalidateHttpSession(true).deleteCookies("JSESSIONID"));
        http.headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; img-src 'self' data:; frame-ancestors 'none';")))
            .addFilterBefore(demoAccess,AuthorizationFilter.class)
            .addFilterAfter(new DriverAuthorizationFilter(access,principal,mode),AuthorizationFilter.class);
        return http.build();
    }
    private void writeProblem(jakarta.servlet.http.HttpServletResponse response,int status) throws java.io.IOException {
        response.setStatus(status);response.setContentType("application/problem+json");
        response.getWriter().write("{\"type\":\"about:blank\",\"status\":"+status+",\"title\":\""+(status==401?"Unauthorized":"Forbidden")+"\"}");
    }
}
