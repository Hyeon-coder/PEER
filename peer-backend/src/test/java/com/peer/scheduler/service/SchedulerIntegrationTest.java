package com.peer.scheduler.service;

import com.peer.TestUtil;
import com.peer.common.exception.CustomException;
import com.peer.common.exception.ErrorCode;
import com.peer.config.TestConfig;
import com.peer.scheduler.dto.EventRequest;
import com.peer.scheduler.dto.EventResponse;
import com.peer.scheduler.dto.TodoRequest;
import com.peer.scheduler.dto.TodoResponse;
import com.peer.scheduler.entity.RepeatRule;
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

import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class SchedulerIntegrationTest {

    @Autowired private EventService eventService;
    @Autowired private TodoService todoService;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("user@hive.fi").name("User").googleId("g-user").build());
        otherUser = userRepository.save(User.builder()
                .email("other@hive.fi").name("Other").googleId("g-other").build());
    }

    // --- Event Tests ---

    @Test
    @DisplayName("Create and retrieve event within date range")
    void createAndGetEvent() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 5, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 5, 10, 0);

        EventRequest req = TestUtil.createDto(EventRequest.class,
                "title", "Meeting", "startTime", start, "endTime", end,
                "repeatRule", RepeatRule.NONE);
        eventService.createEvent(req, user.getId());

        List<EventResponse> events = eventService.getEvents(user.getId(),
                LocalDateTime.of(2026, 3, 1, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59));
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getTitle()).isEqualTo("Meeting");
    }

    @Test
    @DisplayName("End time before start time throws INVALID_EVENT_TIME")
    void createEvent_invalidTime() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 5, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 5, 9, 0);

        EventRequest req = TestUtil.createDto(EventRequest.class,
                "title", "Bad Event", "startTime", start, "endTime", end,
                "repeatRule", RepeatRule.NONE);

        assertThatThrownBy(() -> eventService.createEvent(req, user.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_EVENT_TIME));
    }

    @Test
    @DisplayName("User cannot access another user's event")
    void getEvent_otherUser() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 5, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 5, 10, 0);

        EventRequest req = TestUtil.createDto(EventRequest.class,
                "title", "Private", "startTime", start, "endTime", end,
                "repeatRule", RepeatRule.NONE);
        EventResponse created = eventService.createEvent(req, user.getId());

        assertThatThrownBy(() -> eventService.getEvent(created.getId(), otherUser.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND));
    }

    // --- Todo Tests ---

    @Test
    @DisplayName("Create todo and toggle completion")
    void createAndToggleTodo() {
        TodoRequest req = TestUtil.createDto(TodoRequest.class, "title", "Study algorithms");
        TodoResponse todo = todoService.createTodo(req, user.getId());
        assertThat(todo.isCompleted()).isFalse();

        TodoResponse toggled = todoService.toggleTodo(todo.getId(), user.getId());
        assertThat(toggled.isCompleted()).isTrue();

        TodoResponse toggledBack = todoService.toggleTodo(todo.getId(), user.getId());
        assertThat(toggledBack.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("Create subtask under parent todo")
    void createSubtask() {
        TodoRequest parentReq = TestUtil.createDto(TodoRequest.class, "title", "Parent Task");
        TodoResponse parent = todoService.createTodo(parentReq, user.getId());

        TodoRequest childReq = TestUtil.createDto(TodoRequest.class,
                "title", "Sub Task", "parentId", parent.getId());
        todoService.createTodo(childReq, user.getId());

        em.flush();
        em.clear();

        List<TodoResponse> todos = todoService.getTodos(user.getId());
        assertThat(todos).hasSize(1); // only parent at top level
        assertThat(todos.get(0).getSubtasks()).hasSize(1);
    }

    @Test
    @DisplayName("User cannot access another user's todo")
    void toggleTodo_otherUser() {
        TodoRequest req = TestUtil.createDto(TodoRequest.class, "title", "My Todo");
        TodoResponse todo = todoService.createTodo(req, user.getId());

        assertThatThrownBy(() -> todoService.toggleTodo(todo.getId(), otherUser.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.TODO_NOT_FOUND));
    }
}
