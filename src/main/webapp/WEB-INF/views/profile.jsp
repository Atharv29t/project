<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="My scores" />
<%@ include file="includes/header.jspf" %>

<section class="profile-shell container">
    <div class="profile-hero">
        <span class="profile-avatar">${fn:escapeXml(sessionScope.currentUser.initials)}</span>
        <div class="profile-copy">
            <span class="eyebrow">Your account</span>
            <h1><c:out value="${sessionScope.currentUser.name}" /></h1>
            <p><c:out value="${sessionScope.currentUser.email}" /></p>
            <div class="profile-meta">
                <span><svg class="icon" aria-hidden="true"><use href="#i-shield"/></svg> Member since ${sessionScope.currentUser.memberSinceDisplay}</span>
                <span><svg class="icon" aria-hidden="true"><use href="#i-clock"/></svg> Last login ${sessionScope.currentUser.lastLoginDisplay}</span>
            </div>
        </div>
        <a class="btn btn-primary" href="${ctx}/#quizPicker">
            Play another
            <svg class="icon" aria-hidden="true"><use href="#i-arrow-right"/></svg>
        </a>
    </div>

    <div class="stat-cards profile-stats">
        <div class="stat-card stat-card-sky">
            <span class="stat-icon" aria-hidden="true"><svg class="icon"><use href="#i-list"/></svg></span>
            <span class="stat-copy">
                <span class="value">${attemptCount}</span>
                <span class="label">Attempts</span>
            </span>
        </div>
        <div class="stat-card stat-card-gold">
            <span class="stat-icon" aria-hidden="true"><svg class="icon"><use href="#i-target"/></svg></span>
            <span class="stat-copy">
                <span class="value">${bestPercentage}%</span>
                <span class="label">Best score</span>
            </span>
        </div>
        <div class="stat-card stat-card-violet">
            <span class="stat-icon" aria-hidden="true"><svg class="icon"><use href="#i-chart"/></svg></span>
            <span class="stat-copy">
                <span class="value">${averagePercentage}%</span>
                <span class="label">Average</span>
            </span>
        </div>
    </div>

    <section class="progression-card" aria-labelledby="progressionTitle">
        <div class="progression-heading">
            <div>
                <span class="kicker">Your learning streak</span>
                <h2 id="progressionTitle">Level ${progression.level} · ${progression.xp} XP</h2>
            </div>
            <span class="streak-chip">${progression.currentStreak} day streak</span>
        </div>
        <div class="xp-track" role="progressbar" aria-valuenow="${progression.xpIntoLevel}" aria-valuemin="0" aria-valuemax="100">
            <span style="width:${progression.xpIntoLevel}%"></span>
        </div>
        <p>${progression.xpToNextLevel} XP to level ${progression.level + 1}</p>
        <h3>Badges</h3>
        <div class="badge-list">
            <c:choose>
                <c:when test="${empty progression.badges}"><span class="badge locked">Complete a quiz to earn your first badge</span></c:when>
                <c:otherwise>
                    <c:forEach var="badge" items="${progression.badges}">
                        <span class="badge">${fn:replace(badge, '-', ' ')}</span>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </div>
    </section>

    <div class="history-head">
        <div>
            <span class="kicker">Score history</span>
            <h2>Your finished attempts</h2>
        </div>
        <a class="text-link" href="${ctx}/leaderboard">See the leaderboard</a>
    </div>

    <c:choose>
        <c:when test="${empty results}">
            <div class="empty-state">
                <span class="empty-icon" aria-hidden="true"><svg class="icon"><use href="#i-trophy"/></svg></span>
                <h2>No finished attempts yet</h2>
                <p>Pick a track, answer six questions and your score will appear here.</p>
                <a class="btn btn-primary" href="${ctx}/#quizPicker">Choose a track</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="history-list">
                <c:forEach var="result" items="${results}">
                    <a class="history-row" href="${ctx}/result?id=${result.id}" style="--accent:${result.accentColor};">
                        <span class="history-icon" aria-hidden="true">
                            <c:choose>
                                <c:when test="${result.quizId eq 'java'}"><svg class="icon"><use href="#i-java"/></svg></c:when>
                                <c:when test="${result.quizId eq 'dsa'}"><svg class="icon"><use href="#i-dsa"/></svg></c:when>
                                <c:otherwise><svg class="icon"><use href="#i-web"/></svg></c:otherwise>
                            </c:choose>
                        </span>
                        <span class="history-main">
                            <strong><c:out value="${result.quizTitle}" /></strong>
                            <span>${result.completedAtDisplay} · ${result.scoreLabel} correct</span>
                        </span>
                        <span class="history-score">${result.percentage}%</span>
                        <span class="history-arrow" aria-hidden="true"><svg class="icon"><use href="#i-arrow-right"/></svg></span>
                    </a>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="includes/footer.jspf" %>
