package com.isep.asnap.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.HashtagNode;
import com.isep.asnap.core.TweetNode;
import com.isep.asnap.core.UserNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.*;

/**
 * Loads user pool from users.json (120 users) and randomly selects
 * 40 users + generates tweets/interactions each time the app starts.
 *
 * <p>This simulates a real database-backed system where different
 * user subsets are sampled for testing.
 */
@Configuration
public class SampleDataLoader {

    
    private static final int SAMPLE_SIZE = 40;
    private static final int TWEET_COUNT = 30;

    /**
     * Represents a user record from the JSON database.
     */
    public static class UserRecord {
        public String username;
        public long followers;
        public String category;

        public UserRecord() {}
    }

    @Bean
    public DirectedWeightedGraph graph() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        Random rng = new Random();

        // Load all 120 users from JSON
        List<UserRecord> allUsers = loadUsers();
        System.out.println("[SampleDataLoader] Loaded " + allUsers.size() + " users from database");

        // Randomly select 40 users (use shuffle + take first 40)
        List<UserRecord> pool = new ArrayList<>(allUsers);
        Collections.shuffle(pool, rng);
        List<UserRecord> selected = pool.subList(0, Math.min(SAMPLE_SIZE, pool.size()));
        System.out.println("[SampleDataLoader] Selected " + selected.size() + " random users for this session");

        // Add selected users to graph
        for (int i = 0; i < selected.size(); i++) {
            UserRecord ur = selected.get(i);
            g.addNode(new UserNode(i, ur.username, ur.followers));
        }

        // Generate follow edges (prefer popular users as targets)
        for (int u = 0; u < selected.size(); u++) {
            int targets = 5 + rng.nextInt(8);
            for (int k = 0; k < targets; k++) {
                int v;
                // 55% chance to follow a popular user (top 20% by followers)
                if (rng.nextDouble() < 0.55) {
                    v = (int) Math.floor(Math.pow(rng.nextDouble(), 3) * selected.size());
                } else {
                    v = rng.nextInt(selected.size());
                }
                if (v == u) continue;
                double interaction = 1.0 + rng.nextDouble() * 9.0;
                double cost = 1.0 / interaction;
                g.addEdge(Edge.follow(u, v, cost));
            }
        }

        // Tweet templates
        String[] templates = {
                "Just shipped a new build - feedback welcome",
                "What a game last night!",
                "Thoughts on the new release?",
                "AI is moving incredibly fast right now",
                "Coffee, code, repeat",
                "On stage in 5 minutes!",
                "Big news coming next week",
                "Best concert of the year",
                "Weekend hike was unreal",
                "Reading the new biography - highly recommend",
                "Building something new today",
                "Open-sourced my latest project",
                "Off to Paris for the launch",
                "Dropping a new track Friday",
                "This product is a game changer",
                "Never stop learning",
                "The future is now",
                "Back to the studio tonight",
                "Just finished an amazing workout",
                "New article up on the blog"
        };

        long now = System.currentTimeMillis();
        for (int i = 0; i < TWEET_COUNT; i++) {
            int author = (int) Math.floor(Math.pow(rng.nextDouble(), 1.5) * selected.size());
            long tweetId = 1_000_000L + i;
            String content = templates[rng.nextInt(templates.length)];
            long likes = 100 + rng.nextInt(50_000);
            long timestamp = now - rng.nextInt(604_800_000); // within last week
            g.addNode(new TweetNode(tweetId, author, content, timestamp, likes));
            g.addEdge(new Edge(author, tweetId, Edge.EdgeType.AUTHORED, 1.0));

            int interactions = 3 + rng.nextInt(8);
            for (int k = 0; k < interactions; k++) {
                int liker = rng.nextInt(selected.size());
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

    /**
     * Loads the 120-user database from the JSON resource file.
     */
    private List<UserRecord> loadUsers() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getClassLoader().getResourceAsStream("users.json");
            if (is == null) {
                System.err.println("[SampleDataLoader] users.json not found, using fallback");
                return fallbackUsers();
            }
            return mapper.readValue(is, new TypeReference<List<UserRecord>>() {});
        } catch (Exception e) {
            System.err.println("[SampleDataLoader] Failed to load users.json: " + e.getMessage());
            return fallbackUsers();
        }
    }

    /**
     * Fallback: hardcoded 40 users if JSON loading fails.
     */
    private List<UserRecord> fallbackUsers() {
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
        List<UserRecord> list = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            UserRecord ur = new UserRecord();
            ur.username = names[i];
            ur.followers = (long) (1_000_000L * Math.pow(0.85, i)) + 1000;
            ur.category = "other";
            list.add(ur);
        }
        return list;
    }
}