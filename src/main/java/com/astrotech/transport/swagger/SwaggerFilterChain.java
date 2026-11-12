package com.astrotech.transport.swagger;



import com.astrotech.transport.configProperties.SwaggerProperties;
import com.astrotech.transport.core.TrimWhiteSpace;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SwaggerFilterChain {
    private final PasswordEncoder passwordEncoder;
    private final SwaggerProperties swaggerProperties;

    public SwaggerFilterChainResponse getSwaggerFilterChain() {
        var email = TrimWhiteSpace.trimWhiteSpaceWithUpperCase(swaggerProperties.username(), false);

        var swaggerUser = User.builder()
                .username(email)
                .password(passwordEncoder.encode(swaggerProperties.password()))
                .roles(swaggerProperties.role())
                .build();

        var swaggerUserDetailsService =
                new InMemoryUserDetailsManager(swaggerUser);

        var swaggerAuthenticationProvider =
                new DaoAuthenticationProvider(swaggerUserDetailsService);
        swaggerAuthenticationProvider.setPasswordEncoder(passwordEncoder);

        var swaggerAuthenticationManager =
                new ProviderManager(swaggerAuthenticationProvider);
        return new SwaggerFilterChainResponse(
                swaggerAuthenticationManager,
                swaggerProperties.role()
        );
    }
}
