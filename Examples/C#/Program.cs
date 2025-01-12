using Azure;
using Azure.AI.OpenAI;
using OpenAI.Images;
using static System.Environment;

{
    string endpoint = GetEnvironmentVariable("AZURE_OPENAI_ENDPOINT");
    string key = GetEnvironmentVariable("AZURE_OPENAI_API_KEY");


    AzureOpenAIClient azureClient = new(
        new Uri(endpoint),
        new AzureKeyCredential(key));

    // This must match the custom deployment name you chose for your model
    ImageClient chatClient = azureClient.GetImageClient("dalle-3");

    var imageGeneration = await chatClient.GenerateImageAsync(
            "a happy monkey sitting in a tree, in watercolor",
            new ImageGenerationOptions()
            {
                Size = GeneratedImageSize.W1024xH1024
            }
        );

    Console.WriteLine(imageGeneration.Value.ImageUri);
}
{
    using Azure;
    using Azure.AI.OpenAI.Assistants;

    // Assistants is a beta API and subject to change
    // Acknowledge its experimental status by suppressing the matching warning.
    string endpoint = Environment.GetEnvironmentVariable("AZURE_OPENAI_ENDPOINT");
    string key = Environment.GetEnvironmentVariable("AZURE_OPENAI_API_KEY");

    var openAIClient = new AzureOpenAIClient(new Uri(endpoint), new AzureKeyCredential(key));

    // Use for passwordless auth
    //var openAIClient = new AzureOpenAIClient(new Uri(endpoint), new DefaultAzureCredential()); 

    FileClient fileClient = openAIClient.GetFileClient();
    AssistantClient assistantClient = openAIClient.GetAssistantClient();

    // First, let's contrive a document we'll use retrieval with and upload it.
    using Stream document = BinaryData.FromString("""
                {
                    "description": "This document contains the sale history data for Contoso products.",
                    "sales": [
                        {
                            "month": "January",
                            "by_product": {
                                "113043": 15,
                                "113045": 12,
                                "113049": 2
                            }
                        },
                        {
                            "month": "February",
                            "by_product": {
                                "113045": 22
                            }
                        },
                        {
                            "month": "March",
                            "by_product": {
                                "113045": 16,
                                "113055": 5
                            }
                        }
                    ]
                }
                """).ToStream();

    OpenAIFileInfo salesFile = await fileClient.UploadFileAsync(
        document,
        "monthly_sales.json",
        FileUploadPurpose.Assistants);

    // Now, we'll create a client intended to help with that data
    AssistantCreationOptions assistantOptions = new()
    {
        Name = "Example: Contoso sales RAG",
        Instructions =
            "You are an assistant that looks up sales data and helps visualize the information based"
            + " on user queries. When asked to generate a graph, chart, or other visualization, use"
            + " the code interpreter tool to do so.",
        Tools =
                {
                    new FileSearchToolDefinition(),
                    new CodeInterpreterToolDefinition(),
                },
        ToolResources = new()
        {
            FileSearch = new()
            {
                NewVectorStores =
                        {
                            new VectorStoreCreationHelper([salesFile.Id]),
                        }
            }
        },
    };

    Assistant assistant = await assistantClient.CreateAssistantAsync(deploymentName, assistantOptions);

    // Create and run a thread with a user query about the data already associated with the assistant
    ThreadCreationOptions threadOptions = new()
    {
        InitialMessages = { "How well did product 113045 sell in February? Graph its trend over time." }
    };

    ThreadRun threadRun = await assistantClient.CreateThreadAndRunAsync(assistant.Id, threadOptions);

    // Check back to see when the run is done
    do
    {
        Thread.Sleep(TimeSpan.FromSeconds(1));
        threadRun = assistantClient.GetRun(threadRun.ThreadId, threadRun.Id);
    } while (!threadRun.Status.IsTerminal);

    // Finally, we'll print out the full history for the thread that includes the augmented generation
    AsyncCollectionResult<ThreadMessage> messages
        = assistantClient.GetMessagesAsync(
            threadRun.ThreadId,
            new MessageCollectionOptions() { Order = MessageCollectionOrder.Ascending });

    await foreach (ThreadMessage message in messages)
    {
        Console.Write($"[{message.Role.ToString().ToUpper()}]: ");
        foreach (MessageContent contentItem in message.Content)
        {
            if (!string.IsNullOrEmpty(contentItem.Text))
            {
                Console.WriteLine($"{contentItem.Text}");

                if (contentItem.TextAnnotations.Count > 0)
                {
                    Console.WriteLine();
                }

                // Include annotations, if any.
                foreach (TextAnnotation annotation in contentItem.TextAnnotations)
                {
                    if (!string.IsNullOrEmpty(annotation.InputFileId))
                    {
                        Console.WriteLine($"* File citation, file ID: {annotation.InputFileId}");
                    }
                    if (!string.IsNullOrEmpty(annotation.OutputFileId))
                    {
                        Console.WriteLine($"* File output, new file ID: {annotation.OutputFileId}");
                    }
                }
            }
            if (!string.IsNullOrEmpty(contentItem.ImageFileId))
            {
                OpenAIFileInfo imageInfo = await fileClient.GetFileAsync(contentItem.ImageFileId);
                BinaryData imageBytes = await fileClient.DownloadFileAsync(contentItem.ImageFileId);
                using FileStream stream = File.OpenWrite($"{imageInfo.Filename}.png");
                imageBytes.ToStream().CopyTo(stream);

                Console.WriteLine($"<image: {imageInfo.Filename}.png>");
            }
        }
        Console.WriteLine();
    }
}
{
    using System;
    using Azure.AI.OpenAI;
    using System.ClientModel;
    using Azure.AI.OpenAI.Chat;
    using OpenAI.Chat;
    using static System.Environment;

    string azureOpenAIEndpoint = GetEnvironmentVariable("AZURE_OPENAI_ENDPOINT");
    string azureOpenAIKey = GetEnvironmentVariable("AZURE_OPENAI_API_KEY");
    string deploymentName = GetEnvironmentVariable("AZURE_OPENAI_DEPLOYMENT_ID");
    string searchEndpoint = GetEnvironmentVariable("AZURE_AI_SEARCH_ENDPOINT");
    string searchKey = GetEnvironmentVariable("AZURE_AI_SEARCH_API_KEY");
    string searchIndex = GetEnvironmentVariable("AZURE_AI_SEARCH_INDEX");

    AzureOpenAIClient azureClient = new(
                new Uri(azureOpenAIEndpoint),
                new ApiKeyCredential(azureOpenAIKey));
    ChatClient chatClient = azureClient.GetChatClient(deploymentName);

    // Extension methods to use data sources with options are subject to SDK surface changes. Suppress the
    // warning to acknowledge and this and use the subject-to-change AddDataSource method.
    #pragma warning disable AOAI001

    ChatCompletionOptions options = new();
    options.AddDataSource(new AzureSearchChatDataSource()
    {
        Endpoint = new Uri(searchEndpoint),
        IndexName = searchIndex,
        Authentication = DataSourceAuthentication.FromApiKey(searchKey),
    });

    ChatCompletion completion = chatClient.CompleteChat(
        [
            new UserChatMessage("What health plans are available?"),
                ],
        options);

    ChatMessageContext onYourDataContext = completion.GetMessageContext();

    if (onYourDataContext?.Intent is not null)
    {
        Console.WriteLine($"Intent: {onYourDataContext.Intent}");
    }
    foreach (ChatCitation citation in onYourDataContext?.Citations ?? [])
    {
        Console.WriteLine($"Citation: {citation.Content}");
    }
}
{
    using Azure;
    using Azure.AI.OpenAI;
    using Azure.Identity; // Required for Passwordless auth

    var endpoint = new Uri("YOUR_OPENAI_ENDPOINT");
    var credentials = new AzureKeyCredential("YOUR_OPENAI_KEY");
    // var credentials = new DefaultAzureCredential(); // Use this line for Passwordless auth
    var deploymentName = "whisper"; // Default deployment name, update with your own if necessary
    var audioFilePath = "YOUR_AUDIO_FILE_PATH";

    var openAIClient = new AzureOpenAIClient(endpoint, credentials);

    var audioClient = openAIClient.GetAudioClient(deploymentName);

    var result = await audioClient.TranscribeAudioAsync(audioFilePath);

    Console.WriteLine("Transcribed text:");
    foreach (var item in result.Value.Text)
    {
        Console.Write(item);
    }
}
{
    using Azure;
    using Azure.AI.OpenAI;
    using Azure.Identity; // Required for Passwordless auth

    var endpoint = new Uri(
        Environment.GetEnvironmentVariable("YOUR_OPENAI_ENDPOINT") ?? throw new ArgumentNullException());
    var credentials = new DefaultAzureCredential();

    // Use this line for key auth
    // var credentials = new AzureKeyCredential(
    //    Environment.GetEnvironmentVariable("YOUR_OPENAI_KEY") ?? throw new ArgumentNullException());

    var deploymentName = "tts"; // Default deployment name, update with your own if necessary
    var speechFilePath = "YOUR_AUDIO_FILE_PATH";

    var openAIClient = new AzureOpenAIClient(endpoint, credentials);
    var audioClient = openAIClient.GetAudioClient(deploymentName);

    var result = await audioClient.GenerateSpeechAsync(
                    "the quick brown chicken jumped over the lazy dogs");

    Console.WriteLine("Streaming response to ${speechFilePath}");
    await File.WriteAllBytesAsync(speechFilePath, result.Value.ToArray());
    Console.WriteLine("Finished streaming");
}
{
    using Azure;
    using Azure.AI.OpenAI;
    using static System.Environment;

    string endpoint = GetEnvironmentVariable("AZURE_OPENAI_ENDPOINT");
    string key = GetEnvironmentVariable("AZURE_OPENAI_API_KEY");

    var client = new OpenAIClient(
        new Uri(endpoint),
        new AzureKeyCredential(key));

    CompletionsOptions completionsOptions = new()
    {
        DeploymentName = "gpt-35-turbo-instruct",
        Prompts = { "When was Microsoft founded?" },
    };

    Response<Completions> completionsResponse = client.GetCompletions(completionsOptions);
    string completion = completionsResponse.Value.Choices[0].Text;
    Console.WriteLine($"Chatbot: {completion}");
}
