import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;

public class HackerNewsIndexer {

    private static final String TOP_STORIES_URL = "https://hacker-news.firebaseio.com/v0/topstories.json";
    private static final String ITEM_URL = "https://hacker-news.firebaseio.com/v0/item/";

    public static void main(String[] args) throws IOException {
        String keyword = "AI"; // Podes mudar ou pedir input ao utilizador

        List<String> matchingUrls = new ArrayList<>();

        // Vai buscar os IDs das top stories
        JSONArray topStoryIds = new JSONArray(fetchData(TOP_STORIES_URL));

        int count = 0;
        for (int i = 0; i < topStoryIds.length() && count < 30; i++) { // Vamos limitar para não ser lento
            int id = topStoryIds.getInt(i);
            JSONObject story = new JSONObject(fetchData(ITEM_URL + id + ".json"));

            if (story.has("title") && story.has("url")) {
                String title = story.getString("title");
                String url = story.getString("url");

                if (title.toLowerCase().contains(keyword.toLowerCase())) {
                    matchingUrls.add(url);
                    System.out.println("Match: " + title + " -> " + url);
                }
            }
            count++;
        }

        System.out.println("\nEncontradas " + matchingUrls.size() + " histórias relacionadas com '" + keyword + "'");
    }

    private static String fetchData(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        return response.toString();
    }
}
