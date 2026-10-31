package playlist;

/**
 * Activity 7: a main() that demonstrates the whole workflow on the console.
 *
 * <p>Run it with {@code mvn -q compile exec:java}
 *
 * <p>The JUnit tests check the behaviour; this is the demonstration the practical asks for, and the
 * place to practise predicting output. BEFORE running, write the expected output for at least three
 * of the steps in ANSWERS.md. If the real output differs, work out which reference
 * (first, last, previous or successor) is wrong.
 */
public class PlaylistMain {

    public static void main(String[] args) {
        // TODO Activity 7, step 5: create an empty playlist and display it (both directions).
        MusicPlaylist myList = new MusicPlaylist();
        myList.printForward();
        myList.printBackward();


        // TODO step 6: add six songs (Activity 1, step 4) and display them both ways.
        Song Song1 = new Song("First1", "ABBA", 100);
        Song Song2 = new Song("First2", "BccB", 120);
        Song Song3 = new Song("First3", "CddC", 140);
        Song Song4 = new Song("First4", "Deed", 160);
        Song Song5 = new Song("First5", "EffE", 180);
        Song Song6 = new Song("First6", "FggF", 200);

        myList.addSong(Song1);
        myList.addSong(Song2);
        myList.addSong(Song3);
        myList.addSong(Song4);
        myList.addSong(Song5);
        myList.addSong(Song6);

        //myList.printForward();
        //myList.printBackward();


        // TODO step 7: prepend one song, and insert another before an existing song. Display.
        myList.prependSong(Song6);
        myList.addBefore("First4", Song1);



        // TODO step 8: remove songs from the beginning, middle and end. Display both ways after each.
        myList.removeSong("First3");
        myList.removeSong("First6");
        myList.removeSong("First6");

        //myList.printForward();
        //myList.printBackward();

        // TODO step 9: playFirst(), then nextSong() to the end and previousSong() back to the start,

System.out.println("Playing forwards:");
Song s = myList.playFirst();
while (s != null) {
    System.out.println("  " + s);
    s = myList.nextSong();
}
System.out.println("At the end, nextSong() returns: " + myList.nextSong());
System.out.println("current is still: " + myList.currentSong());
        
        //  printing each song. Show what happens at both ends.

        // TODO step 10: addBefore and removeSong with a title that doesn't exist; print the result.

        // TODO step 11: remove every song, then show isEmpty() is true and both listings are empty.
    }
}
