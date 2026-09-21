package com.example.lab.module.auth.presentation;

import com.example.lab.global.error.ApplicationException;
import com.example.lab.global.web.ApiSuccessResponse;
import com.example.lab.module.auth.infra.security.AuthCookieFactory;
import com.example.lab.module.auth.infra.security.AuthProperties;
import com.example.lab.module.auth.usecase.AuthErrorCode;
import com.example.lab.module.auth.usecase.AuthenticationResult;
import com.example.lab.module.auth.usecase.LoginUseCase;
import com.example.lab.module.auth.usecase.LogoutUseCase;
import com.example.lab.module.auth.usecase.RefreshAuthenticationUseCase;
import com.example.lab.module.auth.usecase.SignUpUseCase;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SignUpUseCase signUpUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshAuthenticationUseCase refreshAuthenticationUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuthCookieFactory authCookieFactory;
    private final AuthProperties authProperties;

    public AuthController(
            SignUpUseCase signUpUseCase,
            LoginUseCase loginUseCase,
            RefreshAuthenticationUseCase refreshAuthenticationUseCase,
            LogoutUseCase logoutUseCase,
            AuthCookieFactory authCookieFactory,
            AuthProperties authProperties) {
        this.signUpUseCase = signUpUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshAuthenticationUseCase = refreshAuthenticationUseCase;
        this.logoutUseCase = logoutUseCase;
        this.authCookieFactory = authCookieFactory;
        this.authProperties = authProperties;
    }

    @GetMapping("/csrf")
    public ApiSuccessResponse<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ApiSuccessResponse.empty();
    }

    @PostMapping("/sign-up")
    public ResponseEntity<ApiSuccessResponse<AuthenticationResponse>> signUp(
            @Valid @RequestBody SignUpRequest request) {
        String email = request.email();
        String password = request.password();
        boolean rememberMe = request.rememberMe();
        AuthenticationResult authentication = signUpUseCase.execute(email, password, rememberMe);
        return authenticationResponse(authentication);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiSuccessResponse<AuthenticationResponse>> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email();
        String password = request.password();
        boolean rememberMe = request.rememberMe();
        AuthenticationResult authentication = loginUseCase.execute(email, password, rememberMe);
        return authenticationResponse(authentication);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiSuccessResponse<AuthenticationResponse>> refresh(HttpServletRequest request) {
        String refreshToken = findRefreshToken(request);
        if (refreshToken == null) {
            throw new ApplicationException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        AuthenticationResult authentication = refreshAuthenticationUseCase.execute(refreshToken);
        return authenticationResponse(authentication);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiSuccessResponse<Void>> logout(HttpServletRequest request) {
        String refreshToken = findRefreshToken(request);
        if (refreshToken != null) {
            logoutUseCase.execute(refreshToken);
        }

        ResponseCookie expiredAccessToken = authCookieFactory.expireAccessToken();
        ResponseCookie expiredRefreshToken = authCookieFactory.expireRefreshToken();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, expiredAccessToken.toString());
        headers.add(HttpHeaders.SET_COOKIE, expiredRefreshToken.toString());
        ApiSuccessResponse<Void> response = ApiSuccessResponse.empty();
        return ResponseEntity.ok().headers(headers).body(response);
    }

    private ResponseEntity<ApiSuccessResponse<AuthenticationResponse>> authenticationResponse(
            AuthenticationResult authentication) {
        String accessTokenValue = authentication.accessToken();
        var accessExpiresAt = authentication.accessExpiresAt();
        ResponseCookie accessToken = authCookieFactory.accessToken(accessTokenValue, accessExpiresAt);
        String refreshTokenValue = authentication.refreshToken();
        var refreshExpiresAt = authentication.refreshExpiresAt();
        ResponseCookie refreshToken = authCookieFactory.refreshToken(refreshTokenValue, refreshExpiresAt);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, accessToken.toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshToken.toString());
        AuthenticationResponse result = AuthenticationResponse.from(authentication);
        ApiSuccessResponse<AuthenticationResponse> response = ApiSuccessResponse.of(result);
        return ResponseEntity.ok().headers(headers).body(response);
    }

    private String findRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (authProperties.refreshTokenCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public record SignUpRequest(
            @NotBlank String email, @NotBlank String password, boolean rememberMe) {}

    public record LoginRequest(
            @NotBlank String email, @NotBlank String password, boolean rememberMe) {}
}
