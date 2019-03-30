package demo.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CompoundX509AuthenticationToken extends PreAuthenticatedAuthenticationToken {

    private final X509Certificate certificate;

    private final Object preAuthenticatedPrincipal;

    public CompoundX509AuthenticationToken(Authentication authentication, PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken) {
        super(authentication.getPrincipal(), authentication.getCredentials(), combineAuthorities(authentication, preAuthenticatedAuthenticationToken.getAuthorities()));
        this.certificate = (X509Certificate) preAuthenticatedAuthenticationToken.getCredentials();
        this.preAuthenticatedPrincipal = preAuthenticatedAuthenticationToken.getPrincipal();
    }

    private static Collection<? extends GrantedAuthority> combineAuthorities(Authentication authentication, Collection<? extends GrantedAuthority> additionalAuthorities) {
        Set<GrantedAuthority> combined = new HashSet<>();
        combined.addAll(authentication.getAuthorities());
        combined.addAll(additionalAuthorities);
        return combined;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public Object getPreAuthenticatedPrincipal() {
        return preAuthenticatedPrincipal;
    }
}
