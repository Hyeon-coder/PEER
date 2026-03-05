package com.peer.algobank.service;

import com.peer.TestUtil;
import com.peer.algobank.dto.EvaluationRequest;
import com.peer.algobank.dto.EvaluationResponse;
import com.peer.algobank.dto.SolutionRequest;
import com.peer.algobank.dto.SolutionResponse;
import com.peer.algobank.entity.Difficulty;
import com.peer.algobank.entity.Problem;
import com.peer.algobank.repository.ProblemRepository;
import com.peer.common.exception.CustomException;
import com.peer.common.exception.ErrorCode;
import com.peer.config.TestConfig;
import com.peer.user.entity.User;
import com.peer.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class EvaluationServiceTest {

    @Autowired private EvaluationService evaluationService;
    @Autowired private SolutionService solutionService;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private UserRepository userRepository;

    private User author;
    private User evaluator;
    private User thirdUser;
    private Long solutionId;

    @BeforeEach
    void setUp() {
        author = userRepository.save(User.builder()
                .email("author@hive.fi").name("Author").googleId("g-author").build());
        evaluator = userRepository.save(User.builder()
                .email("eval@hive.fi").name("Evaluator").googleId("g-eval").build());
        thirdUser = userRepository.save(User.builder()
                .email("third@hive.fi").name("Third").googleId("g-third").build());

        Problem problem = problemRepository.save(Problem.builder()
                .author(author).title("Problem").description("Desc")
                .difficulty(Difficulty.MEDIUM).build());

        SolutionRequest solReq = TestUtil.createDto(SolutionRequest.class,
                "code", "solution code", "language", "java");
        SolutionResponse sol = solutionService.createSolution(problem.getId(), solReq, author.getId());
        solutionId = sol.getId();
    }

    private EvaluationRequest evalRequest(int c, int r, int cl, int cs) {
        return TestUtil.createDto(EvaluationRequest.class,
                "correctness", c, "codeReadability", r,
                "commentsClarity", cl, "conditionSatisfaction", cs);
    }

    @Test
    @DisplayName("Evaluation awards +2 XP to evaluator and avg score XP to author")
    void createEvaluation_awardsXp() {
        long evalXpBefore = evaluator.getTotalXp();
        long authorXpBefore = author.getTotalXp();

        EvaluationResponse result = evaluationService.createEvaluation(
                solutionId, evalRequest(4, 5, 3, 4), evaluator.getId());

        // Evaluator gets +2 XP
        assertThat(evaluator.getTotalXp()).isEqualTo(evalXpBefore + 2);
        // Author gets +avg XP (avg of 4,5,3,4 = 4)
        assertThat(author.getTotalXp()).isEqualTo(authorXpBefore + result.getAverageScore());
    }

    @Test
    @DisplayName("Self-evaluation throws SELF_EVALUATION")
    void createEvaluation_selfEval() {
        assertThatThrownBy(() -> evaluationService.createEvaluation(
                solutionId, evalRequest(3, 3, 3, 3), author.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.SELF_EVALUATION));
    }

    @Test
    @DisplayName("Duplicate evaluation throws DUPLICATE_EVALUATION")
    void createEvaluation_duplicate() {
        evaluationService.createEvaluation(solutionId, evalRequest(3, 3, 3, 3), evaluator.getId());

        assertThatThrownBy(() -> evaluationService.createEvaluation(
                solutionId, evalRequest(5, 5, 5, 5), evaluator.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EVALUATION));
    }

    @Test
    @DisplayName("Multiple evaluators can evaluate the same solution")
    void createEvaluation_multipleEvaluators() {
        evaluationService.createEvaluation(solutionId, evalRequest(3, 3, 3, 3), evaluator.getId());
        evaluationService.createEvaluation(solutionId, evalRequest(5, 5, 5, 5), thirdUser.getId());

        List<EvaluationResponse> evals = evaluationService.getEvaluations(solutionId);
        assertThat(evals).hasSize(2);
    }

    @Test
    @DisplayName("XP accumulates and level increases correctly")
    void xpAndLevelProgression() {
        // Author starts with 1 XP (from solution submission)
        // Give author lots of evaluations to test leveling
        // Level 1 requires 10 XP, so after receiving a few evals author should level up
        assertThat(author.getLevel()).isEqualTo(0);

        // 9 more XP needed for level 1 (author already has 1 from solution)
        // Each max eval gives 5 XP to author
        evaluationService.createEvaluation(solutionId, evalRequest(5, 5, 5, 5), evaluator.getId());
        // author now has 1 + 5 = 6 XP, still level 0
        assertThat(author.getTotalXp()).isEqualTo(6);
        assertThat(author.getLevel()).isEqualTo(0);

        evaluationService.createEvaluation(solutionId, evalRequest(5, 5, 5, 5), thirdUser.getId());
        // author now has 6 + 5 = 11 XP, level 1 (requires 10)
        assertThat(author.getTotalXp()).isEqualTo(11);
        assertThat(author.getLevel()).isEqualTo(1);
    }
}
