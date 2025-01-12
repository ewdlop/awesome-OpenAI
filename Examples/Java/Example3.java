public class Example {
    private static final String ENDPOINT = "https://api.openai.com";

    /**
     * This sample demonstrates how to get completions from the OpenAI API.
     * https://learn.microsoft.com/en-us/java/api/overview/azure/ai-openai-readme?view=azure-java-preview
     */
	public static void main(String[] args) throws Exception {

        // Replace {key} with your subscription key
		System.out.println("Hello, World!");

        if(args.length != 1) {
            System.out.println("Please provide your subscription key as an argument");
            return;
        }

        OpenAIAsyncClient azureKeyedClient = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential("{key}"))
            .endpoint("{endpoint}")
            .buildAsyncClient();


        String openaiSecretKey = args[1];
        OpenAIAsyncClient openAPIKeyedClient = new OpenAIClientBuilder()
            .credential(new KeyCredential("{openai-secret-key}"))
            .buildAsyncClient();


            // Proxy options
        final String hostname = "{your-host-name}";
        final int port = 447; // your port number


        ProxyOptions proxyOptions = new ProxyOptions(ProxyOptions.Type.HTTP, new InetSocketAddress(hostname, port))
            .setCredentials("{username}", "{password}");

        OpenAIClient azureKeyedProxiedClient = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential("{key}"))
            .endpoint("{endpoint}")
            .clientOptions(new HttpClientOptions().setProxyOptions(proxyOptions))
            .buildClient();

        List<String> prompt = new ArrayList<>();
        prompt.add("Say this is a test");

        Completions completions = client.getCompletions("{deploymentOrModelName}", new CompletionsOptions(prompt));

        System.out.printf("Model ID=%s is created at %s.%n", completions.getId(), completions.getCreatedAt());
        for (Choice choice : completions.getChoices()) {
            System.out.printf("Index: %d, Text: %s.%n", choice.getIndex(), choice.getText());
        }

        List<String> prompt = new ArrayList<>();
        prompt.add("How to bake a cake?");

        IterableStream<Completions> completionsStream = client
            .getCompletionsStream("{deploymentOrModelName}", new CompletionsOptions(prompt));

        completionsStream.stream().forEach(completions -> System.out.print(completions.getChoices().get(0).getText()));

        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatRequestSystemMessage("You are a helpful assistant. You will talk like a pirate."));
        chatMessages.add(new ChatRequestUserMessage("Can you help me?"));
        chatMessages.add(new ChatRequestAssistantMessage("Of course, me hearty! What can I do for ye?"));
        chatMessages.add(new ChatRequestUserMessage("What's the best way to train a parrot?"));

        ChatCompletions chatCompletions = client.getChatCompletions("{deploymentOrModelName}",
            new ChatCompletionsOptions(chatMessages));

        System.out.printf("Model ID=%s is created at %s.%n", chatCompletions.getId(), chatCompletions.getCreatedAt());
        for (ChatChoice choice : chatCompletions.getChoices()) {
            ChatResponseMessage message = choice.getMessage();
            System.out.printf("Index: %d, Chat Role: %s.%n", choice.getIndex(), message.getRole());
            System.out.println("Message:");
            System.out.println(message.getContent());
        }

        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatRequestSystemMessage("You are a helpful assistant. You will talk like a pirate."));
        chatMessages.add(new ChatRequestUserMessage("Can you help me?"));
        chatMessages.add(new ChatRequestAssistantMessage("Of course, me hearty! What can I do for ye?"));
        chatMessages.add(new ChatRequestUserMessage("What's the best way to train a parrot?"));

        client.getChatCompletionsStream("{deploymentOrModelName}", new ChatCompletionsOptions(chatMessages))
            .forEach(chatCompletions -> {
            if (CoreUtils.isNullOrEmpty(chatCompletions.getChoices())) {
                return;
            }
            ChatResponseMessage delta = chatCompletions.getChoices().get(0).getDelta();
            if (delta.getRole() != null) {
                System.out.println("Role = " + delta.getRole());
            }
            if (delta.getContent() != null) {
                String content = delta.getContent();
                System.out.print(content);
            }
        });

        EmbeddingsOptions embeddingsOptions = new EmbeddingsOptions(
        Arrays.asList("Your text string goes here"));

        Embeddings embeddings = client.getEmbeddings("{deploymentOrModelName}", embeddingsOptions);

        for (EmbeddingItem item : embeddings.getData()) {
            System.out.printf("Index: %d.%n", item.getPromptIndex());
            for (Float embedding : item.getEmbedding()) {
                System.out.printf("%f;", embedding);
            }
        }

        ImageGenerationOptions imageGenerationOptions = new ImageGenerationOptions(
            "A drawing of the Seattle skyline in the style of Van Gogh");
            ImageGenerations images = client.getImageGenerations("{deploymentOrModelName}", imageGenerationOptions);

        for (ImageGenerationData imageGenerationData : images.getData()) {
            System.out.printf(
                "Image location URL that provides temporary access to download the generated image is %s.%n",
                imageGenerationData.getUrl());
        }

        String fileName = "{your-file-name}";
        Path filePath = Paths.get("{your-file-path}" + fileName);

        byte[] file = BinaryData.fromFile(filePath).toBytes();
        AudioTranscriptionOptions transcriptionOptions = new AudioTranscriptionOptions(file)
            .setResponseFormat(AudioTranscriptionFormat.JSON);

        AudioTranscription transcription = client.getAudioTranscription("{deploymentOrModelName}", fileName, transcriptionOptions);

        System.out.println("Transcription: " + transcription.getText());

        List<ChatRequestMessage> chatMessages = new ArrayList<>();
            chatMessages.add(new ChatRequestSystemMessage("You are a helpful assistant that describes images"));
            chatMessages.add(new ChatRequestUserMessage(Arrays.asList(
                    new ChatMessageTextContentItem("Please describe this image"),
                    new ChatMessageImageContentItem(
                            new ChatMessageImageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/4/44/Microsoft_logo.svg/512px-Microsoft_logo.svg.png"))
            )));

            ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(chatMessages);
            ChatCompletions chatCompletions = client.getChatCompletions("{deploymentOrModelName}", chatCompletionsOptions);

            System.out.println("Chat completion: " + chatCompletions.getChoices().get(0).getMessage().getContent());

        List<ChatRequestMessage> chatMessages = Arrays.asList(
        new ChatRequestSystemMessage("You are a helpful assistant."),
        new ChatRequestUserMessage("What sort of clothing should I wear today in Berlin?"));

        ChatCompletionsToolDefinition toolDefinition = new ChatCompletionsFunctionToolDefinition(
                new ChatCompletionsFunctionToolDefinitionFunction("MyFunctionName"));

        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(chatMessages);
        chatCompletionsOptions.setTools(Arrays.asList(toolDefinition));

        ChatCompletions chatCompletions = client.getChatCompletions("{deploymentOrModelName}", chatCompletionsOptions);

        ChatChoice choice = chatCompletions.getChoices().get(0);
        // The LLM is requesting the calling of the function we defined in the original request
        if (choice.getFinishReason() == CompletionsFinishReason.TOOL_CALLS) {
            ChatCompletionsFunctionToolCall toolCall = (ChatCompletionsFunctionToolCall) choice.getMessage().getToolCalls().get(0);
            String functionArguments = toolCall.getFunction().getArguments();

            // As an additional step, you may want to deserialize the parameters, so you can call your function
            MyFunctionCallArguments parameters = BinaryData.fromString(functionArguments).toObject(MyFunctionCallArguments.class);

            String functionCallResult = "{the-result-of-my-function}"; // myFunction(parameters...);

            ChatRequestAssistantMessage assistantMessage = new ChatRequestAssistantMessage("");
            assistantMessage.setToolCalls(choice.getMessage().getToolCalls());

            // We include:
            // - The past 2 messages from the original request
            // - A new ChatRequestAssistantMessage with the tool calls from the original request
            // - A new ChatRequestToolMessage with the result of our function call
            List<ChatRequestMessage> followUpMessages = Arrays.asList(
                    chatMessages.get(0),
                    chatMessages.get(1),
                    assistantMessage,
                    new ChatRequestToolMessage(functionCallResult, toolCall.getId())
            );

            ChatCompletionsOptions followUpChatCompletionsOptions = new ChatCompletionsOptions(followUpMessages);

            ChatCompletions followUpChatCompletions = client.getChatCompletions("{deploymentOrModelName}", followUpChatCompletionsOptions);

            // This time the finish reason is STOPPED
            ChatChoice followUpChoice = followUpChatCompletions.getChoices().get(0);
            if (followUpChoice.getFinishReason() == CompletionsFinishReason.STOPPED) {
                System.out.println("Chat Completions Result: " + followUpChoice.getMessage().getContent());
            }
        }

        List<ChatRequestMessage> chatMessages = Arrays.asList(
        new ChatRequestSystemMessage("You are a helpful assistant."),
        new ChatRequestUserMessage("What sort of clothing should I wear today in Berlin?")
        );

        ChatCompletionsToolDefinition toolDefinition = new ChatCompletionsFunctionToolDefinition(
                new ChatCompletionsFunctionToolDefinitionFunction("MyFunctionName"));

        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(chatMessages);
        chatCompletionsOptions.setTools(Arrays.asList(toolDefinition));

        ChatCompletions chatCompletions = client.getChatCompletions("{deploymentOrModelName}", chatCompletionsOptions);

        ChatChoice choice = chatCompletions.getChoices().get(0);
        // The LLM is requesting the calling of the function we defined in the original request
        if (choice.getFinishReason() == CompletionsFinishReason.TOOL_CALLS) {
            ChatCompletionsFunctionToolCall toolCall = (ChatCompletionsFunctionToolCall) choice.getMessage().getToolCalls().get(0);
            String functionArguments = toolCall.getFunction().getArguments();

            // As an additional step, you may want to deserialize the parameters, so you can call your function
            MyFunctionCallArguments parameters = BinaryData.fromString(functionArguments).toObject(MyFunctionCallArguments.class);

            String functionCallResult = "{the-result-of-my-function}"; // myFunction(parameters...);

            ChatRequestAssistantMessage assistantMessage = new ChatRequestAssistantMessage("");
            assistantMessage.setToolCalls(choice.getMessage().getToolCalls());

            // We include:
            // - The past 2 messages from the original request
            // - A new ChatRequestAssistantMessage with the tool calls from the original request
            // - A new ChatRequestToolMessage with the result of our function call
            List<ChatRequestMessage> followUpMessages = Arrays.asList(
                    chatMessages.get(0),
                    chatMessages.get(1),
                    assistantMessage,
                    new ChatRequestToolMessage(functionCallResult, toolCall.getId())
            );

            ChatCompletionsOptions followUpChatCompletionsOptions = new ChatCompletionsOptions(followUpMessages);

            ChatCompletions followUpChatCompletions = client.getChatCompletions("{deploymentOrModelName}", followUpChatCompletionsOptions);

            // This time the finish reason is STOPPED
            ChatChoice followUpChoice = followUpChatCompletions.getChoices().get(0);
            if (followUpChoice.getFinishReason() == CompletionsFinishReason.STOPPED) {
                System.out.println("Chat Completions Result: " + followUpChoice.getMessage().getContent());
            }
        }

        List<ChatRequestMessage> chatMessages = Arrays.asList(
            new ChatRequestSystemMessage("You are a helpful assistant."),
            new ChatRequestUserMessage("What sort of clothing should I wear today in Berlin?")
        );
        ChatCompletionsToolDefinition toolDefinition = new ChatCompletionsFunctionToolDefinition(
                new ChatCompletionsFunctionToolDefinitionFunction("MyFunctionName"));

        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(chatMessages);
        chatCompletionsOptions.setTools(Arrays.asList(toolDefinition));

        ChatCompletions chatCompletions = client.getChatCompletions("{deploymentOrModelName}", chatCompletionsOptions);

        ChatChoice choice = chatCompletions.getChoices().get(0);
        // The LLM is requesting the calling of the function we defined in the original request
        if (choice.getFinishReason() == CompletionsFinishReason.TOOL_CALLS) {
            ChatCompletionsFunctionToolCall toolCall = (ChatCompletionsFunctionToolCall) choice.getMessage().getToolCalls().get(0);
            String functionArguments = toolCall.getFunction().getArguments();

            // As an additional step, you may want to deserialize the parameters, so you can call your function
            MyFunctionCallArguments parameters = BinaryData.fromString(functionArguments).toObject(MyFunctionCallArguments.class);

            String functionCallResult = "{the-result-of-my-function}"; // myFunction(parameters...);

            ChatRequestAssistantMessage assistantMessage = new ChatRequestAssistantMessage("");
            assistantMessage.setToolCalls(choice.getMessage().getToolCalls());

            // We include:
            // - The past 2 messages from the original request
            // - A new ChatRequestAssistantMessage with the tool calls from the original request
            // - A new ChatRequestToolMessage with the result of our function call
            List<ChatRequestMessage> followUpMessages = Arrays.asList(
                    chatMessages.get(0),
                    chatMessages.get(1),
                    assistantMessage,
                    new ChatRequestToolMessage(functionCallResult, toolCall.getId())
            );

            ChatCompletionsOptions followUpChatCompletionsOptions = new ChatCompletionsOptions(followUpMessages);

            ChatCompletions followUpChatCompletions = client.getChatCompletions("{deploymentOrModelName}", followUpChatCompletionsOptions);

            // This time the finish reason is STOPPED
            ChatChoice followUpChoice = followUpChatCompletions.getChoices().get(0);
            if (followUpChoice.getFinishReason() == CompletionsFinishReason.STOPPED) 
            {
                System.out.println("Chat Completions Result: " + followUpChoice.getMessage().getContent());
            }
        }

        String deploymentOrModelId = "{azure-open-ai-deployment-model-id}";
        SpeechGenerationOptions options = new SpeechGenerationOptions(
                "Today is a wonderful day to build something people love!",
                SpeechVoice.ALLOY);
        BinaryData speech = client.generateSpeechFromText(deploymentOrModelId, options);
        // Checkout your generated speech in the file system.
        Path path = Paths.get("{your-local-file-path}/speech.wav");
        Files.write(path, speech.toBytes());

        // Upload a file
        FileDetails fileDetails = new FileDetails(
            BinaryData.fromFile(Paths.get("{your-local-file-path}/batch_tasks.jsonl")),
            "batch_tasks.jsonl");
        OpenAIFile file = client.uploadFile(fileDetails, FilePurpose.BATCH);
        String fileId = file.getId();
        // Get single file
        OpenAIFile fileFromBackend = client.getFile(fileId);
        // List files
        List<OpenAIFile> files = client.listFiles(FilePurpose.ASSISTANTS);
        // Delete file
        FileDeletionStatus deletionStatus = client.deleteFile(fileId);

        String fileId = "{fileId-from-service-side}";
        // Create a batch
        Batch batch = client.createBatch(new BatchCreateRequest("/chat/completions", fileId, "24h"));
        // Get single file
        byte[] fileContent = client.getFileContent(batch.getOutputFileId());
        // List batches
        PageableList<Batch> batchPageableList = client.listBatches();
        // Cancel a batch
        Batch cancelledBatch = client.cancelBatch(batch.getId());

        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(Arrays.asList(new ChatRequestUserMessage("What is the weather in Seattle?")))
            // Previously, the response_format parameter was only available to specify that the model should return a valid JSON.
            // In addition to this, we are introducing a new way of specifying which JSON schema to follow.
            .setResponseFormat(new ChatCompletionsJsonSchemaResponseFormat(
                new ChatCompletionsJsonSchemaResponseFormatJsonSchema("get_weather")
                    .setStrict(true)
                    .setDescription("Fetches the weather in the given location")
                    .setSchema(BinaryData.fromObject(new Parameters()))));
        
                    CreateUploadRequest createUploadRequest = new CreateUploadRequest("{fileNameToCreate}", CreateUploadRequestPurpose.ASSISTANTS,
                    totalFilesSize, "text/plain");
                Upload upload = client.createUpload(createUploadRequest);
                String uploadId = upload.getId();
                
                UploadPart uploadPartAdded = client.addUploadPart(uploadId,
                    new AddUploadPartRequest(new DataFileDetails(BinaryData.fromFile(path)).setFilename("{fileName}")));
                String uploadPartAddedId = uploadPartAdded.getId();
                System.out.println("Upload part added, upload part ID = " + uploadPartAddedId);
                
                UploadPart uploadPartAdded2 = client.addUploadPart(uploadId,
                    new AddUploadPartRequest(new DataFileDetails(BinaryData.fromFile(path2)).setFilename("{fileName2}")));
                String uploadPartAddedId2 = uploadPartAdded2.getId();
                System.out.println("Upload part 2 added, upload part ID = " + uploadPartAddedId2);
                
                CompleteUploadRequest completeUploadRequest = new CompleteUploadRequest(Arrays.asList(uploadPartAddedId, uploadPartAddedId2));
                Upload completeUpload = client.completeUpload(uploadId, completeUploadRequest);
                System.out.println("Upload completed, upload ID = " + completeUpload.getId());
    }
}

    