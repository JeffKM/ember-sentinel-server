package com.inhacapstone04.embersentinelserver.camera_edge;

import com.inhacapstone04.embersentinelserver.camera_edge.controller.CameraEdgeCommandController;
import com.inhacapstone04.embersentinelserver.camera_edge.dto.response.CameraEdgeResponse;
import com.inhacapstone04.embersentinelserver.camera_edge.service.CameraEdgeCommandService;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.support.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CameraEdgeCommandController.class)
@DisplayName("CameraEdgeCommandController MockMvc 테스트")
class CameraEdgeCommandControllerTest extends MockMvcTestSupport {

    @MockitoBean
    private CameraEdgeCommandService cameraEdgeCommandService;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 5L;

    @BeforeEach
    void setUp() {
        mockValidJwt(USER_ID);
    }

    @Test
    @DisplayName("POST /room/{roomId}/camera-edge → 201 Created (카메라 등록 성공)")
    void createCameraEdge_Success() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        CameraEdgeResponse response = new CameraEdgeResponse(
                10L, "uuid-123", "천장캠-01", "generated-api-key", now, now
        );
        when(cameraEdgeCommandService.createCameraEdge(eq(USER_ID), eq(ROOM_ID), any()))
                .thenReturn(response);

        mockMvc.perform(post("/room/" + ROOM_ID + "/camera-edge")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceUuid\":\"uuid-123\",\"cameraEdgeAlias\":\"천장캠-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cameraId").value(10))
                .andExpect(jsonPath("$.deviceUuid").value("uuid-123"));
    }

    @Test
    @DisplayName("DELETE /room/{roomId}/camera-edge/{cameraEdgeId} → 204 No Content (카메라 삭제 성공)")
    void deleteCameraEdge_Success() throws Exception {
        doNothing().when(cameraEdgeCommandService).deleteCameraEdge(USER_ID, ROOM_ID, 10L);

        mockMvc.perform(delete("/room/" + ROOM_ID + "/camera-edge/10")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /room/{roomId}/camera-edge → 403 Forbidden (권한 없음)")
    void createCameraEdge_Fail_NotAuthorized() throws Exception {
        when(cameraEdgeCommandService.createCameraEdge(eq(USER_ID), eq(ROOM_ID), any()))
                .thenThrow(new CustomException(ErrorCode.NOT_AUTHORIZED_ACCESS_BY_ID));

        mockMvc.perform(post("/room/" + ROOM_ID + "/camera-edge")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceUuid\":\"uuid-123\",\"cameraEdgeAlias\":\"천장캠-01\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_AUTHORIZED_ACCESS_BY_ID"));
    }
}
