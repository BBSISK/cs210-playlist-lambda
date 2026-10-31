package playlist.api;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** A store with the same rules as DynamoDB (including version checks), used by the tests. */
public final class InMemoryPlaylistStore implements PlaylistStore {

    private final ConcurrentMap<String, StoredPlaylist> playlists = new ConcurrentHashMap<>();

    @Override
    public Optional<StoredPlaylist> load(String id) {
        return Optional.ofNullable(playlists.get(id));
    }

    @Override
    public StoredPlaylist save(StoredPlaylist playlist) {
        StoredPlaylist saved = new StoredPlaylist(
                playlist.id(), playlist.songs(), playlist.currentIndex(), playlist.version() + 1);
        StoredPlaylist result = playlists.compute(playlist.id(), (id, existing) -> {
            long storedVersion = existing == null ? 0 : existing.version();
            if (storedVersion != playlist.version()) {
                throw new ConflictException("Playlist " + id + " changed since it was loaded");
            }
            return saved;
        });
        return result;
    }

    @Override
    public boolean delete(String id) {
        return playlists.remove(id) != null;
    }
}
