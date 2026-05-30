package com.isep.asnap.core;

public record UserNode(long id, String username, long followersCount) implements Node {
    public UserNode {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }
}
