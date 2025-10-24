package org.example.football.statistics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FootballStatistics {

    private Map<String, TeamStats> teamsData = new HashMap<>();

    public static void main(String[] args) {
        FootballStatistics stats = new FootballStatistics();
        stats.processMessages("messages.txt");
    }

    public void processMessages(String filename) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                processMessage(line.trim());
            }

            reader.close();
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    private void processMessage(String jsonMessage) {
        try {
            JSONObject message = new JSONObject(jsonMessage);
            String type = message.getString("type");

            if ("RESULT".equals(type)) {
                handleResultMessage(message);
            } else if ("GET_STATISTICS".equals(type)) {
                handleStatisticsMessage(message);
            }
        } catch (JSONException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
    }

    private void handleResultMessage(JSONObject message) {
        JSONObject result = message.getJSONObject("result");

        String homeTeam = result.getString("home_team");
        String awayTeam = result.getString("away_team");
        int homeScore = result.getInt("home_score");
        int awayScore = result.getInt("away_score");

        updateTeamStats(homeTeam, homeScore, awayScore, true);
        updateTeamStats(awayTeam, awayScore, homeScore, false);

        printSimplifiedStats(homeTeam);
        printSimplifiedStats(awayTeam);
    }

    private void handleStatisticsMessage(JSONObject message) {
        JSONObject statsRequest = message.getJSONObject("get_statistics");
        JSONArray teamsArray = statsRequest.getJSONArray("teams");

        for (int i = 0; i < teamsArray.length(); i++) {
            String teamName = teamsArray.getString(i);
            printDetailedStats(teamName);
        }
    }

    private void updateTeamStats(String teamName, int goalsScored, int goalsConceded, boolean isHome) {
        TeamStats stats = teamsData.getOrDefault(teamName, new TeamStats());

        stats.played++;
        stats.goalsScored += goalsScored;
        stats.goalsConceded += goalsConceded;

        char result;
        if (goalsScored > goalsConceded) {
            stats.points += 3;
            result = 'W';
        } else if (goalsScored == goalsConceded) {
            stats.points += 1;
            result = 'D';
        } else {
            result = 'L';
        }

        stats.recentResults.add(result);
        if (stats.recentResults.size() > 3) {
            stats.recentResults.remove(0);
        }

        teamsData.put(teamName, stats);
    }

    private void printSimplifiedStats(String teamName) {
        TeamStats stats = teamsData.get(teamName);
        System.out.printf("%s %d %d %d %d%n",
                teamName, stats.played, stats.points, stats.goalsScored, stats.goalsConceded);
    }

    private void printDetailedStats(String teamName) {
        TeamStats stats = teamsData.get(teamName);

        StringBuilder form = new StringBuilder();
        for (char result : stats.recentResults) {
            form.append(result);
        }

        double avgGoals = 0.0;
        if (stats.played > 0) {
            avgGoals = (double)(stats.goalsScored + stats.goalsConceded) / stats.played;
            avgGoals = Math.round(avgGoals * 100.0) / 100.0;
        }

        System.out.printf("%s %s %.2f %d %d %d %d%n",
                teamName, form.toString(), avgGoals, stats.played, stats.points,
                stats.goalsScored, stats.goalsConceded);
    }

    private static class TeamStats {
        int played = 0;
        int points = 0;
        int goalsScored = 0;
        int goalsConceded = 0;
        List<Character> recentResults = new ArrayList<>();
    }
}
