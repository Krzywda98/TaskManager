package com.example.taskmanager;

import com.example.taskmanager.models.Task;
import com.example.taskmanager.models.User;
import com.example.taskmanager.models.enums.Role;
import com.example.taskmanager.models.enums.TaskPriority;
import com.example.taskmanager.models.enums.TaskStatus;
import com.example.taskmanager.repositories.TaskRepository;
import com.example.taskmanager.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTests {
    private static final String PASSWORD = "Secret1!";
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TaskRepository tasks;
    @Autowired PasswordEncoder encoder;
    private User alice;
    private User bob;
    private User admin;
    private Task aliceTask;
    private Task bobTask;
    private Task legacyTask;

    @BeforeEach
    void setUp() {
        tasks.deleteAll();
        users.deleteAll();
        alice = createUser("alice@example.com", Role.USER);
        bob = createUser("bob@example.com", Role.USER);
        admin = createUser("admin@example.com", Role.ADMIN);
        aliceTask = createTask("Alice task", alice);
        bobTask = createTask("Bob task", bob);
        legacyTask = createTask("Legacy task", null);
    }

    @Test
    void onlyRegistrationIsPublic() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
        mvc.perform(get("/users/{id}", alice.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/users/email").param("email", alice.getEmail())).andExpect(status().isUnauthorized());
        mvc.perform(put("/users/{id}", alice.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(profile(alice.getEmail(), null))).andExpect(status().isUnauthorized());
        mvc.perform(delete("/users/{id}", alice.getId())).andExpect(status().isUnauthorized());
        mvc.perform(put("/users/{id}/role", alice.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/tasks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/tasks/{id}", aliceTask.getId())).andExpect(status().isUnauthorized());
        mvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content(taskBody("New", null)))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/tasks/{id}", aliceTask.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(taskBody("New", null))).andExpect(status().isUnauthorized());
        mvc.perform(delete("/tasks/{id}", aliceTask.getId())).andExpect(status().isUnauthorized());
        assertThat(users.existsById(alice.getId())).isTrue();
        assertThat(tasks.existsById(aliceTask.getId())).isTrue();
    }

    @Test
    void registrationHashesPasswordAndReturnsOnlyPublicFields() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content(profile("new@example.com", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.joinDate").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.authorities").doesNotExist());
        User registered = users.findByEmail("new@example.com").orElseThrow();
        assertThat(encoder.matches(PASSWORD, registered.getPassword())).isTrue();
        assertThat(registered.getJoinDate()).isNotNull();
        mvc.perform(get("/users/{id}", registered.getId()).with(httpBasic(registered.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void validationCollectsMultipleErrorsAndAllowsTwelveCharacterNames() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"surname\":\"Nowak\",\"email\":\"bad\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.errors.password", hasSize(greaterThanOrEqualTo(2))));
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content(profile("long@example.com", PASSWORD).replace("Alicja", "Abcdefghijkl")))
                .andExpect(status().isCreated());
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content(profile(alice.getEmail(), PASSWORD)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Email already exists"));
        mvc.perform(put("/users/{id}", alice.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(profile(bob.getEmail(), null)))
                .andExpect(status().isConflict());
        assertThat(users.findById(alice.getId()).orElseThrow().getEmail()).isEqualTo(alice.getEmail());
    }

    @Test
    void userCanReadAndUpdateOwnAccountButCannotAccessOthers() throws Exception {
        mvc.perform(get("/users/{id}", alice.getId()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/users/email").param("email", alice.getEmail()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(get("/users").with(httpBasic(alice.getEmail(), PASSWORD))).andExpect(status().isForbidden());
        mvc.perform(get("/users/{id}", bob.getId()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/users/email").param("email", bob.getEmail()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/users/{id}", bob.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(profile(bob.getEmail(), "Changed2!")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/users/{id}", bob.getId()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isForbidden());
        assertThat(encoder.matches(PASSWORD, users.findById(bob.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void passwordUpdateUsesUrlIdAndNewHashAndPreservesJoinDate() throws Exception {
        var originalDate = users.findById(alice.getId()).orElseThrow().getJoinDate();
        mvc.perform(put("/users/{id}", alice.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(profile(alice.getEmail(), "Changed2!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(alice.getId()))
                .andExpect(jsonPath("$.password").doesNotExist());
        User updated = users.findById(alice.getId()).orElseThrow();
        assertThat(encoder.matches("Changed2!", updated.getPassword())).isTrue();
        assertThat(updated.getJoinDate()).isEqualTo(originalDate);
        assertThat(users.count()).isEqualTo(3);
        mvc.perform(get("/users/{id}", alice.getId()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/users/{id}", alice.getId()).with(httpBasic(alice.getEmail(), "Changed2!")))
                .andExpect(status().isOk());
    }

    @Test
    void omittedPasswordIsPreservedButInvalidPasswordIsRejected() throws Exception {
        String hash = users.findById(alice.getId()).orElseThrow().getPassword();
        mvc.perform(put("/users/{id}", alice.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(profile(alice.getEmail(), null)))
                .andExpect(status().isOk());
        assertThat(users.findById(alice.getId()).orElseThrow().getPassword()).isEqualTo(hash);
        mvc.perform(put("/users/{id}", alice.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(profile(alice.getEmail(), "")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.password").isArray());
    }

    @Test
    void clientCannotInjectIdOrRole() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content(profile("injected@example.com", PASSWORD).replace("}", ",\"role\":\"ADMIN\"}")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/users/{id}", alice.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(profile(alice.getEmail(), null).replace("}", ",\"id\":" + bob.getId() + "}")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/users/{id}", alice.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(profile(alice.getEmail(), null).replace("}", ",\"role\":\"ADMIN\"}")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/users/{id}/role", alice.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        assertThat(users.findById(alice.getId()).orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void adminCanManageAllUsersAndRoles() throws Exception {
        mvc.perform(get("/users").with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].password").isEmpty());
        mvc.perform(get("/users/{id}", bob.getId()).with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(get("/users/email").param("email", bob.getEmail()).with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(put("/users/{id}", bob.getId()).with(httpBasic(admin.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(profile("updated@example.com", "Changed2!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("updated@example.com"));
        mvc.perform(put("/users/{id}/role", bob.getId()).with(httpBasic(admin.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(get("/users").with(httpBasic("updated@example.com", "Changed2!")))
                .andExpect(status().isOk());
        mvc.perform(put("/users/{id}/role", bob.getId()).with(httpBasic(admin.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/users").with(httpBasic("updated@example.com", "Changed2!")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/users/{id}", bob.getId()).with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isNoContent());
        assertThat(users.existsById(bob.getId())).isFalse();
        assertThat(tasks.existsById(bobTask.getId())).isFalse();
        assertThat(tasks.existsById(aliceTask.getId())).isTrue();
    }

    @Test
    void deletingOwnAccountDeletesOnlyItsTasks() throws Exception {
        mvc.perform(delete("/users/{id}", alice.getId()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isNoContent());
        assertThat(users.existsById(alice.getId())).isFalse();
        assertThat(tasks.existsById(aliceTask.getId())).isFalse();
        assertThat(tasks.existsById(bobTask.getId())).isTrue();
        assertThat(tasks.existsById(legacyTask.getId())).isTrue();
    }

    @Test
    void taskListsAndDetailsRespectOwnership() throws Exception {
        mvc.perform(get("/tasks").with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(aliceTask.getId()))
                .andExpect(jsonPath("$.content[0].ownerId").value(alice.getId()))
                .andExpect(jsonPath("$.content[0].owner").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        mvc.perform(get("/tasks/{id}", aliceTask.getId()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(aliceTask.getId()));
        mvc.perform(get("/tasks/{id}", bobTask.getId()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/tasks/{id}", legacyTask.getId()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanCreateUpdateAndDeleteOwnTask() throws Exception {
        mvc.perform(post("/tasks").with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(taskBody("Created task", null)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId").value(alice.getId()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
        mvc.perform(put("/tasks/{id}", aliceTask.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(taskBody("Updated task", null)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated task"))
                .andExpect(jsonPath("$.ownerId").value(alice.getId()));
        mvc.perform(delete("/tasks/{id}", aliceTask.getId()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isNoContent());
        assertThat(tasks.existsById(aliceTask.getId())).isFalse();
    }

    @Test
    void userCannotModifyOthersTasksOrAssignTasksToOthers() throws Exception {
        mvc.perform(put("/tasks/{id}", bobTask.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(taskBody("Stolen", alice.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/tasks/{id}", bobTask.getId()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/tasks").with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(taskBody("Assigned", bob.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(put("/tasks/{id}", aliceTask.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(taskBody("Transferred", bob.getId())))
                .andExpect(status().isForbidden());
        assertThat(tasks.findById(bobTask.getId()).orElseThrow().getTitle()).isEqualTo("Bob task");
        assertThat(tasks.count()).isEqualTo(3);
    }

    @Test
    void adminCanManageAllTasksIncludingUnassignedOnes() throws Exception {
        mvc.perform(get("/tasks").with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/tasks/{id}", bobTask.getId()).with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(get("/tasks/{id}", legacyTask.getId()).with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(put("/tasks/{id}", bobTask.getId()).with(httpBasic(admin.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(taskBody("Admin updated", null)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(bob.getId()));
        mvc.perform(post("/tasks").with(httpBasic(admin.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(taskBody("Admin created", alice.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId").value(alice.getId()));
        mvc.perform(put("/tasks/{id}", legacyTask.getId()).with(httpBasic(admin.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(taskBody("Assigned legacy", alice.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(alice.getId()));
        mvc.perform(get("/tasks/{id}", legacyTask.getId()).with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(delete("/tasks/{id}", bobTask.getId()).with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isNoContent());
    }

    @Test
    void taskValidationAndDatabaseLengthAgree() throws Exception {
        mvc.perform(post("/tasks").with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskBody("Long description", null).replace("Description", "x".repeat(500))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.description").value("x".repeat(500)));
        mvc.perform(post("/tasks").with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskBody("Too long", null).replace("Description", "x".repeat(501))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.description").isArray());
        mvc.perform(post("/tasks").with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.title").isArray())
                .andExpect(jsonPath("$.errors.status").isArray());
    }

    @Test
    void taskRequestRejectsServerManagedFieldsAndReportsUniqueTitleConflicts() throws Exception {
        mvc.perform(post("/tasks").with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskBody("Injected", null).replace("}", ",\"id\":" + bobTask.getId() + "}")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/tasks").with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(taskBody(aliceTask.getTitle(), null)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void missingResourcesReturn404() throws Exception {
        mvc.perform(get("/tasks/999999").with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Task not found"));
        mvc.perform(delete("/tasks/999999").with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/tasks/999999").with(httpBasic(admin.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(taskBody("Missing", null)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/users/999999").with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("User not found"));
        mvc.perform(get("/users/email").param("email", "missing@example.com").with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/users/999999").with(httpBasic(admin.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(profile("missing@example.com", null)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/users/999999").with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/tasks").with(httpBasic(admin.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(taskBody("Missing owner", 999999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedRequestsReturnConsistent400() throws Exception {
        mvc.perform(get("/tasks/not-a-number").with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        mvc.perform(get("/users/email").with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/tasks").with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isMap());
        mvc.perform(post("/tasks").with(httpBasic(alice.getEmail(), PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(taskBody("Invalid enum", null).replace("TODO", "INVALID")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void taskFiltersCombineAndRespectOwnershipAndAdminAccess() throws Exception {
        aliceTask.setPriority(TaskPriority.HIGH);
        tasks.saveAndFlush(aliceTask);
        bobTask.setPriority(TaskPriority.HIGH);
        tasks.saveAndFlush(bobTask);
        Task completed = createTask("Completed Alice task", alice);
        completed.setStatus(TaskStatus.COMPLETED);
        completed.setPriority(TaskPriority.HIGH);
        tasks.saveAndFlush(completed);
        createTask("Medium Alice task", alice);

        mvc.perform(get("/tasks").param("status", "TODO").param("priority", "HIGH")
                        .with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(aliceTask.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/tasks").param("status", "TODO").param("priority", "HIGH")
                        .param("size", "1").param("page", "1").with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(bobTask.getId()))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/tasks").param("status", "TODO").with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/tasks").param("priority", "HIGH").with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/tasks").param("status", "IN_PROGRESS").with(httpBasic(alice.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void taskPagesSupportSortingAndOutOfRangePages() throws Exception {
        mvc.perform(get("/tasks").param("size", "2").param("sort", "title,desc")
                        .with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(legacyTask.getId()))
                .andExpect(jsonPath("$.content[1].id").value(bobTask.getId()))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/tasks").param("size", "2").param("page", "1").param("sort", "title,desc")
                        .with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(aliceTask.getId()))
                .andExpect(jsonPath("$.page").value(1));
        mvc.perform(get("/tasks").param("page", "20").with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/tasks").param("size", "100").with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content[0].id").value(aliceTask.getId()));
    }

    @Test
    void deadlineSortingUsesIdToBreakTies() throws Exception {
        for (Task task : new Task[]{aliceTask, bobTask, legacyTask}) {
            task.setDeadline(java.time.LocalDate.of(2030, 1, 1));
            tasks.saveAndFlush(task);
        }
        mvc.perform(get("/tasks").param("sort", "deadline,asc").param("size", "1").param("page", "1")
                        .with(httpBasic(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(bobTask.getId()));
    }

    @Test
    void invalidTaskListParametersReturn400() throws Exception {
        String[][] invalidParameters = {
                {"page", "-1"}, {"page", "abc"}, {"size", "0"}, {"size", "101"},
                {"size", "-1"}, {"size", "abc"}, {"status", "INVALID"}, {"priority", "INVALID"},
                {"sort", "owner.password,asc"}, {"sort", "missing,asc"}, {"sort", "id,wrong"},
                {"sort", "id"}, {"sort", "id,asc,extra"}, {"sort", "id,"}
        };
        for (String[] parameter : invalidParameters) {
            mvc.perform(get("/tasks").param(parameter[0], parameter[1]).with(httpBasic(alice.getEmail(), PASSWORD)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.errors").isMap());
        }
    }

    @Test
    void ownerCanChangeTaskStatusWithoutChangingOtherFields() throws Exception {
        aliceTask.setDeadline(java.time.LocalDate.of(2030, 1, 1));
        tasks.saveAndFlush(aliceTask);
        Task before = tasks.findById(aliceTask.getId()).orElseThrow();

        mvc.perform(patch("/tasks/{id}/status", aliceTask.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceTask.getId()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.title").value(before.getTitle()))
                .andExpect(jsonPath("$.description").value(before.getDescription()))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.deadline").value("2030-01-01"))
                .andExpect(jsonPath("$.ownerId").value(alice.getId()));

        Task updated = tasks.findById(aliceTask.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(updated.getTitle()).isEqualTo(before.getTitle());
        assertThat(updated.getDescription()).isEqualTo(before.getDescription());
        assertThat(updated.getPriority()).isEqualTo(before.getPriority());
        assertThat(updated.getDeadline()).isEqualTo(before.getDeadline());
        assertThat(updated.getOwner().getId()).isEqualTo(alice.getId());
        assertThat(updated.getCreatedAt()).isEqualTo(before.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isAfter(before.getUpdatedAt());
    }

    @Test
    void adminCanChangeStatusOfOwnedAndUnassignedTasks() throws Exception {
        for (Task task : new Task[]{bobTask, legacyTask}) {
            mvc.perform(patch("/tasks/{id}/status", task.getId()).with(httpBasic(admin.getEmail(), PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(task.getId()))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
            assertThat(tasks.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(TaskStatus.COMPLETED);
        }
        assertThat(tasks.findById(bobTask.getId()).orElseThrow().getOwner().getId()).isEqualTo(bob.getId());
        assertThat(tasks.findById(legacyTask.getId()).orElseThrow().getOwner()).isNull();
    }

    @Test
    void userCannotChangeStatusOfOthersOrUnassignedTasks() throws Exception {
        for (Task task : new Task[]{bobTask, legacyTask}) {
            Task before = tasks.findById(task.getId()).orElseThrow();
            mvc.perform(patch("/tasks/{id}/status", task.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                    .andExpect(status().isForbidden());
            Task unchanged = tasks.findById(task.getId()).orElseThrow();
            assertThat(unchanged.getStatus()).isEqualTo(before.getStatus());
            assertThat(unchanged.getUpdatedAt()).isEqualTo(before.getUpdatedAt());
        }
    }

    @Test
    void taskStatusChangeRequiresAuthentication() throws Exception {
        Task before = tasks.findById(aliceTask.getId()).orElseThrow();
        mvc.perform(patch("/tasks/{id}/status", aliceTask.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isUnauthorized());
        Task unchanged = tasks.findById(aliceTask.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(before.getStatus());
        assertThat(unchanged.getUpdatedAt()).isEqualTo(before.getUpdatedAt());
    }

    @Test
    void changingStatusOfMissingTaskReturns404() throws Exception {
        for (User actor : new User[]{alice, admin}) {
            mvc.perform(patch("/tasks/999999/status").with(httpBasic(actor.getEmail(), PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Task not found"));
        }
        assertThat(tasks.count()).isEqualTo(3);
    }

    @Test
    void invalidStatusChangesReturn400WithoutModifyingTask() throws Exception {
        String[] invalidBodies = {
                "{}", "{\"status\":null}", "{\"status\":\"INVALID\"}",
                "{\"status\":\"COMPLETED\",\"title\":\"Injected\"}",
                "{\"status\":\"COMPLETED\",\"ownerId\":" + bob.getId() + "}"
        };
        Task before = tasks.findById(aliceTask.getId()).orElseThrow();
        for (String body : invalidBodies) {
            mvc.perform(patch("/tasks/{id}/status", aliceTask.getId()).with(httpBasic(alice.getEmail(), PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.errors").isMap());
            Task unchanged = tasks.findById(aliceTask.getId()).orElseThrow();
            assertThat(unchanged.getStatus()).isEqualTo(before.getStatus());
            assertThat(unchanged.getTitle()).isEqualTo(before.getTitle());
            assertThat(unchanged.getOwner().getId()).isEqualTo(alice.getId());
            assertThat(unchanged.getUpdatedAt()).isEqualTo(before.getUpdatedAt());
        }
    }

    private User createUser(String email, Role role) {
        User user = new User();
        user.setName("Alicja");
        user.setSurname("Nowak");
        user.setEmail(email);
        user.setPassword(encoder.encode(PASSWORD));
        user.setRole(role);
        return users.saveAndFlush(user);
    }

    private Task createTask(String title, User owner) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription("Description");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        task.setOwner(owner);
        return tasks.saveAndFlush(task);
    }

    private String profile(String email, String password) {
        return "{\"name\":\"Alicja\",\"surname\":\"Nowak\",\"email\":\"" + email + "\""
                + (password == null ? "" : ",\"password\":\"" + password + "\"") + "}";
    }

    private String taskBody(String title, Long ownerId) {
        return "{\"title\":\"" + title + "\",\"description\":\"Description\",\"status\":\"TODO\",\"priority\":\"MEDIUM\""
                + (ownerId == null ? "" : ",\"ownerId\":" + ownerId) + "}";
    }
}
