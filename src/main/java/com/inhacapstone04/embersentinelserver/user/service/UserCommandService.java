package com.inhacapstone04.embersentinelserver.user.service;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.user.dto.UserLoginResultDTO;
import com.inhacapstone04.embersentinelserver.user.entity.User;
import com.inhacapstone04.embersentinelserver.user.entity.UserRole;
import com.inhacapstone04.embersentinelserver.user.entity.oauth.OAuth2UserInfo;
import com.inhacapstone04.embersentinelserver.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
// 이 서비스가 실제 DB CUD를 담당하므로 @Transactional을 붙입니다.
@Transactional
public class UserCommandService {

    private final UserRepository userRepository;

    /**
     * OAuth 정보로 사용자를 찾거나, 없으면 새로 생성(회원가입)합니다.
     * 기존 사용자는 정보를 업데이트합니다.
     *
     * @param userInfo OAuth2 제공자로부터 받은 사용자 정보
     * @return UserLoginResult (사용자 엔티티와, 신규 가입 여부)
     */
    public UserLoginResultDTO findOrCreateUser(OAuth2UserInfo userInfo) {

        // 1. 사용자 조회 (email, authType 기준)
        Optional<User> existingUserOptional = userRepository.findByEmailAndAuthType(userInfo.getEmail(), userInfo.getAuthType());

        // 2. 신규 유저인지 미리 판단, Optional이 비어있으면(isEmpty) 신규 유저입니다.
        boolean isNewUser = existingUserOptional.isEmpty();

        // 3. User 객체 가져오기 (있으면 가져오고, 없으면 람다 실행)
        User user = existingUserOptional.orElseGet(() -> {
            // 2-1. 신규 유저인 경우 (회원가입)
            // (이제 밖의 isNewUser 변수를 건드리지 않아도 됩니다)
            User newUser = new User();
            newUser.setEmail(userInfo.getEmail());
            newUser.setNickname(userInfo.getNickname());
            newUser.setProfileImageUrl(userInfo.getProfileImageUrl());
            newUser.setAuthType(userInfo.getAuthType());
            newUser.setUserRole(UserRole.USER);
            return userRepository.save(newUser);
        });

        // 2-2. 기존 유저인 경우 (정보 업데이트)
        if (!isNewUser) {
            user.setNickname(userInfo.getNickname());
            user.setProfileImageUrl(userInfo.getProfileImageUrl());
        }

        return UserLoginResultDTO.of(user, isNewUser);
    }

    /**
     * 사용자의 FCM 토큰을 등록하거나 갱신합니다.
     *
     * @param userId   요청한 사용자 ID
     * @param fcmToken 저장할 FCM 토큰
     */
    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_BY_ID, "사용자를 찾을 수 없습니다."));

        // 기존 토큰과 다를 경우에만 업데이트 (Dirty Checking)
        if (!fcmToken.equals(user.getFcmToken())) {
            user.setFcmToken(fcmToken);
        }
    }
}
