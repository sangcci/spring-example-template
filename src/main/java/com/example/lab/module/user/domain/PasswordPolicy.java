package com.example.lab.module.user.domain;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    private static final int MINIMUM_PASSWORD_LENGTH = 8;
    private static final int MAXIMUM_PASSWORD_LENGTH = 16;
    private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHARACTER = Pattern.compile(".*[!@#$%^&*()\\-_=+\\[{\\]}\\\\|;:'\",<.>/?].*");
    private static final Pattern WHITESPACE = Pattern.compile(".*\\s.*");

    public boolean isValid(String password) {
        return password.length() >= MINIMUM_PASSWORD_LENGTH
                && password.length() <= MAXIMUM_PASSWORD_LENGTH
                && UPPERCASE.matcher(password).matches()
                && LOWERCASE.matcher(password).matches()
                && DIGIT.matcher(password).matches()
                && SPECIAL_CHARACTER.matcher(password).matches()
                && !WHITESPACE.matcher(password).matches();
    }
}
