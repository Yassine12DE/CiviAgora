package tn.esprit.tic.civiAgora.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dto.chatDto.ChatRequest;
import tn.esprit.tic.civiAgora.dto.chatDto.ChatResponse;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Single endpoint for both anonymous and authenticated users.
     * ChatService checks SecurityContext internally to decide the mode.
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(response);
    }
}
