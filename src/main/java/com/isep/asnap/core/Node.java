package com.isep.asnap.core;

public sealed interface Node permits UserNode, TweetNode, HashtagNode {
    long id();
}
