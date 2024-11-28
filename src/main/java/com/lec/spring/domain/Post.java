package com.lec.spring.domain;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
// Model 객체 (domain)


/**
 * DTO 객체
 * : Data Transfer Object 라고도 함.
 * <p>
 * 객체 -> DB
 * DB -> 객체
 * request -> 객체
 * 객체 -> response
 * ..
 */


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    private Long id;
    private String subject;
    private String content;
    private LocalDateTime regDate;
    private Long viewCnt;

    private User user; // 글 작성자(FK)

    @ToString.Exclude
    @Builder.Default
    private List<Attachment> fileList = new ArrayList<>();
}
