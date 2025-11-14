package com.inhacapstone04.embersentinelserver.common.resolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER) // (1) 파라미터에만 사용
@Retention(RetentionPolicy.RUNTIME) // (2) 런타임에 이 어노테이션 정보를 읽을 수 있어야 함
public @interface AuthorizedUser {

}
