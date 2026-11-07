<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Online quizzes" />
<%@ include file="includes/header.jspf" %>

<section class="hero">
    <div class="container hero-grid">
        <div class="hero-copy">
            <span class="eyebrow">Java web quiz</span>
            <h1>Play. Learn.<br><span>Climb the board.</span></h1>
            <p class="lead">
                Three short tracks on Java, data structures and the web. Answer six questions,
                see a clear review of every option, and find out where you land.
            </p>
            <div class="hero-actions">
                <a class="btn btn-primary" href="#quizPicker">
                    Get started
                    <svg class="icon" aria-hidden="true"><use href="#i-arrow-right"/></svg>
                </a>
                <a class="btn btn-ghost" href="${ctx}/leaderboard">View leaderboard</a>
                <a class="btn btn-ghost" href="${ctx}/room">Play live with friends</a>
            </div>
            <dl class="hero-stats">
                <div class="stat stat-sky">
                    <dt>Quiz tracks</dt>
                    <dd>${quizCount}</dd>
                </div>
                <div class="stat stat-coral">
                    <dt>Questions</dt>
                    <dd>${questionCount}</dd>
                </div>
                <div class="stat stat-violet">
                    <dt>Attempts</dt>
                    <dd>${attemptCount}</dd>
                </div>
            </dl>
        </div>

        <div class="hero-mosaic" aria-label="How Quizly works">
            <a class="mosaic-tile tile-blue" href="#quizPicker">
                <span class="tile-kicker">
                    <svg class="icon" aria-hidden="true"><use href="#i-spark"/></svg>
                    Start here
                </span>
                <span class="tile-title">Play<br>Learn<br>Win</span>
                <svg class="tile-art tile-art-boy" aria-hidden="true"><use href="#il-boy"/></svg>
                <span class="tile-bubble">?</span>
            </a>

            <div class="mosaic-stack">
                <a class="mosaic-tile tile-violet" href="#quizPicker">
                    <span class="tile-small">Every answer<br><span>gets a clear review</span></span>
                    <svg class="tile-art tile-art-girl" aria-hidden="true"><use href="#il-girl-question"/></svg>
                </a>

                <div class="mosaic-split">
                    <a class="mosaic-tile tile-dark" href="#quizPicker">
                        <svg class="tile-icon" aria-hidden="true"><use href="#i-star"/></svg>
                        <span class="tile-small">${questionCount} questions<br><span>across ${quizCount} tracks</span></span>
                    </a>
                    <a class="mosaic-tile tile-coral" href="${ctx}/leaderboard">
                        <svg class="tile-art tile-art-trophy" aria-hidden="true"><use href="#il-trophy"/></svg>
                        <span class="tile-small tile-small-bottom">Top 10<br><span>make the board</span></span>
                    </a>
                </div>
            </div>
        </div>
    </div>
</section>

<section id="quizPicker" class="container section" aria-labelledby="quizHeading">
    <div class="section-head">
        <div>
            <span class="kicker">Start a run</span>
            <h2 id="quizHeading">Choose your track</h2>
        </div>
        <p>Six questions per run. Questions and options are shuffled every time.</p>
    </div>

    <form id="quizStartForm" class="quiz-form" method="post" action="${ctx}/quiz/start">
        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
        <div class="player-card account-card">
            <c:choose>
                <c:when test="${not empty sessionScope.currentUser}">
                    <span class="account-avatar">${fn:escapeXml(sessionScope.currentUser.initials)}</span>
                    <div class="account-copy">
                        <span class="field-label">Playing as</span>
                        <strong><c:out value="${sessionScope.currentUser.name}" /></strong>
                        <p class="field-hint">This account name is shown on your result and on the leaderboard.</p>
                    </div>
                    <a class="text-link" href="${ctx}/profile">View my scores</a>
                </c:when>
                <c:otherwise>
                    <span class="account-avatar guest" aria-hidden="true"><svg class="icon"><use href="#i-user"/></svg></span>
                    <div class="account-copy">
                        <span class="field-label">Save your progress</span>
                        <strong>Log in to play</strong>
                        <p class="field-hint">Scores and answer reviews are stored against your free account.</p>
                    </div>
                    <a class="btn btn-primary btn-sm" href="${ctx}/login?next=/">Log in</a>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="track-grid">
            <c:forEach var="quiz" items="${quizzes}" varStatus="status">
                <article class="track-card track-${quiz.id}" style="--accent:${quiz.accentColor};">
                    <div class="track-top">
                        <span class="track-icon" aria-hidden="true">
                            <c:choose>
                                <c:when test="${quiz.id eq 'java'}"><svg class="icon"><use href="#i-java"/></svg></c:when>
                                <c:when test="${quiz.id eq 'dsa'}"><svg class="icon"><use href="#i-dsa"/></svg></c:when>
                                <c:otherwise><svg class="icon"><use href="#i-web"/></svg></c:otherwise>
                            </c:choose>
                        </span>
                        <span class="level-pill">${fn:escapeXml(quiz.level)}</span>
                    </div>

                    <div class="track-body">
                        <h3>${fn:escapeXml(quiz.title)}</h3>
                        <p>${fn:escapeXml(quiz.tagline)}</p>
                    </div>

                    <div class="track-meta">
                        <span>
                            <svg class="icon" aria-hidden="true"><use href="#i-list"/></svg>
                            ${quiz.questionCount} question bank · 6 per run
                        </span>
                        <span>
                            <svg class="icon" aria-hidden="true"><use href="#i-clock"/></svg>
                            ~${quiz.estimatedMinutes} min
                        </span>
                    </div>

                    <c:choose>
                        <c:when test="${not empty sessionScope.currentUser}">
                            <button class="btn track-btn" type="submit" name="quizId" value="${quiz.id}">
                                Start track
                                <svg class="icon" aria-hidden="true"><use href="#i-arrow-right"/></svg>
                            </button>
                        </c:when>
                        <c:otherwise>
                            <a class="btn track-btn" href="${ctx}/login?next=/">
                                Log in to start
                                <svg class="icon" aria-hidden="true"><use href="#i-arrow-right"/></svg>
                            </a>
                        </c:otherwise>
                    </c:choose>
                </article>
            </c:forEach>
        </div>
    </form>
