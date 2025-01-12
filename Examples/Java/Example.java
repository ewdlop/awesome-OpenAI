public class Example {
    /**
     * Configuration class to handle all placeholder values
     */
    private static class OpenAIConfig {
        private final String apiKey;
        private final String endpoint;
        private final String openaiSecretKey;
        private final String deploymentModelName;
        private final String deploymentModelId;
        private final ProxyConfig proxyConfig;
        private final FileConfig fileConfig;

        public OpenAIConfig(String[] args) {
            if (args.length < 11) {
                throw new IllegalArgumentException(getUsageMessage());
            }
            this.apiKey = args[0];
            this.endpoint = ENDPOINT;
            this.openaiSecretKey = args[1];
            this.deploymentModelName = DEPLOYMENT_MODEL_NAME;
            this.deploymentModelId = DEPLOYMENT_MODEL_ID;
            this.proxyConfig = new ProxyConfig(args[2], Integer.parseInt(args[3]), args[4], args[5]);
            this.fileConfig = new FileConfig(args[6], args[7], args[8], args[9], args[10]);
        }

        private static String getUsageMessage() {
            return "Please provide all required arguments:\n" +
                   "1. API Key\n" +
                   "2. OpenAI Secret Key\n" +
                   "3. Proxy Hostname\n" +
                   "4. Proxy Port\n" +
                   "5. Proxy Username\n" +
                   "6. Proxy Password\n" +
                   "7. File Path for Audio\n" +
                   "8. File Name for Audio\n" +
                   "9. Local File Path for Speech Output\n" +
                   "10. Local File Path for Batch Tasks\n" +
                   "11. File ID from Service Side";
        }
    }

    private static class ProxyConfig {
        private final String hostname;
        private final int port;
        private final String username;
        private final String password;

        public ProxyConfig(String hostname, int port, String username, String password) {
            this.hostname = hostname;
            this.port = port;
            this.username = username;
            this.password = password;
        }
    }

    private static class FileConfig {
        private final String audioFilePath;
        private final String audioFileName;
        private final String speechOutputPath;
        private final String batchTasksPath;
        private final String serviceFileId;

        public FileConfig(String audioFilePath, String audioFileName, String speechOutputPath, 
                         String batchTasksPath, String serviceFileId) {
            this.audioFilePath = audioFilePath;
            this.audioFileName = audioFileName;
            this.speechOutputPath = speechOutputPath;
            this.batchTasksPath = batchTasksPath;
            this.serviceFileId = serviceFileId;
        }
    }

    private static final String ENDPOINT = "https://api.openai.com";
    private static final String DEPLOYMENT_MODEL_NAME = "deploymentOrModelName";
    private static final String DEPLOYMENT_MODEL_ID = "deploymentOrModelId";

    /**
     * This sample demonstrates how to get completions from the OpenAI API with proper configuration.
     */
    public static void main(String[] args) throws Exception {
        try {
            OpenAIConfig config = new OpenAIConfig(args);
            
            // Initialize base client
            OpenAIClient client = createOpenAIClient(config);
            
            // Example of completions with proper placeholder replacement
            demonstrateCompletions(client, config);
            
            // Example of chat completions
            demonstrateChatCompletions(client, config);
            
            // Example of audio transcription
            demonstrateAudioTranscription(client, config);
            
            // Example of speech generation
            demonstrateSpeechGeneration(client, config);
            
            // Example of file operations
            demonstrateFileOperations(client, config);
            
            // Example of batch operations
            demonstrateBatchOperations(client, config);
            
            // Example of upload operations
            demonstrateUploadOperations(client, config);
            
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static OpenAIClient createOpenAIClient(OpenAIConfig config) {
        ProxyOptions proxyOptions = new ProxyOptions(
            ProxyOptions.Type.HTTP,
            new InetSocketAddress(config.proxyConfig.hostname, config.proxyConfig.port))
            .setCredentials(config.proxyConfig.username, config.proxyConfig.password);

        return new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(config.apiKey))
            .endpoint(config.endpoint)
            .clientOptions(new HttpClientOptions().setProxyOptions(proxyOptions))
            .buildClient();
    }

    private static void demonstrateCompletions(OpenAIClient client, OpenAIConfig config) {
        List<String> prompt = new ArrayList<>();
        prompt.add("Say this is a test");

        Completions completions = client.getCompletions(
            config.deploymentModelName,
            new CompletionsOptions(prompt));

        System.out.printf("Model ID=%s is created at %s.%n",
            completions.getId(),
            completions.getCreatedAt());

        for (Choice choice : completions.getChoices()) {
            System.out.printf("Index: %d, Text: %s.%n",
                choice.getIndex(),
                choice.getText());
        }
    }

    private static void demonstrateChatCompletions(OpenAIClient client, OpenAIConfig config) {
        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatRequestSystemMessage(
            "You are a helpful assistant. You will talk like a pirate."));
        chatMessages.add(new ChatRequestUserMessage("Can you help me?"));

        ChatCompletions chatCompletions = client.getChatCompletions(
            config.deploymentModelName,
            new ChatCompletionsOptions(chatMessages));

        for (ChatChoice choice : chatCompletions.getChoices()) {
            ChatResponseMessage message = choice.getMessage();
            System.out.printf("Index: %d, Chat Role: %s.%n",
                choice.getIndex(),
                message.getRole());
            System.out.println("Message: " + message.getContent());
        }
    }

    private static void demonstrateAudioTranscription(OpenAIClient client, OpenAIConfig config) throws IOException {
        Path filePath = Paths.get(config.fileConfig.audioFilePath, config.fileConfig.audioFileName);
        byte[] file = BinaryData.fromFile(filePath).toBytes();

        AudioTranscriptionOptions transcriptionOptions = new AudioTranscriptionOptions(file)
            .setResponseFormat(AudioTranscriptionFormat.JSON);

        AudioTranscription transcription = client.getAudioTranscription(
            config.deploymentModelName,
            config.fileConfig.audioFileName,
            transcriptionOptions);

        System.out.println("Transcription: " + transcription.getText());
    }

    private static void demonstrateSpeechGeneration(OpenAIClient client, OpenAIConfig config) throws IOException {
        SpeechGenerationOptions speechOptions = new SpeechGenerationOptions(
            "Today is a wonderful day to build something people love!",
            SpeechVoice.ALLOY);

        BinaryData speech = client.generateSpeechFromText(
            config.deploymentModelId,
            speechOptions);

        Path speechPath = Paths.get(config.fileConfig.speechOutputPath, "speech.wav");
        Files.write(speechPath, speech.toBytes());
    }

    private static void demonstrateFileOperations(OpenAIClient client, OpenAIConfig config) throws IOException {
        FileDetails fileDetails = new FileDetails(
            BinaryData.fromFile(Paths.get(config.fileConfig.batchTasksPath, "batch_tasks.jsonl")),
            "batch_tasks.jsonl");

        OpenAIFile file = client.uploadFile(fileDetails, FilePurpose.BATCH);
        System.out.println("Uploaded file ID: " + file.getId());
    }

    private static void demonstrateBatchOperations(OpenAIClient client, OpenAIConfig config) {
        Batch batch = client.createBatch(
            new BatchCreateRequest("/chat/completions", config.fileConfig.serviceFileId, "24h"));

        System.out.println("Created batch ID: " + batch.getId());
        byte[] fileContent = client.getFileContent(batch.getOutputFileId());
        System.out.println("Retrieved batch output file content length: " + fileContent.length);
    }

    private static void demonstrateUploadOperations(OpenAIClient client, OpenAIConfig config) throws IOException {
        long totalFilesSize = Files.size(Paths.get(config.fileConfig.batchTasksPath));
        CreateUploadRequest createUploadRequest = new CreateUploadRequest(
            "upload.txt",
            CreateUploadRequestPurpose.ASSISTANTS,
            totalFilesSize,
            "text/plain");

        Upload upload = client.createUpload(createUploadRequest);

        UploadPart uploadPart = client.addUploadPart(
            upload.getId(),
            new AddUploadPartRequest(
                new DataFileDetails(BinaryData.fromFile(Paths.get(config.fileConfig.batchTasksPath)))
                    .setFilename("part1.txt")));

        CompleteUploadRequest completeUploadRequest = new CompleteUploadRequest(
            Arrays.asList(uploadPart.getId()));

        Upload completeUpload = client.completeUpload(
            upload.getId(),
            completeUploadRequest);

        System.out.println("Upload completed, upload ID = " + completeUpload.getId());
    }
}