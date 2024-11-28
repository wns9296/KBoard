package com.lec.spring.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
public class QryCommentList extends QryResult {
    @ToString.Exclude
    @JsonProperty("data")
    private List<Comment> list;
}
