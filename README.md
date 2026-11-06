# Quizly — Online Quiz Application (Group 4)

A compact Java web application for multiple-choice learning quizzes. It is
built around the syllabus requirements:

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
- Six shuffled multiple-choice questions per attempt, with option-order
  randomisation on every restart.
- Answers are evaluated on completion and a full answer review is shown.
- Percentage score, feedback card, pass indication and rank banner.
- Restart-safe leaderboard (top 10) by quiz and overall.
- Basic CSRF protection, session handling, UTF-8 encoding and security headers.
- Playful warm theme (cream · orange · sky · violet) with black pill badges,
  colour-mosaic stat tiles, a gold/silver/bronze top-3 podium, and a floating
  trophy on the win screen.
- Polished motion design: staggered card/row reveals, an animated score ring,
  option hover/selection feedback and a result-page celebration (theme-coloured
  confetti plus a quiet, synthesised win chime via the Web Audio API — no audio
  files or extra dependencies). Everything respects `prefers-reduced-motion`.
- Unit tests for the service, DSA leaderboard logic and CSV repository.

## Technology

| Layer | Technology |
|---|---|
| Language / runtime | Java 17 |
| Web framework | Jakarta Servlet 6.0 (`@WebServlet`, `@WebFilter`, `@WebListener`) |
| View | JSP 3.1 + JSTL 3.0 (`jakarta.tags.*`) |
| Persistence | In-memory repository + CSV file (`CSV` with quote escaping) |
| Build/product | Maven WAR + Cargo Tomcat 10.1 profile |
| Test | JUnit 5 |

## Requirements

- JDK 17+
- Maven 3.8+
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

You can change the port again by editing `cargo.servlet.port` in the `dev`
profile of `pom.xml`.

To stop the dev server press `Ctrl + C`.

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
| `/result` | GET | Show percentage, feedback and answer review |
| `/leaderboard` | GET | Top 10 results, optionally filtered with `?quiz=java` |

## Where scores are stored

The app stores a CSV summary at:

```
~/.quizly/results.csv
```

You can override this path when starting Tomcat:

```
-Dquizly.data.file=/path/to/results.csv
```

Only the leaderboard summary is persisted. The full answer review is kept in
the HTTP session for the current user and is deleted when a new attempt starts.

## Project structure

```
src/main/java/com/quizly
├── data/          # immutable quiz catalogue
├── model/         # domain classes (JavaBean getters, JSP EL friendly)
├── repository/    # result repository + CSV persistence
├── service/       # quiz service and heap-based leaderboard service
└── web/           # servlets, filter, listener and application constants

src/main/webapp
├── WEB-INF/
│   ├── views/     # JSP views + includes
│   └── web.xml    # welcome-file, error pages, session/JSP settings
└── assets/        # CSS / JS / favicon

src/test/java/com/quizly  # JUnit tests
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
4. Show the result page and open some answer explanations.
5. Open the leaderboard, switch quiz filters, and show that the new score appears.
6. Show the console test report (`mvn test`) and explain the heap tie-break logic.
7. Restart Tomcat, refresh the leaderboard, and show that data is still there.

## Adding a new quiz

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
