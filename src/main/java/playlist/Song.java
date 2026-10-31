package playlist;

/**
 * Activity 1: one song in the playlist.
 *
 * <p>Tests: Activity1SongTest. Run with {@code mvn test -Dtest=Activity1SongTest}
 */
public class Song {

    // TODO Activity 1: three PRIVATE instance variables: title (String), artist (String),
    //  durationSeconds (int).

    private String title;
    private String artist;
    private int durationSeconds;

    /** TODO Activity 1: initialise all three instance variables. */
    public Song(String title, String artist, int durationSeconds) {
        // your code here
        this.title=title;
        this.artist=artist;
        this.durationSeconds= durationSeconds;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * TODO Activity 1: a readable description, exactly in this format (the tests check it):
     * {@code Midnight Drive — The Signals (215 seconds)}
     * The separator is a space, an em dash (—), and a space.
     */
    @Override
    public String toString() {
        String str;
        str = title + " \u2014 " + artist + " (" + durationSeconds + " seconds)";
        return str;
    }
}
