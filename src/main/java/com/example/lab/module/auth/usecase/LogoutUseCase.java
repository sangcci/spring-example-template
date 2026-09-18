package com.example.lab.module.auth.usecase;

import com.example.lab.module.auth.infra.persistence.RefreshSession;
import com.example.lab.module.auth.infra.persistence.RefreshSessionStore;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class LogoutUseCase {

    private final RefreshSessionStore refreshSessionStore;

    public LogoutUseCase(RefreshSessionStore refreshSessionStore) {
        this.refreshSessionStore = refreshSessionStore;
    }

    public void execute(String rawRefreshToken) {
        Optional<RefreshSession> sessionResult = refreshSessionStore.find(rawRefreshToken);
        if (sessionResult.isPresent()) {
            refreshSessionStore.revokeFamily(sessionResult.get());
        }
    }
}
