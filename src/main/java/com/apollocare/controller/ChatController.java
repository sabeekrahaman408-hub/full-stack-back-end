package com.apollocare.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apollocare.dto.ChatRequest;
import com.apollocare.dto.ChatResponse;
import com.apollocare.service.ChatService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Public endpoint behind the FAQ page chat box (no login needed, rate limited, nothing is stored). */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, HttpServletRequest http) {
        return chatService.reply(request, http.getRemoteAddr());
    }
}
