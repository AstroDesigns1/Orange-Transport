package com.astrotech.transport.validators.email.verified;

import com.astrotech.transport.dto.request.AuthenticatedUser;
import com.astrotech.transport.enums.UserRole;
import com.astrotech.transport.exceptions.UnAuthorizedUserException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class EmailVerificationAspect {
    @Around("@annotation(emailVerified)")
    public Object verifyEmailStatus(
            ProceedingJoinPoint joinPoint,
            EmailVerified emailVerified
    ) throws Throwable {

            var authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            var principal =
                    (AuthenticatedUser) authentication.getPrincipal();
            var role = UserRole.valueOf(principal.role());

            if (!principal.emailVerified() && role != UserRole.ADMIN) {
            throw new UnAuthorizedUserException(
                    emailVerified.message()
            );
        }

        return joinPoint.proceed();}

}