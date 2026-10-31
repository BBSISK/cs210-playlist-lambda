package playlist;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static playlist.LinkChecker.assertLinks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Boundary behaviour chosen for this project (Activity 6 asks you to decide and document it;
 * if you choose differently, change these tests AND the Javadoc in MusicPlaylist together):
 *
 * - nextSong() at the last song returns null and current STAYS on the last song (no wrap-around).
 * - previousSong() at the first song returns null and current STAYS on the first song.
 * - nextSong()/previousSong() before anything is selected return null (call playFirst() first).
 * - removing the current song moves current to its successor, or to its previous if it was last,
 *   or to null if it was the only song.
 */
@DisplayName("Activity 6: navigation")
class Activity6NavigationTest {

    private static String title(Song s) {
        return s == null ? null : s.getTitle();
    }

    @Test
    @DisplayName("empty playlist: every navigation method returns null, never throws")
    void emptyIsSafe() {
        MusicPlaylist p = new MusicPlaylist();
        assertDoesNotThrow(() -> {
            assertNull(p.currentSong());
            assertNull(p.playFirst());
            assertNull(p.nextSong());
            assertNull(p.previousSong());
            assertNull(p.currentSong());
        });
    }

    @Test
    @DisplayName("nothing selected yet: next/previous return null")
    void nothingSelected() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertNull(p.currentSong());
        assertNull(p.nextSong());
        assertNull(p.previousSong());
        assertNull(p.currentSong());
    }

    @Test
    @DisplayName("playFirst selects and returns the first song")
    void playFirst() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertEquals("Midnight Drive", title(p.playFirst()));
        assertEquals("Midnight Drive", title(p.currentSong()));
        assertSame(p.first, p.current);
    }

    @Test
    @DisplayName("walk forwards to the end, then stay at the end")
    void forwardToEnd() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        p.playFirst();
        for (int i = 1; i < TestSongs.SIX_TITLES.length; i++) {
            assertEquals(TestSongs.SIX_TITLES[i], title(p.nextSong()));
        }
        assertNull(p.nextSong(), "At the last song nextSong() should return null");
        assertEquals("Last Bus Home", title(p.currentSong()), "current should stay on the last song");
        assertSame(p.last, p.current);
    }

    @Test
    @DisplayName("walk backwards to the start, then stay at the start")
    void backwardToStart() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        p.playFirst();
        while (p.nextSong() != null) { /* move to the end */ }
        for (int i = TestSongs.SIX_TITLES.length - 2; i >= 0; i--) {
            assertEquals(TestSongs.SIX_TITLES[i], title(p.previousSong()));
        }
        assertNull(p.previousSong(), "At the first song previousSong() should return null");
        assertEquals("Midnight Drive", title(p.currentSong()));
    }

    @Test
    @DisplayName("single song: next and previous both stay put")
    void singleSong() {
        MusicPlaylist p = new MusicPlaylist();
        p.addSong(TestSongs.midnightDrive());
        assertEquals("Midnight Drive", title(p.playFirst()));
        assertNull(p.nextSong());
        assertNull(p.previousSong());
        assertEquals("Midnight Drive", title(p.currentSong()));
    }

    @Test
    @DisplayName("currentSong does not move")
    void currentDoesNotMove() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        p.playFirst();
        p.nextSong();
        assertEquals("Paper Lanterns", title(p.currentSong()));
        assertEquals("Paper Lanterns", title(p.currentSong()));
    }

    @Test
    @DisplayName("navigation sees songs added with prepend and addBefore")
    void navigationAfterInsertions() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        p.prependSong(TestSongs.openingAct());
        p.addBefore("Paper Lanterns", TestSongs.interlude());
        assertEquals("Opening Act", title(p.playFirst()));
        assertEquals("Midnight Drive", title(p.nextSong()));
        assertEquals("Interlude", title(p.nextSong()));
        assertEquals("Paper Lanterns", title(p.nextSong()));
        assertEquals("Interlude", title(p.previousSong()));
    }

    @Test
    @DisplayName("removing the current song moves current to the next song")
    void removeCurrentMiddle() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        p.playFirst();
        p.nextSong(); // Paper Lanterns
        p.removeSong("Paper Lanterns");
        assertEquals("Northbound Train", title(p.currentSong()));
        assertLinks(p, "Midnight Drive", "Northbound Train", "Salt and Static", "Glass Harbour", "Last Bus Home");
        assertEquals("Midnight Drive", title(p.previousSong()));
    }

    @Test
    @DisplayName("removing the current LAST song moves current back one")
    void removeCurrentLast() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        p.playFirst();
        while (p.nextSong() != null) { /* move to the end */ }
        p.removeSong("Last Bus Home");
        assertEquals("Glass Harbour", title(p.currentSong()));
        assertNull(p.nextSong());
    }

    @Test
    @DisplayName("removing the only (current) song leaves nothing selected")
    void removeCurrentOnly() {
        MusicPlaylist p = new MusicPlaylist();
        p.addSong(TestSongs.midnightDrive());
        p.playFirst();
        p.removeSong("Midnight Drive");
        assertNull(p.currentSong());
        assertNull(p.nextSong());
        assertLinks(p);
    }

    @Test
    @DisplayName("removing a different song leaves current where it was")
    void removeOtherSong() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        p.playFirst();
        p.nextSong();
        p.nextSong(); // Northbound Train
        p.removeSong("Midnight Drive");
        p.removeSong("Last Bus Home");
        assertEquals("Northbound Train", title(p.currentSong()));
        assertLinks(p, "Paper Lanterns", "Northbound Train", "Salt and Static", "Glass Harbour");
    }
}
