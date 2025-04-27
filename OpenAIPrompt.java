import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class OpenAIPrompt {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_API_KEY = "AQUI_A_TUA_API_KEY"; // <--- mete a tua API key aqui

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Escreve o que queres pesquisar: ");
        String userInput = scanner.nextLine();

        String prompt = "Explica de forma clara e resumida sobre: " + userInput;

        String explanation = askOpenAI(prompt);
        System.out.println("\n--- Resposta da OpenAI ---\n");
        System.out.println(explanation);
    }

    private static String askOpenAI(String prompt) throws IOException {
        URL url = new URL(OPENAI_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject json = new JSONObject();
        json.put("model", "gpt-3.5-turbo"); // ou gpt-4 se quiseres (mais caro)

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.put(message);

        json.put("messages", messages);

        OutputStream os = conn.getOutputStream();
        os.write(json.toString().getBytes());
        os.flush();
        os.close();

        InputStream inputStream = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JSONObject responseJson = new JSONObject(response.toString());
        return responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }
}
