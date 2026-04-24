import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuizDataFetcher {

  public static void main(String[] args) {
      String baseUrl="https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz";
       String regNo = "RA2311003011238"; 
      
    HttpClient client = HttpClient.newHttpClient();
     StringBuilder allData = new StringBuilder();

      for(int i=0; i<10; i++){
          try {
              String url = baseUrl + "/messages?regNo=" + regNo + "&poll=" + i; 
               HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
             HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
              allData.append(response.body());
              
              if(i < 9) {
                  Thread.sleep(5000); 
              }
          } catch(Exception e) {
           e.printStackTrace();
          }
      }

      Map<String, Integer> map = new HashMap<>();
      Pattern p = Pattern.compile("\"roundId\":\"(.*?)\",\"participant\":\"(.*?)\",\"score\":(\\d+)");
       Matcher m = p.matcher(allData.toString());

      while (m.find()) {
        String rId = m.group(1);
         String part = m.group(2);
          int sc = Integer.parseInt(m.group(3));
          String key = rId + "_" + part; 
           map.put(key, sc); 
      }

      Map<String, Integer> totals = new HashMap<>();
      for (Map.Entry<String, Integer> entry : map.entrySet()) {
          String pName = entry.getKey().split("_")[1];
           int val = entry.getValue();
          totals.put(pName, totals.getOrDefault(pName, 0) + val);
      }

     List<Map.Entry<String, Integer>> board = new ArrayList<>(totals.entrySet());
      board.sort((x, y) -> y.getValue().compareTo(x.getValue()));

      try {
          String payload = "{\n\"regNo\": \"" + regNo + "\",\n\"leaderboard\": [\n";
          for (int j = 0; j < board.size(); j++) {
               payload += "{\"participant\": \"" + board.get(j).getKey() + "\", \"totalScore\": " + board.get(j).getValue() + "}";
              if (j < board.size() - 1) {
                   payload += ",\n";
              } else {
                  payload += "\n";
              }
          }
          payload += "]\n}";

           HttpRequest req = HttpRequest.newBuilder()
                  .uri(URI.create(baseUrl + "/submit"))
                  .header("Content-Type", "application/json")
                  .POST(HttpRequest.BodyPublishers.ofString(payload))
                  .build();

          HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
          System.out.println(res.body());

      } catch (Exception e) {
            e.printStackTrace();
      }
  }
}
