package playlist.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

/**
 * Stores each playlist as one DynamoDB item:
 *
 * <pre>
 * playlistId   (S)  partition key
 * songs        (S)  JSON array of {title, artist, durationSeconds}, first to last
 * currentIndex (N)  position of the current song, -1 for none
 * version      (N)  incremented on every save; writes are conditional on it (optimistic locking)
 * updatedAt    (S)  ISO-8601 timestamp
 * expiresAt    (N)  epoch seconds; DynamoDB TTL deletes the demo playlist 30 days after its last change
 * </pre>
 */
public final class DynamoDbPlaylistStore implements PlaylistStore {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<SongRecord>> SONG_LIST = new TypeReference<>() {};
    static final Duration TIME_TO_LIVE = Duration.ofDays(30);

    private final DynamoDbClient dynamo;
    private final String tableName;
    private final Clock clock;

    public DynamoDbPlaylistStore(DynamoDbClient dynamo, String tableName, Clock clock) {
        this.dynamo = dynamo;
        this.tableName = tableName;
        this.clock = clock;
    }

    /** Uses TABLE_NAME (set by template.yaml) and the Lambda's own region and IAM role. */
    public static DynamoDbPlaylistStore fromEnvironment() {
        String table = System.getenv("TABLE_NAME");
        if (table == null || table.isBlank()) {
            throw new IllegalStateException("TABLE_NAME environment variable is not set");
        }
        DynamoDbClient client = DynamoDbClient.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        return new DynamoDbPlaylistStore(client, table, Clock.systemUTC());
    }

    private static Map<String, AttributeValue> key(String id) {
        return Map.of("playlistId", AttributeValue.fromS(id));
    }

    @Override
    public Optional<StoredPlaylist> load(String id) {
        GetItemResponse response = dynamo.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key(id))
                .consistentRead(true)
                .build());
        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }
        Map<String, AttributeValue> item = response.item();
        try {
            List<SongRecord> songs = JSON.readValue(item.get("songs").s(), SONG_LIST);
            int currentIndex = Integer.parseInt(item.get("currentIndex").n());
            long version = Long.parseLong(item.get("version").n());
            return Optional.of(new StoredPlaylist(id, songs, currentIndex, version));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Stored playlist " + id + " is not valid JSON", e);
        }
    }

    @Override
    public StoredPlaylist save(StoredPlaylist playlist) {
        long newVersion = playlist.version() + 1;
        String songsJson;
        try {
            songsJson = JSON.writeValueAsString(playlist.songs());
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, AttributeValue> item = new HashMap<>(key(playlist.id()));
        item.put("songs", AttributeValue.fromS(songsJson));
        item.put("currentIndex", AttributeValue.fromN(Integer.toString(playlist.currentIndex())));
        item.put("version", AttributeValue.fromN(Long.toString(newVersion)));
        item.put("updatedAt", AttributeValue.fromS(clock.instant().toString()));
        item.put("expiresAt", AttributeValue.fromN(
                Long.toString(clock.instant().plus(TIME_TO_LIVE).getEpochSecond())));

        PutItemRequest.Builder request = PutItemRequest.builder().tableName(tableName).item(item);
        if (playlist.version() == 0) {
            request.conditionExpression("attribute_not_exists(playlistId)");
        } else {
            // "version" is a DynamoDB reserved word, so it needs a placeholder name.
            request.conditionExpression("#v = :expected")
                    .expressionAttributeNames(Map.of("#v", "version"))
                    .expressionAttributeValues(Map.of(
                            ":expected", AttributeValue.fromN(Long.toString(playlist.version()))));
        }
        try {
            dynamo.putItem(request.build());
        } catch (ConditionalCheckFailedException e) {
            throw new ConflictException("Playlist " + playlist.id() + " changed since it was loaded");
        }
        return new StoredPlaylist(playlist.id(), playlist.songs(), playlist.currentIndex(), newVersion);
    }

    @Override
    public boolean delete(String id) {
        DeleteItemResponse response = dynamo.deleteItem(DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key(id))
                .returnValues(ReturnValue.ALL_OLD)
                .build());
        return response.hasAttributes() && !response.attributes().isEmpty();
    }
}
