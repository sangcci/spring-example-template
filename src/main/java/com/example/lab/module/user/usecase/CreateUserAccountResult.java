package com.example.lab.module.user.usecase;

import com.example.lab.module.user.domain.UserRole;

public record CreateUserAccountResult(long accountId, UserRole role) {}
