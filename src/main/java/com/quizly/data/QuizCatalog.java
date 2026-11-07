package com.quizly.data;

import com.quizly.model.Question;
import com.quizly.model.Quiz;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory quiz catalogue.
 *
 * <p>DSA note: quizzes are indexed by id in a hash map, so finding a quiz by id
 * is O(1) on average. The insertion order is preserved for the home page.</p>
 */
public final class QuizCatalog {

    private static final Map<String, Quiz> QUIZZES;

    static {
        Map<String, Quiz> catalogue = new LinkedHashMap<>();

        register(catalogue, new Quiz(
                "java",
                "Java Fundamentals",
                "Lambdas, collections & streams",
                "Practise the language features that make modern Java expressive, from functional interfaces to the Streams API.",
                "☕",
                "Beginner",
                4,
                "#38BDF8",
                List.of(
                        new Question(
                                "java-1",
                                "What is a lambda expression in Java?",
                                List.of(
                                        "A class that can only contain static methods",
                                        "A concise way to implement a functional interface",
                                        "A special kind of for-loop",
                                        "A replacement for the JVM"
                                ),
                                1,
                                "A lambda is syntactic sugar for an instance of a functional interface. For example, (a, b) -> a + b implements a single abstract method instead of requiring a full anonymous class.",
                                1
                        ),
                        new Question(
                                "java-2",
                                "Which of these is a functional interface?",
                                List.of("ArrayList", "Runnable", "HashMap", "StringBuilder"),
                                1,
                                "Runnable declares exactly one abstract method, run(), so it can be implemented with a lambda: Runnable task = () -> System.out.println(\"Go\");",
                                1
                        ),
                        new Question(
                                "java-3",
                                "Which collection type guarantees unique elements?",
                                List.of("List", "Set", "Queue", "Deque"),
                                1,
                                "A Set does not allow duplicates. HashSet gives average O(1) membership checks, while LinkedHashSet also preserves insertion order.",
                                2
                        ),
                        new Question(
                                "java-4",
                                "What does filter() do in the Java Streams API?",
                                List.of(
                                        "Transforms every element",
                                        "Keeps only the elements that match a predicate",
                                        "Sorts the stream in place",
                                        "Closes the underlying collection"
                                ),
                                1,
                                "filter(Predicate) is a lazy intermediate operation. It passes through only the elements for which the predicate returns true; the work happens when a terminal operation such as toList() runs.",
                                2
                        ),
                        new Question(
                                "java-5",
                                "Which method reference is equivalent to the lambda s -> s.toUpperCase()?",
                                List.of("String::toUpperCase", "String::new", "System.out::println", "String::length"),
                                0,
                                "String::toUpperCase refers to an instance method that is invoked on each stream element. Method references are compact lambdas: the receiver becomes the implicit first argument.",
                                3
                        ),
                        new Question(
                                "java-6",
                                "What is the job of Collectors.toList() in a stream pipeline?",
                                List.of(
                                        "It collects the stream elements into a List",
                                        "It filters duplicate values",
                                        "It converts a List into a Map",
                                        "It sorts the stream by natural order"
                                ),
                                0,
                                "collect(Collectors.toList()) is a terminal operation that gathers the remaining stream elements into a mutable List. In modern Java, stream.toList() is a convenient alternative.",
                                3
                        ),
                        new Question(
                                "java-7",
                                "Which keyword prevents a local variable from being reassigned after initialization?",
                                List.of("static", "final", "volatile", "transient"),
                                1,
                                "A local variable declared final can be assigned only once. This is especially useful when the variable is captured by a lambda.",
                                1
                        ),
                        new Question(
                                "java-8",
                                "Which collection preserves insertion order while preventing duplicate elements?",
                                List.of("HashSet", "TreeSet", "LinkedHashSet", "PriorityQueue"),
                                2,
                                "LinkedHashSet combines set uniqueness with a linked structure that preserves insertion order.",
                                1
                        ),
                        new Question(
                                "java-9",
                                "Which Stream operation converts each element into another value?",
                                List.of("filter", "map", "distinct", "limit"),
                                1,
                                "map applies a function to every element and emits the transformed values.",
                                2
                        ),
                        new Question(
                                "java-10",
                                "What does Optional.orElseGet() do?",
                                List.of(
                                        "Always throws when a value exists",
                                        "Supplies a fallback lazily when the Optional is empty",
                                        "Removes the wrapped value",
                                        "Converts the value to a stream"
                                ),
                                1,
                                "orElseGet invokes its supplier only when the Optional is empty, unlike orElse which evaluates its fallback argument eagerly.",
                                2
                        ),
                        new Question(
                                "java-11",
                                "Which functional interface accepts one input and returns a boolean?",
                                List.of("Consumer", "Supplier", "Predicate", "Runnable"),
                                2,
                                "Predicate<T> represents a boolean-valued test of one input and is commonly used by Stream.filter().",
                                3
                        ),
                        new Question(
                                "java-12",
                                "What does Stream.collect() typically do?",
                                List.of(
                                        "Terminates the stream and combines its elements into a result",
                                        "Restarts the stream",
                                        "Sorts the source collection in place",
                                        "Creates a thread for every element"
                                ),
                                0,
                                "collect is a terminal reduction operation that gathers stream elements into containers or other summary results.",
                                3
                        )
                )
        ));

        register(catalogue, new Quiz(
                "dsa",
                "DSA Essentials",
                "Search, sort & structures",
                "Think in complexities and choose the right structure for the job. This track covers the core ideas you will use in coding rounds.",
                "🧩",
                "Intermediate",
                5,
                "#8B5CF6",
                List.of(
                        new Question(
                                "dsa-1",
                                "What is the time complexity of binary search on a sorted array?",
                                List.of("O(1)", "O(log n)", "O(n)", "O(n log n)"),
                                1,
                                "Binary search halves the remaining search space on every comparison, so it needs at most log₂(n) steps. The array must be sorted first.",
                                2
                        ),
                        new Question(
                                "dsa-2",
                                "Which data structure follows the LIFO (last in, first out) principle?",
                                List.of("Queue", "Stack", "Min-heap", "Graph"),
                                1,
                                "A stack removes the most recently added item first — think of the undo history in an editor or the call stack used by the JVM.",
                                1
                        ),
                        new Question(
                                "dsa-3",
                                "What is the average-case lookup time in a HashMap?",
                                List.of("O(n)", "O(log n)", "O(1)", "O(n²)"),
                                2,
                                "Hashing maps a key directly to a bucket, giving average O(1) lookup. In the worst case, many keys collide and the cost can degrade toward O(n).",
                                2
                        ),
                        new Question(
                                "dsa-4",
                                "Which sorting algorithm repeatedly selects the smallest remaining element?",
                                List.of("Bubble sort", "Selection sort", "Merge sort", "Quick sort"),
                                1,
                                "Selection sort scans the unsorted portion to find its minimum, then swaps it into place. It always takes O(n²) comparisons regardless of the input order.",
                                1
                        ),
                        new Question(
                                "dsa-5",
                                "Which structure does breadth-first search (BFS) normally use?",
                                List.of("Stack", "Queue", "Priority queue only", "Hash set only"),
                                1,
                                "BFS visits nodes level by level. A queue's FIFO behaviour ensures that nodes discovered earlier are explored before their neighbours.",
                                3
                        ),
                        new Question(
                                "dsa-6",
                                "In a min-heap, what is stored at the root?",
                                List.of("The smallest element", "The largest element", "The median element", "A randomly chosen element"),
                                0,
                                "A min-heap maintains the heap property: every parent is smaller than or equal to its children. The root is therefore the minimum, which makes heaps ideal for priority queues.",
                                3
                        ),
                        new Question(
                                "dsa-7",
                                "Which data structure is best suited for processing items in arrival order?",
                                List.of("Stack", "Queue", "Set", "Tree"),
                                1,
                                "A queue follows FIFO order, so the earliest inserted item is processed first.",
                                1
                        ),
                        new Question(
                                "dsa-8",
                                "What is the time complexity of reading an array element by index?",
                                List.of("O(1)", "O(log n)", "O(n)", "O(n log n)"),
                                0,
                                "Arrays support direct indexed access, which takes constant time.",
                                1
                        ),
                        new Question(
                                "dsa-9",
                                "Which traversal visits the left subtree, node, then right subtree?",
                                List.of("Preorder", "Inorder", "Postorder", "Level order"),
                                1,
                                "Inorder traversal processes the left child, the current node, and then the right child.",
                                2
                        ),
                        new Question(
                                "dsa-10",
                                "What is the worst-case time complexity of merge sort?",
                                List.of("O(1)", "O(log n)", "O(n)", "O(n log n)"),
                                3,
                                "Merge sort divides the input into logarithmically many levels and merges all n elements per level, giving O(n log n).",
                                2
                        ),
                        new Question(
                                "dsa-11",
                                "Which algorithm finds shortest paths from one source when edge weights are non-negative?",
                                List.of("Binary search", "Dijkstra's algorithm", "Kruskal's algorithm", "Depth-first search"),
                                1,
                                "Dijkstra's algorithm repeatedly finalizes the closest unvisited vertex and is valid when edge weights are non-negative.",
                                3
                        ),
                        new Question(
                                "dsa-12",
                                "What property must a binary search tree satisfy?",
                                List.of(
                                        "Every node has exactly two children",
                                        "Left values are smaller and right values are larger than the node",
                                        "All leaves are at the same depth",
                                        "The root is always the smallest value"
                                ),
                                1,
                                "In a binary search tree, values in the left subtree are ordered below the node and values in the right subtree above it.",
                                3
                        )
                )
        ));

        register(catalogue, new Quiz(
                "web",
                "Web & JSP",
                "Servlets, JSP & HTTP",
                "Follow one request from the browser, through a servlet, into a JSP view and back. A practical tour of the Java web stack.",
                "🌐",
                "Beginner",
                4,
                "#F97316",
                List.of(
                        new Question(
                                "web-1",
                                "What is the main role of a JSP page?",
                                List.of(
                                        "To generate dynamic HTML on the server",
                                        "To manage database indexes",
                                        "To replace CSS stylesheets",
                                        "To compile Java bytecode"
                                ),
                                0,
                                "JSP pages are server-side templates that generate markup. In an MVC design they usually act as the view, while servlets handle request logic.",
                                1
                        ),
                        new Question(
                                "web-2",
                                "Which HTTP method is most appropriate for submitting a quiz answer that changes server state?",
                                List.of("GET", "POST", "HEAD", "TRACE"),
                                1,
                                "POST sends data in the request body and is intended for state-changing operations. GET should be safe and idempotent, so it is a poor fit for submitting an answer.",
                                2
                        ),
                        new Question(
                                "web-3",
                                "In the MVC pattern, which component is a JSP page normally used as?",
                                List.of("Controller", "Model", "View", "Database"),
                                2,
                                "The view renders data for the user. The servlet acts as the controller and the Java model/service classes hold the business logic and state.",
                                1
                        ),
                        new Question(
                                "web-4",
                                "Which scope keeps data for one user across multiple requests?",
                                List.of("page", "request", "session", "application"),
                                2,
                                "Session scope survives between requests from the same browser. It is the natural home for an in-progress quiz attempt.",
                                2
                        ),
                        new Question(
                                "web-5",
                                "What does the expression ${quiz.title} use to read data?",
                                List.of("A scriptlet", "Expression Language (EL)", "A CSS selector", "A SQL query"),
                                1,
                                "EL reads properties from scoped attributes and calls the matching JavaBean getter, so ${quiz.title} invokes getTitle() on the quiz object.",
                                3
                        ),
                        new Question(
                                "web-6",
                                "Which JSTL tag is used to iterate over a collection?",
                                List.of("<c:forEach>", "<c:if>", "<c:out>", "<c:url>"),
                                0,
                                "c:forEach is the iteration tag. c:if performs conditional rendering, c:out safely prints a value, and c:url builds context-aware URLs.",
                                3
                        ),
                        new Question(
                                "web-7",
                                "Which HTTP status code means a request succeeded?",
                                List.of("200", "301", "404", "500"),
                                0,
                                "HTTP 200 OK indicates that the request was successfully processed.",
                                1
                        ),
                        new Question(
                                "web-8",
                                "Which HTTP method should retrieve data without changing server state?",
                                List.of("POST", "GET", "DELETE", "PATCH"),
                                1,
                                "GET is intended for safe retrieval and should not change application state.",
                                1
                        ),
                        new Question(
                                "web-9",
                                "What does a servlet container such as Tomcat manage?",
                                List.of(
                                        "HTTP requests and servlet lifecycles",
                                        "Only browser CSS",
                                        "DNS records for a domain",
                                        "The user's operating system"
                                ),
                                0,
                                "A servlet container receives HTTP requests, invokes the appropriate servlet, and manages servlet lifecycle and configuration.",
                                2
                        ),
                        new Question(
                                "web-10",
                                "Why should user-provided text be escaped before rendering in HTML?",
                                List.of(
                                        "To make the page load more slowly",
                                        "To prevent it from being interpreted as markup or script",
                                        "To convert it into a database index",
                                        "To change the HTTP method"
                                ),
                                1,
                                "Escaping encodes special characters so untrusted input is displayed as text instead of being interpreted as HTML or script.",
                                2
                        ),
                        new Question(
                                "web-11",
                                "Which response header helps prevent browsers from MIME-sniffing content?",
                                List.of(
                                        "Content-Type: text/html",
                                        "X-Content-Type-Options: nosniff",
                                        "Accept-Language: en",
                                        "Cache-Control: public"
                                ),
                                1,
                                "X-Content-Type-Options: nosniff tells browsers to respect the declared content type instead of guessing one.",
                                3
                        ),
                        new Question(
                                "web-12",
                                "In a typical servlet MVC application, which layer should contain business rules?",
                                List.of("Service layer", "JSP markup", "URL fragment", "CSS stylesheet"),
                                0,
                                "Business rules belong in the service or domain layer, keeping request handling and JSP rendering focused on their own responsibilities.",
                                3
                        )
                )
        ));

        QUIZZES = Collections.unmodifiableMap(catalogue);
    }

    private QuizCatalog() {
    }

    private static void register(Map<String, Quiz> map, Quiz quiz) {
        if (map.put(quiz.getId(), quiz) != null) {
            throw new IllegalStateException("Duplicate quiz id: " + quiz.getId());
        }
    }

    /**
     * @return quizzes in display order
     */
    public static List<Quiz> all() {
        return List.copyOf(QUIZZES.values());
    }

    /**
     * @return the quiz with this id, or empty when no match exists
     */
    public static Optional<Quiz> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(QUIZZES.get(id));
    }

    public static int totalQuestions() {
        return QUIZZES.values().stream()
                .mapToInt(Quiz::getQuestionCount)
                .sum();
    }

    public static int difficultyForQuestion(String quizId, String questionId, int position) {
        return findById(quizId)
                .flatMap(quiz -> quiz.getQuestions().stream()
                        .filter(question -> question.getId().equals(questionId))
                        .findFirst())
                .map(Question::getDifficulty)
                .orElse(position < 2 ? 1 : position < 4 ? 2 : 3);
    }
}
