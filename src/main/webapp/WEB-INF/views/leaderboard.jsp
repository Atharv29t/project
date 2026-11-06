<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Leaderboard" />
<%@ include file="includes/header.jspf" %>

<section class="board-shell container">
    <div class="page-hero">
        <span class="eyebrow">Hall of fame</span>
        <h1>Leaderboard</h1>
        <p>The top 10 attempts by correct-answer percentage. Ties go to the earlier completion time.</p>
    </div>

    <nav class="filter-tabs" aria-label="Leaderboard filters">
        <a class="filter-tab ${empty selectedQuizId ? 'is-active' : ''}" href="${ctx}/leaderboard">All quizzes</a>
        <c:forEach var="quiz" items="${quizzes}">
            <a class="filter-tab ${selectedQuizId eq quiz.id ? 'is-active' : ''}"
               href="${ctx}/leaderboard?quiz=${quiz.id}">
                <c:choose>
                    <c:when test="${quiz.id eq 'java'}"><svg class="icon" aria-hidden="true"><use href="#i-java"/></svg></c:when>
                    <c:when test="${quiz.id eq 'dsa'}"><svg class="icon" aria-hidden="true"><use href="#i-dsa"/></svg></c:when>
                    <c:otherwise><svg class="icon" aria-hidden="true"><use href="#i-web"/></svg></c:otherwise>
                </c:choose>
                ${fn:escapeXml(quiz.title)}
            </a>
        </c:forEach>
    </nav>

    <div class="stat-cards">
        <div class="stat-card stat-card-sky">
            <span class="stat-icon" aria-hidden="true"><svg class="icon"><use href="#i-list"/></svg></span>
            <span class="stat-copy">
                <span class="value">${stats.attempts}</span>
                <span class="label">Attempts</span>
            </span>
        </div>
        <div class="stat-card stat-card-gold">
            <span class="stat-icon" aria-hidden="true"><svg class="icon"><use href="#i-target"/></svg></span>
            <span class="stat-copy">
                <span class="value">${stats.bestPercentage}%</span>
                <span class="label">Best score</span>
            </span>
        </div>
        <div class="stat-card stat-card-violet">
            <span class="stat-icon" aria-hidden="true"><svg class="icon"><use href="#i-chart"/></svg></span>
            <span class="stat-copy">
                <span class="value">${stats.averagePercentage}%</span>
                <span class="label">Average</span>
            </span>
        </div>
    </div>

    <c:choose>
        <c:when test="${fn:length(entries) == 0}">
            <div class="empty-state">
                <span class="empty-icon" aria-hidden="true"><svg class="icon"><use href="#i-trophy"/></svg></span>
                <h2>No scores here yet</h2>
                <p>Take a quiz and this board will update straight away.</p>
                <a class="btn btn-primary" href="${ctx}/#quizPicker">Start a quiz</a>
            </div>
        </c:when>
        <c:otherwise>
            <c:set var="e1" value="${entries[0]}" />
            <c:set var="e2" value="${entries[1]}" />
            <c:set var="e3" value="${entries[2]}" />

            <div class="podium" aria-label="Top three">
                <c:if test="${not empty e2}">
                    <div class="podium-col p2" style="--accent:${e2.result.accentColor};">
                        <div class="podium-person">
                            <span class="avatar avatar-lg">${fn:escapeXml(e2.result.initials)}</span>
                            <strong><c:out value="${e2.result.playerName}" /></strong>
                            <span class="podium-score">${e2.result.percentage}%</span>
                            <span class="podium-detail">${e2.result.scoreLabel} correct</span>
                        </div>
                        <div class="podium-block" data-rank="2"></div>
                    </div>
                </c:if>

                <c:if test="${not empty e1}">
                    <div class="podium-col p1" style="--accent:${e1.result.accentColor};">
                        <span class="crown" aria-hidden="true"><svg class="icon"><use href="#i-crown"/></svg></span>
                        <div class="podium-person">
                            <span class="avatar avatar-lg">${fn:escapeXml(e1.result.initials)}</span>
                            <strong><c:out value="${e1.result.playerName}" /></strong>
                            <span class="podium-score">${e1.result.percentage}%</span>
                            <span class="podium-detail">${e1.result.scoreLabel} correct</span>
                        </div>
                        <div class="podium-block" data-rank="1"></div>
                    </div>
                </c:if>

                <c:if test="${not empty e3}">
                    <div class="podium-col p3" style="--accent:${e3.result.accentColor};">
                        <div class="podium-person">
                            <span class="avatar avatar-lg">${fn:escapeXml(e3.result.initials)}</span>
                            <strong><c:out value="${e3.result.playerName}" /></strong>
                            <span class="podium-score">${e3.result.percentage}%</span>
                            <span class="podium-detail">${e3.result.scoreLabel} correct</span>
                        </div>
                        <div class="podium-block" data-rank="3"></div>
                    </div>
                </c:if>
            </div>

            <div class="board-list">
                <c:forEach var="entry" items="${entries}" begin="3" varStatus="status">
                    <div class="board-row ${entry.result.id eq lastResultId ? 'you-row' : ''}" style="--accent:${entry.result.accentColor};">
                        <span class="board-rank">${entry.rank}</span>
                        <span class="avatar">${fn:escapeXml(entry.result.initials)}</span>
                        <span class="board-name">
                            <strong>
                                <c:out value="${entry.result.playerName}" />
                                <c:if test="${entry.result.id eq lastResultId}"><span class="you-pill">You</span></c:if>
                            </strong>
                            <span>
                                <c:choose>
                                    <c:when test="${entry.result.quizId eq 'java'}"><svg class="icon"><use href="#i-java"/></svg></c:when>
                                    <c:when test="${entry.result.quizId eq 'dsa'}"><svg class="icon"><use href="#i-dsa"/></svg></c:when>
                                    <c:otherwise><svg class="icon"><use href="#i-web"/></svg></c:otherwise>
                                </c:choose>
                                <c:out value="${entry.result.quizTitle}" />
                            </span>
                        </span>
                        <span class="board-score">
                            <strong>${entry.result.percentage}%</strong>
                            <span>${entry.result.scoreLabel}</span>
                        </span>
                        <span class="board-when">${fn:escapeXml(entry.result.completedAtDisplay)}</span>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>

    <p class="board-note">The top ten are selected with a bounded min-heap, so the repository never has to sort the full score list.</p>
</section>

<%@ include file="includes/footer.jspf" %>
