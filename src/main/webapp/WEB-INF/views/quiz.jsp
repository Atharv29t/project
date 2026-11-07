<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="question" value="${attempt.currentQuestion}" />
<c:set var="pageTitle" value="${fn:escapeXml(attempt.quiz.title)}" />
<%@ include file="includes/header.jspf" %>

<section class="quiz-shell container" style="--accent:${attempt.quiz.accentColor};">
    <div class="quiz-progress">
        <div class="quiz-toolbar">
            <a class="quiet-link" href="${ctx}/" data-confirm="Leave the quiz? This attempt will not be graded.">
                <svg class="icon" aria-hidden="true"><use href="#i-arrow-left"/></svg>
                Exit quiz
            </a>
            <span class="quiz-title"><c:out value="${attempt.quiz.title}" /></span>
            <span class="progress-count">${attempt.currentNumber} / ${attempt.totalQuestions}</span>
        </div>
        <div class="progress-track" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${attempt.progressPercent}">
            <div class="progress-fill" style="width:${attempt.progressPercent}%"></div>
        </div>
    </div>

    <div class="quiz-layout">
        <article class="question-card">
            <div class="question-head">
                <span class="q-number">Question ${attempt.currentNumber}</span>
                <span class="q-type">Adaptive · <c:choose><c:when test="${question.difficulty eq 1}">Easy</c:when><c:when test="${question.difficulty eq 3}">Hard</c:when><c:otherwise>Medium</c:otherwise></c:choose></span>
            </div>
            <h1><c:out value="${question.prompt}" /></h1>
            <div class="quiz-tools">
                <span class="question-timer" data-seconds="${attempt.remainingSeconds}" aria-live="polite">
                    <span>Time left</span>:
                    <strong id="timerSeconds">${attempt.remainingSeconds}</strong>s
                </span>
                <button class="btn btn-ghost btn-sm" type="button" data-voice-read>
                    Read question aloud
                </button>
                <button class="btn btn-ghost btn-sm" type="button" data-voice-answer>
                    Answer by voice
                </button>
                <span class="voice-status" aria-live="polite"></span>
            </div>

            <form class="answer-form" method="post" action="${ctx}/quiz">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="questionId" value="${question.id}">

                <fieldset>
                    <legend class="sr-only">Choose one answer</legend>
                    <div class="option-list" role="list">
                        <c:forEach var="option" items="${attempt.currentOptions}" varStatus="status">
                            <label class="option" role="listitem">
                                <input type="radio" name="answer" required value="${fn:escapeXml(option)}">
                                <span class="option-letter" aria-hidden="true">${fn:substring('ABCDEF', status.count - 1, status.count)}</span>
                                <span class="option-text"><c:out value="${option}" /></span>
                                <span class="option-check" aria-hidden="true">
                                    <svg class="icon"><use href="#i-check"/></svg>
                                </span>
                            </label>
                        </c:forEach>
                    </div>
                </fieldset>

                <div class="answer-actions">
                    <button class="btn btn-primary submit-answer" type="submit">
                        ${attempt.lastQuestion ? 'Finish quiz' : 'Next question'}
                        <svg class="icon" aria-hidden="true"><use href="#i-arrow-right"/></svg>
                    </button>
                    <button class="sr-only" type="submit" name="timeout" value="1" formnovalidate data-timeout-submit>Time expired</button>
                </div>
                <div class="power-up-bar" aria-label="Power-ups, each usable once per quiz">
                    <strong>Power-ups · one use each</strong>
                    <button class="power-up" type="submit" name="powerUp" value="fiftyFifty" formnovalidate
                            ${not attempt.isPowerUpAvailable('fiftyFifty') ? 'disabled' : ''}>50/50</button>
                    <button class="power-up" type="submit" name="powerUp" value="doublePoints" formnovalidate
                            ${not attempt.isPowerUpAvailable('doublePoints') ? 'disabled' : ''}>Double points</button>
                </div>
            </form>
        </article>

        <aside class="quiz-aside">
            <div class="aside-card track-summary">
                <span class="track-icon" aria-hidden="true">
                    <c:choose>
                        <c:when test="${attempt.quiz.id eq 'java'}"><svg class="icon"><use href="#i-java"/></svg></c:when>
                        <c:when test="${attempt.quiz.id eq 'dsa'}"><svg class="icon"><use href="#i-dsa"/></svg></c:when>
                        <c:otherwise><svg class="icon"><use href="#i-web"/></svg></c:otherwise>
                    </c:choose>
                </span>
                <h2><c:out value="${attempt.quiz.title}" /></h2>
                <p><c:out value="${attempt.quiz.tagline}" /></p>
            </div>

            <div class="aside-card fact-card">
                <h3>This run</h3>
                <ul class="fact-list">
                    <li>
                        <span class="fact-value">${attempt.totalQuestions}</span>
                        <span class="fact-label">questions</span>
                    </li>
                    <li>
                        <span class="fact-value">30s</span>
                        <span class="fact-label">per question</span>
                    </li>
                    <li>
                        <span class="fact-value">${attempt.progressPercent}%</span>
                        <span class="fact-label">complete</span>
                    </li>
                </ul>
            </div>

            <details class="aside-card quiz-note">
                <summary>
                    <svg class="icon" aria-hidden="true"><use href="#i-info"/></svg>
                    Can I change an answer?
                </summary>
                <p>Your next question adapts to your answer: correct answers raise the difficulty, and misses lower it. Use 50/50 to remove two wrong choices, or activate Double points before answering to earn a bonus point and XP for a correct answer. Each power-up can be used once per quiz.</p>
            </details>
        </aside>
    </div>
</section>

<%@ include file="includes/footer.jspf" %>
