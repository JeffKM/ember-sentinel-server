package com.inhacapstone04.embersentinelserver.user.service;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.user.dto.UserInfoResponse;
import com.inhacapstone04.embersentinelserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.inhacapstone04.embersentinelserver.common.exception.ErrorCode.NOT_FOUND_BY_ID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {
    private final UserRepository userRepository;

    public UserInfoResponse findUserById(Long userId) {
        return UserInfoResponse.of(userRepository.findById(userId).orElseThrow(
                () -> new CustomException(NOT_FOUND_BY_ID)));
    }
}
