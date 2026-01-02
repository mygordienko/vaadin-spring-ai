package com.example.vaadinspringai.documentanalysis;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.TransferContext;
import com.vaadin.flow.server.streams.TransferProgressListener;
import com.vaadin.flow.server.streams.UploadHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Duration;

@Menu(title = "Document Analysis", order = 2)
@Route("document-analysis")
@Slf4j
public class DocumentAnalysis extends VerticalLayout {

    public DocumentAnalysis(ChatClient.Builder builder, DocumentAnalysisConfigurationProperties properties) {
        this.chatClient = builder.build();
        buildView(properties.getMaxFileSizeClientCheckMB());
    }

    private final ChatClient chatClient;
    private final Div output = new Div();

    private static final Duration REJECTED_FILE_NOTIFICATION_DURATION = Duration.ofSeconds(5);
    public static final String SUMMARIZATION_SYSTEM_MESSAGE = """
        Summarize the following text into a concise paragraph that captures the main points and essential details without losing important information. 
        The summary should be as short as possible while remaining clear and informative.
        Use bullet points or numbered lists to organize the information if it helps to clarify the meaning. 
        Focus on the key facts, events, and conclusions. 
        Avoid including minor details or examples unless they are crucial for understanding the main ideas.
        """;

    private void buildView(int maxFileSizeMB) {

        var inMemoryUploadHandler = UploadHandler.inMemory(
                (metadata, data) -> {
                    var text = new String(data);
                    analyzeText(text);
                }, new TransferProgressListener() {
                    @Override
                    public void onStart(TransferContext context) {
                        log.info("{} upload started, content-type {}", context.fileName(), context.request().getContentType());
                    }

                    @Override
                    public void onProgress(TransferContext context, long transferredBytes, long totalBytes) {
                        log.info("-- {} of {} uploaded", transferredBytes, totalBytes);
                    }

                    @Override
                    public void onError(TransferContext context, IOException reason) {
                        log.error("{} upload failed:", context.fileName(), reason);
                    }

                    @Override
                    public void onComplete(TransferContext context, long transferredBytes) {
                        log.info("{} upload completed", context.fileName());
                    }
                }
        );

        var upload = new Upload(inMemoryUploadHandler);
        upload.setAcceptedFileTypes(
                MediaType.APPLICATION_PDF_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.TEXT_PLAIN_VALUE,
                MediaType.TEXT_MARKDOWN_VALUE,
                MediaType.TEXT_HTML_VALUE);
        // NOTE: client-side constraint, server side check must be set independently
        upload.setMaxFileSize(maxFileSizeMB*1024*1024);
        upload.setMaxFiles(1);
        upload.addFileRejectedListener(event -> {
            var errorMessage = event.getErrorMessage();
            Notification.show(errorMessage, ((int) REJECTED_FILE_NOTIFICATION_DURATION.toMillis()), Notification.Position.MIDDLE);
        });

        var heading = new H1("Upload document to analyze");
        heading.addClassName("heading-text");

        var header = new HorizontalLayout(heading, upload);
        header.setAlignItems(Alignment.BASELINE);
        header.addClassName("flex-wrap");

        add(header, output);
    }

    private void analyzeText(String text) {
        output.removeAll();
        var response = new Markdown();
        output.add(response);

        var ui = UI.getCurrent();
        chatClient.prompt()
                .system(SUMMARIZATION_SYSTEM_MESSAGE)
                .user("Text to summarize: " + text)
                .stream()
                .content()
                .subscribe(s -> {
                    ui.access(() -> response.appendContent(s));
                });
    }
}
