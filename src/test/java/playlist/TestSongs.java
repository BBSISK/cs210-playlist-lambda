package playlist;

/** Test data: six made-up songs (Activity 1, step 4) plus two extras for inserting. */
final class TestSongs {

    private TestSongs() {}

    static Song midnightDrive() { return new Song("Midnight Drive", "The Signals", 215); }
    static Song paperLanterns() { return new Song("Paper Lanterns", "Ivy Hale", 187); }
    static Song northboundTrain() { return new Song("Northbound Train", "The Copper Pines", 242); }
    static Song saltAndStatic() { return new Song("Salt and Static", "Mara Quinn", 198); }
    static Song glassHarbour() { return new Song("Glass Harbour", "Lowland Echo", 263); }
    static Song lastBusHome() { return new Song("Last Bus Home", "The Night Shift", 176); }

    static Song openingAct() { return new Song("Opening Act", "Warm-Up Band", 120); }
    static Song interlude() { return new Song("Interlude", "Quiet Hours", 95); }

    /** Six songs, in this order. */
    static Song[] six() {
        return new Song[] {
            midnightDrive(), paperLanterns(), northboundTrain(),
            saltAndStatic(), glassHarbour(), lastBusHome()
        };
    }

    static final String[] SIX_TITLES = {
        "Midnight Drive", "Paper Lanterns", "Northbound Train",
        "Salt and Static", "Glass Harbour", "Last Bus Home"
    };

    static MusicPlaylist playlistOfSix() {
        MusicPlaylist p = new MusicPlaylist();
        for (Song s : six()) {
            p.addSong(s);
        }
        return p;
    }
}
