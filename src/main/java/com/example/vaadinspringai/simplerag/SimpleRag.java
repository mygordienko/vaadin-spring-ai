package com.example.vaadinspringai.simplerag;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.TransferContext;
import com.vaadin.flow.server.streams.TransferProgressListener;
import com.vaadin.flow.server.streams.UploadHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Menu(title = "RAG: PDF/DOC analysis", order = 3)
@Route("simple-rag")
@Slf4j
public class SimpleRag extends VerticalLayout {

    public SimpleRag(ChatClient.Builder builder, VectorStore vectorStore, SimpleRagConfigurationProperties properties) {
        this.vectorStore = vectorStore;
        this.chatClient = builder
                .defaultAdvisors(RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .build())
                        .build())
                .build();

        buildView(properties.getMaxFileSizeClientCheck());
    }

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    private static final Duration REJECTED_FILE_NOTIFICATION_DURATION = Duration.ofSeconds(5);

    private void buildView(DataSize maxFileSizeClientCheck) {
        var messageList = new MessageList();
        messageList.setMarkdown(true);
        var messageInput = new MessageInput();
        var popover = new Popover();
        var contextButton = new Button("Add context");
        var inputLayout = new HorizontalLayout();

        var inMemoryUploadHandler = UploadHandler.inMemory(
                (metadata, data) -> {
                    processDocument(data);
                    countDocs();
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
                MediaType.IMAGE_PNG_VALUE,
                MediaType.IMAGE_JPEG_VALUE);
        // NOTE: client-side constraint, server side check must be set independently
        upload.setMaxFileSize(((int) maxFileSizeClientCheck.toBytes()));
        upload.setMaxFiles(1);
        upload.addFileRejectedListener(event -> {
            var errorMessage = event.getErrorMessage();
            Notification.show(errorMessage, ((int) REJECTED_FILE_NOTIFICATION_DURATION.toMillis()), Notification.Position.MIDDLE);
        });
        upload.addFileRemovedListener(event -> {
            log.info("Clear context");
        });

        popover.setTarget(contextButton);
//        popover.setPosition(PopoverPosition.TOP);
        popover.add(upload);

        inputLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        inputLayout.addAndExpand(messageInput);
        inputLayout.add(contextButton);

        messageInput.addSubmitListener(e -> {
            var prompt = e.getValue();

            messageList.addItem(new MessageListItem(prompt, Instant.now(), "You"));

            var answer = new MessageListItem("", Instant.now(), "AI");

            var ui = UI.getCurrent();
            messageList.addItem(answer);
            chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content()
                    .subscribe(s -> ui.access(() -> answer.appendText(s)));
        });

        var scroller = new Scroller(messageList);
        scroller.setHeightFull();
        addAndExpand(scroller);
        add(inputLayout);
    }

    private void countDocs() {
        var size = vectorStore.similaritySearch("").size();
        log.info("Documents in store: {}", size);
    }

    private void processDocument(byte[] data) {
        var documentReader = new TikaDocumentReader(new ByteArrayResource(data));
        var documentList = documentReader.read();

        // NOTE: token splitter is required to avoid "the input length exceeds the context length" error
        //  The error means at least one chunk of text sent to your embedding model is longer than the model’s maximum input length
        //  , this is a token limit (not file size or character count).
        var documentTransformer = new TokenTextSplitter();
        vectorStore.write(documentTransformer.apply(documentList));
    }
}
