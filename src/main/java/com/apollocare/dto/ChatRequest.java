package com.apollocare.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** One chat message from the FAQ page, plus the last few turns so the bot remembers the conversation. */
public record ChatRequest(
        @NotBlank(message = "Please type a question")
        @Size(max = 500, message = "Please keep your question under 500 characters")
        String message,
        @Size(max = 12, message = "This conversation is getting long - please start a new chat")
        List<@Valid Turn> history) {

    public record Turn(
            @Pattern(regexp = "user|assistant", message = "Invalid message role") String role,
            @NotBlank(message = "Empty message in history") @Size(max = 2000, message = "A message in the history is too long") String content) {
    }
}
