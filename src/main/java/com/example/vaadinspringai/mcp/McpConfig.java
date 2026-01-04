package com.example.vaadinspringai.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class McpConfig {

    @Bean(destroyMethod = "close")
    public McpSyncClient mcpClient() {

        // based on
        // https://github.com/modelcontextprotocol/servers/tree/main/src/time
        var stdioParams = ServerParameters.builder("python")
            .args("-m", "mcp_server_time")
            .build();

        var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams, McpJsonMapper.createDefault()))
            .requestTimeout(Duration.ofSeconds(10)).build();

        var init = mcpClient.initialize();

        log.info("MCP Initialized: {}", init);

        return mcpClient;

    }
}
