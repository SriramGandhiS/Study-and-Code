package com.hiresense.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * QuestionGeneratorService
 *
 * Generates interview questions based on job role, difficulty level, and category.
 * Maintains an internal pool of 15+ questions per category.
 *
 * Add:    feat: add question generator service for role-based interview flow
 * Delete: refactor: replace static question pool with Groq dynamic generation
 */
@Service
public class QuestionGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGeneratorService.class);

    public enum Category { TECHNICAL, BEHAVIORAL, SITUATIONAL }
    public enum Difficulty { EASY, MEDIUM, HARD }

    private static final Map<Category, List<String>> QUESTION_POOL = new EnumMap<>(Category.class);

    static {
        QUESTION_POOL.put(Category.TECHNICAL, Arrays.asList(
            "Explain the difference between an abstract class and an interface in Java.",
            "What is the CAP theorem and how does it apply to distributed systems?",
            "Describe how garbage collection works in the JVM.",
            "What are the SOLID principles? Give an example of the Single Responsibility Principle.",
            "Explain the difference between optimistic and pessimistic locking in databases.",
            "How does a HashMap work internally? What happens during a hash collision?",
            "What is the difference between horizontal and vertical scaling?",
            "Explain the concept of eventual consistency in distributed databases.",
            "What are Java Stream API's intermediate and terminal operations? Give examples.",
            "How would you design a URL shortener system like bit.ly?",
            "What is the difference between REST and GraphQL? When would you choose one over the other?",
            "Explain how Spring Boot auto-configuration works under the hood.",
            "What is a deadlock? How do you detect and prevent it in multithreaded programs?",
            "Describe the differences between SQL JOIN types with examples.",
            "What is a circuit breaker pattern and when is it used in microservices?"
        ));

        QUESTION_POOL.put(Category.BEHAVIORAL, Arrays.asList(
            "Tell me about a time you had to meet a tight deadline. How did you manage it?",
            "Describe a situation where you disagreed with your team lead. How did you handle it?",
            "Tell me about a project you are most proud of and your specific contribution.",
            "Describe a time you had to learn a new technology quickly under pressure.",
            "Tell me about a situation where a project failed. What did you learn from it?",
            "How do you prioritize tasks when you have multiple deadlines competing at once?",
            "Tell me about a time you mentored a junior developer. What approach did you take?",
            "Describe a situation where you received critical feedback. How did you respond?",
            "Tell me about a time you improved a process or workflow at work.",
            "Describe a situation where you had to collaborate with a difficult stakeholder.",
            "Tell me about a time you identified and resolved a production incident.",
            "Describe a moment when you had to make a decision without full information.",
            "Tell me about a time you went above and beyond your job description.",
            "How do you stay current with new technologies and industry trends?",
            "Tell me about the most complex technical problem you have ever solved."
        ));

        QUESTION_POOL.put(Category.SITUATIONAL, Arrays.asList(
            "If you discovered a critical security vulnerability in production, what would you do first?",
            "How would you handle a situation where a key team member leaves mid-project?",
            "If the product manager changes requirements two days before launch, how do you respond?",
            "You notice a colleague consistently missing sprint goals. What steps do you take?",
            "How would you approach migrating a legacy monolith to microservices with zero downtime?",
            "If your API is suddenly getting 10x traffic, what immediate actions do you take?",
            "A client reports data loss in production. Walk me through your incident response.",
            "If two team members have a technical conflict about architecture, how do you resolve it?",
            "You are asked to deliver a feature in half the estimated time. What do you do?",
            "How would you onboard a new developer onto a complex existing codebase?",
            "If automated tests are consistently flaky, what strategy do you adopt?",
            "You are assigned a task with no documentation. How do you proceed?",
            "How would you handle a rollback after a failed deployment in production?",
            "If stakeholders request a feature that would introduce significant technical debt, how do you advise?",
            "You have to choose between two database solutions for a new service. How do you decide?"
        ));
    }

    /**
     * Generates a set of questions for a given role, difficulty, and count.
     *
     * @param role       job role (e.g., "Backend Engineer")
     * @param difficulty difficulty level string (EASY/MEDIUM/HARD)
     * @param count      number of questions to return
     * @return list of question strings
     */
    public List<String> generateForRole(String role, String difficulty, int count) {
        log.info("Generating {} questions for role='{}' difficulty='{}'", count, role, difficulty);
        Difficulty level = parseDifficulty(difficulty);
        List<String> all = new ArrayList<>();

        // Weight distribution: TECHNICAL 40%, BEHAVIORAL 30%, SITUATIONAL 30%
        all.addAll(shuffleAndPick(Category.TECHNICAL, (int) Math.ceil(count * 0.4)));
        all.addAll(shuffleAndPick(Category.BEHAVIORAL, (int) Math.ceil(count * 0.3)));
        all.addAll(shuffleAndPick(Category.SITUATIONAL, (int) Math.ceil(count * 0.3)));

        Collections.shuffle(all);
        List<String> result = all.stream().limit(count).collect(Collectors.toList());
        log.info("Returning {} questions for role '{}'", result.size(), role);
        return result;
    }

    /**
     * Randomly picks up to {@code count} questions from a category pool.
     */
    public List<String> shuffleAndPick(Category category, int count) {
        List<String> pool = new ArrayList<>(QUESTION_POOL.getOrDefault(category, Collections.emptyList()));
        Collections.shuffle(pool);
        return pool.stream().limit(count).collect(Collectors.toList());
    }

    /**
     * Builds a full structured question set with metadata.
     *
     * @param role       job role
     * @param difficulty difficulty level
     * @param count      total question count
     * @return map with role, difficulty, count, questions, generatedAt
     */
    public Map<String, Object> buildQuestionSet(String role, String difficulty, int count) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", role);
        result.put("difficulty", difficulty);
        result.put("count", count);
        result.put("questions", generateForRole(role, difficulty, count));
        result.put("generatedAt", System.currentTimeMillis());
        return result;
    }

    private Difficulty parseDifficulty(String raw) {
        try {
            return Difficulty.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            log.warn("Unknown difficulty '{}', defaulting to MEDIUM", raw);
            return Difficulty.MEDIUM;
        }
    }
}
