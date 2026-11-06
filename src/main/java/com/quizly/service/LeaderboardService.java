package com.quizly.service;

import com.quizly.model.LeaderboardEntry;
import com.quizly.model.LeaderboardStats;
import com.quizly.model.Result;
import com.quizly.repository.ResultRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Computes leaderboard standings using a bounded priority queue (min-heap).
 *
 * <p>For the top K entries, the heap keeps the K best-known results and lets
 * the worst one fall off whenever a better one appears. That means every result
 * is only compared against a constant-size heap: O(n log K), even when there
 * are many stored attempts.</p>
 */
public final class LeaderboardService {

    private final ResultRepository repository;

    public LeaderboardService(ResultRepository repository) {
        this.repository = repository;
    }

    public List<LeaderboardEntry> topEntries(String quizId, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        List<Result> candidates = repository.findAll().stream()
                .filter(filterFor(quizId))
                .toList();

        // Worst first: the head of the heap is always the weakest qualifying result.
        Comparator<Result> worstFirst = Comparator
                .comparingInt(Result::getPercentage)
                .thenComparing(Result::getCompletedAt, Comparator.reverseOrder());

        PriorityQueue<Result> heap = new PriorityQueue<>(Math.min(limit, Math.max(1, candidates.size())), worstFirst);
        for (Result result : candidates) {
            if (heap.size() < limit) {
                heap.offer(result);
            } else if (worstFirst.compare(result, heap.peek()) > 0) {
                heap.poll();
                heap.offer(result);
            }
        }

        List<Result> top = new ArrayList<>(heap);
        top.sort(worstFirst.reversed());

        return IntStream.range(0, top.size())
                .mapToObj(index -> new LeaderboardEntry(index + 1, top.get(index)))
                .toList();
    }

    public LeaderboardStats stats(String quizId) {
        List<Result> qualifying = repository.findAll().stream()
                .filter(filterFor(quizId))
                .toList();

        if (qualifying.isEmpty()) {
            return new LeaderboardStats(0, 0, 0);
        }

        int average = (int) Math.round(qualifying.stream()
                .mapToInt(Result::getPercentage)
                .average()
                .orElse(0));
        int best = qualifying.stream().mapToInt(Result::getPercentage).max().orElse(0);
        return new LeaderboardStats(qualifying.size(), average, best);
    }

    public int rankOf(String resultId, String quizId) {
        return topEntries(quizId, 10).stream()
                .filter(entry -> entry.getResult().getId().equals(resultId))
                .mapToInt(LeaderboardEntry::getRank)
                .findFirst()
                .orElse(0);
    }

    /**
     * Null or blank means "all quizzes"; otherwise only match the chosen id.
     */
    private Predicate<Result> filterFor(String quizId) {
        return result -> quizId == null || quizId.isBlank() || quizId.equals(result.getQuizId());
    }
}
