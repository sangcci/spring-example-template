package com.example.lab.module.user.infra.persistence;

import static com.example.lab.generated.jooq.tables.UserAccount.USER_ACCOUNT;

import com.example.lab.module.user.usecase.UserAccount;
import com.example.lab.module.user.usecase.UserAccountStatus;
import com.example.lab.module.user.usecase.UserRole;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
public class UserAccountMapper {

    private final DSLContext dsl;

    public UserAccountMapper(DSLContext dsl) {
        this.dsl = dsl;
    }

    public long insert(String email, String passwordHash, Instant now) {
        OffsetDateTime timestamp = now.atOffset(ZoneOffset.UTC);
        Long accountId = dsl.insertInto(USER_ACCOUNT)
                .set(USER_ACCOUNT.EMAIL, email)
                .set(USER_ACCOUNT.PASSWORD_HASH, passwordHash)
                .set(USER_ACCOUNT.STATUS, UserAccountStatus.ACTIVE.name())
                .set(USER_ACCOUNT.ROLE, UserRole.USER.name())
                .set(USER_ACCOUNT.CREATED_AT, timestamp)
                .set(USER_ACCOUNT.UPDATED_AT, timestamp)
                .returningResult(USER_ACCOUNT.ID)
                .fetchOne(USER_ACCOUNT.ID);
        return accountId;
    }

    public Optional<UserAccount> findByEmail(String email) {
        return dsl.select(
                        USER_ACCOUNT.ID,
                        USER_ACCOUNT.EMAIL,
                        USER_ACCOUNT.PASSWORD_HASH,
                        USER_ACCOUNT.STATUS,
                        USER_ACCOUNT.ROLE,
                        USER_ACCOUNT.RESTRICTION_ENDS_AT)
                .from(USER_ACCOUNT)
                .where(USER_ACCOUNT.EMAIL.eq(email))
                .and(USER_ACCOUNT.STATUS.ne(UserAccountStatus.WITHDRAWN.name()))
                .fetchOptional(record -> new UserAccount(
                        record.get(USER_ACCOUNT.ID),
                        record.get(USER_ACCOUNT.EMAIL),
                        record.get(USER_ACCOUNT.PASSWORD_HASH),
                        UserAccountStatus.valueOf(record.get(USER_ACCOUNT.STATUS)),
                        UserRole.valueOf(record.get(USER_ACCOUNT.ROLE)),
                        toInstant(record.get(USER_ACCOUNT.RESTRICTION_ENDS_AT))));
    }

    public Optional<UserAccount> findById(long accountId) {
        return dsl.select(
                        USER_ACCOUNT.ID,
                        USER_ACCOUNT.EMAIL,
                        USER_ACCOUNT.PASSWORD_HASH,
                        USER_ACCOUNT.STATUS,
                        USER_ACCOUNT.ROLE,
                        USER_ACCOUNT.RESTRICTION_ENDS_AT)
                .from(USER_ACCOUNT)
                .where(USER_ACCOUNT.ID.eq(accountId))
                .fetchOptional(record -> new UserAccount(
                        record.get(USER_ACCOUNT.ID),
                        record.get(USER_ACCOUNT.EMAIL),
                        record.get(USER_ACCOUNT.PASSWORD_HASH),
                        UserAccountStatus.valueOf(record.get(USER_ACCOUNT.STATUS)),
                        UserRole.valueOf(record.get(USER_ACCOUNT.ROLE)),
                        toInstant(record.get(USER_ACCOUNT.RESTRICTION_ENDS_AT))));
    }

    public boolean existsRecentlyWithdrawn(String email, Instant since) {
        OffsetDateTime withdrawnSince = since.atOffset(ZoneOffset.UTC);
        return dsl.fetchExists(dsl.selectOne()
                .from(USER_ACCOUNT)
                .where(USER_ACCOUNT.EMAIL.eq(email))
                .and(USER_ACCOUNT.STATUS.eq(UserAccountStatus.WITHDRAWN.name()))
                .and(USER_ACCOUNT.WITHDRAWN_AT.ge(withdrawnSince)));
    }

    public boolean activateExpiredRestriction(long accountId, Instant now) {
        OffsetDateTime timestamp = now.atOffset(ZoneOffset.UTC);
        int updated = dsl.update(USER_ACCOUNT)
                .set(USER_ACCOUNT.STATUS, UserAccountStatus.ACTIVE.name())
                .set(USER_ACCOUNT.RESTRICTION_REASON, (String) null)
                .set(USER_ACCOUNT.RESTRICTION_ENDS_AT, (OffsetDateTime) null)
                .set(USER_ACCOUNT.UPDATED_AT, timestamp)
                .where(USER_ACCOUNT.ID.eq(accountId))
                .and(USER_ACCOUNT.STATUS.eq(UserAccountStatus.RESTRICTED.name()))
                .and(USER_ACCOUNT.RESTRICTION_ENDS_AT.isNotNull())
                .and(USER_ACCOUNT.RESTRICTION_ENDS_AT.le(timestamp))
                .execute();
        return updated == 1;
    }

    public boolean updatePassword(long accountId, String currentPasswordHash, String newPasswordHash, Instant now) {
        OffsetDateTime timestamp = now.atOffset(ZoneOffset.UTC);
        int updated = dsl.update(USER_ACCOUNT)
                .set(USER_ACCOUNT.PASSWORD_HASH, newPasswordHash)
                .set(USER_ACCOUNT.UPDATED_AT, timestamp)
                .where(USER_ACCOUNT.ID.eq(accountId))
                .and(USER_ACCOUNT.STATUS.eq(UserAccountStatus.ACTIVE.name()))
                .and(USER_ACCOUNT.PASSWORD_HASH.eq(currentPasswordHash))
                .execute();
        return updated == 1;
    }

    public boolean withdraw(long accountId, Instant now) {
        OffsetDateTime timestamp = now.atOffset(ZoneOffset.UTC);
        int updated = dsl.update(USER_ACCOUNT)
                .set(USER_ACCOUNT.STATUS, UserAccountStatus.WITHDRAWN.name())
                .set(USER_ACCOUNT.WITHDRAWN_AT, timestamp)
                .set(USER_ACCOUNT.UPDATED_AT, timestamp)
                .where(USER_ACCOUNT.ID.eq(accountId))
                .and(USER_ACCOUNT.STATUS.eq(UserAccountStatus.ACTIVE.name()))
                .execute();
        return updated == 1;
    }

    private Instant toInstant(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toInstant();
    }
}
