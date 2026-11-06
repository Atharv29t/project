<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Result" />
<%@ include file="includes/header.jspf" %>

<section class="result-shell container" style="--accent:${result.accentColor};">
    <div class="result-hero ${result.tone}">
        <div class="result-score-wrap">
            <div class="score-ring" style="--score:${result.percentage};">
                <div class="ring-inner">
                    <span class="score-percent">${result.percentage}%</span>
                    <span class="score-fraction">${result.scoreLabel}</span>
                    <span class="score-caption">correct</span>
                </div>
            </div>
            <div class="result-art" aria-hidden="true">
                <svg><use href="#il-trophy"/></svg>
            </div>
        </div>

        <div class="result-copy">
            <span class="kicker"><c:out value="${result.quizTitle}" /></span>
            <h1>${result.feedbackTitle}</h1>
            <p>${result.feedbackMessage}</p>

            <ul class="result-meta">
                <li>
                    <svg class="icon" aria-hidden="true"><use href="#i-user"/></svg>
                    <span>Player</span>
                    <strong><c:out value="${result.playerName}" /></strong>
                </li>
                <li>
                    <svg class="icon" aria-hidden="true"><use href="#i-clock"/></svg>
                    <span>Completed</span>
                    <strong><c:out value="${result.completedAtDisplay}" /></strong>
                </li>
            </ul>

            <c:if test="${leaderboardRank > 0}">
                <div class="rank-banner">
                    <svg class="icon" aria-hidden="true"><use href="#i-medal"/></svg>
                    Ranked <strong>#${leaderboardRank}</strong> on this track
                </div>
            </c:if>

            <div class="result-actions">
                <form method="post" action="${ctx}/quiz/start">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="playerName" value="${fn:escapeXml(result.playerName)}">
                    <input type="hidden" name="quizId" value="${fn:escapeXml(result.quizId)}">
                    <button class="btn btn-primary" type="submit">
                        Retake this quiz
                        <svg class="icon" aria-hidden="true"><use href="#i-arrow-right"/></svg>
                    </button>
                </form>
                <a class="btn btn-ghost" href="${ctx}/leaderboard?quiz=${fn:escapeXml(result.quizId)}">See board</a>
                <a class="text-link" href="${ctx}/#quizPicker">Choose another track</a>
            </div>
        </div>
    </div>

    <div class="result-breakdown">
        <div class="breakdown-card breakdown-correct">
            <span class="breakdown-icon" aria-hidden="true"><svg class="icon"><use href="#i-check"/></svg></span>
            <span class="value">${result.score}</span>
            <span class="label">Correct</span>
        </div>
        <div class="breakdown-card breakdown-wrong">
            <span class="breakdown-icon" aria-hidden="true"><svg class="icon"><use href="#i-x"/></svg></span>
            <span class="value">${result.wrongCount}</span>
            <span class="label">Incorrect</span>
        </div>
        <div class="breakdown-card breakdown-skip">
            <span class="breakdown-icon" aria-hidden="true"><svg class="icon"><use href="#i-arrow-right"/></svg></span>
            <span class="value">${result.skippedCount}</span>
            <span class="label">Skipped</span>
        </div>
        <div class="breakdown-card breakdown-pass">
            <span class="breakdown-icon" aria-hidden="true"><svg class="icon"><use href="#i-target"/></svg></span>
            <span class="value">${result.isPassed() ? 'Yes' : 'Not yet'}</span>
            <span class="label">Passed</span>
        </div>
    </div>

    <div class="review-section">
        <div class="section-head tight">
            <div>
                <span class="kicker">Answer review</span>
                <h2>Every question, explained</h2>
            </div>
            <p>Compare your answer with the correct one. The explanation is the useful part.</p>
        </div>

        <div class="review-list">
            <c:forEach var="review" items="${result.reviews}" varStatus="status">
                <article class="review-row ${review.getStatusClass()}">
                    <header>
                        <span class="review-number">Q${review.number}</span>
                        <h3><c:out value="${review.prompt}" /></h3>
                        <span class="status-pill">${review.status}</span>
                    </header>
                    <div class="answer-grid">
                        <div class="answer-row player ${not review.answered ? 'empty' : ''}">
                            <span>Your answer</span>
                            <strong><c:out value="${review.selectedAnswerDisplay}" /></strong>
                        </div>
                        <div class="answer-row correct-row">
                            <span>Correct answer</span>
                            <strong><c:out value="${review.correctAnswer}" /></strong>
                        </div>
                    </div>
                    <details class="explanation"<c:if test="${not review.correct}"> open</c:if>>
                        <summary>
                            <svg class="icon" aria-hidden="true"><use href="#i-info"/></svg>
                            Why this is the answer
                        </summary>
                        <p><c:out value="${review.explanation}" /></p>
                    </details>
                </article>
            </c:forEach>
        </div>
    </div>
</section>

<%@ include file="includes/footer.jspf" %>
