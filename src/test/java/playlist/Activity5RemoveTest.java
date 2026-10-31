package playlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static playlist.LinkChecker.assertLinks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Activity 5: removeSong")
class Activity5RemoveTest {

    @Test
    @DisplayName("remove the first song: first moves on, new first.previous is null")
    void removeFirst() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertTrue(p.removeSong("Midnight Drive"));
        assertLinks(p, "Paper Lanterns", "Northbound Train", "Salt and Static", "Glass Harbour", "Last Bus Home");
    }

    @Test
    @DisplayName("remove the last song: last moves back, new last.successor is null")
    void removeLast() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertTrue(p.removeSong("Last Bus Home"));
        assertLinks(p, "Midnight Drive", "Paper Lanterns", "Northbound Train", "Salt and Static", "Glass Harbour");
    }

    @Test
    @DisplayName("remove a middle song: both neighbours are re-linked")
    void removeMiddle() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertTrue(p.removeSong("Northbound Train"));
        assertLinks(p, "Midnight Drive", "Paper Lanterns", "Salt and Static", "Glass Harbour", "Last Bus Home");
    }

    @Test
    @DisplayName("remove the only song: first and last both become null")
    void removeOnly() {
        MusicPlaylist p = new MusicPlaylist();
        p.addSong(TestSongs.midnightDrive());
        assertTrue(p.removeSong("Midnight Drive"));
        assertLinks(p);
    }

    @Test
    @DisplayName("title not present: returns false, list unchanged")
    void removeMissing() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertFalse(p.removeSong("No Such Song"));
        assertLinks(p, TestSongs.SIX_TITLES);
    }

    @Test
    @DisplayName("empty playlist and null title: false, no crash")
    void removeFromEmptyAndNull() {
        MusicPlaylist p = new MusicPlaylist();
        assertFalse(p.removeSong("Midnight Drive"));
        assertFalse(p.removeSong(null));
        assertLinks(p);
    }

    @Test
    @DisplayName("duplicate titles: removes only the FIRST match")
    void removeFirstOfDuplicates() {
        MusicPlaylist p = new MusicPlaylist();
        p.addSong(TestSongs.midnightDrive());
        p.addSong(TestSongs.paperLanterns());
        p.addSong(new Song("Midnight Drive", "Cover Band", 200));
        assertTrue(p.removeSong("Midnight Drive"));
        assertLinks(p, "Paper Lanterns", "Midnight Drive");
        assertEquals("Cover Band", p.first.successor.element.getArtist());
    }

    @Test
    @DisplayName("remove all six one by one, from alternating ends, until empty")
    void removeAll() {
        MusicPlaylist p = TestSongs.playlistOfSix();
        assertTrue(p.removeSong("Midnight Drive"));
        assertTrue(p.removeSong("Last Bus Home"));
        assertLinks(p, "Paper Lanterns", "Northbound Train", "Salt and Static", "Glass Harbour");
        assertTrue(p.removeSong("Salt and Static"));
        assertTrue(p.removeSong("Paper Lanterns"));
        assertLinks(p, "Northbound Train", "Glass Harbour");
        assertTrue(p.removeSong("Glass Harbour"));
        assertLinks(p, "Northbound Train");
        assertTrue(p.removeSong("Northbound Train"));
        assertLinks(p);
        assertFalse(p.removeSong("Northbound Train"));
    }

    @Test
    @DisplayName("after removals the list still works: add, prepend and insert again")
    void reuseAfterRemoval() {
        MusicPlaylist p = new MusicPlaylist();
        p.addSong(TestSongs.midnightDrive());
        p.removeSong("Midnight Drive");
        p.addSong(TestSongs.paperLanterns());
        p.prependSong(TestSongs.openingAct());
        p.addBefore("Paper Lanterns", TestSongs.interlude());
        assertLinks(p, "Opening Act", "Interlude", "Paper Lanterns");
    }
}
