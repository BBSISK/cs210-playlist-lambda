package playlist;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static playlist.LinkChecker.assertLinks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Activity 4: addBefore")
class Activity4AddBeforeTest {

    @Test
    @DisplayName("before the first song: first must change")
    void beforeFirst() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertTrue(p.addBefore("Midnight Drive", TestSongs.openingAct()));
        assertLinks(p, "Opening Act", "Midnight Drive", "Paper Lanterns", "Northbound Train",
                "Salt and Static", "Glass Harbour", "Last Bus Home");
    }

    @Test
    @DisplayName("before a middle song: four references change")
    void beforeMiddle() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertTrue(p.addBefore("Salt and Static", TestSongs.interlude()));
        assertLinks(p, "Midnight Drive", "Paper Lanterns", "Northbound Train",
                "Interlude", "Salt and Static", "Glass Harbour", "Last Bus Home");
    }

    @Test
    @DisplayName("before the last song: last must NOT change")
    void beforeLast() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertTrue(p.addBefore("Last Bus Home", TestSongs.interlude()));
        assertLinks(p, "Midnight Drive", "Paper Lanterns", "Northbound Train",
                "Salt and Static", "Glass Harbour", "Interlude", "Last Bus Home");
    }

    @Test
    @DisplayName("before the only song")
    void beforeOnlySong() {
        MusicPlaylist p = new MusicPlaylist();
        p.addSong(TestSongs.midnightDrive());
        assertTrue(p.addBefore("Midnight Drive", TestSongs.openingAct()));
        assertLinks(p, "Opening Act", "Midnight Drive");
    }

    @Test
    @DisplayName("target title not found: returns false and changes nothing")
    void targetMissing() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertFalse(p.addBefore("No Such Song", TestSongs.interlude()));
        assertLinks(p, TestSongs.SIX_TITLES);
    }

    @Test
    @DisplayName("empty playlist: returns false, no crash")
    void emptyPlaylist() {
        MusicPlaylist p = new MusicPlaylist();
        assertFalse(p.addBefore("Midnight Drive", TestSongs.interlude()));
        assertLinks(p);
    }

    @Test
    @DisplayName("null target title: returns false, no crash")
    void nullTarget() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertFalse(p.addBefore(null, TestSongs.interlude()));
        assertLinks(p, TestSongs.SIX_TITLES);
    }

    @Test
    @DisplayName("duplicate titles: inserts before the FIRST match")
    void duplicateTitles() {
        MusicPlaylist p = new MusicPlaylist();
        p.addSong(TestSongs.midnightDrive());
        p.addSong(TestSongs.paperLanterns());
        p.addSong(new Song("Midnight Drive", "Cover Band", 200));
        assertTrue(p.addBefore("Midnight Drive", TestSongs.interlude()));
        assertLinks(p, "Interlude", "Midnight Drive", "Paper Lanterns", "Midnight Drive");
    }
}
