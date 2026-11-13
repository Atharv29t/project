<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Quiz planner" />
<c:set var="trackFormRetry" value="${adminFormKind eq 'track'}" />
<c:set var="questionFormRetry" value="${adminFormKind eq 'question'}" />
<%@ include file="includes/header.jspf" %>

<section class="admin-hero container section">
    <div>
        <span class="kicker">Admin planner</span>
        <h1>Manage quiz content</h1>
        <p>Create tracks, write questions, and control what players can see. Archived content stays available in saved result reviews.</p>
    </div>
    <a class="btn btn-primary" href="${ctx}/admin?quizId=new">Create quiz track</a>
</section>

<section class="container admin-layout">
    <aside class="admin-sidebar" aria-label="Quiz tracks">
        <div class="admin-section-head"><h2>Quiz tracks</h2><span>${fn:length(quizzes)}</span></div>
        <c:forEach var="item" items="${quizzes}">
            <a class="admin-track-link ${selectedQuiz.id eq item.id ? 'is-selected' : ''}" href="${ctx}/admin?quizId=${fn:escapeXml(item.id)}">
                <span class="admin-track-icon"><c:out value="${item.icon}" /></span>
                <span class="admin-track-name"><strong><c:out value="${item.title}" /></strong><small><c:out value="${item.statusValue}" /> · ${item.questionCount} questions</small></span>
            </a>
        </c:forEach>
        <c:if test="${empty quizzes}"><p class="admin-empty">No tracks yet. Create the first one.</p></c:if>
    </aside>

    <div class="admin-main">
        <c:if test="${not empty selectedQuiz}">
            <section class="admin-panel">
                <div class="admin-section-head">
                    <div><span class="kicker">Track details</span><h2><c:choose><c:when test="${empty selectedQuiz.id}">New quiz track</c:when><c:otherwise>Edit track</c:otherwise></c:choose></h2></div>
                    <span class="status-pill status-${selectedQuiz.statusValue}"><c:out value="${selectedQuiz.statusValue}" /></span>
                </div>
                <form class="admin-form" method="post" action="${ctx}/admin">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="saveTrack">
                    <input type="hidden" name="quizId" value="${empty selectedQuiz.id ? 'new' : fn:escapeXml(selectedQuiz.id)}">
                    <c:if test="${not empty selectedQuiz.id}"><input type="hidden" name="revision" value="${selectedQuiz.revision}"></c:if>
                    <label>Track ID
                        <input name="id" required maxlength="48" pattern="[a-z0-9]+(-[a-z0-9]+)*" value="${fn:escapeXml(trackFormRetry ? adminFormValues.id : selectedQuiz.id)}" ${not empty selectedQuiz.id ? 'readonly' : ''} placeholder="for-example networking">
                        <small>Use lowercase letters, numbers, and hyphens. This ID cannot be changed after creation.</small>
                    </label>
                    <div class="admin-form-grid">
                        <label>Title<input name="title" required maxlength="80" value="${fn:escapeXml(trackFormRetry ? adminFormValues.title : selectedQuiz.title)}"></label>
                        <label>Tagline<input name="tagline" maxlength="120" value="${fn:escapeXml(trackFormRetry ? adminFormValues.tagline : selectedQuiz.tagline)}"></label>
                        <label>Icon<input name="icon" required maxlength="8" value="${fn:escapeXml(trackFormRetry ? adminFormValues.icon : selectedQuiz.icon)}"></label>
                        <label>Level<input name="level" required maxlength="30" value="${fn:escapeXml(trackFormRetry ? adminFormValues.level : selectedQuiz.level)}"></label>
                        <label>Estimated minutes<input name="estimatedMinutes" type="number" min="1" max="180" required value="${trackFormRetry ? adminFormValues.estimatedMinutes : selectedQuiz.estimatedMinutes}"></label>
                        <label>Accent colour<input name="accentColor" type="color" value="${fn:escapeXml(trackFormRetry ? adminFormValues.accentColor : selectedQuiz.accentColor)}"></label>
                    </div>
                    <label>Description<textarea name="description" rows="3" maxlength="500"><c:out value="${trackFormRetry ? adminFormValues.description : selectedQuiz.description}" /></textarea></label>
                    <label>Status
                        <select name="status">
                            <option value="draft" ${(trackFormRetry and adminFormValues.status eq 'draft') or (not trackFormRetry and selectedQuiz.statusValue eq 'draft') ? 'selected' : ''}>Draft</option>
                            <option value="published" ${(trackFormRetry and adminFormValues.status eq 'published') or (not trackFormRetry and selectedQuiz.statusValue eq 'published') ? 'selected' : ''}>Published</option>
                            <option value="archived" ${(trackFormRetry and adminFormValues.status eq 'archived') or (not trackFormRetry and selectedQuiz.statusValue eq 'archived') ? 'selected' : ''}>Archived</option>
                        </select>
                    </label>
                    <div class="admin-publish-check" aria-live="polite">
                        <strong>Published questions needed before publishing</strong>
                        <span>Easy <b>${publishedEasyCount}/2</b></span>
                        <span>Medium <b>${publishedMediumCount}/2</b></span>
                        <span>Hard <b>${publishedHardCount}/2</b></span>
                    </div>
                    <button class="btn btn-primary" type="submit">Save track</button>
                </form>
            </section>

            <c:if test="${not empty selectedQuiz.id}">
                <section class="admin-panel">
                    <div class="admin-section-head">
                        <div><span class="kicker">Question bank</span><h2>Questions <span class="admin-count">${selectedQuiz.questionCount}</span></h2><p class="admin-hint">Add or edit questions below. Keep at least two published questions at each difficulty to publish this track.</p></div>
                        <a class="btn btn-primary btn-sm" href="${ctx}/admin?quizId=${fn:escapeXml(selectedQuiz.id)}&amp;newQuestion=1">Add question</a>
                    </div>
                    <div class="admin-question-list">
                        <c:forEach var="question" items="${selectedQuiz.questions}" varStatus="status">
                            <article class="admin-question-row">
                                <div class="admin-question-number">${status.count}</div>
                                <div class="admin-question-copy">
                                    <strong><c:out value="${question.prompt}" /></strong>
                                    <small>Difficulty ${question.difficulty} · <c:out value="${question.statusValue}" /></small>
                                </div>
                                <a class="text-link" href="${ctx}/admin?quizId=${fn:escapeXml(selectedQuiz.id)}&amp;questionId=${fn:escapeXml(question.id)}">Edit</a>
                                <form method="post" action="${ctx}/admin">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="changeQuestionStatus">
                                    <input type="hidden" name="quizId" value="${fn:escapeXml(selectedQuiz.id)}">
                                    <input type="hidden" name="questionId" value="${fn:escapeXml(question.id)}">
                                    <input type="hidden" name="revision" value="${selectedQuiz.revision}">
                                    <input type="hidden" name="status" value="${question.statusValue eq 'archived' ? 'published' : 'archived'}">
                                    <button class="admin-text-button" type="submit">${question.statusValue eq 'archived' ? 'Restore' : 'Archive'}</button>
                                </form>
                            </article>
                        </c:forEach>
                        <c:if test="${empty selectedQuiz.questions}"><p class="admin-empty">This track has no questions yet. Add questions before publishing.</p></c:if>
                    </div>
                </section>

                <c:if test="${param.newQuestion eq '1' or not empty selectedQuestion or questionFormRetry}">
                    <section class="admin-panel" id="questionEditor">
                        <div class="admin-section-head"><div><span class="kicker">Question editor</span><h2><c:choose><c:when test="${empty selectedQuestion}">Add a question</c:when><c:otherwise>Edit question</c:otherwise></c:choose></h2></div></div>
                        <form class="admin-form" method="post" action="${ctx}/admin">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="saveQuestion">
                            <input type="hidden" name="quizId" value="${fn:escapeXml(selectedQuiz.id)}">
                            <input type="hidden" name="questionId" value="${fn:escapeXml(questionFormRetry ? adminFormValues.questionId : selectedQuestion.id)}">
                            <input type="hidden" name="revision" value="${selectedQuiz.revision}">
                            <label>Question<textarea name="prompt" required minlength="5" maxlength="600" rows="3"><c:out value="${questionFormRetry ? adminFormValues.prompt : selectedQuestion.prompt}" /></textarea></label>
                            <label>Answer options <small>One option per line, between two and six.</small>
                                <textarea name="options" required rows="6" placeholder="First answer&#10;Second answer&#10;Third answer&#10;Fourth answer"><c:choose><c:when test="${questionFormRetry}"><c:out value="${adminFormValues.options}" /></c:when><c:when test="${not empty selectedQuestion}"><c:out value="${selectedQuestion.optionsMultiline}" /></c:when></c:choose></textarea>
                            </label>
                            <div class="admin-form-grid">
                                <label>Correct option number
                                    <select name="correctOptionIndex">
                                        <option value="0" ${(questionFormRetry and adminFormValues.correctOptionIndex eq '0') or (not questionFormRetry and selectedQuestion.correctOptionIndex eq 0) ? 'selected' : ''}>Option 1</option>
                                        <option value="1" ${(questionFormRetry and adminFormValues.correctOptionIndex eq '1') or (not questionFormRetry and selectedQuestion.correctOptionIndex eq 1) ? 'selected' : ''}>Option 2</option>
                                        <option value="2" ${(questionFormRetry and adminFormValues.correctOptionIndex eq '2') or (not questionFormRetry and selectedQuestion.correctOptionIndex eq 2) ? 'selected' : ''}>Option 3</option>
                                        <option value="3" ${(questionFormRetry and adminFormValues.correctOptionIndex eq '3') or (not questionFormRetry and selectedQuestion.correctOptionIndex eq 3) ? 'selected' : ''}>Option 4</option>
                                        <option value="4" ${(questionFormRetry and adminFormValues.correctOptionIndex eq '4') or (not questionFormRetry and selectedQuestion.correctOptionIndex eq 4) ? 'selected' : ''}>Option 5</option>
                                        <option value="5" ${(questionFormRetry and adminFormValues.correctOptionIndex eq '5') or (not questionFormRetry and selectedQuestion.correctOptionIndex eq 5) ? 'selected' : ''}>Option 6</option>
                                    </select>
                                </label>
                                <label>Difficulty<select name="difficulty"><option value="1" ${(questionFormRetry and adminFormValues.difficulty eq '1') or (not questionFormRetry and selectedQuestion.difficulty eq 1) or (not questionFormRetry and empty selectedQuestion) ? 'selected' : ''}>Easy</option><option value="2" ${(questionFormRetry and adminFormValues.difficulty eq '2') or (not questionFormRetry and selectedQuestion.difficulty eq 2) ? 'selected' : ''}>Medium</option><option value="3" ${(questionFormRetry and adminFormValues.difficulty eq '3') or (not questionFormRetry and selectedQuestion.difficulty eq 3) ? 'selected' : ''}>Hard</option></select></label>
                                <label>Status<select name="questionStatus"><option value="draft" ${(questionFormRetry and adminFormValues.questionStatus eq 'draft') or (not questionFormRetry and not empty selectedQuestion and selectedQuestion.statusValue eq 'draft') ? 'selected' : ''}>Draft</option><option value="published" ${(questionFormRetry and adminFormValues.questionStatus eq 'published') or (not questionFormRetry and (empty selectedQuestion or selectedQuestion.statusValue eq 'published')) ? 'selected' : ''}>Published</option><option value="archived" ${(questionFormRetry and adminFormValues.questionStatus eq 'archived') or (not questionFormRetry and selectedQuestion.statusValue eq 'archived') ? 'selected' : ''}>Archived</option></select></label>
                            </div>
                            <label>Explanation<textarea name="explanation" rows="3" maxlength="1000"><c:out value="${questionFormRetry ? adminFormValues.explanation : selectedQuestion.explanation}" /></textarea></label>
                            <div class="admin-form-actions">
                                <button class="btn btn-primary" type="submit">Save question</button>
                                <c:if test="${empty selectedQuestion}"><button class="btn btn-ghost" type="submit" name="saveMode" value="addAnother">Save and add another</button></c:if>
                            </div>
                        </form>
                    </section>
                </c:if>
            </c:if>
        </c:if>
    </div>
