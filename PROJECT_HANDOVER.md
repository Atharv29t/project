# Quizly Project Handover

This guide explains how the application is structured, how its main features
work, how to run and deploy it, and what the next maintainer should know before
changing it.

## 1. Project at a glance

Quizly is a Java 17 server-rendered quiz application. Jakarta Servlets handle
HTTP requests, JSP/JSTL render HTML, Tomcat runs the WAR, and Firebase Cloud
Firestore stores accounts, quiz data, results, and progression. Browser
JavaScript and CSS provide the interactive behavior and visual design.

| Concern | Implementation |
|---|---|
| Language | Java 17 |
| HTTP/application layer | Jakarta Servlet 6.0 |
| Live quiz rooms | Jakarta WebSocket 2.1 |
| HTML rendering | JSP 3.1, JSTL 3.0, JSP Expression Language |
| Runtime | Apache Tomcat 10.1 |
| Persistent database | Cloud Firestore, through Firebase Admin SDK |
| Build/package | Maven WAR |
| Unit tests | JUnit 5 |
| Production host | Vercel Container Runtime running the project Dockerfile |
| Live site | <https://online-quiz-app-nine-sooty.vercel.app/> |

This is a traditional server-rendered Java application, not a React, Spring
Boot, or Node.js application. The browser submits HTML forms to Servlets; the
Servlets call services and repositories, then forward to JSP views or redirect
to another route.

## 2. Repository map

```text
pom.xml                         Maven dependencies, Java version, test and dev profiles
Dockerfile                      Multi-stage Maven build and Tomcat production image
README.md                       Short project introduction and common commands
PROJECT_HANDOVER.md             Architecture and operations guide (this document)

src/main/java/com/quizly/
├── data/                       Built-in quiz question catalogue
├── db/                         Firestore abstraction, implementation, migration tool
├── model/                      Quiz, attempt, user, result and view-model types
├── repository/                 Firestore reads/writes and document mapping
├── security/                   Password hashing
├── service/                    Authentication, quiz, leaderboard and room rules
└── web/                        Servlets, filters, WebSocket endpoint and bootstrap

src/main/webapp/
├── WEB-INF/web.xml             Servlet filters, session config and error pages
├── WEB-INF/views/              JSP pages and shared fragments (not directly public)
├── assets/css/styles.css       Theme and responsive UI
├── assets/js/app.js            Browser interactions, voice and room UI
└── favicon.svg                 Site icon

src/test/java/com/quizly/       JUnit tests; data tests use an in-memory document store
```

### Package responsibilities

- **`data`**: `QuizCatalog` defines the initial Java Fundamentals, DSA
  Essentials, and Web & JSP tracks and their question content.
- **`model`**: Domain objects carry application state and expose JavaBean
  getters for JSP EL. `Attempt` is mutable per-player quiz state; most other
  objects are immutable.
- **`db`**: `DocumentStore` is the storage interface. Production uses
  `FirestoreDocumentStore`; tests use `InMemoryDocumentStore`. `Database`
  validates configuration and seeds quiz documents. The MongoDB migration
  tool is an optional one-off utility, not the live application database.
- **`repository`**: Maps between Firestore document maps and Java domain
  objects; owns persistence-specific operations.
- **`service`**: Contains business rules independent of servlet rendering.
- **`web`**: The servlet/JSP boundary. `AppContextListener` constructs shared
  services at startup and publishes them in the ServletContext.

## 3. Application startup and request flow

### Startup

1. Tomcat starts the WAR and invokes `AppContextListener`.
2. `Database.appDatabase()` reads the Firebase project ID and initializes the
   Firebase Admin SDK credentials.
3. `Database.initialize()` creates missing starter quiz documents and marks
   them as managed. It performs a one-time merge for legacy quiz documents
   without the `contentManaged` marker; later restarts do not overwrite admin
   edits or restore archived content.
4. The listener constructs repositories and services once and stores them in
   the ServletContext under names defined in `App`.
