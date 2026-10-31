package playlist.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Lambda handler, with DynamoDB swapped for an in-memory store. These pass only once your
 * MusicPlaylist works, because every request rebuilds and uses your linked list.
 */
@DisplayName("Lambda API (needs Activities 1-6 and the extension)")
class PlaylistHandlerTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final InMemoryPlaylistStore store = new InMemoryPlaylistStore();
    private final PlaylistHandler handler = new PlaylistHandler(store);

    // ---- helpers that build API Gateway proxy events -----------------------------------------

    private Map<String, Object> call(String method, String resource, String id, String body,
                                     Map<String, String> query) {
        Map<String, Object> event = new HashMap<>();
        event.put("httpMethod", method);
        event.put("resource", resource);
        event.put("pathParameters", id == null ? null : Map.of("id", id));
        event.put("queryStringParameters", query);
        event.put("body", body);
        event.put("isBase64Encoded", false);
        return handler.handleRequest(event, null);
    }

    private static int status(Map<String, Object> response) {
        return (Integer) response.get("statusCode");
    }

    private static JsonNode body(Map<String, Object> response) throws Exception {
        return JSON.readTree((String) response.get("body"));
    }

    private Map<String, Object> add(String id, String title, String artist, int seconds, String extra) {
        String body = "{\"title\":\"" + title + "\",\"artist\":\"" + artist
                + "\",\"durationSeconds\":" + seconds + (extra == null ? "" : "," + extra) + "}";
        return call("POST", "/playlists/{id}/songs", id, body, null);
    }

    private Map<String, Object> play(String id, String action) {
        return call("POST", "/playlists/{id}/play", id, "{\"action\":\"" + action + "\"}", null);
    }

    private static List<String> titles(JsonNode songs) {
        List<String> out = new ArrayList<>();
        songs.forEach(s -> out.add(s.get("title").asText()));
        return out;
    }

    // ---- tests ------------------------------------------------------------------------------

    @Test
    @DisplayName("first add creates the playlist (201); state shows forward, backward and stats")
    void addAndGet() throws Exception {
        assertEquals(201, status(add("mix", "Paper Lanterns", "Ivy Hale", 187, null)));
        assertEquals(200, status(add("mix", "Northbound Train", "The Copper Pines", 242, null)));
        assertEquals(200, status(add("mix", "Midnight Drive", "The Signals", 215, "\"position\":\"start\"")));
        assertEquals(200, status(add("mix", "Interlude", "Quiet Hours", 95, "\"before\":\"Northbound Train\"")));

        Map<String, Object> got = call("GET", "/playlists/{id}", "mix", null, null);
        assertEquals(200, status(got));
        JsonNode state = body(got);
        List<String> expected = List.of("Midnight Drive", "Paper Lanterns", "Interlude", "Northbound Train");
        assertEquals(expected, titles(state.get("forward")));
        List<String> backward = new ArrayList<>(titles(state.get("backward")));
        java.util.Collections.reverse(backward);
        assertEquals(expected, backward);
        assertEquals(4, state.get("count").asInt());
        assertEquals(215 + 187 + 95 + 242, state.get("totalDurationSeconds").asInt());
        assertEquals("Midnight Drive — The Signals (215 seconds)",
                state.get("forward").get(0).get("display").asText());
        assertTrue(state.get("current").isNull());
    }

    @Test
    @DisplayName("navigation is saved between requests (current survives a reload)")
    void navigationPersists() throws Exception {
        add("nav", "A", "X", 100, null);
        add("nav", "B", "X", 100, null);
        add("nav", "C", "X", 100, null);

        assertEquals("A", body(play("nav", "first")).get("song").get("title").asText());
        assertEquals("B", body(play("nav", "next")).get("song").get("title").asText());
        assertEquals("C", body(play("nav", "next")).get("song").get("title").asText());

        JsonNode atEnd = body(play("nav", "next"));
        assertTrue(atEnd.get("song").isNull());
        assertEquals("Already at the last song.", atEnd.get("note").asText());
        assertEquals("C", atEnd.get("playlist").get("current").get("title").asText());

        assertEquals("B", body(play("nav", "previous")).get("song").get("title").asText());
        assertEquals("B", body(play("nav", "current")).get("song").get("title").asText());
        assertEquals(1, store.load("nav").orElseThrow().currentIndex());
    }

    @Test
    @DisplayName("remove: first, middle, last, then 404 for a missing title")
    void remove() throws Exception {
        add("rm", "A", "X", 100, null);
        add("rm", "B", "X", 100, null);
        add("rm", "C", "X", 100, null);
        add("rm", "D", "X", 100, null);

        assertEquals(200, status(call("DELETE", "/playlists/{id}/songs", "rm", null, Map.of("title", "A"))));
        assertEquals(200, status(call("DELETE", "/playlists/{id}/songs", "rm", null, Map.of("title", "C"))));
        Map<String, Object> last = call("DELETE", "/playlists/{id}/songs", "rm", null, Map.of("title", "D"));
        assertEquals(List.of("B"), titles(body(last).get("forward")));

        Map<String, Object> missing = call("DELETE", "/playlists/{id}/songs", "rm", null, Map.of("title", "Z"));
        assertEquals(404, status(missing));
        assertEquals("No song titled 'Z' in this playlist.", body(missing).get("error").asText());
    }

    @Test
    @DisplayName("removing every song leaves an empty playlist, not an error")
    void removeAllSongs() throws Exception {
        add("one", "A", "X", 100, null);
        call("DELETE", "/playlists/{id}/songs", "one", null, Map.of("title", "A"));
        JsonNode state = body(call("GET", "/playlists/{id}", "one", null, null));
        assertEquals(0, state.get("count").asInt());
        assertEquals(0, state.get("forward").size());
        JsonNode first = body(play("one", "first"));
        assertEquals("The playlist is empty.", first.get("note").asText());
    }

    @Test
    @DisplayName("addBefore a missing title: 404 and nothing saved")
    void addBeforeMissing() throws Exception {
        add("ab", "A", "X", 100, null);
        Map<String, Object> r = add("ab", "B", "X", 100, "\"before\":\"Nope\"");
        assertEquals(404, status(r));
        assertEquals(1, store.load("ab").orElseThrow().songs().size());
    }

    @Test
    @DisplayName("bad input gets a 400 with a readable message")
    void validation() throws Exception {
        assertEquals(400, status(call("GET", "/playlists/{id}", "bad id!", null, null)));
        assertEquals(400, status(call("POST", "/playlists/{id}/songs", "v", "not json", null)));
        assertEquals(400, status(call("POST", "/playlists/{id}/songs", "v", "{\"title\":\"A\"}", null)));
        assertEquals(400, status(add("v", "A", "X", 0, null)));
        assertEquals(400, status(add("v", "A", "X", 3601, null)));
        assertEquals(400, status(add("v", " ", "X", 100, null)));
        assertEquals(400, status(add("v", "A", "X", 100, "\"position\":\"middle\"")));
        assertEquals(400, status(call("POST", "/playlists/{id}/songs", "v",
                "{\"title\":\"A\",\"artist\":\"X\",\"durationSeconds\":\"200\"}", null)));
        assertEquals(400, status(call("DELETE", "/playlists/{id}/songs", "v", null, null)));
        assertEquals(413, status(call("POST", "/playlists/{id}/songs", "v", "x".repeat(10_001), null)));
        add("v", "A", "X", 100, null);
        assertEquals(400, status(play("v", "shuffle")));
        assertFalse(body(play("v", "shuffle")).get("error").asText().isEmpty());
    }

    @Test
    @DisplayName("unknown playlist: 404; unknown route: 404")
    void notFound() {
        assertEquals(404, status(call("GET", "/playlists/{id}", "ghost", null, null)));
        assertEquals(404, status(play("ghost", "first")));
        assertEquals(404, status(call("DELETE", "/playlists/{id}", "ghost", null, null)));
        assertEquals(404, status(call("PUT", "/playlists/{id}", "x", null, null)));
    }

    @Test
    @DisplayName("delete a whole playlist")
    void deletePlaylist() {
        add("gone", "A", "X", 100, null);
        assertEquals(200, status(call("DELETE", "/playlists/{id}", "gone", null, null)));
        assertEquals(404, status(call("GET", "/playlists/{id}", "gone", null, null)));
    }

    @Test
    @DisplayName("a playlist is capped at 200 songs")
    void songCap() {
        for (int i = 0; i < PlaylistHandler.MAX_SONGS; i++) {
            assertTrue(status(add("big", "Song " + i, "X", 60, null)) < 300);
        }
        assertEquals(400, status(add("big", "One too many", "X", 60, null)));
    }

    @Test
    @DisplayName("a write that loses a race gets 409, not a silent overwrite")
    void conflict() throws Exception {
        PlaylistStore racing = new PlaylistStore() {
            @Override public Optional<StoredPlaylist> load(String id) { return store.load(id); }
            @Override public StoredPlaylist save(StoredPlaylist p) {
                throw new ConflictException("someone else saved first");
            }
            @Override public boolean delete(String id) { return store.delete(id); }
        };
        Map<String, Object> event = new HashMap<>();
        event.put("httpMethod", "POST");
        event.put("resource", "/playlists/{id}/songs");
        event.put("pathParameters", Map.of("id", "race"));
        event.put("body", "{\"title\":\"A\",\"artist\":\"X\",\"durationSeconds\":100}");
        Map<String, Object> r = new PlaylistHandler(racing).handleRequest(event, null);
        assertEquals(409, status(r));
    }

    @Test
    @DisplayName("in-memory store enforces versions like DynamoDB's conditional write")
    void storeVersions() {
        PlaylistStore.StoredPlaylist v0 = new PlaylistStore.StoredPlaylist("s", List.of(), -1, 0);
        PlaylistStore.StoredPlaylist v1 = store.save(v0);
        assertEquals(1L, v1.version());
        org.junit.jupiter.api.Assertions.assertThrows(PlaylistStore.ConflictException.class, () -> store.save(v0));
        assertEquals(2L, store.save(v1).version());
    }

    @Test
    @DisplayName("duplicate titles: current is restored by position, not by title")
    void duplicateTitlesKeepCurrent() throws Exception {
        add("dup", "Same", "First", 100, null);
        add("dup", "Same", "Second", 100, null);
        play("dup", "first");
        play("dup", "next");
        JsonNode current = body(play("dup", "current")).get("song");
        assertEquals("Second", current.get("artist").asText());
    }
}
