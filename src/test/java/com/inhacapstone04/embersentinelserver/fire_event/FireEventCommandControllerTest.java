package com.inhacapstone04.embersentinelserver.fire_event;

import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.fire_event.controller.FireEventCommandController;
import com.inhacapstone04.embersentinelserver.fire_event.dto.response.FireEventStreamInfoResponse;
import com.inhacapstone04.embersentinelserver.fire_event.service.FireEventCommandService;
import com.inhacapstone04.embersentinelserver.support.MockMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FireEventCommandController.class)
@DisplayName("FireEventCommandController MockMvc 테스트")
class FireEventCommandControllerTest extends MockMvcTestSupport {

    @MockitoBean
    private FireEventCommandService fireEventCommandService;

    private static final Long CAMERA_ID = 10L;
    private static final String VALID_API_KEY = "test-api-key";

    private void mockDeviceAuth() {
        CameraEdge mockCamera = mock(CameraEdge.class);
        when(mockCamera.getId()).thenReturn(CAMERA_ID);
        when(cameraEdgeRepository.findByApiKey(VALID_API_KEY))
                .thenReturn(Optional.of(mockCamera));
    }

    @Test
    @DisplayName("POST /embedded/fire-event/publish → 201 Created (화재 이벤트 생성)")
    void startFireEvent_Success() throws Exception {
        mockDeviceAuth();
        FireEventStreamInfoResponse response = FireEventStreamInfoResponse.of(
                "livekit-token", "stream-room-1", 100L
        );
        when(fireEventCommandService.startFireEvent(any(), eq(CAMERA_ID))).thenReturn(response);

        mockMvc.perform(post("/embedded/fire-event/publish")
                        .header("X-Device-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceUuid\":\"uuid-123\",\"detectionType\":\"FIRE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("livekit-token"))
                .andExpect(jsonPath("$.fireEventId").value(100));
    }

    @Test
    @DisplayName("POST /embedded/fire-event/publish → 401 Unauthorized (API Key 없음)")
    void startFireEvent_Fail_NoApiKey() throws Exception {
        mockMvc.perform(post("/embedded/fire-event/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceUuid\":\"uuid-123\",\"detectionType\":\"FIRE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /embedded/fire-event/publish → 401 Unauthorized (잘못된 API Key)")
    void startFireEvent_Fail_InvalidApiKey() throws Exception {
        when(cameraEdgeRepository.findByApiKey("invalid-key"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/embedded/fire-event/publish")
                        .header("X-Device-API-Key", "invalid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceUuid\":\"uuid-123\",\"detectionType\":\"FIRE\"}"))
                .andExpect(status().isUnauthorized());
    }
}
