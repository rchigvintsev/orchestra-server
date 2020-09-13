package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskStatus;
import org.briarheart.orchestra.service.TaskService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

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
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder().id(1L).title("Test task").author(authenticationMock.getName()).build();
        when(taskService.getUnprocessedTasks(eq(authenticationMock.getName()), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/unprocessed").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldReturnNumberOfAllUnprocessedTasks() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");
        when(taskService.getUnprocessedTaskCount(eq(authenticationMock.getName()))).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/unprocessed/count").exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnAllProcessedTasks() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder()
                .id(1L)
                .title("Test task")
                .author(authenticationMock.getName())
                .status(TaskStatus.PROCESSED)
                .build();
        when(taskService.getProcessedTasks(eq(authenticationMock.getName()), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/processed").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldReturnNumberOfAllProcessedTasks() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");
        when(taskService.getProcessedTaskCount(eq(authenticationMock.getName()))).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/processed/count").exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnProcessedTasksWithoutDeadline() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder()
                .id(1L)
                .title("Test task")
                .author(authenticationMock.getName())
                .status(TaskStatus.PROCESSED)
                .build();
        when(taskService.getProcessedTasks(null, null, authenticationMock.getName(), PageRequest.of(0, 20)))
                .thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/processed?deadlineDateFrom=&deadlineDateTo=").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithoutDeadline() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");
        when(taskService.getProcessedTaskCount(null, null, authenticationMock.getName())).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/processed/count?deadlineDateFrom=&deadlineDateTo=").exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadline() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder()
                .id(1L)
                .title("Test task")
                .author(authenticationMock.getName())
                .status(TaskStatus.PROCESSED)
                .deadlineDate(LocalDate.parse("2020-01-10", DateTimeFormatter.ISO_DATE))
                .build();

        String deadlineDateFrom = "2020-01-01";
        String deadlineDateTo = "2020-01-31";

        when(taskService.getProcessedTasks(
                LocalDate.parse(deadlineDateFrom, DateTimeFormatter.ISO_DATE),
                LocalDate.parse(deadlineDateTo, DateTimeFormatter.ISO_DATE),
                authenticationMock.getName(),
                PageRequest.of(0, 20)
        )).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/processed?deadlineDateFrom=" + deadlineDateFrom  + "&deadlineDateTo=" + deadlineDateTo)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadline() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        String deadlineDateFrom = "2020-01-01";
        String deadlineDateTo = "2020-01-31";

        when(taskService.getProcessedTaskCount(
                LocalDate.parse(deadlineDateFrom, DateTimeFormatter.ISO_DATE),
                LocalDate.parse(deadlineDateTo, DateTimeFormatter.ISO_DATE),
                authenticationMock.getName()
        )).thenReturn(Mono.just(1L));

        String uri = "/tasks/processed/count"
                + "?deadlineDateFrom=" + deadlineDateFrom + "&deadlineDateTo=" + deadlineDateTo;
        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri(uri)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder().id(1L).title("Test task").author(authenticationMock.getName()).build();
        when(taskService.getUncompletedTasks(eq(authenticationMock.getName()), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/uncompleted").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldReturnNumberOfAllUncompletedTasks() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");
        when(taskService.getUncompletedTaskCount(eq(authenticationMock.getName()))).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/uncompleted/count").exchange()
                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnTaskById() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder().id(1L).title("Test task").author(authenticationMock.getName()).build();
        when(taskService.getTask(task.getId(), authenticationMock.getName())).thenReturn(Mono.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/1").exchange()
                .expectStatus().isOk()
                .expectBody(Task.class).isEqualTo(task);
    }

    @Test
    void shouldReturnNotFoundStatusCodeWhenTaskIsNotFoundById() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        String errorMessage = "Task is not found";

        when(taskService.getTask(anyLong(), anyString()))
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
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder().title("New task").build();
        Task savedTask = Task.builder().id(2L).title(task.getTitle()).author(authenticationMock.getName()).build();

        when(taskService.createTask(task, authenticationMock.getName())).thenReturn(Mono.just(savedTask));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf()).post()
                .uri("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(task)
                .exchange()

                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/tasks/" + savedTask.getId())
                .expectBody(Task.class).isEqualTo(savedTask);
    }

    @Test
    void shouldRejectTaskCreationWhenTitleIsNull() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

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
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder().title("").build();

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
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

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
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

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
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder()
                .title("Test title")
                .deadlineDate(LocalDate.now().minus(3, ChronoUnit.DAYS))
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
                .jsonPath("$.fieldErrors.deadlineDate").isEqualTo("Value must not be in past");
    }

    @Test
    void shouldUpdateTask() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder().id(2L).title("Test task").author(authenticationMock.getName()).build();
        Task updatedTask = Task.builder().title("Updated test task").build();
        when(taskService.getTask(task.getId(), authenticationMock.getName())).thenReturn(Mono.just(task));
        when(taskService.updateTask(updatedTask, task.getId(), authenticationMock.getName()))
                .thenReturn(Mono.just(updatedTask));

        testClient.mutateWith(csrf())
                .mutateWith(mockAuthentication(authenticationMock))
                .put()
                .uri("/tasks/2")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedTask)
                .exchange()

                .expectStatus().isOk()
                .expectBody(Task.class).isEqualTo(updatedTask);
    }

    @Test
    void shouldCompleteTask() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder().id(1L).title("Test task").author(authenticationMock.getName()).build();

        when(taskService.getTask(task.getId(), authenticationMock.getName())).thenReturn(Mono.just(task));
        when(taskService.completeTask(task.getId(), authenticationMock.getName())).thenReturn(Mono.empty());

        testClient.mutateWith(csrf())
                .mutateWith(mockAuthentication(authenticationMock))
                .post()
                .uri("/tasks/1/complete")
                .exchange()

                .expectStatus().isNoContent();
    }
}
