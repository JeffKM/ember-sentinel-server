package com.inhacapstone04.embersentinelserver.room;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.room.controller.UserRoomMembershipCommandController;
import com.inhacapstone04.embersentinelserver.room.dto.RoomMemberResponse;
import com.inhacapstone04.embersentinelserver.room.service.UserRoomMembershipCommandService;
import com.inhacapstone04.embersentinelserver.support.MockMvcTestSupport;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
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

@WebMvcTest(UserRoomMembershipCommandController.class)
@DisplayName("UserRoomMembershipCommandController MockMvc 테스트")
class UserRoomMembershipCommandControllerTest extends MockMvcTestSupport {

    @MockitoBean
    private UserRoomMembershipCommandService userRoomMembershipCommandService;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 5L;

    @BeforeEach
    void setUp() {
        mockValidJwt(USER_ID);
    }

    @Test
    @DisplayName("POST /room/{roomId}/user → 201 Created (멤버 추가 성공)")
    void addMemberToRoom_Success() throws Exception {
        RoomMemberResponse response = new RoomMemberResponse(
                2L, "NewUser", null, AuthType.EMAIL, "VIEWER"
        );
        when(userRoomMembershipCommandService.addMemberToRoom(eq(USER_ID), eq(ROOM_ID), any()))
                .thenReturn(response);

        mockMvc.perform(post("/room/" + ROOM_ID + "/user")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"new@test.com\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.nickname").value("NewUser"))
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    @DisplayName("DELETE /room/{roomId}/user/{userId} → 204 No Content (멤버 삭제 성공)")
    void removeMemberFromRoom_Success() throws Exception {
        doNothing().when(userRoomMembershipCommandService)
                .removeMemberFromRoom(USER_ID, ROOM_ID, 2L);

        mockMvc.perform(delete("/room/" + ROOM_ID + "/user/2")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /room/{roomId}/user → 409 Conflict (이미 멤버인 사용자)")
    void addMemberToRoom_Fail_AlreadyMember() throws Exception {
        when(userRoomMembershipCommandService.addMemberToRoom(eq(USER_ID), eq(ROOM_ID), any()))
                .thenThrow(new CustomException(ErrorCode.ALREADY_MEMBER_OF_ROOM));

        mockMvc.perform(post("/room/" + ROOM_ID + "/user")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"existing@test.com\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_MEMBER_OF_ROOM"));
    }
}
