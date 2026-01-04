package com.example.vaadinspringai.mcp;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;

import java.time.Instant;

@Menu(title = "MCP time", order = 4)
@Route("mcp")
@Slf4j
public class Mcp extends VerticalLayout {

    public Mcp(ChatClient.Builder builder,
               ChatMemory chatMemory,
               McpSyncClient mcpSyncClient) {
        setSizeFull();
        this.chatClient = builder
                .defaultSystem("Use the provided tools to get information you do not know. If not explicitly specified in question, use your default time zone to answer.")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(SyncMcpToolCallbackProvider.builder().addMcpClient(mcpSyncClient).build())
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

            log.info("Response: {}", response);

            messages.addItem(new MessageListItem(response, Instant.now(), "AI"));

        });

        var heading = new H1("Chat with enabled MCP time server");
        heading.addClassName(LumoUtility.FontSize.XLARGE);

        var header = new HorizontalLayout(heading);

        add(header);
        addAndExpand(new Scroller(messages));
        add(input);
    }
}
