package playlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Activity 2: DLNode")
class Activity2DLNodeTest {

    @Test
    @DisplayName("constructor sets element, previous and successor")
    void constructorSetsAllThree() {
        DLNode<String> before = new DLNode<>("A", null, null);
        DLNode<String> after = new DLNode<>("C", null, null);
        DLNode<String> middle = new DLNode<>("B", before, after);
        assertEquals("B", middle.element);
        assertSame(before, middle.previous);
        assertSame(after, middle.successor);
    }

    @Test
    @DisplayName("three connected nodes: the diagram from the practical")
    void threeConnectedNodes() {
        //   null <- [A] <-> [B] <-> [C] -> null
        DLNode<String> a = new DLNode<>("A", null, null);
        DLNode<String> b = new DLNode<>("B", a, null);
        DLNode<String> c = new DLNode<>("C", b, null);
        a.successor = b;
        b.successor = c;

        assertNull(a.previous);
        assertSame(b, a.successor);
        assertSame(a, b.previous);
        assertSame(c, b.successor);
        assertSame(b, c.previous);
        assertNull(c.successor);
        assertEquals("C", a.successor.successor.element);
        assertEquals("A", c.previous.previous.element);
    }

    @Test
    @DisplayName("DLNode is generic: it holds Songs too")
    void holdsSongs() {
        Song s = TestSongs.midnightDrive();
        DLNode<Song> node = new DLNode<>(s, null, null);
        assertSame(s, node.element);
    }
}
