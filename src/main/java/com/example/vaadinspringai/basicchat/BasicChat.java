package com.example.vaadinspringai.basicchat;

import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

import java.time.Instant;

@Menu(title = "Basic Chat", order = 1)
@Route("")
@Slf4j
public class BasicChat extends VerticalLayout {
    public BasicChat(
            ChatClient.Builder builder,
            ChatMemory chatMemory) {
        setSizeFull();

        // use default advisor with default chat memory bean to make LLM remember the conversation context.
        chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        buildView();
    }

    private final ChatClient chatClient;

    private void buildView() {
        var messages = new MessageList();
        var input = new MessageInput();

        messages.setMarkdown(true);
        input.setWidthFull();

        input.addSubmitListener(event -> {
            var userMessage = event.getValue();

            messages.addItem(new MessageListItem(userMessage, Instant.now(), "You"));

            var response = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();

            log.info("response: {}...", response.substring(0,12));

            messages.addItem(new MessageListItem(response, Instant.now(), "AI"));
        });

        addAndExpand(new Scroller(messages));
        add(input);
    }
}
