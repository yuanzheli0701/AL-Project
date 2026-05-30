package com.isep.asnap.core;

public record TweetNode(long id, long authorId, String content, long timestamp, long likes) implements Node {
    public TweetNode {
        if (content == null) content = "";
    }
}
