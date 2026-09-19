package com.example.lab.module.auth.presentation;

import com.example.lab.global.web.ApiSuccessResponse;
import com.example.lab.module.auth.infra.security.AuthCookieFactory;
import com.example.lab.module.auth.infra.security.AuthenticatedAccountId;
import com.example.lab.module.auth.usecase.ChangePasswordUseCase;
import com.example.lab.module.auth.usecase.WithdrawAccountUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class AccountSecurityController {

    private final ChangePasswordUseCase changePasswordUseCase;
    private final WithdrawAccountUseCase withdrawAccountUseCase;
    private final AuthCookieFactory authCookieFactory;

    public AccountSecurityController(
            ChangePasswordUseCase changePasswordUseCase,
            WithdrawAccountUseCase withdrawAccountUseCase,
            AuthCookieFactory authCookieFactory) {
        this.changePasswordUseCase = changePasswordUseCase;
        this.withdrawAccountUseCase = withdrawAccountUseCase;
        this.authCookieFactory = authCookieFactory;
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiSuccessResponse<Void>> changePassword(
            @AuthenticatedAccountId long accountId, @Valid @RequestBody ChangePasswordRequest request) {
        String currentPassword = request.currentPassword();
        String newPassword = request.newPassword();
        changePasswordUseCase.execute(accountId, currentPassword, newPassword);
        return expiredAuthenticationResponse();
    }

    @DeleteMapping
    public ResponseEntity<ApiSuccessResponse<Void>> withdraw(
            @AuthenticatedAccountId long accountId, @Valid @RequestBody WithdrawRequest request) {
        String currentPassword = request.currentPassword();
        withdrawAccountUseCase.execute(accountId, currentPassword);
        return expiredAuthenticationResponse();
    }

    private ResponseEntity<ApiSuccessResponse<Void>> expiredAuthenticationResponse() {
        ResponseCookie expiredAccessToken = authCookieFactory.expireAccessToken();
        ResponseCookie expiredRefreshToken = authCookieFactory.expireRefreshToken();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, expiredAccessToken.toString());
        headers.add(HttpHeaders.SET_COOKIE, expiredRefreshToken.toString());
        ApiSuccessResponse<Void> response = ApiSuccessResponse.empty();
        return ResponseEntity.ok().headers(headers).body(response);
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword, @NotBlank String newPassword) {}

    public record WithdrawRequest(@NotBlank String currentPassword) {}
}
