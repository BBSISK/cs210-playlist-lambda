package playlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Activity 1: Song")
class Activity1SongTest {

    @Test
    @DisplayName("constructor stores title, artist and duration; getters return them")
    void constructorAndGetters() {
        Song s = new Song("Midnight Drive", "The Signals", 215);
        assertEquals("Midnight Drive", s.getTitle());
        assertEquals("The Signals", s.getArtist());
        assertEquals(215, s.getDurationSeconds());
    }

    @Test
    @DisplayName("toString matches the practical's example")
    void toStringFormat() {
        assertEquals("Midnight Drive — The Signals (215 seconds)",
                new Song("Midnight Drive", "The Signals", 215).toString());
    }

    @Test
    @DisplayName("three private instance variables")
    void fieldsArePrivate() {
        int instanceFields = 0;
        for (Field f : Song.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            instanceFields++;
            assertTrue(Modifier.isPrivate(f.getModifiers()), "Field '" + f.getName() + "' should be private");
        }
        assertEquals(3, instanceFields, "Song should have three instance variables: title, artist, durationSeconds");
    }

    @Test
    @DisplayName("six different test songs exist")
    void sixSongs() {
        Song[] songs = TestSongs.six();
        assertEquals(6, songs.length);
        for (int i = 0; i < songs.length; i++) {
            assertEquals(TestSongs.SIX_TITLES[i], songs[i].getTitle());
        }
    }
}