5. Servlets obtain those shared instances with `WebSupport.service(...)`.
6. On shutdown, the listener closes the room service and Firestore connection.

Missing or invalid Firebase configuration is a startup error: the application
does not silently fall back to in-memory storage in production.

### Request handling

- Servlets parse and validate requests, call services/repositories, and choose
  whether to forward to a JSP or redirect.
- JSP files live under `WEB-INF/views`, so a browser cannot fetch them directly.
  `WebSupport.forward` is the common forwarding helper.
- `AppFilter` applies UTF-8, browser security headers, CSRF-token initialization,
  and one-request flash messages.
- `AuthenticationFilter` protects quiz, result, profile, and room routes.
- `AdminAuthorizationFilter` re-reads the account role from Firestore and
  protects every `/admin` request.
- Forms use POST for state-changing operations and include a CSRF token.
- `App` centralizes ServletContext and session attribute names.

The full route list is in [README.md](./README.md#routes).

## 4. Quiz and account features

### Quiz catalogue and question banks

`QuizCatalog` defines three tracks with 24 questions each (eight easy, eight
medium, eight hard). The catalogue is the source for seeding; normal runtime
reads the Firestore `quizzes` collection through `QuizRepository`.

At startup the seed process uses each track ID as its Firestore document ID.
New starter tracks are created once. Legacy track documents without
`contentManaged: true` receive one compatibility merge of built-in questions;
the marker is then written. Admin-managed documents are left alone on later
starts, so archived content is not reintroduced from `QuizCatalog`.

Quiz tracks and questions have `draft`, `published`, and `archived` states.
Missing status fields in existing Firestore data default to `published`.
Player flows receive only published tracks and questions; publishing a track
requires at least two published questions at each difficulty. Archiving keeps
the content for previous answer reviews. `QuizRepository` uses a short-lived
15-second cache, invalidated immediately after an admin write, so other app
instances pick up updates after cache expiry.

The first admin is provisioned manually: register the account normally, then
set `role: "admin"` on its `users/{userId}` document in Firestore. New accounts
and legacy accounts without a role are players. There is no admin signup form.
The `/admin` planner manages tracks and MCQs, and `adminAuditLogs` stores the
actor, action, target track, summary, and timestamp for each content change.

### Adaptive quiz attempts

1. The signed-in player submits a track to `POST /quiz/start`.
2. `QuizService` loads its question bank and makes a per-attempt copy of each
   question with its answer options shuffled.
3. It shuffles candidates within each difficulty tier, samples two questions
   from each tier (six questions total), shuffles the selected six, and puts
   an easiest available question first.
4. The `Attempt` object keeps the chosen questions, answers, current index,
   timer deadline, and used power-ups in the HTTP session.
5. `GET /quiz` renders the current question. `POST /quiz` validates the
   question ID, answer, deadline, and CSRF token before advancing the attempt.
6. The next question is selected from the remaining sampled questions by difficulty:
   a correct answer targets a harder question; an incorrect or unanswered
   answer targets an easier one. If the target tier is unavailable, the
   closest remaining tier is used.
7. The final attempt is graded by `QuizService.grade`, persisted by
   `ResultRepository`, and shown by `/result`.

The timer is checked by Java on submission; the browser countdown is a user
interface aid, not the authority for whether an answer is on time.

The adaptive step only reorders the six questions chosen at the start; it does
not draw additional questions from the remaining 18 in the track. With eight
questions per tier and two selected from each, each track has
`C(8, 2) × C(8, 2) × C(8, 2) = 21,952` possible six-question sets before
answer-option permutations.

### Power-ups

`Attempt` enforces one use of each power-up per attempt:

- **50/50** hides two incorrect options for the current question.
- **Double points** marks the current question. If answered correctly, it
  grants one bonus point and 10 bonus XP.

The normal score is the number correct out of six. The percentage is based on
that normal score; the double-points bonus is reported separately.

### Registration, login, and profile

- `AuthService` validates names, email addresses, password length, and password
  confirmation.
- `PasswordHasher` stores salted PBKDF2-HMAC-SHA256 hashes, never plaintext
  passwords.
- `UserRepository` stores account documents and a SHA-256 email lookup document
  to enforce unique normalized email addresses transactionally.
- `AuthenticationFilter` redirects anonymous users from protected pages to
  login. Successful login rotates the servlet session ID.
- `ProfileServlet` combines a player's result history and progression into the
  profile view.

### XP, streaks, and badges

`ProgressionRepository` stores progression in Firestore and updates it in a
Firestore transaction. An attempt-ID award document makes awarding XP
idempotent if the same completion is processed again.

- Completion: 15 XP.
- Correct answer: 10 XP.
- Correct double-points question: another 10 XP.
- Streak: 2 XP per streak day, up to 14 XP.
- The streak uses UTC calendar days; repeating a quiz on the same UTC day does
  not increment the streak again, and missing a day restarts it.
- One level is earned per 100 XP.
- Badges: first quiz, 3-day streak, 7-day streak, 100 XP, and 500 XP.

### Results and leaderboard

`ResultRepository` writes a completed result and embedded answer-review entries
to Firestore. The profile uses saved results by user ID; the result page can
also load a saved result by ID while enforcing ownership for signed-in accounts.

`LeaderboardService` calculates track or overall top-ten results with a bounded
`PriorityQueue` min-heap. It also calculates attempt count, average score, best
score, and a result's top-ten rank. The repositories currently read result
documents and filter/sort them in Java; for large production datasets, consider
Firestore queries and indexes instead of reading the complete collection.

## 5. Multiplayer and browser features

### Live rooms

`RoomServlet` creates a room, joins players, and allows the host to start. Room
codes are six characters, and rooms support 2–20 registered players. The
`RoomService` tracks room/participant progress; the `RoomEndpoint` publishes
updates over Jakarta WebSocket. A room connection requires a short-lived,
single-use token, and `RoomEndpointConfigurator` rejects cross-origin
connections.

Room state and active WebSocket connections are held in **application memory**,
not Firestore. Rooms expire after three hours. They are lost when the Tomcat
process restarts and are not shared across instances. For consistent multiplayer
on a horizontally scaled or frequently recycled host, move room state to a
shared store and use a pub/sub or equivalent cross-instance event mechanism.

### Voice and interface

- `app.js` uses browser speech synthesis to read questions and native
  speech-recognition APIs to select a matching visible answer. Recognition
  requires browser support and microphone permission; regular answer controls
  remain available.
- The same script handles option selection, the quiz countdown, WebSocket
  room updates, and result score animation.
- `styles.css` provides the responsive theme and motion. Animations honor
  `prefers-reduced-motion`.
- The win chime is synthesized with the Web Audio API; there is no sound asset.

## 6. Firestore data layout

Firestore collection names and main document contents:

| Collection | Document ID | Purpose |
|---|---|---|
| `quizzes` | Track ID, e.g. `java` | Track metadata and question list |
| `users` | Random user UUID | Name, normalized email, password hash, created/login timestamps, optional `role` (`player` or `admin`) |
| `adminAuditLogs` | Random event UUID | Admin content action, actor ID, track ID, summary, timestamp |
| `userEmailLookups` | SHA-256 hex of normalized email | Maps email key to user ID; supports unique registration and lookup |
| `results` | Attempt UUID | Score, user/track metadata, completion time, answer reviews |
| `playerProgress` | User ID | XP, current streak, last played UTC date, completed attempts, badges |
| `progressAwards` | Attempt UUID | Idempotency record for completion XP |

Live room data is not part of this Firestore layout in the current
implementation; it is in process memory as described above.

`FirestoreDocumentStore` wraps Admin SDK reads, writes, updates, deletes, and
transactions behind `DocumentStore`. Firebase Admin uses privileged server
credentials and is not constrained by client-side Firestore security rules.
Give the service account only the permissions the app needs and never put its
private key in source control or browser code.

## 7. Configuration and local development

### Requirements

- JDK 17 or newer.
- Maven 3.8 or newer.
- A Firebase project with Cloud Firestore enabled.
- A service account or other Google Application Default Credentials with
  Firestore access.

### Local run

PowerShell example:

```powershell
$env:QUIZLY_FIREBASE_PROJECT_ID = "your-firebase-project-id"
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\secure\firebase-service-account.json"
mvn -Pdev clean package cargo:run
```

Open <http://localhost:8081/quizly/>. Cargo downloads Tomcat 10.1 for the
development profile; no separate Tomcat installation is needed.

### Configuration variables

| Variable / property | Required | Used for |
|---|---:|---|
| `QUIZLY_FIREBASE_PROJECT_ID` or `-Dquizly.firebase.projectId=...` | Yes | Selects the Firebase project |
| `QUIZLY_FIREBASE_CREDENTIALS_JSON` | Optional | Inline service-account JSON; useful for hosted environments |
| `GOOGLE_APPLICATION_CREDENTIALS` | Optional if inline JSON is supplied | Path to ADC/service-account JSON for local or server execution |
| `QUIZLY_MONGO_URI` | Migration only | Source MongoDB URI |
| `QUIZLY_MONGO_DATABASE` | Migration only | Source MongoDB database name |

The `QUIZLY_FIREBASE_CREDENTIALS_JSON` value is read by the Admin SDK
initialization code and must contain the complete service-account JSON. It is
a secret: configure it in the hosting provider's secret environment-variable
store. Do not write it into a committed file or print it in build logs.

### Build and tests

```powershell
mvn test
mvn clean package
```

The WAR is `target/quizly.war`. Tests use the in-memory document store or a
test-only embedded MongoDB server; they do not require production Firestore
credentials. A successful current baseline is 32 tests.

## 8. Production deployment

### Current Vercel deployment

The Vercel project uses the **Container** framework and builds the root
`Dockerfile`. The Dockerfile compiles the WAR in a Maven/JDK 17 build stage,
then copies it into Tomcat 10.1 as the root web application. The Tomcat
connector is adjusted to Vercel's `PORT` at container startup.

Required Vercel Production environment variables:

- `QUIZLY_FIREBASE_PROJECT_ID`
- `QUIZLY_FIREBASE_CREDENTIALS_JSON` (sensitive; full JSON key contents)

After changing production variables, create a new deployment for the change to
take effect. The currently configured production site is
<https://online-quiz-app-nine-sooty.vercel.app/>.

Vercel Container Runtime is a container/function platform, not a permanently
running single Tomcat VM. The app uses ordinary Tomcat `HttpSession` state and
in-memory multiplayer rooms. Container recycling or multiple instances can
therefore interrupt logins/quiz attempts or split room state. Firestore-backed
results and progression remain persistent. If all user flows must be reliable
under scale-out/restarts, use a persistent Tomcat host or first implement
shared session and room state; do not assume the current process-local state is
distributed.

### Existing Tomcat

```powershell
mvn clean package
```

Copy `target/quizly.war` into Tomcat 10.1's `webapps` folder, configure the
Firebase variables above in the Tomcat process environment, then open
`http://localhost:8080/quizly/` (or your server's hostname). Tomcat 8/9 uses
the older `javax.*` APIs and is not compatible.

## 9. Optional MongoDB migration

The live database is Firestore. `MongoToFirestoreMigrator` is retained only for
an intentional migration of a MongoDB source. It reads non-system collections,
maps MongoDB document IDs to Firestore document IDs, converts common BSON types,
and creates email lookup records for users. It does not delete or modify MongoDB
source data. Do not run it unless a MongoDB source should actually be copied.

Example:

```powershell
$env:QUIZLY_FIREBASE_PROJECT_ID = "your-firebase-project-id"
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\secure\firebase-service-account.json"
$env:QUIZLY_MONGO_URI = "mongodb://localhost:27017"
$env:QUIZLY_MONGO_DATABASE = "quizly"
mvn -DskipTests compile exec:java "-Dexec.mainClass=com.quizly.db.MongoToFirestoreMigrator"
```

Keep both credentials private. Verify migrated counts and sample records in
Firestore before directing the application at the migrated data.

## 10. Security and operational notes

- Passwords use PBKDF2-HMAC-SHA256 with a per-password random salt and
  120,000 iterations. Never log passwords, hashes, or credential JSON.
- Account routes require login for quiz, room, profile, and result actions.
- `/admin` routes require an account whose Firestore `role` is `admin`; the
  role is rechecked on every planner request.
- State-changing forms use session-bound CSRF tokens.
- Login rotates the session ID; cookies are configured HttpOnly.
- `web.xml` configures a 24-hour inactivity timeout. The browser cookie is
  still a session cookie, and the session itself is held in the Tomcat process.
- Browser headers include CSP, `X-Frame-Options`, `X-Content-Type-Options`, and
  `Referrer-Policy`.
- Result IDs are not treated as authorization: a signed-in user cannot load
  another registered user's result by changing `?id=...`.
- WebSocket room connections use a one-use token and same-origin validation.
- Firebase Admin access is server-side and privileged; do not embed credentials
  in JSP, JavaScript, or the Git repository.
- A Firestore/service-account error during application initialization prevents
  the app from starting. Check environment variable names, project ID,
  Firestore status, and service-account IAM permissions.
- For a host that supports multiple Tomcat instances, sticky sessions alone do
  not make the in-memory multiplayer room state shareable.

## 11. Extending the project

### Manage quiz tracks and questions

Use the `/admin` planner for live quiz content. Track IDs are lowercase slugs
and cannot be changed after creation. Each question has two to six unique
options, one correct-option index, a difficulty, an explanation, and a status.
Use archive status instead of deleting content so past result reviews remain
available. `QuizCatalog.java` supplies only starter content for tracks not yet
created in Firestore; it is not the live authoring interface.

### Add a feature

Keep responsibilities in their existing layers:

1. Add/change domain types under `model`.
2. Put business rules in `service`.
3. Add Firestore mapping and persistence in `repository`.
4. Register shared services in `AppContextListener` if needed.
5. Add or update a Servlet under `web` and render with a JSP under
   `WEB-INF/views`.
6. For POST forms include the CSRF token and use `WebSupport.isCsrfValid`.
7. Add focused JUnit tests. Use `TestDatabase` for Firestore-like repository
   tests without production credentials.
8. Update the README and this handover document when behavior or operations
   change.

### Main files to start with

| Task | Start here |
|---|---|
| Quiz list/questions | `data/QuizCatalog.java`, `repository/QuizRepository.java` |
| Sampling/adaptive grading | `service/QuizService.java`, `model/Attempt.java` |
| Account validation | `service/AuthService.java`, `security/PasswordHasher.java` |
| Firestore schema/mapping | `db/DocumentStore.java`, `db/FirestoreDocumentStore.java`, `repository/` |
| XP/streaks/badges | `repository/ProgressionRepository.java` |
| Results/top 10 | `repository/ResultRepository.java`, `service/LeaderboardService.java` |
| Live game | `service/RoomService.java`, `web/RoomServlet.java`, `web/RoomEndpoint.java` |
| Shared styles/interactions | `assets/css/styles.css`, `assets/js/app.js` |
| Routes and configuration | `web/*.java`, `WEB-INF/web.xml` |

## 12. Test coverage

The test suite covers quiz catalogue validation, question selection and grading,
adaptive attempt behavior and power-ups, account authentication, leaderboard
ranking, progression idempotency, room rules and one-use WebSocket tokens,
Firestore initialization, migration conversion, and repository persistence.

Run the complete suite before a code change is handed off:

```powershell
mvn test
```
