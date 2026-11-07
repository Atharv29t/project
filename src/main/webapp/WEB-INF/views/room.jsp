<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Live quiz rooms" />
<%@ include file="includes/header.jspf" %>

<section class="room-shell container">
    <c:choose>
        <c:when test="${empty room}">
            <header class="room-intro">
                <span class="kicker">Play together</span>
                <h1>Live quiz rooms</h1>
                <p>Create a room, share its six-character code, then see everyone's progress and scores update live.</p>
            </header>
            <div class="room-forms">
                <form class="room-card" method="post" action="${ctx}/room/create">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <span class="kicker">Host a game</span>
                    <h2>Create a room</h2>
                    <label for="roomQuiz">Quiz track</label>
                    <select id="roomQuiz" name="quizId" required>
                        <c:forEach var="quiz" items="${quizzes}">
                            <option value="${quiz.id}"><c:out value="${quiz.title}" /></option>
                        </c:forEach>
                    </select>
                    <button class="btn btn-primary" type="submit">Create room</button>
                </form>
                <form class="room-card" method="post" action="${ctx}/room/join">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <span class="kicker">Got a code?</span>
                    <h2>Join a room</h2>
                    <label for="roomCode">Room code</label>
                    <input id="roomCode" name="code" maxlength="6" minlength="6" pattern="[A-Za-z0-9]{6}" required autocomplete="off">
                    <button class="btn btn-primary" type="submit">Join room</button>
                </form>
            </div>
            <p class="room-note">All players need an account and must connect to this same running Quizly server. Rooms expire after three hours.</p>
        </c:when>
        <c:otherwise>
            <section class="room-card room-lobby"
                     data-room-code="${room.code}"
                     data-room-token="${roomWebSocketToken}"
                     data-room-context="${ctx}"
                     data-room-host="${roomIsHost}"
                     data-room-started="${room.started}"
                     data-room-active="${roomAttemptActive}"
                     data-room-finished="${roomPlayerFinished}">
                <span class="kicker">Room code</span>
                <h1 class="room-code"><c:out value="${room.code}" /></h1>
                <p>Share this code with friends so they can join your room.</p>
                <button class="btn btn-ghost btn-sm" type="button" data-copy-room="${room.code}">Copy invite code</button>
                <div class="room-status">
                    <strong><c:out value="${room.quizTitle}" /></strong>
                    <span data-room-connection>Connecting for live updates…</span>
                </div>
                <h2>Live leaderboard</h2>
                <ol class="room-players" data-room-players>
                    <c:forEach var="player" items="${room.players}">
                        <li>
                            <span><c:out value="${player.name}" /></span>
                            <strong><c:choose><c:when test="${player.finished}">${player.points} pts · ${player.percentage}%</c:when><c:otherwise>${player.answered}/${player.total} · Playing</c:otherwise></c:choose></strong>
                        </li>
                    </c:forEach>
                </ol>
                <c:if test="${roomIsHost and not room.started}">
                    <form method="post" action="${ctx}/room/start">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <button class="btn btn-primary" type="submit" ${room.players.size() lt 2 ? 'disabled' : ''}>
                            Start quiz for everyone
                        </button>
                    </form>
                    <p class="room-note">At least two players must join before the host can start.</p>
                </c:if>
                <c:if test="${room.started and roomAttemptActive}">
                    <a class="btn btn-primary" href="${ctx}/quiz">Resume quiz</a>
                </c:if>
                <c:if test="${room.started and roomPlayerFinished}">
                    <p class="room-note">You finished this room quiz. The live board will stay available while others finish.</p>
                </c:if>
                <form class="sr-only" method="post" action="${ctx}/quiz/start" data-room-quiz-form>
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="quizId" value="${room.quizId}">
                    <input type="hidden" name="roomCode" value="${room.code}">
                    <button type="submit">Start room quiz</button>
                </form>
            </section>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="includes/footer.jspf" %>
