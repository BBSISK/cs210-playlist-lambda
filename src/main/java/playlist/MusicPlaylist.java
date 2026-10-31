package playlist;

import java.util.function.Consumer;

/**
 * Activities 3-6 and the extension: a playlist stored in YOUR OWN doubly linked list.
 *
 * <p>Rules from the practical: don't use java.util.LinkedList or ArrayList to store the songs.
 * The only storage is the chain of DLNode objects between {@code first} and {@code last}.
 *
 * <p>The fields are package-private so the tests (LinkChecker) can check every reference, not just
 * what gets printed. Don't make them private, and don't add other fields that hold songs.
 *
 * <p>Each method's Javadoc says what it must do, including the edge cases the tests check. Work
 * through the tests one activity at a time:
 * <pre>
 *   mvn test -Dtest=Activity3PlaylistTest
 *   mvn test -Dtest=Activity4AddBeforeTest
 *   ...
 * </pre>
 */
public class MusicPlaylist {

    /** What both listings return for an empty playlist (given, so the tests can compare). */
    public static final String EMPTY_LISTING = "(empty playlist)\n";

    DLNode<Song> first;
    DLNode<Song> last;
    /* * Activity 6: the song being played, or null if nothing is selected. */
    DLNode<Song> current;
    int size = 0;

    // ============================== Activity 3 ==============================

    /** True when there are no songs. */
    public boolean isEmpty() {
        return first == null;
    }

    /**
     * Append a song at the end. Must be O(1): use {@code last}, don't walk the list.
     * Think about what changes when the playlist is empty.
     */
    public void addSong(Song song) {
        DLNode<Song> node = new DLNode<>(song, null, null);
        if(isEmpty()) {
            first=node;
            last=node;
        }   else{
            last.successor=node; // sets the currentlast to point to new node

            node.previous=last;  // sets the previous pointer on the new node to point to the current last
            last=node;  // resets the last node to being the new node 
        }
        size++;

        }


    /** Add a song at the beginning. Must be O(1). Empty playlist: first AND last change. */
    public void prependSong(Song song) {
        DLNode<Song> node = new DLNode<>(song,null,null);

        if(isEmpty()) {

            first=node;
            last=node;

        }   else {

            node.successor=first;  // the enw node points to the current first node
            first.previous = node; // previous on current first node points to new ndoe
            first=node;  // first is now assigned to the new node 


        }

        size++;

    }

    /**
     * The playlist from first to last, one numbered line per song, each ending in '\n':
     * <pre>
     * 1. Midnight Drive — The Signals (215 seconds)
     * 2. Paper Lanterns — Ivy Hale (187 seconds)
     * </pre>
     * Return {@link #EMPTY_LISTING} when the playlist is empty. Hint: StringBuilder.
     */
    public String forwardListing() {
        StringBuilder sb = new StringBuilder();
        int counter =1;
    

        if(isEmpty()){

            return EMPTY_LISTING;
        } 
            DLNode<Song> currentSong = first;

            while (currentSong != null) {
                sb.append(counter +". " + currentSong.element +"\n");
                currentSong=currentSong.successor;
                counter++;
            }
            return sb.toString();
    }

    /** Same format, from last to first, by following {@code previous} references from last. */
    public String backwardListing() {
        StringBuilder sb = new StringBuilder();
        int counter =1;
    

        if(isEmpty()){

            return EMPTY_LISTING;
        }
            DLNode<Song> currentSong = last;

            while (currentSong != null) {
                sb.append(counter +". " + currentSong.element +"\n");
                currentSong=currentSong.previous;
                counter++;
            }
            return sb.toString();
        
    }

    /** Activity 3's printForward(): given, because the work is in forwardListing(). */
    public void printForward() {
        System.out.print(forwardListing());
    }

    /** Activity 3's printBackward(): given, because the work is in backwardListing(). */
    public void printBackward() {
        System.out.print(backwardListing());
    }

    // ============================== Activity 4 ==============================

