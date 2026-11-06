package com.quizly.web;

import com.quizly.db.Database;
import com.quizly.repository.QuizRepository;
import com.quizly.repository.ResultRepository;
import com.quizly.repository.UserRepository;
import com.quizly.service.AuthService;
import com.quizly.service.LeaderboardService;
import com.quizly.service.QuizService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Logger;

/**
 * Creates the database and service objects once, when Tomcat starts the web
 * application. They are shared by every servlet for the whole lifetime of the
 * app.
 */
@WebListener
public final class AppContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Database database = Database.appDatabase();
        database.initialize();

        QuizRepository quizRepository = new QuizRepository(database);
        UserRepository userRepository = new UserRepository(database);
        ResultRepository resultRepository = new ResultRepository(database);
        QuizService quizService = new QuizService(quizRepository);
        AuthService authService = new AuthService(userRepository);
        LeaderboardService leaderboardService = new LeaderboardService(resultRepository);

        event.getServletContext().setAttribute(App.DATABASE, database);
        event.getServletContext().setAttribute(App.QUIZ_REPOSITORY, quizRepository);
        event.getServletContext().setAttribute(App.USER_REPOSITORY, userRepository);
        event.getServletContext().setAttribute(App.RESULT_REPOSITORY, resultRepository);
        event.getServletContext().setAttribute(App.QUIZ_SERVICE, quizService);
        event.getServletContext().setAttribute(App.AUTH_SERVICE, authService);
        event.getServletContext().setAttribute(App.LEADERBOARD_SERVICE, leaderboardService);

        LOG.info("Quizly database ready with "
                + quizRepository.findAll().size() + " quiz track(s), "
                + quizRepository.totalQuestions() + " question(s), "
                + resultRepository.count() + " result(s) and "
                + userRepository.count() + " player account(s)");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Every repository opens short-lived connections; there is no pool to close.
    }
}