</section>

<section class="container section board-preview" aria-labelledby="previewHeading">
    <div class="board-preview-copy">
        <span class="kicker">On the board</span>
        <h2 id="previewHeading">Top scores right now</h2>
        <p>Every finished attempt is stored, so the board is still there when the app restarts.</p>
        <a class="btn btn-ghost" href="${ctx}/leaderboard">
            Open leaderboard
            <svg class="icon" aria-hidden="true"><use href="#i-arrow-right"/></svg>
        </a>
    </div>

    <div class="preview-card">
        <div class="preview-head">
            <h3>All quizzes</h3>
            <span>Top 3</span>
        </div>
        <c:choose>
            <c:when test="${fn:length(topPreview) gt 0}">
                <ol class="preview-list">
                    <c:forEach var="entry" items="${topPreview}" varStatus="status">
                        <li>
                            <span class="preview-rank rank-${entry.rank}">${entry.rank}</span>
                            <span class="avatar avatar-sm" style="--accent:${entry.result.accentColor}">${fn:escapeXml(entry.result.initials)}</span>
                            <span class="preview-name">
                                <strong><c:out value="${entry.result.playerName}" /></strong>
                                <span><c:out value="${entry.result.quizTitle}" /></span>
                            </span>
                            <span class="percent-pill">${entry.result.percentage}%</span>
                        </li>
                    </c:forEach>
                </ol>
            </c:when>
            <c:otherwise>
                <div class="preview-empty">
                    <span class="preview-empty-icon" aria-hidden="true"><svg class="icon"><use href="#i-trophy"/></svg></span>
                    <p>The board is empty. The first score could be yours.</p>
                    <a class="text-link" href="#quizPicker">Start a track</a>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<section class="container section" aria-labelledby="howHeading">
    <div class="section-head">
        <div>
            <span class="kicker">How it works</span>
            <h2 id="howHeading">Three steps to the board</h2>
        </div>
        <p>Everything runs on the Java server. The browser only sends your answers.</p>
    </div>

    <div class="steps-grid">
        <article class="step-card">
            <span class="step-number">01</span>
            <span class="step-icon" aria-hidden="true"><svg class="icon"><use href="#i-target"/></svg></span>
            <h3>Pick a track</h3>
            <p>Choose Java, DSA or the web and play under your account name.</p>
        </article>
        <article class="step-card">
            <span class="step-number">02</span>
            <span class="step-icon" aria-hidden="true"><svg class="icon"><use href="#i-list"/></svg></span>
            <h3>Answer six</h3>
            <p>One question at a time. You can skip, but each answer moves you forward.</p>
        </article>
        <article class="step-card">
            <span class="step-number">03</span>
            <span class="step-icon" aria-hidden="true"><svg class="icon"><use href="#i-review"/></svg></span>
            <h3>Review and rank</h3>
            <p>Read the explanation for every question, then check your place on the board.</p>
        </article>
    </div>
</section>

<%@ include file="includes/footer.jspf" %>
