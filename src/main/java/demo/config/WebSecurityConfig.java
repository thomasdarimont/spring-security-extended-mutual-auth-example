package demo.config;

import demo.security.CompoundX509BasicAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Slf4j
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${http.client.ssl.key-alias}")
    private String clientCertAlias;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().anyRequest().hasRole("USER")
                .and()
                .x509()
                .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                .userDetailsService(clientUserDetailsService())
                .and()
                .httpBasic()
                .and()
                .userDetailsService(userDetailsService())
                .addFilter(new CompoundX509BasicAuthenticationFilter(authenticationManagerBean()))
        ;
    }


    /**
     * used to lookup allowed clients
     *
     * @return
     */
    @Bean
    public UserDetailsService clientUserDetailsService() {
        return username -> {

            log.info("loadClientByUsername " + username);

            if (username.equals(clientCertAlias)) {
                return new User(username, "",
                        AuthorityUtils
                                .commaSeparatedStringToAuthorityList("ROLE_TRUSTED_CLIENT"));
            }

            return null;
        };
    }

    /**
     * used to lookup allowed users
     *
     * @return
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(User.withUsername("tester").password("{noop}test").roles("USER").build()) {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

                log.info("loadUserByUsername " + username);

                return super.loadUserByUsername(username);
            }
        };
    }

}
