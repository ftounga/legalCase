package fr.ailegalcase.help;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/help")
public class HelpChatController {

    private final HelpChatService helpChatService;

    public HelpChatController(HelpChatService helpChatService) {
        this.helpChatService = helpChatService;
    }

    @PostMapping("/chat")
    public HelpChatResponse chat(@RequestBody @Valid HelpChatRequest request) {
        return helpChatService.chat(request.message());
    }
}
