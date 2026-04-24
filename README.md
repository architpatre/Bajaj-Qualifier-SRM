# Quiz Leaderboard System - Bajaj Finserv Health Qualifier

## Objective
This project consumes API responses from an external validator system, processes the data to handle distributed system duplicates, and aggregates scores to generate a correct leaderboard.

## Approach
1. **Data Fetching:** Utilized Java's built-in `HttpClient` to poll the API 10 times, maintaining the mandatory 5-second delay between requests.
2. **Deduplication:** Parsed the incoming data and utilized a Java `HashMap`. By creating a unique composite key (`roundId_participant`), I ensured that duplicate events appearing across multiple polls were strictly ignored.
3. **Aggregation:** Grouped the clean, unique data by participant and summed their scores to calculate the `totalScore`.
4. **Submission:** Formatted the aggregated leaderboard into a JSON payload and successfully submitted it via a POST request, matching the expected `submittedTotal`.

## How to Run
1. Ensure Java (JDK 11 or higher) is installed.
2. Compile and run the `QuizDataFetcher.java` file.
