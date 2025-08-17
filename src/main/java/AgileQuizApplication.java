import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class AgileQuizApplication {
    private static final String QUIZ_FILE = "agile_quiz.json";
    private static final Scanner scanner = new Scanner(System.in);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode quizData;
    private List<Question> questions;
    private List<UserAnswer> userAnswers;
    private boolean showExplanations;

    public static void main(String[] args) {
        AgileQuizApplication app = new AgileQuizApplication();
        app.run();
    }

    public void run() {
        try {
            loadQuizData();
            displayWelcome();
            configureQuiz();
            takeQuiz();
            showResults();
        } catch (Exception e) {
            System.err.println("Error running quiz: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private void loadQuizData() throws IOException {
        File file = new File(QUIZ_FILE);
        if (!file.exists()) {
            throw new IOException("Quiz file not found: " + QUIZ_FILE);
        }

        quizData = objectMapper.readTree(file);
        questions = new ArrayList<>();
        userAnswers = new ArrayList<>();

        JsonNode questionsNode = quizData.get("quiz").get("questions");
        for (JsonNode questionNode : questionsNode) {
            questions.add(new Question(questionNode));
        }

        System.out.println("✓ Quiz data loaded successfully: " + questions.size() + " questions");
    }

    private void displayWelcome() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("           " + quizData.get("quiz").get("title").asText());
        System.out.println("=".repeat(60));
        System.out.println(quizData.get("quiz").get("description").asText());
        System.out.println("\nTotal Questions: " + questions.size());
        System.out.println("Passing Score: " + quizData.get("quiz").get("passingScore").asInt() + "/" + questions.size());
        System.out.println("=".repeat(60));
    }

    private void configureQuiz() {
        System.out.print("\nDo you want to see explanations after each question? (y/n): ");
        String input = scanner.nextLine().toLowerCase().trim();
        showExplanations = input.equals("y") || input.equals("yes");

        System.out.print("\nDo you want to shuffle questions? (y/n): ");
        input = scanner.nextLine().toLowerCase().trim();
        if (input.equals("y") || input.equals("yes")) {
            Collections.shuffle(questions);
            System.out.println("Questions shuffled!");
        }

        System.out.println("\nPress Enter to start the quiz...");
        scanner.nextLine();
    }

    private void takeQuiz() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                    QUIZ STARTED");
        System.out.println("=".repeat(60));

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            displayQuestion(i + 1, question);

            int userChoice = getUserChoice(question.getOptions().size());
            UserAnswer answer = new UserAnswer(question.getId(), userChoice,
                    userChoice == question.getCorrectAnswer());
            userAnswers.add(answer);

            if (showExplanations) {
                showQuestionResult(question, userChoice);
            }

            // Show progress
            if ((i + 1) % 10 == 0 || i == questions.size() - 1) {
                System.out.printf("\nProgress: %d/%d questions completed%n",
                        i + 1, questions.size());
            }

            System.out.println();
        }
    }

    private void displayQuestion(int questionNumber, Question question) {
        System.out.printf("Question %d/%d%n", questionNumber, questions.size());
        System.out.println("Section: " + question.getSection());
        System.out.println("-".repeat(50));
        System.out.println(question.getQuestion());
        System.out.println();

        List<String> options = question.getOptions();
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("%c) %s%n", 'a' + i, options.get(i));
        }
    }

    private int getUserChoice(int maxOptions) {
        while (true) {
            System.out.printf("\nYour answer (a-%c): ", 'a' + maxOptions - 1);
            String input = scanner.nextLine().toLowerCase().trim();

            if (input.length() == 1 && input.charAt(0) >= 'a' &&
                    input.charAt(0) < 'a' + maxOptions) {
                return input.charAt(0) - 'a';
            }

            System.out.println("Invalid input. Please enter a letter from 'a' to '" +
                    (char) ('a' + maxOptions - 1) + "'");
        }
    }

    private void showQuestionResult(Question question, int userChoice) {
        System.out.println("\n" + "-".repeat(30));
        if (userChoice == question.getCorrectAnswer()) {
            System.out.println("✓ CORRECT!");
        } else {
            System.out.println("✗ INCORRECT");
            System.out.println("Correct answer: " +
                    (char) ('a' + question.getCorrectAnswer()) + ") " +
                    question.getOptions().get(question.getCorrectAnswer()));
        }
        System.out.println("\nExplanation: " + question.getExplanation());
        System.out.println("-".repeat(30));
    }

    private void showResults() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                   QUIZ COMPLETED");
        System.out.println("=".repeat(60));

        int correctAnswers = (int) userAnswers.stream().mapToInt(a -> a.isCorrect() ? 1 : 0).sum();
        int totalQuestions = questions.size();
        double percentage = (double) correctAnswers / totalQuestions * 100;

        // Overall results
        System.out.printf("Score: %d/%d (%.1f%%)%n",
                correctAnswers, totalQuestions, percentage);

        // Grade determination
        JsonNode grading = quizData.get("quiz").get("grading");
        String grade = determineGrade(correctAnswers, grading);
        System.out.println("Grade: " + grade);

        // Pass/Fail
        int passingScore = quizData.get("quiz").get("passingScore").asInt();
        System.out.println("Result: " + (correctAnswers >= passingScore ? "PASSED ✓" : "FAILED ✗"));

        // Section breakdown
        showSectionBreakdown();

        // Wrong answers review
        if (!showExplanations) {
            showWrongAnswersReview();
        }

        // Study recommendations
        showStudyRecommendations();
    }

    private String determineGrade(int score, JsonNode grading) {
        if (score >= grading.get("excellent").get("min").asInt()) {
            return grading.get("excellent").get("message").asText();
        } else if (score >= grading.get("veryGood").get("min").asInt()) {
            return grading.get("veryGood").get("message").asText();
        } else if (score >= grading.get("good").get("min").asInt()) {
            return grading.get("good").get("message").asText();
        } else if (score >= grading.get("fair").get("min").asInt()) {
            return grading.get("fair").get("message").asText();
        } else {
            return grading.get("poor").get("message").asText();
        }
    }

    private void showSectionBreakdown() {
        Map<String, int[]> sectionStats = new HashMap<>(); // [correct, total]

        for (int i = 0; i < questions.size(); i++) {
            String section = questions.get(i).getSection();
            boolean correct = userAnswers.get(i).isCorrect();
            sectionStats.putIfAbsent(section, new int[2]);
            sectionStats.get(section)[0] += correct ? 1 : 0;
            sectionStats.get(section)[1]++;
        }

        System.out.println("\nSection Breakdown:");
        for (Map.Entry<String, int[]> entry : sectionStats.entrySet()) {
            int correct = entry.getValue()[0];
            int total = entry.getValue()[1];
            double pct = (double) correct / total * 100;
            System.out.printf("  %s: %d/%d (%.1f%%)%n", entry.getKey(), correct, total, pct);
        }
    }

    private void showWrongAnswersReview() {
        System.out.println("\nReview of Incorrect Answers:");
        for (int i = 0; i < questions.size(); i++) {
            if (!userAnswers.get(i).isCorrect()) {
                Question q = questions.get(i);
                System.out.printf("Q%d: %s%n", i + 1, q.getQuestion());
                System.out.printf("   Correct: %s%n", q.getOptions().get(q.getCorrectAnswer()));
                System.out.printf("   Explanation: %s%n%n", q.getExplanation());
            }
        }
    }

    private void showStudyRecommendations() {
        System.out.println("\nStudy Recommendations:");
        if (userAnswers.stream().filter(UserAnswer::isCorrect).count() < questions.size()) {
            System.out.println("- Review Agile principles and the sections where you scored less.");
            System.out.println("- Revisit Scrum roles, events, and artifacts.");
            System.out.println("- Practice PERT and project cost exercises.");
        } else {
            System.out.println("- Excellent work! Maintain your knowledge with occasional reviews.");
        }
    }

    static class Question {
        private final String id;
        private final String section;
        private final String question;
        private final List<String> options;
        private final int correctAnswer;
        private final String explanation;

        public Question(JsonNode node) {
            this.id = node.get("id").asText();
            this.section = node.get("section").asText();
            this.question = node.get("question").asText();
            this.options = new ArrayList<>();
            for (JsonNode opt : node.get("options")) {
                options.add(opt.asText());
            }
            this.correctAnswer = node.get("correctAnswer").asInt();
            this.explanation = node.get("explanation").asText();
        }

        public String getId() {
            return id;
        }

        public String getSection() {
            return section;
        }

        public String getQuestion() {
            return question;
        }

        public List<String> getOptions() {
            return options;
        }

        public int getCorrectAnswer() {
            return correctAnswer;
        }

        public String getExplanation() {
            return explanation;
        }
    }

    class UserAnswer {
        private String questionId;
        private int chosenOption;
        private boolean correct;

        public UserAnswer(String questionId, int chosenOption, boolean correct) {
            this.questionId = questionId;
            this.chosenOption = chosenOption;
            this.correct = correct;
        }

        public boolean isCorrect() {
            return correct;
        }
    }
}