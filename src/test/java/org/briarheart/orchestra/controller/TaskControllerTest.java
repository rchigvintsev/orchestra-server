package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.*;
import org.briarheart.orchestra.service.TaskService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(TaskController.class)
@Import(PermitAllSecurityConfig.class)
class TaskControllerTest {
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    @Autowired
    private WebTestClient testClient;

    @MockBean
    private TaskService taskService;

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterAll
    static void afterAll() {
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test
    void shouldReturnAllUnprocessedTasks() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder().id(2L).title("Test task").userId(user.getId()).build();
        when(taskService.getUnprocessedTasks(eq(user), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/unprocessed").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[]{task});
    }

    @Test
    void shouldReturnNumberOfAllUnprocessedTasks() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        when(taskService.getUnprocessedTaskCount(user)).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/unprocessed/count").exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnAllProcessedTasks() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder()
                .id(2L)
                .title("Test task")
                .userId(user.getId())
                .status(TaskStatus.PROCESSED)
                .build();
        when(taskService.getProcessedTasks(eq(user), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/processed").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[]{task});
    }

    @Test
    void shouldReturnNumberOfAllProcessedTasks() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        when(taskService.getProcessedTaskCount(user)).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/processed/count").exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnProcessedTasksWithoutDeadline() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder()
                .id(2L)
                .title("Test task")
                .userId(user.getId())
                .status(TaskStatus.PROCESSED)
                .build();
        when(taskService.getProcessedTasks(null, null, user, PageRequest.of(0, 20)))
                .thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/processed?deadlineFrom=&deadlineTo=").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[]{task});
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithoutDeadline() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        when(taskService.getProcessedTaskCount(null, null, user)).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/processed/count?deadlineFrom=&deadlineTo=").exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadline() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder()
                .id(2L)
                .title("Test task")
                .userId(user.getId())
                .status(TaskStatus.PROCESSED)
                .deadline(LocalDateTime.parse("2020-01-10T00:00:00", DateTimeFormatter.ISO_DATE_TIME))
                .build();

        String deadlineFrom = "2020-01-01T00:00";
        String deadlineTo = "2020-01-31T23:59";

        when(taskService.getProcessedTasks(
                LocalDateTime.parse(deadlineFrom, DateTimeFormatter.ISO_DATE_TIME),
                LocalDateTime.parse(deadlineTo, DateTimeFormatter.ISO_DATE_TIME),
                user,
                PageRequest.of(0, 20)
        )).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/processed?deadlineFrom=" + deadlineFrom + "&deadlineTo=" + deadlineTo)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[]{task});
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadline() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        String deadlineFrom = "2020-01-01T00:00";
        String deadlineTo = "2020-01-31T23:59";

        when(taskService.getProcessedTaskCount(
                LocalDateTime.parse(deadlineFrom, DateTimeFormatter.ISO_DATE_TIME),
                LocalDateTime.parse(deadlineTo, DateTimeFormatter.ISO_DATE_TIME),
                user
        )).thenReturn(Mono.just(1L));

        String uri = "/tasks/processed/count"
                + "?deadlineFrom=" + deadlineFrom + "&deadlineTo=" + deadlineTo;
        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri(uri)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder().id(2L).title("Test task").userId(user.getId()).build();
        when(taskService.getUncompletedTasks(eq(user), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/uncompleted").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[]{task});
    }

    @Test
    void shouldReturnNumberOfAllUncompletedTasks() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        when(taskService.getUncompletedTaskCount(eq(user))).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/uncompleted/count").exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnTaskById() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder().id(2L).title("Test task").userId(user.getId()).build();
        when(taskService.getTask(task.getId(), user)).thenReturn(Mono.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/" + task.getId()).exchange()
                .expectStatus().isOk()
                .expectBody(Task.class).isEqualTo(task);
    }

    @Test
    void shouldReturnNotFoundStatusCodeWhenTaskIsNotFoundById() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        String errorMessage = "Task is not found";

        when(taskService.getTask(anyLong(), any(User.class)))
                .thenReturn(Mono.error(new EntityNotFoundException(errorMessage)));

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrors(List.of(errorMessage));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/1").exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class).isEqualTo(errorResponse);
    }

    @Test
    void shouldCreateTask() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder().title("New task").build();

        long taskId = 2L;

        Task responseBody = task.copy();
        responseBody.setId(taskId);
        responseBody.setUserId(user.getId());

        when(taskService.createTask(task)).thenAnswer(args -> {
            Task t = args.getArgument(0);
            t.setId(taskId);
            return Mono.just(t);
        });

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf()).post()
                .uri("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(task)
                .exchange()

                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/tasks/" + taskId)
                .expectBody(Task.class).isEqualTo(responseBody);
    }

    @Test
    void shouldRejectTaskCreationWhenTitleIsNull() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder().build();

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf()).post()
                .uri("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(task)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors.title").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectTaskCreationWhenTitleIsBlank() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder().title(" ").build();

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf()).post()
                .uri("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(task)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors.title").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectTaskCreationWhenTitleIsTooLong() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder().title("L" + "o".repeat(247) + "ng title").build();

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf()).post()
                .uri("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(task)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors.title").isEqualTo("Value length must not be greater than 255");
    }

    @Test
    void shouldRejectTaskCreationWhenDescriptionIsTooLong() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder()
                .title("Test title")
                .description("L" + "o".repeat(9986) + "ng description")
                .build();

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf()).post()
                .uri("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(task)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors.description").isEqualTo("Value length must not be greater than 10000");
    }

    @Test
    void shouldRejectTaskCreationWhenDeadlineIsInPast() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder()
                .title("Test title")
                .deadline(LocalDateTime.now().minus(3, ChronoUnit.DAYS))
                .build();

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf()).post()
                .uri("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(task)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors.deadline").isEqualTo("Value must not be in past");
    }

    @Test
    void shouldUpdateTask() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        when(taskService.updateTask(any(Task.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();

        Task updatedTask = task.copy();
        updatedTask.setUserId(null);
        updatedTask.setTitle("Updated test task");

        Task responseBody = updatedTask.copy();
        responseBody.setId(task.getId());
        responseBody.setUserId(user.getId());

        testClient.mutateWith(csrf())
                .mutateWith(mockAuthentication(authenticationMock))
                .put()
                .uri("/tasks/" + task.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(responseBody)
                .exchange()

                .expectStatus().isOk()
                .expectBody(Task.class).isEqualTo(responseBody);
    }

    @Test
    void shouldCompleteTask() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;

        when(taskService.completeTask(taskId, user)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(csrf())
                .mutateWith(mockAuthentication(authenticationMock))
                .put().uri("/tasks/completed/" + taskId)
                .exchange()

                .expectStatus().isNoContent();
    }

    @Test
    void shouldDeleteTask() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;

        when(taskService.deleteTask(taskId, user)).thenReturn(Mono.empty());

        testClient.mutateWith(csrf())
                .mutateWith(mockAuthentication(authenticationMock))
                .delete().uri("/tasks/" + taskId)
                .exchange()

                .expectStatus().isNoContent();
    }

    @Test
    void shouldReturnTagsForTask() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;

        Tag tag = Tag.builder().id(2L).userId(user.getId()).name("Test tag").build();
        Mockito.when(taskService.getTags(taskId, user)).thenReturn(Flux.just(tag));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/" + taskId + "/tags").exchange()
                .expectStatus().isOk()
                .expectBody(Tag[].class).isEqualTo(new Tag[]{tag});
    }

    @Test
    void shouldAssignTagToTask() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;
        long tagId = 3L;

        when(taskService.assignTag(taskId, tagId, user)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf()).put()
                .uri("/tasks/" + taskId + "/tags/" + tagId)
                .exchange()

                .expectStatus().isNoContent();
    }

    @Test
    void shouldRemoveTagFromTask() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;
        long tagId = 3L;

        when(taskService.removeTag(taskId, tagId, user)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf()).delete()
                .uri("/tasks/" + taskId + "/tags/" + tagId)
                .exchange()

                .expectStatus().isNoContent();
    }

    @Test
    void shouldReturnCommentsForTask() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        TaskComment comment = TaskComment.builder()
                .userId(user.getId())
                .id(2L)
                .taskId(3L)
                .commentText("Test comment")
                .build();
        Mockito.when(taskService.getComments(
                comment.getTaskId(),
                user,
                PageRequest.of(0, 20)
        )).thenReturn(Flux.just(comment));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/" + comment.getTaskId() + "/comments").exchange()
                .expectStatus().isOk()
                .expectBody(TaskComment[].class).isEqualTo(new TaskComment[]{comment});
    }

    @Test
    void shouldAddCommentToTask() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;
        long taskCommentId = 3L;

        TaskComment comment = TaskComment.builder().commentText("Test comment").build();

        when(taskService.addComment(any(TaskComment.class))).thenAnswer(args -> {
            TaskComment c = args.getArgument(0);
            c.setId(taskCommentId);
            return Mono.just(c);
        });

        TaskComment responseBody = comment.copy();
        responseBody.setId(taskCommentId);
        responseBody.setTaskId(taskId);
        responseBody.setUserId(user.getId());

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf())
                .post().uri("/tasks/" + taskId + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment)
                .exchange()

                .expectStatus().isCreated()
                .expectBody(TaskComment.class).value(c -> {
                    assertEquals(responseBody.getId(), c.getId());
                    assertEquals(responseBody.getUserId(), c.getUserId());
                    assertEquals(responseBody.getTaskId(), c.getTaskId());
                    assertEquals(responseBody.getCommentText(), c.getCommentText());
                    assertNotNull(c.getCreatedAt());
                });
    }

    @Test
    void shouldRejectCommentAddingWhenCommentTextIsNull() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        TaskComment comment = TaskComment.builder().build();

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf())
                .post().uri("/tasks/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors.commentText").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectCommentAddingWhenCommentTextIsBlank() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        TaskComment comment = TaskComment.builder().commentText(" ").build();

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf())
                .post().uri("/tasks/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors.commentText").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectCommentAddingWhenCommentTextIsTooLong() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        TaskComment comment = TaskComment.builder().commentText("L" + "o".repeat(9993) + "ng text").build();

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf())
                .post().uri("/tasks/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors.commentText").isEqualTo("Value length must not be greater than 10000");
    }

    private Authentication createAuthentication(User user) {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(user.getEmail());
        when(authenticationMock.getPrincipal()).thenReturn(user);
        return authenticationMock;
    }
}
