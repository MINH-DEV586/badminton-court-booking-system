package com.badmintonhub.backend.controller;

import com.badmintonhub.backend.auth.AuthDtos;
import com.badmintonhub.backend.dto.request.*;
import com.badmintonhub.backend.dto.response.*;
import com.badmintonhub.backend.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import java.util.*;
import java.time.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration(classes = UserController.class)
class UserControllerTests extends ControllerTestSupport {

    @Test
    void profileUsesJwtIdentityAndDoesNotExposePassword() throws Exception {
        when(users.getProfile(callerId)).thenReturn(new AuthDtos.UserResponse(callerId, "a@example.com", "A", null, UserAccount.Role.CUSTOMER));
        mvc.perform(get("/api/users/me").with(identity("CUSTOMER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(callerId.toString()))
            .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(header().string("Cache-Control", "no-store"));
        verify(users).getProfile(callerId);
    }

    @Test
    void updateRejectsBlankNameAndUsesAuthenticatedId() throws Exception {
        mvc.perform(put("/api/users/me").with(identity("CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"fullName\":\"New name\",\"phone\":null,\"role\":\"ADMIN\"}")).andExpect(status().isOk());
        verify(users).updateProfile(eq(callerId), argThat(request -> request.fullName().equals("New name")));
        mvc.perform(put("/api/users/me").with(identity("CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"fullName\":\" \"}")).andExpect(status().isBadRequest());
        verify(users, times(1)).updateProfile(any(), any());
    }

    @Test
    void customerCannotChangeRolesAndAdminMustProvideValidValues() throws Exception {
        var id = UUID.randomUUID();
        mvc.perform(patch("/api/admin/users/{id}/role", id).with(identity("CUSTOMER"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/users/{id}/role", id).with(identity("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"INVALID\"}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/users/{id}/active", id).with(identity("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        verifyNoInteractions(users);
    }

    @Test
    void adminCanSetFalseExplicitlyAndListUsers() throws Exception {
        var id = UUID.randomUUID();
        mvc.perform(patch("/api/admin/users/{id}/active", id).with(identity("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isOk());
        verify(users).setUserActive(id, false);
        when(users.getAllUsers(any())).thenReturn(Page.empty());
        mvc.perform(get("/api/admin/users").with(identity("ADMIN"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

}
