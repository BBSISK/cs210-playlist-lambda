package playlist.api;

import java.util.List;
import java.util.Optional;

/**
 * Where playlists live between Lambda invocations.
 *
 * <p>Lambda keeps nothing between requests, so each request loads the playlist, rebuilds it as a
 * {@link playlist.MusicPlaylist} (the CS210 doubly linked list), runs one operation, and saves it
 * back. The store only ever sees a plain list of songs plus the index of the current song; the
 * linked list exists in memory while a request runs.
 */
public interface PlaylistStore {

    /** One song as stored. */
    record SongRecord(String title, String artist, int durationSeconds) {}

    /**
     * A stored playlist.
     *
     * @param currentIndex position of the song being played, or -1 if none is selected
     * @param version      0 for a playlist that has never been saved; otherwise the version loaded
     */
    record StoredPlaylist(String id, List<SongRecord> songs, int currentIndex, long version) {
        public StoredPlaylist {
            songs = List.copyOf(songs);
        }
    }

    /** Thrown when another request saved the playlist after this one loaded it. */
    final class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    Optional<StoredPlaylist> load(String id);

    /**
     * Saves only if nobody else has saved since {@code playlist.version()} was loaded
     * (optimistic locking). Returns the saved playlist with its new version.
     *
     * @throws ConflictException if the stored version has moved on
     */
    StoredPlaylist save(StoredPlaylist playlist);

    /** Returns true if a playlist was deleted. */
    boolean delete(String id);
}
