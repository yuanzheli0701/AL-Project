package com.isep.asnap.core;

public record Edge(long source, long target, EdgeType type, double weight) {

    public enum EdgeType {
        FOLLOW, LIKE, RETWEET, REPLY, MENTION, AUTHORED, TAGGED
    }

    public Edge {
        if (weight < 0) {
            throw new IllegalArgumentException("edge weight must be non-negative");
        }
    }

    public static Edge follow(long source, long target, double weight) {
        return new Edge(source, target, EdgeType.FOLLOW, weight);
    }

    public static Edge like(long userId, long tweetId, double weight) {
        return new Edge(userId, tweetId, EdgeType.LIKE, weight);
    }

    public static Edge retweet(long userId, long tweetId, double weight) {
        return new Edge(userId, tweetId, EdgeType.RETWEET, weight);
    }

    public static Edge mention(long source, long target, double weight) {
        return new Edge(source, target, EdgeType.MENTION, weight);
    }

    public static Edge reply(long userId, long tweetId, double weight) {
        return new Edge(userId, tweetId, EdgeType.REPLY, weight);
    }

    public static Edge tagged(long tweetId, long hashtagId, double weight) {
        return new Edge(tweetId, hashtagId, EdgeType.TAGGED, weight);
    }
}
