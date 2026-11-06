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
                <span class="q-type">Multiple choice</span>
            </div>
            <h1><c:out value="${question.prompt}" /></h1>

            <form class="answer-form" method="post" action="${ctx}/quiz">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="questionId" value="${question.id}">

                <fieldset>
                    <legend class="sr-only">Choose one answer</legend>
                    <div class="option-list" role="list">
                        <c:forEach var="option" items="${question.options}" varStatus="status">
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
                    <button class="skip-btn" type="submit" name="skip" value="1" formnovalidate>Skip this one</button>
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
                        <span class="fact-value">1</span>
                        <span class="fact-label">mark each</span>
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
                <p>No. Each answer advances the attempt. If you skip, the question is marked as not answered in the final review.</p>
            </details>
        </aside>
    </div>
</section>

<%@ include file="includes/footer.jspf" %>