</section>

<section class="container section admin-panel admin-audit" id="changeLog">
    <div class="admin-section-head"><div><span class="kicker">Recent activity</span><h2>Change log</h2></div></div>
    <c:choose>
        <c:when test="${not empty recentAuditLogs}">
            <div class="admin-audit-list">
                <c:forEach var="entry" items="${recentAuditLogs}">
                    <div class="admin-audit-row">
                        <span class="status-pill"><c:out value="${entry.action}" /></span>
                        <strong><c:out value="${entry.summary}" /></strong>
                        <span>by <c:out value="${entry.actorId}" /></span>
                        <time><c:out value="${entry.timestamp}" /></time>
                        <c:if test="${not empty entry.changes}">
                            <div class="admin-audit-changes">
                                <c:forEach var="change" items="${entry.changes}">
                                    <div class="admin-audit-change"><b><c:out value="${change.field}" /></b><span><c:out value="${change.before}" /></span><span aria-hidden="true">→</span><span><c:out value="${change.after}" /></span></div>
                                </c:forEach>
                            </div>
                        </c:if>
                    </div>
                </c:forEach>
            </div>
        </c:when>
        <c:otherwise><p class="admin-empty">Changes made in the planner will appear here.</p></c:otherwise>
    </c:choose>
</section>

<%@ include file="includes/footer.jspf" %>
