# Quizly — Online Quiz Application (Group 4)

A compact Java web application for multiple-choice learning quizzes. It is
built around the syllabus requirements:

For the complete architecture, feature implementation, Firestore layout,
development setup, production hosting, and handover notes, see
[PROJECT_HANDOVER.md](./PROJECT_HANDOVER.md).

- **Lambda expressions** — predicates, comparators, streams and method
  references drive filtering, randomisation and sorting.
- **Collections framework** — hash maps for lookups, linked maps/lists for
  attempt state, shuffling with `Collections.shuffle`.
- **JSP** — JSP views with JSTL + Expression Language render every page; there
  are no scriptlets in the views.
- **DSA** — the leaderboard uses a bounded min-heap (`PriorityQueue`) to keep
  only the top K results, giving `O(n log K)` selection instead of sorting all
  rows; repositories and filters use standard collection operations.

## Features

- Three quiz tracks: **Java Fundamentals**, **DSA Essentials**, **Web & JSP**.
- Twenty-four questions in each track's bank (eight per difficulty); each attempt
  samples six questions (two per difficulty) and adapts toward harder or
  easier questions based on each answer.
- Answers are evaluated on completion and a full answer review is shown.
- Percentage score, feedback card, pass indication and rank banner.
- Daily streaks, persistent XP, 100-XP levels, and five unlockable badges.
- Two once-per-quiz power-ups: 50/50 and double points. Each can be used once
  per attempt; double points adds one point and 10 XP for a correct answer.
- Voice mode reads questions and choices aloud and can select a spoken answer.
- A protected admin quiz planner manages tracks and MCQs, including drafts,
  publishing, archiving, restoring and a content change log.
- Live room-code games with 2–20 signed-in players and a WebSocket leaderboard.
- Restart-safe leaderboard (top 10) by quiz and overall.
- Basic CSRF protection, 24-hour servlet-session inactivity timeout, UTF-8
  encoding and security headers.
- Playful warm theme (cream · orange · sky · violet) with black pill badges,
  colour-mosaic stat tiles, a gold/silver/bronze top-3 podium, and a floating
  trophy on the win screen.
- Polished motion design: staggered card/row reveals, an animated score ring,
  option hover/selection feedback and a result-page celebration (theme-coloured
  confetti plus a quiet, synthesised win chime via the Web Audio API — no audio
  files or extra dependencies). Everything respects `prefers-reduced-motion`.
- Unit tests for adaptive attempts, power-ups, progression, live-room rules,
  repositories and Firestore-backed data access.

## Technology

| Layer | Technology |
|---|---|
| Language / runtime | Java 17 |
| Web framework | Jakarta Servlet 6.0 (`@WebServlet`, `@WebFilter`, `@WebListener`) |
| Live updates | Jakarta WebSocket 2.1 |
| View | JSP 3.1 + JSTL 3.0 (`jakarta.tags.*`) |
| Persistence | Firebase Cloud Firestore (Firebase Admin SDK) |
| Build/product | Maven WAR + Cargo Tomcat 10.1 profile |
| Test | JUnit 5 |

## Requirements

- JDK 17+
- Maven 3.8+
- A Firebase project with Cloud Firestore enabled
- Google Application Default Credentials (a service account JSON key kept outside the repository)
- A modern browser; microphone permission and Web Speech API support are needed for voice answers
- No Tomcat installation is required if you use the included dev profile.

> **WSL note:** from WSL, building directly from a mounted Windows path can
> fail while Maven is writing its target files (Path/ACL restrictions on
> mounted drives). If that happens, copy the project to an Ubuntu-only path
> such as `/tmp/quizly` or build from Windows itself.

## Run locally

From the project root:

```bash
mvn -Pdev clean package cargo:run
```

Maven downloads Tomcat 10.1.61 into `target/`, packages the web app as
`target/quizly.war`, deploys it and waits for requests. The dev server uses
**port 8081** so it does not clash with a personal Tomcat on 8080. Open:

```
http://localhost:8081/quizly/
```

Configure Firebase before starting the app. In PowerShell, set the project ID
and path to your downloaded service-account key:

```powershell
$env:QUIZLY_FIREBASE_PROJECT_ID = "your-firebase-project-id"
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\secure\path\firebase-service-account.json"
mvn -Pdev clean package cargo:run
```

