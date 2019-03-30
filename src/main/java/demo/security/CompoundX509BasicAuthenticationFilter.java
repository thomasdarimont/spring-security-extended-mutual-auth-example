package demo.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

public class CompoundX509BasicAuthenticationFilter extends BasicAuthenticationFilter {

    private RememberMeServices rememberMeServices = new NullRememberMeServices();

    private final ThreadLocal<PreAuthenticatedAuthenticationToken> currentX509Auth = new ThreadLocal<>();

    public CompoundX509BasicAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

        // remember preauth if present
        PreAuthenticatedAuthenticationToken x509PreAuth = Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(PreAuthenticatedAuthenticationToken.class::cast)
                .orElse(null);

        SecurityContextHolder.clearContext();

        if (x509PreAuth == null) {
            AuthenticationException failed = new PreAuthenticatedCredentialsNotFoundException("Missing required X509 Client Authentication");

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Authentication request for failed: " + failed);
            }

            this.rememberMeServices.loginFail(request, response);

            onUnsuccessfulAuthentication(request, response, failed);

            if (this.isIgnoreFailure()) {
                chain.doFilter(request, response);
            } else {
                getAuthenticationEntryPoint().commence(request, response, failed);
            }

            return;
        }

        try {
            currentX509Auth.set(x509PreAuth);
            super.doFilterInternal(request, response, chain);
        } finally {
            currentX509Auth.remove();
        }
    }

    @Override
    protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException {

        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(auth -> new CompoundX509AuthenticationToken(auth, currentX509Auth.get()))
                .ifPresent(SecurityContextHolder.getContext()::setAuthentication);
    }

    @Override
    public void setRememberMeServices(RememberMeServices rememberMeServices) {
        this.rememberMeServices = rememberMeServices;
        super.setRememberMeServices(rememberMeServices);
    }
}
