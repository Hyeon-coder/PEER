package com.peer.algobank.service;

import com.peer.TestUtil;
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
class SolutionServiceTest {

    @Autowired private SolutionService solutionService;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private UserRepository userRepository;

    private User user;
    private User otherUser;
    private Problem problem;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("test@hive.fi").name("Test User").googleId("google-1").build());
        otherUser = userRepository.save(User.builder()
                .email("other@hive.fi").name("Other User").googleId("google-2").build());
        problem = problemRepository.save(Problem.builder()
                .author(user).title("Two Sum").description("Find two numbers")
                .difficulty(Difficulty.EASY).build());
    }

    private SolutionRequest solutionRequest(String code, String language) {
        return TestUtil.createDto(SolutionRequest.class,
                "code", code, "language", language);
    }

    @Test
    @DisplayName("Solution submission awards +1 XP to user")
    void createSolution_awardsXp() {
        long xpBefore = user.getTotalXp();
        solutionService.createSolution(problem.getId(), solutionRequest("int[] twoSum() {}", "java"), user.getId());
        assertThat(user.getTotalXp()).isEqualTo(xpBefore + 1);
    }

    @Test
    @DisplayName("Duplicate solution throws DUPLICATE_SOLUTION")
    void createSolution_duplicate() {
        SolutionRequest req = solutionRequest("code", "java");
        solutionService.createSolution(problem.getId(), req, user.getId());

        assertThatThrownBy(() -> solutionService.createSolution(problem.getId(), req, user.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_SOLUTION));
    }

    @Test
    @DisplayName("Different users can submit solutions to the same problem")
    void createSolution_differentUsers() {
        solutionService.createSolution(problem.getId(), solutionRequest("sol1", "java"), user.getId());
        solutionService.createSolution(problem.getId(), solutionRequest("sol2", "python"), otherUser.getId());

        List<SolutionResponse> solutions = solutionService.getSolutions(problem.getId());
        assertThat(solutions).hasSize(2);
    }

    @Test
    @DisplayName("Only author can update their solution")
    void updateSolution_notAuthor_throws() {
        SolutionResponse created = solutionService.createSolution(
                problem.getId(), solutionRequest("original", "java"), user.getId());

        assertThatThrownBy(() -> solutionService.updateSolution(
                created.getId(), solutionRequest("updated", "python"), otherUser.getId()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Delete solution removes it")
    void deleteSolution() {
        SolutionResponse created = solutionService.createSolution(
                problem.getId(), solutionRequest("code", "java"), user.getId());

        solutionService.deleteSolution(created.getId(), user.getId());

        assertThat(solutionService.getSolutions(problem.getId())).isEmpty();
    }
}
