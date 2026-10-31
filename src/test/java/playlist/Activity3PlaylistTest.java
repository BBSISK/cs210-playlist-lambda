package playlist;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static playlist.LinkChecker.assertLinks;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Activity 3: create the playlist")
class Activity3PlaylistTest {

    @Test
    @DisplayName("a new playlist is empty")
    void newPlaylistIsEmpty() {
        MusicPlaylist p = new MusicPlaylist();
        assertTrue(p.isEmpty());
        assertLinks(p);
    }

    @Test
    @DisplayName("empty playlist: listings show the empty message, printing doesn't crash")
    void emptyListings() {
        MusicPlaylist p = new MusicPlaylist();
        assertEquals(MusicPlaylist.EMPTY_LISTING, p.forwardListing());
        assertEquals(MusicPlaylist.EMPTY_LISTING, p.backwardListing());
        assertDoesNotThrow(p::printForward);
        assertDoesNotThrow(p::printBackward);
    }

    @Test
    @DisplayName("addSong on an empty playlist: first and last are the same node")
    void addOneSong() {
        MusicPlaylist p = new MusicPlaylist();
        p.addSong(TestSongs.midnightDrive());
        assertLinks(p, "Midnight Drive");
        assertTrue(p.first == p.last, "With one song, first and last should be the same node");
    }

    @Test
    @DisplayName("addSong x6 keeps them in the order added")
    void addSixSongs() {
        assertLinks(TestSongs.playlistOfSix(), TestSongs.SIX_TITLES);
    }

    @Test
    @DisplayName("prependSong on an empty playlist")
    void prependOnEmpty() {
        MusicPlaylist p = new MusicPlaylist();
        p.prependSong(TestSongs.openingAct());
        assertLinks(p, "Opening Act");
        assertTrue(p.first == p.last, "With one song, first and last should be the same node");
    }

    @Test
    @DisplayName("prependSong puts the song before the existing first song")
    void prependOnSix() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        p.prependSong(TestSongs.openingAct());
        List<String> expected = new ArrayList<>(List.of("Opening Act"));
        expected.addAll(List.of(TestSongs.SIX_TITLES));
        assertLinks(p, expected.toArray(new String[0]));
    }

    @Test
    @DisplayName("mixing prepend and append")
    void mixPrependAndAppend() {
        MusicPlaylist p = new MusicPlaylist();
        p.addSong(TestSongs.paperLanterns());
        p.prependSong(TestSongs.midnightDrive());
        p.addSong(TestSongs.northboundTrain());
        assertLinks(p, "Midnight Drive", "Paper Lanterns", "Northbound Train");
    }

    @Test
    @DisplayName("forward and backward listings show the same songs in opposite order")
    void listingsAreReverses() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        String forward = p.forwardListing();
        String backward = p.backwardListing();
        Song[] songs = TestSongs.six();

        int last = -1;
        for (Song s : songs) {
            int at = forward.indexOf(s.toString());
            assertTrue(at > last, "forwardListing should show '" + s + "' after the previous song:\n" + forward);
            last = at;
        }
        last = -1;
        for (int i = songs.length - 1; i >= 0; i--) {
            int at = backward.indexOf(songs[i].toString());
            assertTrue(at > last, "backwardListing should show '" + songs[i] + "' next:\n" + backward);
            last = at;
        }
    }
}
