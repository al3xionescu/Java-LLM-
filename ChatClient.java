import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ChatClient {
    private static final ObjectMapper M = new ObjectMapper();
    private final OkHttpClient http = new OkHttpClient();

    private final String baseUrl;   // e.g. https://api.openai.com/v1
    private final String apiKey;    // e.g. sk-...
    private final String model;     // e.g. gpt-4o-mini or another provider's model id

    public ChatClient(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String chat(String system, String user) throws IOException {
        // Build OpenAI-compatible messages array
        var messages = List.of(
            Map.of("role", "system", "content", system),
            Map.of("role", "user", "content", user)
        );

        var bodyNode = M.createObjectNode();
        bodyNode.put("model", model);
        bodyNode.set("messages", M.valueToTree(messages));
        // optional knobs
        bodyNode.put("temperature", 0.2);
        bodyNode.put("max_tokens", 300);

        Request request = new Request.Builder()
            .url(baseUrl + "/chat/completions")
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(M.writeValueAsBytes(bodyNode), MediaType.get("application/json")))
            .build();

        try (Response resp = http.newCall(request).execute()) {
            if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code() + ": " + resp.message());
            JsonNode json = M.readTree(resp.body().byteStream());
            return json.path("choices").get(0).path("message").path("content").asText();
        }
    }

    public static void main(String[] args) throws Exception {
        String base = System.getenv().getOrDefault("LLM_BASE_URL", "https://api.openai.com/v1");
        String key  = System.getenv("LLM_API_KEY");
        String model= System.getenv().getOrDefault("LLM_MODEL", "gpt-4o-mini");

        ChatClient client = new ChatClient(base, key, model);
        String answer = client.chat(
            "You are a concise assistant.",
            "In 2 bullets, explain what vector embeddings are."
        );
        System.out.println(answer);
    }
}
