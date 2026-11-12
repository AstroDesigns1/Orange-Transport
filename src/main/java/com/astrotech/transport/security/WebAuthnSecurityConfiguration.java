package com.astrotech.transport.security;

import com.astrotech.transport.OAuth.*;
import com.astrotech.transport.ratelimit.bucketRatelimit.BucketRateLimitFilter;
import com.astrotech.transport.swagger.SwaggerFilterChain;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsFilter;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationProvider;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsFilter;
import org.springframework.security.web.webauthn.registration.WebAuthnRegistrationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.security.enable-webauthn",
        havingValue = "true"
)
public class WebAuthnSecurityConfiguration {
    private static final Logger log =
            LoggerFactory.getLogger(WebAuthnSecurityConfiguration.class);

    private final PublicKeyCredentialCreationOptionsFilter passkeyRegistrationOptionsFilter;
    private final WebAuthnRegistrationFilter passkeyRegistrationFilter;
    private final PublicKeyCredentialRequestOptionsFilter passkeyAuthenticationOptionsFilter;
    private final WebAuthnAuthenticationFilter passkeyAuthenticationFilter;
    private final OAuth2SecurityCustomizer oAuth2SecurityCustomizer;
    private final SecurityFilterChainConfig securityFilterChainConfig;
    private final BucketRateLimitFilter rateLimitFilter;
    private final SwaggerFilterChain swaggerFilterChain;
    private final SecurityCustomizerConfig securityCustomizerConfig;
    private final AuthorizationManagerCustomizerConfig authorizationManagerCustomizerConfig;


    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            WebAuthnRelyingPartyOperations relyingPartyOperations
    ) {
        var daoProvider =
                new DaoAuthenticationProvider(userDetailsService);

        daoProvider.setPasswordEncoder(passwordEncoder);

        var webAuthnProvider =
                new WebAuthnAuthenticationProvider(
                        relyingPartyOperations,
                        userDetailsService
                );


        var providers =
                List.of(
                        daoProvider,
                        webAuthnProvider
                );
        log.info("Authentication providers: {}",
                providers.stream()
                        .map(provider -> provider.getClass().getName())
                        .toList()
        );
        return new ProviderManager(providers);
    }


    @Bean
    @Order(1)
    public SecurityFilterChain adminSwaggerDocsFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/v3/api-docs/admin", "/v3/api-docs/admin/**")
                .authenticationManager(swaggerFilterChain.getSwaggerFilterChain().providerManager())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(securityCustomizerConfig.getSessionManagementCustomizer())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest()
                        .hasRole(
                                swaggerFilterChain.getSwaggerFilterChain().role()
                        )
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {

        return http
                .cors(securityCustomizerConfig.getCorsCustomizer())

                .csrf(securityCustomizerConfig.getCsrfConfigurerCustomizer())

                .sessionManagement(securityCustomizerConfig.getSessionManagementCustomizer())

                .authorizeHttpRequests(authorizationManagerCustomizerConfig.getAuthorizationManagerRequestMatcherRegistryCustomizer())

                .oauth2Login(oAuth2SecurityCustomizer.getOAuth2LoginCustomizer()
                )
                .webAuthn(securityCustomizerConfig.getWebAuthnConfigurerCustomizer())

                .authenticationManager(authenticationManager)

                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)

                .addFilterBefore(securityFilterChainConfig, UsernamePasswordAuthenticationFilter.class)

                .addFilterBefore(passkeyRegistrationOptionsFilter, UsernamePasswordAuthenticationFilter.class)

                .addFilterBefore(passkeyRegistrationFilter, UsernamePasswordAuthenticationFilter.class)

                .addFilterBefore(passkeyAuthenticationOptionsFilter, UsernamePasswordAuthenticationFilter.class)

                .addFilterBefore(passkeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(securityCustomizerConfig.getExceptionHandlingConfigurerCustomizer())

                .headers(securityCustomizerConfig.getHeadersConfigurerCustomizer())

                .build();
    }


}
