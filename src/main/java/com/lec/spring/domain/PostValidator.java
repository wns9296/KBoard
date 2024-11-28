package com.lec.spring.domain;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class PostValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        System.out.println("supports(" + clazz.getName() + ") 호출");


// ↓ 검증할 객체의 클래스 타입인지 확인 : Post = clazz; 가능 여부
        boolean result = Post.class.isAssignableFrom(clazz);
        System.out.println(result);
        return result;
    }

    @Override
    public void validate(Object target, Errors errors) {
        Post post = (Post) target;
        System.out.println("validate()호출 : " + post);

        // ValidationUtils 사용
        // 단순히 빈(empty) 폼 데이터를 처리할때는 아래 와 같이 사용 가능
        // 두번째 매개변수 "subject" 은 반드시 target 클래스의 필드명 이어야 함
        // 게다가 Errors 에 등록될때도 동일한 field 명으로 등록된다.
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "subject", "글 제목은 필수입니다.");
    }
}
