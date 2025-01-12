public class Example {
    private static final String ENDPOINT = "https://api.openai.com";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String DEPLOYMENT_MODEL_NAME = "deploymentOrModelName";
    private static final String DEPLOYMENT_MODEL_ID = "deploymentOrModelId";

    /**
     * This sample demonstrates how to get completions from the OpenAI API.
     * Required command line arguments:
     * args[0]: API Key
     * args[1]: OpenAI Secret Key
     * args[2]: Proxy Hostname
     * args[3]: Proxy Port
     * args[4]: Proxy Username
     * args[5]: Proxy Password
     * args[6]: File Path for Audio
     * args[7]: File Name for Audio
     * args[8]: Local File Path for Speech Output
     * args[9]: Local File Path for Batch Tasks
     * args[10]: File ID from Service Side
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 11) {
            System.out.println("Please provide all required arguments:");
            System.out.println("1. API Key");
            System.out.println("2. OpenAI Secret Key");
            System.out.println("3. Proxy Hostname");
            System.out.println("4. Proxy Port");
            System.out.println("5. Proxy Username");
            System.out.println("6. Proxy Password");
            System.out.println("7. File Path for Audio");
            System.out.println("8. File Name for Audio");
            System.out.println("9. Local File Path for Speech Output");
            System.out.println("10. Local File Path for Batch Tasks");
            System.out.println("11. File ID from Service Side");
            return;
        }

        // Parse command line arguments
        String apiKey = args[0];
        String openaiSecretKey = args[1];
        String proxyHostname = args[2];
        int proxyPort = Integer.parseInt(args[3]);
        String proxyUsername = args[4];
        String proxyPassword = args[5];
        String audioFilePath = args[6];
        String audioFileName = args[7];
        String speechOutputPath = args[8];
        String batchTasksPath = args[9];
        String serviceFileId = args[10];

        // Initialize clients
        OpenAIAsyncClient azureKeyedClient = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(apiKey))
            .endpoint(ENDPOINT)
            .buildAsyncClient();

        OpenAIAsyncClient openAPIKeyedClient = new OpenAIClientBuilder()
            .credential(new KeyCredential(openaiSecretKey))
            .buildAsyncClient();

        // Initialize proxy client
        ProxyOptions proxyOptions = new ProxyOptions(
            ProxyOptions.Type.HTTP, 
            new InetSocketAddress(proxyHostname, proxyPort))
            .setCredentials(proxyUsername, proxyPassword);

        OpenAIClient azureKeyedProxiedClient = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(apiKey))
            .endpoint(ENDPOINT)
            .clientOptions(new HttpClientOptions().setProxyOptions(proxyOptions))
            .buildClient();

        // Completions example
        List<String> prompt = new ArrayList<>();
        prompt.add("Say this is a test");

        Completions completions = azureKeyedProxiedClient.getCompletions(
            DEPLOYMENT_MODEL_NAME, 
            new CompletionsOptions(prompt));

        System.out.printf("Model ID=%s is created at %s.%n", 
            completions.getId(), 
            completions.getCreatedAt());
            
        for (Choice choice : completions.getChoices()) {
            System.out.printf("Index: %d, Text: %s.%n", 
                choice.getIndex(), 
                choice.getText());
        }

        // Chat completions example
        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatRequestSystemMessage(
            "You are a helpful assistant. You will talk like a pirate."));
        chatMessages.add(new ChatRequestUserMessage("Can you help me?"));
        
        ChatCompletions chatCompletions = azureKeyedProxiedClient.getChatCompletions(
            DEPLOYMENT_MODEL_NAME,
            new ChatCompletionsOptions(chatMessages));

        // Audio transcription example
        Path filePath = Paths.get(audioFilePath, audioFileName);
        byte[] file = BinaryData.fromFile(filePath).toBytes();
        
        AudioTranscriptionOptions transcriptionOptions = new AudioTranscriptionOptions(file)
            .setResponseFormat(AudioTranscriptionFormat.JSON);

        AudioTranscription transcription = azureKeyedProxiedClient.getAudioTranscription(
            DEPLOYMENT_MODEL_NAME, 
            audioFileName, 
            transcriptionOptions);

        // Speech generation example
        SpeechGenerationOptions speechOptions = new SpeechGenerationOptions(
            "Today is a wonderful day to build something people love!",
            SpeechVoice.ALLOY);
            
        BinaryData speech = azureKeyedProxiedClient.generateSpeechFromText(
            DEPLOYMENT_MODEL_ID, 
            speechOptions);
            
        Path speechPath = Paths.get(speechOutputPath, "speech.wav");
        Files.write(speechPath, speech.toBytes());

        // File operations example
        FileDetails fileDetails = new FileDetails(
            BinaryData.fromFile(Paths.get(batchTasksPath, "batch_tasks.jsonl")),
            "batch_tasks.jsonl");
            
        OpenAIFile file = azureKeyedProxiedClient.uploadFile(fileDetails, FilePurpose.BATCH);
        String fileId = file.getId();

        // Batch operations example
        Batch batch = azureKeyedProxiedClient.createBatch(
            new BatchCreateRequest("/chat/completions", serviceFileId, "24h"));
            
        byte[] fileContent = azureKeyedProxiedClient.getFileContent(batch.getOutputFileId());
        
        // Upload operations example
        long totalFilesSize = Files.size(Paths.get(batchTasksPath));
        CreateUploadRequest createUploadRequest = new CreateUploadRequest(
            "upload.txt", 
            CreateUploadRequestPurpose.ASSISTANTS,
            totalFilesSize, 
            "text/plain");
            
        Upload upload = azureKeyedProxiedClient.createUpload(createUploadRequest);
        
        // Add upload parts
        UploadPart uploadPartAdded = azureKeyedProxiedClient.addUploadPart(
            upload.getId(),
            new AddUploadPartRequest(
                new DataFileDetails(BinaryData.fromFile(Paths.get(batchTasksPath)))
                    .setFilename("part1.txt")));

        // Complete upload
        CompleteUploadRequest completeUploadRequest = new CompleteUploadRequest(
            Arrays.asList(uploadPartAdded.getId()));
            
        Upload completeUpload = azureKeyedProxiedClient.completeUpload(
            upload.getId(), 
            completeUploadRequest);
            
        System.out.println("Upload completed, upload ID = " + completeUpload.getId());
    }
}