    /**
     * Insert newSong immediately before the FIRST song whose title equals targetTitle
     * (case-sensitive, String.equals).
     *
     * @return true if the target was found and the song inserted; false if not found, if the
     *         playlist is empty, or if targetTitle is null (never throw)
     */
    public boolean addBefore(String targetTitle, Song newSong) {
    

        if(isEmpty()){

            return false;
        }
            DLNode<Song> currentSong = first;
            DLNode<Song> insertSong = new DLNode<>(newSong,null,null);


            while (currentSong != null) {
                if(currentSong.element.getTitle().equals(targetTitle)) {
                
                if(currentSong == first) {
                        insertSong.successor = currentSong;
                        currentSong.previous = insertSong;
                        first=insertSong; 
                        return true;
                }

                        insertSong.successor = currentSong;
                        insertSong.previous = currentSong.previous;
                        currentSong.previous.successor = insertSong;
                        currentSong.previous = insertSong;

                return true;
                    
                }
                currentSong=currentSong.successor;
            }
            return false;
    
    }

    // ============================== Activity 5 ==============================

    /**
     * Remove the FIRST song whose title equals the argument.
     *
     * <p>If the removed song is the {@code current} one (Activity 6), move current to its
     * successor; if it was the last song, to its previous; if it was the only song, to null.
     *
     * @return true if a song was removed; false if not found, if empty, or if title is null
     */
    public boolean removeSong(String title) {
 
        if(isEmpty()){
            return false;
        }
            DLNode<Song> currentSong = first;



            while (currentSong != null) {
                if(currentSong.element.getTitle().equals(title)) {
                
                if (currentSong == current) {
                    if (currentSong.successor != null) {
                    current = currentSong.successor;
                    } else {
                        current = currentSong.previous;     // null if it was the only song
                     }
                } 
                if(first == last) {
                    first= null;
                    last = null;
                    return true;
                }    

                    else if(currentSong == first) {
                        first=currentSong.successor;
                        first.previous=null;
                        return true;

                    }       else if(currentSong == last) {
                            last=currentSong.previous;
                            last.successor=null;
                            return true;

                        } 
                        
                        else {

                        currentSong.previous.successor= currentSong.successor;
                        currentSong.successor.previous = currentSong.previous;
                        current=currentSong.successor;
                        }
                        return true;  
            }
                currentSong=currentSong.successor;
            }
            return false;
    
    }

    // ============================== Activity 6 ==============================
    // Boundary rules for this project (document your reasoning in ANSWERS.md):
    //  - nextSong() at the last song returns null and current STAYS on the last song (no wrap).
    //  - previousSong() at the first song returns null and current STAYS on the first song.
    //  - before anything is selected (current == null), nextSong()/previousSong() return null.
    //  - none of these may throw a NullPointerException, even on an empty playlist.

    /** Select the first song and return it, or return null if the playlist is empty. */
    public Song playFirst() {
        if (first == null) {
            current = null;
            return null;
        }
        current = first;
        return current.element;
    }

    /** Move to the successor and return its song; see the boundary rules above. */
    public Song nextSong() {
        if(current ==null || current.successor ==null) {
            return null;
        }
        current=current.successor;
        return current.element;
    }

    /** Move to the previous node and return its song; see the boundary rules above. */
    public Song previousSong() {
            if(current ==null || current.previous ==null) {
            return null;
        }
        current=current.previous;
        return current.element;
    }

    /** The current song without moving, or null if nothing is selected. */
    public Song currentSong() {
        if(current ==null ){
            return null; 

        }
        return current.element;
    }

    // ============================== Extension ==============================

    /** Total playing time in seconds, by traversal. */
    public int totalDuration() {
        DLNode<Song> currentSong =  first;
        int totalDuration = 0;

        while(currentSong != null) {
            totalDuration = totalDuration + currentSong.element.getDurationSeconds();
            currentSong = currentSong.successor;
        }
        return totalDuration;
    }

    /** Number of songs, by traversal (no size field yet: see the question in ANSWERS.md). */
    public int countSongs() {
          DLNode<Song> currentSong =  first;
          int songCount = 0;

        while(currentSong != null) {
            songCount++;
            currentSong = currentSong.successor;
        }
        return songCount;
    }

    /**
     * Give every song, first to last, to {@code action}: {@code action.accept(song)}.
     * The Lambda API uses this to save the playlist and to show it in both directions.
     */
    public void forEachForward(Consumer<Song> action) {
          DLNode<Song> currentSong =  first;

        while(currentSong != null) {
            action.accept(currentSong.element);;
            currentSong = currentSong.successor;
        }
    }

    /** Same, last to first, following previous references. */
    public void forEachBackward(Consumer<Song> action) {
          DLNode<Song> currentSong =  last;

        while(currentSong != null) {
            action.accept(currentSong.element);;
            currentSong = currentSong.previous;
        }
    }
}
