package playlist;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Extension: statistics and traversal helpers")
class ExtensionStatisticsTest {

    @Test
    @DisplayName("empty playlist: 0 songs, 0 seconds")
    void empty() {
        MusicPlaylist p = new MusicPlaylist();
        assertEquals(0, p.countSongs());
        assertEquals(0, p.totalDuration());
    }

    @Test
    @DisplayName("six songs: count and total duration")
    void six() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertEquals(6, p.countSongs());
        assertEquals(215 + 187 + 242 + 198 + 263 + 176, p.totalDuration());
    }

    @Test
    @DisplayName("stats follow inserts and removals")
    void afterChanges() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        p.prependSong(TestSongs.openingAct());        // +120
        p.addBefore("Glass Harbour", TestSongs.interlude()); // +95
        p.removeSong("Midnight Drive");                // -215
        assertEquals(7, p.countSongs());
        assertEquals(1281 + 120 + 95 - 215, p.totalDuration());
    }

    @Test
    @DisplayName("forEachForward and forEachBackward visit every song in opposite orders")
    void forEach() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        List<String> forward = new ArrayList<>();
        List<String> backward = new ArrayList<>();
        p.forEachForward(s -> forward.add(s.getTitle()));
        p.forEachBackward(s -> backward.add(0, s.getTitle()));
        assertEquals(List.of(TestSongs.SIX_TITLES), forward);
        assertEquals(List.of(TestSongs.SIX_TITLES), backward);

        List<String> none = new ArrayList<>();
        new MusicPlaylist().forEachForward(s -> none.add(s.getTitle()));
        new MusicPlaylist().forEachBackward(s -> none.add(s.getTitle()));
        assertEquals(List.of(), none);
    }
}
