package ar.edu.utn.frre.m3.adapter.security;
import ar.edu.utn.frre.m3.adapter.persistence.entity.DriverJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataDriverRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:m3_security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @ActiveProfiles({"test","production"}) @Transactional
class ProductionSecurityTest {
    static final RSAKey KEY; static final HttpServer JWKS; static final String ISSUER="https://m1.example.test/realms/m3";
    static {
        try {
            KEY=new RSAKeyGenerator(2048).keyID("test-key").generate();
            JWKS=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            JWKS.createContext("/jwks",exchange->{
                byte[] body=new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
            });JWKS.start();
        } catch(Exception ex) { throw new ExceptionInInitializerError(ex); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("OIDC_ISSUER",()->ISSUER);r.add("OIDC_JWKS_URI",()->"http://127.0.0.1:"+JWKS.getAddress().getPort()+"/jwks");
    }
    @AfterAll static void stop() { JWKS.stop(0); }
    @Autowired MockMvc mvc;
    @Autowired SpringDataDriverRepository drivers;
    UUID driver; String path;
    @BeforeEach void seed() {
        driver=UUID.randomUUID();path="/api/v1/drivers/"+driver;
        drivers.saveAndFlush(new DriverJpaEntity(driver,"owner","Security test","PENDING",1L,Instant.now(),Instant.now()));
    }
    String token(String sub,String audience,String scope,String role,Instant expires,RSAKey key) throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer(ISSUER).subject(sub).audience(audience).issueTime(Date.from(Instant.now())).expirationTime(Date.from(expires))
            .claim("scope",scope).claim("typ","Bearer").claim("realm_access",Map.of("roles",role.isBlank()?List.of():List.of(role))).build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY.getKeyID()).build(),claims);jwt.sign(new RSASSASigner(key));return jwt.serialize();
    }
    String token(String sub,String role) throws Exception { return token(sub,"m3-service","",role,Instant.now().plusSeconds(300),KEY); }
    @Test void requiresValidSignatureAudienceExpiryAndIgnoresDevelopmentHeaders() throws Exception {
        mvc.perform(get(path).header("X-Dev-Subject","owner").header("X-Dev-Roles","ADMIN")).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(get(path).header("Authorization","Bearer "+token("owner","DRIVER"))).andExpect(status().isOk());
        for(String invalid:List.of(token("owner","wrong-audience","","DRIVER",Instant.now().plusSeconds(300),KEY),
                token("owner","m3-service","","DRIVER",Instant.now().minusSeconds(300),KEY),
                token("owner","m3-service","","DRIVER",Instant.now().plusSeconds(300),new RSAKeyGenerator(2048).generate())))
            mvc.perform(get(path).header("Authorization","Bearer "+invalid)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization","Bearer "+token("stranger","DRIVER")).header("X-Dev-Subject","owner").header("X-Dev-Roles","ADMIN")).andExpect(status().isForbidden());
    }
    @Test void technicalScopesAndHumanRolesAreSeparated() throws Exception {
        String technical=token("m4","m3-service","m3:eligibility:read","",Instant.now().plusSeconds(300),KEY);
        mvc.perform(get("/api/v1/integration/drivers/"+driver+"/eligibility").header("Authorization","Bearer "+technical)).andExpect(status().isOk());
        mvc.perform(get(path).header("Authorization","Bearer "+technical)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/integration/drivers/"+driver+"/eligibility").header("Authorization","Bearer "+token("owner","DRIVER"))).andExpect(status().isForbidden());
    }
    @Test void webRequiresLoginUsesPkceAndRejectsMutationsWithoutCsrf() throws Exception {
        mvc.perform(get("/ui/profile").header("X-Dev-Subject","owner")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/oauth2/authorization/keycloak")).andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location",containsString("code_challenge="))).andExpect(header().string("Location",containsString("code_challenge_method=S256")));
        mvc.perform(post("/ui/register").param("name","Intruso")).andExpect(status().isForbidden());
        mvc.perform(get("/css/workspace.css")).andExpect(status().isOk());
    }
}
