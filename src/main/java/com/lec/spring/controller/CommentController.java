package com.lec.spring.controller;

import com.lec.spring.domain.QryCommentList;
import com.lec.spring.domain.QryResult;
import com.lec.spring.service.CommentService;
import org.apache.ibatis.session.SqlSession;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/comment")
@RestController
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }


    @GetMapping("/list/{postId}")
    public QryCommentList list(@PathVariable Long postId) {
        return commentService.list(postId);
    }

    @PostMapping("/write")
    public QryResult write(
            @RequestParam("post_id") Long postId,
            @RequestParam("user_id") Long userId,
            String content) {
        return commentService.write(postId, userId, content);
    }

    @PostMapping("/delete")
    public QryResult delete(Long id) {
        return commentService.delete(id);
    }
}
