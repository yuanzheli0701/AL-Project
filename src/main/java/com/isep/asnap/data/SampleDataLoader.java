package com.isep.asnap.data;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.HashtagNode;
import com.isep.asnap.core.TweetNode;
import com.isep.asnap.core.UserNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

/**
 * Generates a synthetic X (Twitter) graph at startup.
 *
 * <p>40 users with realistic follow patterns (a few celebrity hubs, a long
 * tail of regular accounts) and 30 tweets authored by the most-followed users.
 * Edges include FOLLOW (user→user), AUTHORED (user→tweet) and LIKE / RETWEET
 * (user→tweet) so that PageRank produces a non-trivial mix of user influence
 * and tweet salience.
 */
@Configuration
public class SampleDataLoader {

    private static final long SEED = 42L;
    private static final int USER_COUNT = 40;
    private static final int TWEET_COUNT = 30;

    @Bean
    public DirectedWeightedGraph graph() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        Random rng = new Random(SEED);

        String[] names = {
                "elonmusk", "barackobama", "taylorswift13", "cristiano", "rihanna",
                "narendramodi", "katyperry", "justinbieber", "billgates", "neymarjr",
                "jtimberlake", "shakira", "kingjames", "ladygaga", "selenagomez",
                "jlo", "kimkardashian", "ddlovato", "britneyspears", "ed_sheeran",
                "ariana", "bts_twt", "dwaynejohnson", "drake", "kanyewest",
                "oprah", "marvel", "nasa", "nytimes", "espn",
                "tim_cook", "sundarpichai", "satyanadella", "jeffbezos", "markruffalo",
                "stephencurry30", "messi", "kobebryant", "snoopdogg", "ladybug42"
        };

        long[] popularity = new long[USER_COUNT];
        for (int i = 0; i < USER_COUNT; i++) {
            popularity[i] = (long) (1_000_000L * Math.pow(0.85, i)) + 1000;
            g.addNode(new UserNode(i, names[i], popularity[i]));
        }

        for (int u = 0; u < USER_COUNT; u++) {
            int targets = 5 + rng.nextInt(8);
            for (int k = 0; k < targets; k++) {
                int v;
                if (rng.nextDouble() < 0.55) {
                    v = (int) Math.floor(Math.pow(rng.nextDouble(), 3) * USER_COUNT);
                } else {
                    v = rng.nextInt(USER_COUNT);
                }
                if (v == u) continue;
                double interaction = 1.0 + rng.nextDouble() * 9.0;
                double cost = 1.0 / interaction;
                g.addEdge(Edge.follow(u, v, cost));
            }
        }

        String[] templates = {
                "Just shipped a new build — feedback welcome",
                "What a game last night",
                "Thoughts on the new release?",
                "AI is moving incredibly fast right now",
                "Coffee, code, repeat",
                "On stage in 5 minutes",
                "Big news coming next week",
                "Best concert of the year",
                "Weekend hike was unreal",
                "Reading the new biography — highly recommend",
                "Building something new",
                "Open-sourced today",
                "Off to Paris for the launch",
                "Dropping a new track Friday",
                "This product is a game changer"
        };

        long now = System.currentTimeMillis();
        for (int i = 0; i < TWEET_COUNT; i++) {
            int author = (int) Math.floor(Math.pow(rng.nextDouble(), 1.5) * USER_COUNT);
            long tweetId = 1_000_000L + i;
            String content = templates[rng.nextInt(templates.length)];
            long likes = 100 + rng.nextInt(50_000);
            g.addNode(new TweetNode(tweetId, author, content, now - rng.nextInt(86_400_000), likes));
            g.addEdge(new Edge(author, tweetId, Edge.EdgeType.AUTHORED, 1.0));

            int interactions = 3 + rng.nextInt(8);
            for (int k = 0; k < interactions; k++) {
                int liker = rng.nextInt(USER_COUNT);
                if (liker == author) continue;
                double w = 0.5 + rng.nextDouble() * 1.5;
                Edge.EdgeType t = rng.nextDouble() < 0.7 ? Edge.EdgeType.LIKE : Edge.EdgeType.RETWEET;
                g.addEdge(new Edge(liker, tweetId, t, w));
            }
        }

        // Hashtags
        String[] tagNames = {"ai", "tech", "music", "sports", "coding", "news", "gaming", "food", "travel", "fashion"};
        for (int i = 0; i < tagNames.length; i++) {
            long tagId = 2_000_000L + i;
            g.addNode(new HashtagNode(tagId, tagNames[i], rng.nextInt(100)));
            for (int j = 0; j < TWEET_COUNT; j++) {
                if (rng.nextDouble() < 0.3) {
                    long tweetId = 1_000_000L + j;
                    g.addEdge(new Edge(tweetId, tagId, Edge.EdgeType.TAGGED, rng.nextDouble()));
                }
            }
        }

        return g;
    }
}
