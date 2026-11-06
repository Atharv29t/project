<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Something went wrong" />
<%@ include file="includes/header.jspf" %>

<section class="error-shell container">
    <div class="error-card">
        <span class="error-icon" aria-hidden="true">
            <svg class="icon"><use href="#i-info"/></svg>
        </span>
        <c:if test="${not empty pageContext.request.getAttribute('jakarta.servlet.error.status_code')}">
            <span class="error-code">${pageContext.request.getAttribute('jakarta.servlet.error.status_code')}</span>
        </c:if>
        <h1>That page is not available</h1>
        <p>The quiz app could not find or process what you asked for. Your current attempt stays available unless the session has expired.</p>
        <div class="error-actions">
            <a class="btn btn-primary" href="${ctx}/">Back to quizzes</a>
            <a class="btn btn-ghost" href="${ctx}/leaderboard">Leaderboard</a>
        </div>
    </div>
</section>

<%@ include file="includes/footer.jspf" %>
