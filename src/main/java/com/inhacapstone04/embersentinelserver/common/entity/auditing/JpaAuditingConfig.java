package com.inhacapstone04.embersentinelserver.common.entity.auditing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.util.Optional;

@Configuration
// (1) Auditing 기능을 활성화하면서,
// (2) 우리가 생성할 "offsetDateTimeProvider" Bean을 사용하도록 명시합니다.
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaAuditingConfig {

    /**
     * @CreatedDate, @LastModifiedDate가 OffsetDateTime을 사용하도록
     * "현재 시간"을 OffsetDateTime.now()로 반환하는 Provider Bean을 생성합니다.
     */
    @Bean(name = "offsetDateTimeProvider")
    public DateTimeProvider offsetDateTimeProvider() {
        // () -> Optional.of(OffsetDateTime.now())
        // 위 람다식은 DateTimeProvider의 getNow() 메서드를 구현한 것입니다.
        return () -> Optional.of(OffsetDateTime.now());
    }
}
