import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuizDataFetcher {

    public static void main(String[] args) {
        String baseUrl = "https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz";
        String regNo = "RA2311003011238"; // Using your exact registration number
        
        HttpClient client = HttpClient.newHttpClient();
        StringBuilder allData = new StringBuilder();

        System.out.println("--- PHASE 1: FETCHING DATA ---");
        for (int pollIndex = 0; pollIndex < 10; pollIndex++) {
            try {
                String url = baseUrl + "/messages?regNo=" + regNo + "&poll=" + pollIndex; 
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                System.out.println("Poll " + pollIndex + " Completed");
                allData.append(response.body());

                if (pollIndex < 9) Thread.sleep(5000); 

            } catch (Exception e) {
                System.err.println("Error on poll " + pollIndex + ": " + e.getMessage());
            }
        }

        System.out.println("\n--- PHASE 2: PROCESSING DATA ---");
        // We use a Map to store unique events. The Key is "RoundId_Participant" to stop duplicates.
        Map<String, Integer> uniqueEvents = new HashMap<>();
        
        // This pattern searches the text for the exact format the API outputs
        Pattern pattern = Pattern.compile("\"roundId\":\"(.*?)\",\"participant\":\"(.*?)\",\"score\":(\\d+)");
        Matcher matcher = pattern.matcher(allData.toString());

        while (matcher.find()) {
            String roundId = matcher.group(1);
            String participant = matcher.group(2);
            int score = Integer.parseInt(matcher.group(3));
            
            // This string acts as our unique fingerprint (e.g., "R1_George")
            String uniqueKey = roundId + "_" + participant; 
            uniqueEvents.put(uniqueKey, score); // HashMaps automatically overwrite duplicates
        }

        // Now aggregate the scores per participant
        Map<String, Integer> participantTotals = new HashMap<>();
        for (Map.Entry<String, Integer> entry : uniqueEvents.entrySet()) {
            String participant = entry.getKey().split("_")[1];
            int score = entry.getValue();
            participantTotals.put(participant, participantTotals.getOrDefault(participant, 0) + score);
        }

        // Sort participants by score (highest first)
        List<Map.Entry<String, Integer>> leaderboard = new ArrayList<>(participantTotals.entrySet());
        leaderboard.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        System.out.println("Calculated Leaderboard:");
        for (Map.Entry<String, Integer> entry : leaderboard) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        System.out.println("\n--- PHASE 3: SUBMISSION ---");
        try {
            // Manually build the JSON string for the final submission
            StringBuilder jsonPayload = new StringBuilder();
            jsonPayload.append("{\n");
            jsonPayload.append("  \"regNo\": \"").append(regNo).append("\",\n");
            jsonPayload.append("  \"leaderboard\": [\n");
            
            for (int i = 0; i < leaderboard.size(); i++) {
                jsonPayload.append("    {\"participant\": \"").append(leaderboard.get(i).getKey())
                           .append("\", \"totalScore\": ").append(leaderboard.get(i).getValue()).append("}");
                if (i < leaderboard.size() - 1) jsonPayload.append(",");
                jsonPayload.append("\n");
            }
            jsonPayload.append("  ]\n");
            jsonPayload.append("}");

            System.out.println("Sending Payload:\n" + jsonPayload.toString());

            HttpRequest submitRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/submit"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload.toString()))
                    .build();

            HttpResponse<String> submitResponse = client.send(submitRequest, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("\nSubmission Response Code: " + submitResponse.statusCode());
            System.out.println("Submission Response Body: " + submitResponse.body());

        } catch (Exception e) {
             System.err.println("Error during submission: " + e.getMessage());
        }
    }
}