Keep the service-account key private and outside the project folder. Never
commit it or paste it into chat. Firestore must be enabled in the Firebase
console. The app uses the server-side Firebase Admin SDK, which authenticates
with Application Default Credentials.
Voice recognition is browser-dependent (for example, Chrome and Edge support
the Web Speech API); unsupported browsers still have the regular answer controls.

You can change the port again by editing `cargo.servlet.port` in the `dev`
profile of `pom.xml`.

To stop the dev server press `Ctrl + C`.

## Host on Vercel

Quizly is deployed as a Tomcat container using the root `Dockerfile`. Set these
Production environment variables in the Vercel project:

- `QUIZLY_FIREBASE_PROJECT_ID`
- `QUIZLY_FIREBASE_CREDENTIALS_JSON` — the complete service-account JSON,
  stored as a sensitive variable

Never commit or share the service-account key. Redeploy after updating
environment variables. The current production URL is
<https://online-quiz-app-nine-sooty.vercel.app/>.

The application currently stores servlet sessions and live-room state in
process memory. Vercel may recycle or route requests across container
instances, so active quiz sessions and multiplayer rooms are not guaranteed to
survive those events. See [PROJECT_HANDOVER.md](./PROJECT_HANDOVER.md#production-deployment)
before relying on those flows for production users.

## Deploy to an existing Tomcat

1. Build without the dev profile:
   ```bash
   mvn clean package
   ```
2. Copy `target/quizly.war` into Tomcat 10.1's `webapps/` folder.
3. Start Tomcat and open `http://localhost:8080/quizly/`.

Tomcat 10.x is required (jakarta.* packages). Tomcat 8/9 is not compatible.

## Run tests

```bash
mvn test
```

## Routes

| Route | Method | Purpose |
|---|---|---|
| `/` | GET | Home, quiz picker, leaderboard preview |
| `/quiz/start` | POST | Start/restart an attempt for a selected quiz |
| `/quiz` | GET | Show the current question |
| `/quiz` | POST | Save the answer for the current question and advance |
| `/room` | GET | Create a live room or join with a room code |
| `/room/create` | POST | Create a room and become its host |
| `/room/join` | POST | Join a waiting room with its six-character code |
| `/room/start` | POST | Host starts a room quiz for all joined players |
| `/ws/room/{code}/{token}` | WebSocket | Authenticated live room status and leaderboard updates |
| `/result` | GET | Show percentage, feedback and answer review |
| `/leaderboard` | GET | Top 10 results, optionally filtered with `?quiz=java` |
| `/admin` | GET, POST | Admin-only quiz track and MCQ planner |

## Grant the first admin access

The planner uses the normal Quizly account and login flow. Register the chosen
admin account through the site, then update that user's Firestore document in
the `users` collection to include:

```json
{ "role": "admin" }
```

Use the user document ID belonging to the intended account. Do not create a
shared admin password, add admin selection to registration, or store passwords
in Firestore as plain text. Accounts without a `role` field and new accounts
are treated as players. The server checks the role from Firestore for every
`/admin` request, so changing the role back to `player` removes access.

The planner can create and edit quiz tracks and questions, save drafts, publish
content, archive and restore it, and review recent changes. A track needs at
least two published questions at each difficulty before it can be published.
Archived content stays stored and is omitted from new quiz attempts; saved
result reviews are retained.

## Move existing MongoDB data to Firestore

The migration utility copies every non-system MongoDB collection into Firestore
using each MongoDB document's `_id` as its Firestore document ID. It preserves
the MongoDB source (read-only migration) and can be rerun safely. It also
creates the email lookup documents used for sign-in.

Set the Firestore credentials and the MongoDB source, then run:

```powershell
$env:QUIZLY_FIREBASE_PROJECT_ID = "your-firebase-project-id"
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\secure\path\firebase-service-account.json"
$env:QUIZLY_MONGO_URI = "mongodb://localhost:27017"
$env:QUIZLY_MONGO_DATABASE = "quizly"
mvn -DskipTests compile exec:java "-Dexec.mainClass=com.quizly.db.MongoToFirestoreMigrator"
```

Use your existing MongoDB URI if the source is hosted elsewhere. Do not send
the URI or service-account key in chat or commit either value. The migration
prints a per-collection count; compare these counts and verify users, quiz
history, and progression in Firestore before switching the app to Firestore.
The source MongoDB data is not deleted or changed. Once verified, configure
`QUIZLY_FIREBASE_PROJECT_ID` and `GOOGLE_APPLICATION_CREDENTIALS` when running
Quizly; MongoDB environment variables are only needed by the migration tool.

## Progression, power-ups and live play

- Each track has 24 rated questions: eight easy, eight medium, and eight hard.
  Every attempt independently shuffles the bank and answer options, samples two
  questions from each tier, shuffles those six, and starts with an easy one.
  After each response, the next question is selected from the remaining six:
  correct answers target a harder tier and misses target an easier tier; if
  that tier is empty, the closest available tier is used. Adaptation reorders
  the sampled six and does not add questions from the rest of the bank.
- Questions have a 30-second server-checked timer. Expired questions are
  recorded as unanswered.
- **50/50** leaves the correct choice and one distractor; **Double points**
  adds one point and 10 XP when its question is answered correctly. Each power-up
  can be used once per quiz attempt.
- Completion grants 15 XP, 10 XP per correct answer, any double-points bonus,
  and a streak bonus of 2 XP per streak day (capped at 14 XP). A streak advances
  at most once per UTC day and resets after a missed day. Every 100 XP advances
  a level. Badges unlock for a first quiz, 3/7-day streaks, and 100/500 XP.
- The signed-in **Live rooms** page lets a host select a quiz and share its
  six-character room code. At least two players must join before the host can
  start; the room supports up to 20 players. Players must use the same running
  Quizly server. Room state is in application memory, expires after three
  hours, and is not shared across multiple app instances.
- Voice mode uses browser speech synthesis and recognition. Spoken choices are
  matched to the visible options; players can review the selected option before
  submitting it. Browser microphone permission is required.
- Servlet sessions expire after 24 hours of inactivity. On Vercel, session
  state remains in container memory, so a container restart or routing to a
  different instance can still interrupt an active quiz.

## Project structure

```
src/main/java/com/quizly
├── data/          # immutable quiz catalogue
├── db/            # Firestore connection and MongoDB migration utility
├── model/         # domain classes (JavaBean getters, JSP EL friendly)
├── repository/    # Firestore-backed repositories
├── service/       # quiz, progression, live-room and leaderboard services
└── web/           # servlets, filter, listener and application constants

src/main/webapp
├── WEB-INF/
│   ├── views/     # JSP views + includes
│   └── web.xml    # welcome-file, error pages, session/JSP settings
└── assets/        # CSS / JS / favicon

src/test/java/com/quizly  # JUnit tests, using an in-memory document store
```

## Concepts map

| Requirement | Where it appears |
|---|---|
| Multiple-choice question display | `WEB-INF/views/quiz.jsp`, `QuizServlet` |
| Accept answers | `Attempt.answerCurrent`, `POST /quiz` |
| Evaluate answers | `QuizService.grade`, `AnswerReview` |
| Final score | `Result`, result page, percentage + breakdown |
| Store results | `ResultRepository` |
| Lambda expressions | `QuizService`, `LeaderboardService`, `ResultRepository` |
| Collections | Catalogue map, attempt maps/lists, repository list |
| JSP | `WEB-INF/views/*`, servlet-to-view MVC flow |
| DSA | `PriorityQueue` min-heap top-K leaderboard, hash-map catalogue |

## Presentation demo script

1. Open the home page and explain the three tracks.
2. Enter a name and choose a quiz. Point out that POST keeps state out of the URL.
3. Play one attempt and show the progress bar and option-shuffling note.
4. Try an adaptive question, a power-up, and the voice buttons.
5. Show the XP, streak, levels and badges on the account page.
6. Create a live room, join it from another signed-in browser, and start the game.
7. Show the result review and the room leaderboard updating live.
8. Show the console test report (`mvn test`) and the existing leaderboard.

## Initial built-in quiz catalogue

For normal quiz authoring, use the admin planner described above. The source
catalogue below is for adding default content that should be included when a
new quiz document is first seeded; it is not the way to edit live quiz content.

Open `src/main/java/com/quizly/data/QuizCatalog.java` and add another
`register(catalogue, new Quiz(...))` inside the static block:

```java
new Quiz(
    "networking",
    "Networking Basics",
    "IP, HTTP & protocols",
    "Describe the networking track.",
    "📶",
    "Beginner",
    5,
    "#5B8DEF",
    List.of(
        new Question(
           "net-1",
           "Which protocol resolves domain names?",
           List.of("DNS", "HTTP", "SMTP", "UDP"),
           0,
           "DNS resolves a domain name to an IP address."
        )
    )
)
```

No view changes are needed; the home page, result handling and leaderboard
pick the new quiz automatically.
