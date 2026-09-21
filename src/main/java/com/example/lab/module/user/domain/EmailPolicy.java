package com.example.lab.module.user.domain;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class EmailPolicy {

    private static final int MAXIMUM_EMAIL_LENGTH = 320;
    private static final int MAXIMUM_LOCAL_PART_LENGTH = 64;
    private static final int MAXIMUM_DOMAIN_LENGTH = 255;
    private static final Pattern LOCAL_PART_PATTERN = Pattern.compile("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+");
    private static final Pattern DOMAIN_LABEL_PATTERN = Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?");

    public String normalize(String email) {
        String strippedEmail = email.strip();
        return strippedEmail.toLowerCase(Locale.ROOT);
    }

    public boolean isValid(String email) {
        if (email.length() > MAXIMUM_EMAIL_LENGTH) {
            return false;
        }

        int separator = email.indexOf('@');
        if (separator <= 0 || separator != email.lastIndexOf('@')) {
            return false;
        }

        String localPart = email.substring(0, separator);
        String domain = email.substring(separator + 1);
        if (localPart.length() > MAXIMUM_LOCAL_PART_LENGTH
                || localPart.startsWith(".")
                || localPart.endsWith(".")
                || localPart.contains("..")
                || !LOCAL_PART_PATTERN.matcher(localPart).matches()) {
            return false;
        }
        if (domain.length() > MAXIMUM_DOMAIN_LENGTH || !domain.contains(".")) {
            return false;
        }

        String[] labels = domain.split("\\.", -1);
        for (String label : labels) {
            if (!DOMAIN_LABEL_PATTERN.matcher(label).matches()) {
                return false;
            }
        }
        return true;
    }
}
