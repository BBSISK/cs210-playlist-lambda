package playlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Checks every reference in the list, not just what printForward shows.
 *
 * <p>This is what the practical means by "forward and backward traversal are consistent" and
 * "first and last references are correct". Most linked-list bugs pass a forward-only check: e.g.
 * forgetting to set {@code successor.previous} when removing.
 */
final class LinkChecker {

    private LinkChecker() {}

    static void assertLinks(MusicPlaylist p, String... expectedTitles) {
        List<String> expected = Arrays.asList(expectedTitles);

        if (expected.isEmpty()) {
            assertTrue(p.isEmpty(), "isEmpty() should be true");
            assertNull(p.first, "first should be null when the playlist is empty");
            assertNull(p.last, "last should be null when the playlist is empty");
            assertNull(p.current, "current should be null when the playlist is empty");
            return;
        }

        assertFalse(p.isEmpty(), "isEmpty() should be false");
        assertNotNull(p.first, "first is null but the playlist should hold " + expected);
        assertNotNull(p.last, "last is null but the playlist should hold " + expected);
        assertNull(p.first.previous,
                "first.previous should be null, but it points to '" + title(p.first.previous) + "'");
        assertNull(p.last.successor,
                "last.successor should be null, but it points to '" + title(p.last.successor) + "'");

        // Walk forwards, checking each node's previous points back to the node we came from.
        List<String> forward = new ArrayList<>();
        DLNode<Song> came = null;
        int steps = 0;
        for (DLNode<Song> n = p.first; n != null; n = n.successor) {
            if (++steps > expected.size() + 5) {
                fail("Following successor links never reaches null: there is a loop. Seen so far: " + forward);
            }
            assertSame(came, n.previous, "'" + title(n) + "'.previous should be '" + title(came)
                    + "' but is '" + title(n.previous) + "'");
            forward.add(title(n));
            came = n;
        }
        assertEquals(expected, forward, "Order when following successor links from first");
        assertSame(p.last, came, "last should be the node you reach by following successor links ('"
                + title(came) + "'), but last is '" + title(p.last) + "'");

        // Walk backwards from last: must give exactly the reverse.
        List<String> backward = new ArrayList<>();
        steps = 0;
        for (DLNode<Song> n = p.last; n != null; n = n.previous) {
            if (++steps > expected.size() + 5) {
                fail("Following previous links never reaches null: there is a loop. Seen so far: " + backward);
            }
            backward.add(title(n));
        }
        Collections.reverse(backward);
        assertEquals(expected, backward, "Order when following previous links from last (shown reversed)");

        // current (Activity 6) must be null or one of the nodes in this list.
        if (p.current != null) {
            boolean found = false;
            for (DLNode<Song> n = p.first; n != null; n = n.successor) {
                if (n == p.current) found = true;
            }
            assertTrue(found, "current points to '" + title(p.current) + "', which is no longer in the list");
        }
    }

    private static String title(DLNode<Song> node) {
        if (node == null) return "null";
        return node.element == null ? "(node with null element)" : node.element.getTitle();
    }
}
