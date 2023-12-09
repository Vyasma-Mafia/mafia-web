package com.mafia.mafiabackend.model;

public enum Role {
    RED,
    BLACK,
    DON,
    SHERIFF,
    WHORE,
    DOCTOR,
    MANIAC;

    public boolean isBlack() {
        return Role.isBlack(this);
    }

    public static Boolean isBlack(Role role) {
        return switch (role) {
            case BLACK, DON, WHORE -> true;
            default -> false;
        };
    }
}
