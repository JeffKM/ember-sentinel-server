package com.inhacapstone04.embersentinelserver.room;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.room.controller.RoomCommandController;
import com.inhacapstone04.embersentinelserver.room.dto.response.SingleRoomResponse;
import com.inhacapstone04.embersentinelserver.room.service.RoomCommandService;
import com.inhacapstone04.embersentinelserver.support.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomCommandController.class)
@DisplayName("RoomCommandController MockMvc 테스트")
class RoomCommandControllerTest extends MockMvcTestSupport {

    @MockitoBean
    private RoomCommandService roomCommandService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        mockValidJwt(USER_ID);
    }

    @Test
    @DisplayName("POST /room → 201 Created (Room 생성 성공)")
    void createRoom_Success() throws Exception {
        SingleRoomResponse response = new SingleRoomResponse(
                10L, "정보보안연구실", "하이테크센터", "3", "305"
        );
        when(roomCommandService.createRoom(eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(post("/room")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildingId\":1,\"roomAlias\":\"정보보안연구실\",\"floor\":\"3\",\"roomNumber\":\"305\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(10))
                .andExpect(jsonPath("$.roomAlias").value("정보보안연구실"));
    }

    @Test
    @DisplayName("DELETE /room/{roomId} → 204 No Content (Room 삭제 성공)")
    void deleteRoom_Success() throws Exception {
        doNothing().when(roomCommandService).deleteRoom(USER_ID, 10L);

        mockMvc.perform(delete("/room/10")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /room/{roomId} → 403 Forbidden (권한 없음)")
    void deleteRoom_Fail_NotAuthorized() throws Exception {
        doThrow(new CustomException(ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID))
                .when(roomCommandService).deleteRoom(USER_ID, 10L);

        mockMvc.perform(delete("/room/10")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_AUTHORIZED_ACCESS_BY_ID"));
    }

    @Test
    @DisplayName("POST /room → 401 Unauthorized (인증 헤더 없음)")
    void createRoom_Fail_NoAuthHeader() throws Exception {
        mockMvc.perform(post("/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildingId\":1,\"roomAlias\":\"테스트\",\"floor\":\"1\",\"roomNumber\":\"101\"}"))
                .andExpect(status().isUnauthorized());
    }
}
