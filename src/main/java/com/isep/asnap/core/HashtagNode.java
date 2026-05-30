package com.isep.asnap.core;

/**
 * Represents a hashtag node in the social graph.
 * Hashtags act as topic hubs connecting tweets around shared themes.
 */
public record HashtagNode(long id, String tag, long usageCount) implements Node {

    public HashtagNode {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("tag must not be blank");
        }
    }
}
