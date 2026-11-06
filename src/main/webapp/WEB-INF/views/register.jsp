<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Create account" />
<%@ include file="includes/header.jspf" %>

<section class="auth-shell container">
    <div class="auth-card">
        <span class="auth-icon" aria-hidden="true"><svg class="icon"><use href="#i-user"/></svg></span>
        <span class="eyebrow">Join Quizly</span>
        <h1>Create your account</h1>
        <p class="auth-lead">Pick a display name, save every score and keep your answer reviews in one place.</p>

        <c:if test="${not empty formError}">
            <div class="alert alert-error" role="alert">
                <svg class="icon" aria-hidden="true"><use href="#i-info"/></svg>
                <c:out value="${formError}" />
            </div>
        </c:if>

        <form class="auth-form" method="post" action="${ctx}/register">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">

            <div class="field">
                <label for="name">Display name</label>
                <input type="text" id="name" name="name" minlength="2" maxlength="40"
                       autocomplete="name" placeholder="e.g. Ada Lovelace"
                       value="${fn:escapeXml(nameValue)}" required autofocus>
            </div>

            <div class="field">
                <label for="email">Email address</label>
                <input type="email" id="email" name="email" maxlength="190"
                       autocomplete="email" placeholder="you@example.com"
                       value="${fn:escapeXml(emailValue)}" required>
            </div>

            <div class="field">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" minlength="8" maxlength="72"
                       autocomplete="new-password" placeholder="At least 8 characters" required>
            </div>

            <div class="field">
                <label for="confirmPassword">Confirm password</label>
                <input type="password" id="confirmPassword" name="confirmPassword" minlength="8" maxlength="72"
                       autocomplete="new-password" placeholder="Repeat your password" required>
            </div>

            <button class="btn btn-primary auth-submit" type="submit">
                Create account
                <svg class="icon" aria-hidden="true"><use href="#i-arrow-right"/></svg>
            </button>
        </form>

        <p class="auth-switch">Already have an account? <a href="${ctx}/login">Log in</a></p>
    </div>

    <aside class="auth-aside" aria-label="Account benefits">
        <span class="kicker">One account, every track</span>
        <h2>Play once, review forever.</h2>
        <ul class="auth-points">
            <li>
                <span class="auth-point-icon"><svg class="icon"><use href="#i-shield"/></svg></span>
                <span><strong>Private by default</strong><br>Passwords are salted and hashed with PBKDF2 before storage.</span>
            </li>
            <li>
                <span class="auth-point-icon"><svg class="icon"><use href="#i-list"/></svg></span>
                <span><strong>All three tracks</strong><br>Java, DSA and Web &amp; JSP are included from day one.</span>
            </li>
            <li>
                <span class="auth-point-icon"><svg class="icon"><use href="#i-chart"/></svg></span>
                <span><strong>Progress overview</strong><br>See attempts, best score and average on your profile.</span>
            </li>
        </ul>
    </aside>
</section>

<%@ include file="includes/footer.jspf" %>
