# CS210 Week 5: written answers

Fill this in as you go. It's the "brief text file explaining your Big O answers" the practical asks for, and it doubles as exam revision.

## Activity 2: three connected nodes

Draw `[A] <-> [B] <-> [C]` and show where each `previous` and `successor` reference points, including the two that point to `null`.


```
                Node A                                      Node B                                      Node C
                +----------+---------+---------+            +----------+---------+---------+            +----------+---------+---------+
null      <---  | previous | element |successor| <->        | previous | element |successor| <->        | previous | element |successor| ---> null
                |    null  |   "A"   | Node B  |            |  Node A  |   "B"   |  Node C |            |  Node B  |   "C"   |    null |
                +----------+---------+---------+            +----------+---------+---------+            +----------+---------+---------+
                                   ^
                                 first = Node A                                                        last = Node C
```



## Activity 4: inserting between two nodes

Before coding, list the four reference changes needed to insert `N` between `P` and `T` (the target):

1.
2.
3.
4.

What extra change is needed when inserting before the first song?

>

## Activity 5: removing

Why would updating only `successor` references not be enough?

>

## Activity 6: boundary behaviour

What should `nextSong()` do at the last song, and `previousSong()` at the first? Why did you choose that? (The tests assume "return null and stay put". If you prefer wrap-around, change the tests and the Javadoc together.)

>

What happens to `current` when the current song is removed, and why?

>

## Activity 7: predicted output (write this BEFORE running PlaylistMain)

| Step | What I expect to see | What I actually saw | If different: which reference was wrong? |
|---|---|---|---|
| | | | |
| | | | |
| | | | |

## Activity 8: Big O (worst case, n = number of songs)

| Operation | Big O | Explain the work performed |
|---|---|---|
| Check whether playlist is empty | | |
| Append using a last reference | | |
| Prepend using a first reference | | |
| Print all songs forwards | | |
| Print all songs backwards | | |
| Find a song by title | | |
| Insert before a song identified by title | | |
| Remove a song identified by title | | |
| Move to next/previous song when current is known | | |

**Discussion.** Why is adding at the end of this doubly linked list different from inserting at the beginning of an ArrayList?

>

Why is searching by title not necessarily faster in a linked list?

>

## Extension: statistics

Big O of `totalDuration()` and `countSongs()`, and why:

>

How would an extra `size` field change the cost of `countSongs()`, and which methods would then have to maintain it?

>

## Beyond the practical: the Lambda version

Each API request loads the playlist from DynamoDB, rebuilds it with `addSong` (n calls), runs one operation and saves it. What is the Big O of a single "next song" request, as opposed to `nextSong()` on its own? Where does the time go?

>
