# CS210 Playlist API: a doubly linked list on AWS Lambda

A music playlist manager built on my own **doubly linked list in Java**, served as a **serverless API** on **AWS Lambda + Amazon API Gateway**, with playlists saved in **Amazon DynamoDB**.

The linked list is the Week 5 practical from CS210 Algorithms and Data Structures (Higher Diploma in Software Development, Maynooth University): songs can be added, inserted, removed and navigated in both directions. The practical forbids `java.util.LinkedList` and `ArrayList` for storing the playlist, so the only storage is a chain of `DLNode`s with `first`, `last`, `previous` and `successor` references. This project puts that data structure behind a real HTTP API so it gets used, not just unit-tested.

**Status:** deployed and working in eu-west-1

## Architecture

```
client ──HTTP──▶ API Gateway (REST, throttled) ──▶ Lambda (Java 21, arm64)
                                                     │  1. load playlist item
                                                     │  2. rebuild MusicPlaylist (doubly linked list)
                                                     │  3. run ONE operation (addBefore, removeSong, nextSong...)
                                                     │  4. save with a version check
                                                     ▼
                                                  DynamoDB (on-demand, TTL)
```

- **Lambda is stateless**, so nothing survives between requests in memory. Each request rebuilds the linked list from the stored songs, runs one `MusicPlaylist` method, and writes the result back.
- **DynamoDB** stores one item per playlist: the songs in order, the index of the current song, and a version number.
- **Optimistic locking:** every save is a conditional write (`version = :expected`). If two requests change the same playlist at the same moment, the second gets **409 Conflict** instead of silently overwriting the first.
- **Self-cleaning:** a TTL attribute deletes a demo playlist 30 days after its last change.
- **Least privilege:** the function's IAM policy allows only `GetItem`, `PutItem` and `DeleteItem` on this one table.
- **Abuse limits:** API Gateway throttling (5 requests/s, burst 10), at most 200 songs per playlist, and input validation. Error responses never include internals.
- **Infrastructure as code:** `template.yaml` (AWS SAM) defines the table, API, function and permissions.

## Project layout

```
src/main/java/playlist/
  Song.java            Activity 1   title, artist, durationSeconds
  DLNode.java          Activity 2   generic node: element, previous, successor
  MusicPlaylist.java   Activities 3-6 + extension: the doubly linked list
  PlaylistMain.java    Activity 7   console demonstration
src/main/java/playlist/api/
  PlaylistHandler.java       Lambda entry point: routing, validation, JSON
  PlaylistCodec.java         stored list <-> linked list (uses only MusicPlaylist's public methods)
  PlaylistStore.java         storage interface + optimistic-locking contract
  DynamoDbPlaylistStore.java DynamoDB implementation
  InMemoryPlaylistStore.java same rules, in memory (for tests)
src/test/java/playlist/      one JUnit test class per activity + LinkChecker
ANSWERS.md                   diagrams, predictions and Big O analysis
template.yaml                SAM: DynamoDB table, API Gateway, Lambda, IAM
```

## Tests

```bash
mvn test                                   # everything
mvn test -Dtest=Activity3PlaylistTest      # one activity at a time
mvn -q compile exec:java                   # run the Activity 7 demo (PlaylistMain)
```

There is one test class per activity (`Activity1SongTest` … `Activity6NavigationTest`, `ExtensionStatisticsTest`) plus `PlaylistHandlerTest` for the API.

The key helper is **`LinkChecker`**. After every operation it walks the list forwards from `first` and backwards from `last`, and checks every `previous`/`successor` pair, that `first.previous` and `last.successor` are null, that `last` really is the end of the chain, and that `current` still points into the list. Many linked-list bugs pass a forward-only print, such as forgetting to update `successor.previous` on removal. This check catches them and names the node that is wrong.

The tests cover the cases the practical lists: empty, one song and six songs; insert before the first song, a middle song, and a missing title; remove the first, middle, last and only song, and a missing title; safe navigation at both ends.

## API

Base URL: the `PlaylistApiUrl` output of `sam deploy`. Playlist ids are 1-64 letters, digits, `-` or `_`.

| Method and path | Body / query | Linked-list method used |
|---|---|---|
| `POST /playlists/{id}/songs` | `{"title","artist","durationSeconds"}` | `addSong` |
| | `… ,"position":"start"}` | `prependSong` |
| | `… ,"before":"Existing title"}` | `addBefore` (404 if not found) |
| `DELETE /playlists/{id}/songs?title=…` | | `removeSong` (404 if not found) |
| `POST /playlists/{id}/play` | `{"action":"first"\|"next"\|"previous"\|"current"}` | `playFirst`, `nextSong`, `previousSong`, `currentSong` |
| `GET /playlists/{id}` | | `forEachForward`, `forEachBackward`, `countSongs`, `totalDuration` |
| `DELETE /playlists/{id}` | | deletes the stored playlist |

Every response includes the playlist **forwards and backwards**. These come from two separate traversals (`successor` links from `first`, `previous` links from `last`), so a broken link would show up in the API too.

```bash
API=https://<api-id>.execute-api.eu-west-1.amazonaws.com/Prod
curl -s -X POST "$API/playlists/demo/songs" -d '{"title":"Midnight Drive","artist":"The Signals","durationSeconds":215}'
curl -s -X POST "$API/playlists/demo/songs" -d '{"title":"Opening Act","artist":"Warm-Up Band","durationSeconds":120,"position":"start"}'
curl -s -X POST "$API/playlists/demo/play"  -d '{"action":"first"}'
curl -s -X POST "$API/playlists/demo/play"  -d '{"action":"next"}'
curl -s -X DELETE "$API/playlists/demo/songs?title=Opening%20Act"
curl -s "$API/playlists/demo"
```

## Deploy

Prerequisites: JDK 21, Maven, the AWS CLI (configured) and the SAM CLI.

```bash
mvn test                 # must pass first
sam validate --lint
sam build
sam deploy --guided      # stack name e.g. cs210-playlist-api, region eu-west-1
```

Remove everything (API, function, table and data) with `sam delete`.

**Cost:** pay-per-request throughout. Lambda and API Gateway charge per call, and DynamoDB on-demand charges per read/write, with storage in the always-free 25 GB. Idle cost is zero. A demo like this costs cents at most, within the AWS Free Tier credits.

## Design notes and limitations

- **Each request costs O(n), even for O(1) list operations.** Rebuilding the list is n `addSong` calls (each O(1) thanks to `last`). So "next song" is O(1) inside `MusicPlaylist` but O(n) per API request, plus DynamoDB round trips. That's fine at 200 songs. A production design would store each song as its own item with neighbour keys, or keep the list in a long-lived server.
- **Songs are stored as a JSON string attribute**, not as native DynamoDB lists, to keep the storage code small. The 200-song cap keeps items well under DynamoDB's 400 KB item limit.
- **Navigation is saved.** Moving to the next song is a write, because `current` is part of the playlist's state.
- **No authentication.** Anyone with the URL and a playlist id can change that playlist, so the live URL isn't published here. API Gateway API keys or Cognito would be the next step.
- **Cold starts:** Java on Lambda takes a few seconds on the first request after idle. Lambda SnapStart would reduce this.

## How this was built

The CS210 classes (`Song`, `DLNode`, `MusicPlaylist`, `PlaylistMain`) and the written answers are my own work for the practical. The JUnit tests, the `LinkChecker` and the AWS layer (`playlist.api`, `template.yaml`) were scaffolded with an AI assistant and reviewed by me. It follows on from my first serverless project, [dice-roll-api](https://github.com/BBSISK/dice-roll-api), adding Java, a database and conditional writes.
