package com.inhacapstone04.embersentinelserver.room;

import com.inhacapstone04.embersentinelserver.common.response.PageResponse;
import com.inhacapstone04.embersentinelserver.room.controller.RoomQueryController;
import com.inhacapstone04.embersentinelserver.room.dto.response.RoomDashboardResponse;
import com.inhacapstone04.embersentinelserver.room.dto.response.RoomDetailResponse;
import com.inhacapstone04.embersentinelserver.room.dto.response.SingleRoomResponse;
import com.inhacapstone04.embersentinelserver.room.service.RoomQueryService;
import com.inhacapstone04.embersentinelserver.support.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomQueryController.class)
@DisplayName("RoomQueryController MockMvc 테스트")
class RoomQueryControllerTest extends MockMvcTestSupport {

    @MockitoBean
    private RoomQueryService roomQueryService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        mockValidJwt(USER_ID);
    }

    @Test
    @DisplayName("GET /room/list/me → 200 OK (내 Room 목록 조회)")
    void getMyRooms_Success() throws Exception {
        SingleRoomResponse room = new SingleRoomResponse(1L, "연구실", "하이테크센터", "3", "305");
        PageResponse<SingleRoomResponse> response = new PageResponse<>(
                List.of(room), 1, 10, 1L, 1
        );
        when(roomQueryService.getMyRooms(eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(get("/room/list/me")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].roomAlias").value("연구실"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /room/list/me/summary → 200 OK (Room 통계 조회)")
    void getMyRoomStatistics_Success() throws Exception {
        RoomDashboardResponse response = new RoomDashboardResponse(
                2, 5, 1, List.of()
        );
        when(roomQueryService.getRoomStatistics(eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(post("/room/list/me/summary")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomIds\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRoomCount").value(2))
                .andExpect(jsonPath("$.totalCameraCount").value(5));
    }

    @Test
    @DisplayName("GET /room/{roomId}/detail → 200 OK (Room 상세 조회)")
    void getRoomDetail_Success() throws Exception {
        RoomDetailResponse response = new RoomDetailResponse(
                1L, "연구실", "하이테크센터", "3", "305",
                List.of(), List.of()
        );
        when(roomQueryService.getRoomDetail(USER_ID, 1L)).thenReturn(response);

        mockMvc.perform(get("/room/1/detail")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.buildingName").value("하이테크센터"));
    }
}
