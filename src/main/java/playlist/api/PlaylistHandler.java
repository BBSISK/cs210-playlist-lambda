package playlist.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import playlist.MusicPlaylist;
import playlist.Song;
import playlist.api.PlaylistStore.ConflictException;
import playlist.api.PlaylistStore.StoredPlaylist;

/**
 * AWS Lambda entry point behind API Gateway (REST API, proxy integration).
 *
 * <pre>
 * GET    /playlists/{id}                 playlist state: forward + backward order, current song, stats
 * POST   /playlists/{id}/songs           add a song: {"title","artist","durationSeconds", optional
 *                                        "position":"end"|"start" or "before":"Existing title"}
 * DELETE /playlists/{id}/songs?title=X   remove the first song with that title
 * POST   /playlists/{id}/play            {"action":"first"|"next"|"previous"|"current"}
 * DELETE /playlists/{id}                 delete the whole playlist
 * </pre>
 *
 * Every request: load from the store, rebuild the doubly linked list, run ONE MusicPlaylist method,
 * save (with a version check) and return the state.
 */
public class PlaylistHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    static final int MAX_SONGS = 200;
    static final int MAX_TEXT = 100;
    static final int MAX_DURATION_SECONDS = 3600;
    static final int MAX_BODY_CHARS = 10_000;
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final ObjectMapper JSON = new ObjectMapper();

    private static PlaylistStore defaultStore;

    private final PlaylistStore store;

    /** Used by Lambda: one DynamoDB client per container, reused across invocations. */
    public PlaylistHandler() {
        this(defaultStore());
    }

    public PlaylistHandler(PlaylistStore store) {
        this.store = store;
    }

    private static synchronized PlaylistStore defaultStore() {
        if (defaultStore == null) {
            defaultStore = DynamoDbPlaylistStore.fromEnvironment();
        }
        return defaultStore;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        String method = String.valueOf(event.get("httpMethod"));
        String resource = String.valueOf(event.get("resource"));
        Map<String, Object> response;
        try {
            response = route(method, resource, event);
        } catch (ApiError e) {
            response = error(e.status, e.getMessage());
        } catch (ConflictException e) {
            response = error(409, "The playlist was changed by another request at the same time. Try again.");
        } catch (RuntimeException e) {
            // Log the cause for CloudWatch, but never return internals to the caller.
            System.err.println("Unhandled error on " + method + " " + resource + ": " + e);
            response = error(500, "Something went wrong.");
        }
        // Request bodies are never logged: they are user input.
        System.out.println(method + " " + resource + " -> " + response.get("statusCode"));
        return response;
    }

    private Map<String, Object> route(String method, String resource, Map<String, Object> event) {
        switch (method + " " + resource) {
            case "GET /playlists/{id}":
                return getPlaylist(playlistId(event));
            case "DELETE /playlists/{id}":
                return deletePlaylist(playlistId(event));
            case "POST /playlists/{id}/songs":
                return addSong(playlistId(event), body(event));
            case "DELETE /playlists/{id}/songs":
                return removeSong(playlistId(event), queryParam(event, "title"));
            case "POST /playlists/{id}/play":
                return play(playlistId(event), body(event));
            default:
                throw new ApiError(404, "No route for " + method + " " + resource);
        }
    }

    // ---- operations -------------------------------------------------------------------------

    private Map<String, Object> getPlaylist(String id) {
        StoredPlaylist stored = loadExisting(id);
        return json(200, state(id, PlaylistCodec.toPlaylist(stored)));
    }

    private Map<String, Object> deletePlaylist(String id) {
        if (!store.delete(id)) {
            throw new ApiError(404, "No playlist called '" + id + "'.");
        }
        return json(200, Map.of("deleted", id));
    }

    private Map<String, Object> addSong(String id, JsonNode body) {
        Song song = new Song(
                text(body, "title"), text(body, "artist"), duration(body.get("durationSeconds")));
        StoredPlaylist stored = store.load(id).orElse(new StoredPlaylist(id, List.of(), -1, 0));
        if (stored.songs().size() >= MAX_SONGS) {
            throw new ApiError(400, "A playlist can hold at most " + MAX_SONGS + " songs.");
        }
        MusicPlaylist playlist = PlaylistCodec.toPlaylist(stored);

        String before = optionalText(body, "before");
        String position = Optional.ofNullable(optionalText(body, "position")).orElse("end");
        if (before != null) {
            if (!playlist.addBefore(before, song)) {
                throw new ApiError(404, "No song titled '" + before + "' to insert before.");
            }
        } else if (position.equals("start")) {
            playlist.prependSong(song);
        } else if (position.equals("end")) {
            playlist.addSong(song);
        } else {
            throw new ApiError(400, "position must be \"start\" or \"end\".");
        }
        store.save(PlaylistCodec.toStored(id, playlist, stored.version()));
        return json(stored.version() == 0 ? 201 : 200, state(id, playlist));
    }

    private Map<String, Object> removeSong(String id, String title) {
        if (title == null || title.isBlank()) {
            throw new ApiError(400, "Give the song to remove as ?title=...");
        }
        StoredPlaylist stored = loadExisting(id);
        MusicPlaylist playlist = PlaylistCodec.toPlaylist(stored);
        if (!playlist.removeSong(title)) {
            throw new ApiError(404, "No song titled '" + title + "' in this playlist.");
        }
        store.save(PlaylistCodec.toStored(id, playlist, stored.version()));
        return json(200, state(id, playlist));
    }

    private Map<String, Object> play(String id, JsonNode body) {
        String action = text(body, "action");
        StoredPlaylist stored = loadExisting(id);
        MusicPlaylist playlist = PlaylistCodec.toPlaylist(stored);
        Song before = playlist.currentSong();

        Song result;
        String note = null;
        switch (action) {
            case "first" -> {
                result = playlist.playFirst();
                if (result == null) note = "The playlist is empty.";
            }
            case "next" -> {
                result = playlist.nextSong();
                if (result == null) note = before == null
                        ? "Nothing is playing yet: use action \"first\"."
                        : "Already at the last song.";
            }
            case "previous" -> {
                result = playlist.previousSong();
                if (result == null) note = before == null
                        ? "Nothing is playing yet: use action \"first\"."
                        : "Already at the first song.";
            }
            case "current" -> {
                result = playlist.currentSong();
                if (result == null) note = "Nothing is playing yet.";
            }
            default -> throw new ApiError(400, "action must be first, next, previous or current.");
        }
        if (!action.equals("current")) {
            store.save(PlaylistCodec.toStored(id, playlist, stored.version()));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("action", action);
        out.put("song", songJson(result));
        if (note != null) out.put("note", note);
        out.put("playlist", state(id, playlist));
        return json(200, out);
    }

    // ---- state shown to the caller ----------------------------------------------------------

    /** Forward and backward are produced by separate traversals, so a broken link shows up here. */
    static Map<String, Object> state(String id, MusicPlaylist playlist) {
        List<Map<String, Object>> forward = new ArrayList<>();
        List<Map<String, Object>> backward = new ArrayList<>();
        playlist.forEachForward(s -> forward.add(songJson(s)));
        playlist.forEachBackward(s -> backward.add(songJson(s)));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("playlistId", id);
        out.put("count", playlist.countSongs());
        out.put("totalDurationSeconds", playlist.totalDuration());
        out.put("current", songJson(playlist.currentSong()));
        out.put("forward", forward);
        out.put("backward", backward);
        return out;
    }

    private static Map<String, Object> songJson(Song song) {
        if (song == null) return null;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("title", song.getTitle());
        out.put("artist", song.getArtist());
        out.put("durationSeconds", song.getDurationSeconds());
        out.put("display", song.toString());
        return out;
    }

    // ---- request parsing and validation -----------------------------------------------------

    private StoredPlaylist loadExisting(String id) {
        return store.load(id).orElseThrow(() -> new ApiError(404, "No playlist called '" + id + "'."));
    }

    @SuppressWarnings("unchecked")
    private static String playlistId(Map<String, Object> event) {
        Object params = event.get("pathParameters");
        Object id = params instanceof Map ? ((Map<String, Object>) params).get("id") : null;
        if (!(id instanceof String s) || !ID.matcher(s).matches()) {
            throw new ApiError(400, "Playlist id must be 1-64 letters, digits, '-' or '_'.");
        }
        return s;
    }

    @SuppressWarnings("unchecked")
    private static String queryParam(Map<String, Object> event, String name) {
        Object params = event.get("queryStringParameters");
        if (!(params instanceof Map)) return null;
        Object value = ((Map<String, Object>) params).get(name);
        return value instanceof String s ? s : null;
    }

    private static JsonNode body(Map<String, Object> event) {
        if (Boolean.TRUE.equals(event.get("isBase64Encoded"))) {
            throw new ApiError(400, "Send the body as plain JSON.");
        }
        Object raw = event.get("body");
        if (!(raw instanceof String text) || text.isBlank()) {
            throw new ApiError(400, "A JSON body is required.");
        }
        if (text.length() > MAX_BODY_CHARS) {
            throw new ApiError(413, "Request body is too large.");
        }
        try {
            JsonNode node = JSON.readTree(text);
            if (node == null || !node.isObject()) throw new ApiError(400, "The body must be a JSON object.");
            return node;
        } catch (JsonProcessingException e) {
            throw new ApiError(400, "The body is not valid JSON.");
        }
    }

    private static String text(JsonNode body, String field) {
        String value = optionalText(body, field);
        if (value == null) throw new ApiError(400, "\"" + field + "\" is required.");
        return value;
    }

    private static String optionalText(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) return null;
        if (!node.isTextual()) throw new ApiError(400, "\"" + field + "\" must be a string.");
        String value = node.asText().strip();
        if (value.isEmpty() || value.length() > MAX_TEXT) {
            throw new ApiError(400, "\"" + field + "\" must be 1-" + MAX_TEXT + " characters.");
        }
        return value;
    }

    private static int duration(JsonNode node) {
        if (node == null || !node.isInt()) {
            throw new ApiError(400, "\"durationSeconds\" must be a whole number.");
        }
        int seconds = node.asInt();
        if (seconds < 1 || seconds > MAX_DURATION_SECONDS) {
            throw new ApiError(400, "\"durationSeconds\" must be between 1 and " + MAX_DURATION_SECONDS + ".");
        }
        return seconds;
    }

    // ---- responses --------------------------------------------------------------------------

    private static Map<String, Object> json(int status, Object body) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("statusCode", status);
        response.put("headers", Map.of("Content-Type", "application/json"));
        try {
            response.put("body", JSON.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return response;
    }

    private static Map<String, Object> error(int status, String message) {
        return json(status, Map.of("error", message));
    }

    /** An expected failure with an HTTP status and a message that is safe to show the caller. */
    static final class ApiError extends RuntimeException {
        final int status;

        ApiError(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
