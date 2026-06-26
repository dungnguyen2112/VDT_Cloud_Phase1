package com.quiz.classservice.util;

import java.security.SecureRandom;

public final class JoinCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private JoinCodeGenerator() {
    }

    public static String next() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
