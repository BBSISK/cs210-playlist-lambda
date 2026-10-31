package playlist.api;

import java.util.ArrayList;
import java.util.List;
import playlist.MusicPlaylist;
import playlist.Song;
import playlist.api.PlaylistStore.SongRecord;
import playlist.api.PlaylistStore.StoredPlaylist;

/**
 * Converts between the stored form (a list) and the CS210 doubly linked list.
 *
 * <p>It only uses MusicPlaylist's public methods (addSong, playFirst, nextSong, forEachForward,
 * currentSong), so the API exercises the same code the tests check.
 */
final class PlaylistCodec {

    private PlaylistCodec() {}

    /** Rebuilds the linked list: addSong for each song, then playFirst + nextSong to restore "current". */
    static MusicPlaylist toPlaylist(StoredPlaylist stored) {
        MusicPlaylist playlist = new MusicPlaylist();
        for (SongRecord s : stored.songs()) {
            playlist.addSong(new Song(s.title(), s.artist(), s.durationSeconds()));
        }
        if (stored.currentIndex() >= 0 && stored.currentIndex() < stored.songs().size()) {
            playlist.playFirst();
            for (int i = 0; i < stored.currentIndex(); i++) {
                playlist.nextSong();
            }
        }
        return playlist;
    }

    /** Walks the list forwards; the current song is found by identity, so duplicate titles are safe. */
    static StoredPlaylist toStored(String id, MusicPlaylist playlist, long loadedVersion) {
        List<SongRecord> songs = new ArrayList<>();
        Song current = playlist.currentSong();
        int[] currentIndex = {-1};
        playlist.forEachForward(song -> {
            if (song == current && currentIndex[0] < 0) {
                currentIndex[0] = songs.size();
            }
            songs.add(new SongRecord(song.getTitle(), song.getArtist(), song.getDurationSeconds()));
        });
        return new StoredPlaylist(id, songs, currentIndex[0], loadedVersion);
    }
}
