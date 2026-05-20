package com.inhacapstone04.embersentinelserver.user;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.support.MockMvcTestSupport;
import com.inhacapstone04.embersentinelserver.user.controller.AuthController;
import com.inhacapstone04.embersentinelserver.user.dto.response.AuthInfoResponse;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController MockMvc 테스트")
class AuthControllerTest extends MockMvcTestSupport {

    @MockitoBean
    private AuthService authService;

    private final AuthInfoResponse successResponse = AuthInfoResponse.of(
            "access-token", "refresh-token", 3600L, false
    );

    @Test
    @DisplayName("POST /auth/google → 200 OK")
    void loginGoogle_Success() throws Exception {
        when(authService.login(eq(AuthType.GOOGLE), any())).thenReturn(successResponse);

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"google-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.grantType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/kakao → 200 OK")
    void loginKakao_Success() throws Exception {
        when(authService.login(eq(AuthType.KAKAO), any())).thenReturn(successResponse);

        mockMvc.perform(post("/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"kakao-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /auth/email → 200 OK")
    void loginEmail_Success() throws Exception {
        when(authService.loginByEmail(any(), any())).thenReturn(successResponse);

        mockMvc.perform(post("/auth/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"nickname\":\"Test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("POST /auth/token/refresh → 200 OK")
    void refresh_Success() throws Exception {
        when(authService.reissueToken(any())).thenReturn(successResponse);

        mockMvc.perform(post("/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"valid-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("POST /auth/token/refresh → 401 UNAUTHORIZED (블랙리스트)")
    void refresh_Fail_Blacklisted() throws Exception {
        when(authService.reissueToken(any()))
                .thenThrow(new CustomException(ErrorCode.REFRESH_TOKEN_REUSED));

        mockMvc.perform(post("/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"blacklisted-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));
    }
}
