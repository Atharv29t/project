<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Log in" />
<%@ include file="includes/header.jspf" %>

<section class="auth-shell container">
    <div class="auth-card">
        <span class="auth-icon" aria-hidden="true"><svg class="icon"><use href="#i-shield"/></svg></span>
        <span class="eyebrow">Welcome back</span>
        <h1>Log in to Quizly</h1>
        <p class="auth-lead">Your scores, answer reviews and place on the board are tied to your account.</p>

        <c:if test="${not empty formError}">
            <div class="alert alert-error" role="alert">
                <svg class="icon" aria-hidden="true"><use href="#i-info"/></svg>
                <c:out value="${formError}" />
            </div>
        </c:if>

        <form class="auth-form" method="post" action="${ctx}/login">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <input type="hidden" name="next" value="${fn:escapeXml(next)}">

            <div class="field">
                <label for="email">Email address</label>
                <input type="email" id="email" name="email" maxlength="190"
                       autocomplete="email" placeholder="you@example.com"
                       value="${fn:escapeXml(emailValue)}" required autofocus>
            </div>

            <div class="field">
                <label for="password">Password</label>
                <input type="password" id="password" name="password"
                       autocomplete="current-password" placeholder="Your password" required>
            </div>

            <button class="btn btn-primary auth-submit" type="submit">
                Log in
                <svg class="icon" aria-hidden="true"><use href="#i-arrow-right"/></svg>
            </button>
        </form>

        <p class="auth-switch">New to Quizly? <a href="${ctx}/register">Create a free account</a></p>
    </div>

    <aside class="auth-aside" aria-label="Account benefits">
        <span class="kicker">Why sign in</span>
        <h2>Your progress, kept together.</h2>
        <ul class="auth-points">
            <li>
                <span class="auth-point-icon"><svg class="icon"><use href="#i-chart"/></svg></span>
                <span><strong>Saved scores</strong><br>Every finished attempt is stored, even after a restart.</span>
            </li>
            <li>
                <span class="auth-point-icon"><svg class="icon"><use href="#i-review"/></svg></span>
                <span><strong>Full answer history</strong><br>Reopen any previous result and read the explanations again.</span>
            </li>
            <li>
                <span class="auth-point-icon"><svg class="icon"><use href="#i-trophy"/></svg></span>
                <span><strong>Leaderboard identity</strong><br>Your account name is the name shown on the board.</span>
            </li>
        </ul>
    </aside>
</section>

<%@ include file="includes/footer.jspf" %>
