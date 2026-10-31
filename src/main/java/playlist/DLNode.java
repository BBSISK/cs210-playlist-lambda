package playlist;

/**
 * Activity 2: a generic doubly linked node.
 *
 * <p>The three fields are given in the practical (names match the garage example). They are
 * package-private so MusicPlaylist and the tests can follow the links.
 *
 * <p>Tests: Activity2DLNodeTest
 */
public class DLNode<T> {

    T element;
    DLNode<T> previous;
    DLNode<T> successor;

    /** TODO Activity 2: initialise all three fields. */
    public DLNode(T element, DLNode<T> previous, DLNode<T> successor) {
        // your code here
        this.element = element;
        this.previous = previous;
        this.successor= successor;
    }
}
