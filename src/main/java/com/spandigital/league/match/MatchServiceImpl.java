package com.spandigital.league.match;

import com.spandigital.league.match.dto.Match;
import com.spandigital.league.match.dto.MatchResult;
import com.spandigital.league.processor.CSVFileMatchProcessor;
import com.spandigital.league.processor.StdInMatchProcessor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


@Service
public class MatchServiceImpl implements MatchService {

    private final Scanner scanner = new Scanner(System.in);

    public void leagueResultInput(String... args) {
        System.out.println("Welcome to the Span Digital League");
        System.out.println("Please input team results");
        System.out.println("Choose input method:");
        System.out.println("1. Standard Input");
        System.out.println("2. CSV File");

        List<MatchResult> results;
        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline

        switch (choice) {
            case 1:
                var stdInProcessor = StdInMatchProcessor.builder()
                                                        .scanner(scanner)
                                                        .build();
                results = stdInProcessor.processInput();
                break;

            case 2:
                var csvProcessor = CSVFileMatchProcessor.builder()
                                                        .scanner(scanner)
                                                        .build();
                results = csvProcessor.processInput();
                break;

            default:
                System.out.println("Invalid choice. Exiting.");
                return;
        }

        System.out.println("Processing " + results.size() + " match results...");
        processResults(results);
        System.out.println("Processing complete!");
    }

    /**
     * Updates the points for a team in the standings map.
     * The compute() method atomically updates the map entry by applying the provided
     * remapping function. If the team doesn't exist in the map yet (v is null),
     * the team is added with the initial pointsToAdd value. If the team already exists,
     * the new points are added to their current total.
     *
     * @param teamPoints The map containing team standings
     * @param team The name of the team to update
     * @param pointsToAdd The number of points to add to the team's current total
     */
    private void updateTeamPoints(Map<String, Integer> teamPoints, String team, int pointsToAdd) {
        teamPoints.compute(team, (k, v) -> (v == null) ? pointsToAdd : v + pointsToAdd);
    }

    public void processResults(List<MatchResult> results) {
        // calculate team points
        var teamPoints = new HashMap<String, Integer>();

        for (MatchResult result : results) {
            Match match = result.match();
            var teamA = match.teamA();
            var teamB = match.teamB();

            // Update points based on match result
            if (teamA.score() > teamB.score()) {
                // Team A wins
                updateTeamPoints(teamPoints, teamA.name(), 3);
                updateTeamPoints(teamPoints, teamB.name(), 0);
            } else if (teamA.score() < teamB.score()) {
                // Team B wins
                updateTeamPoints(teamPoints, teamB.name(), 3);
                updateTeamPoints(teamPoints, teamA.name(), 0);
            } else {
                // Draw
                updateTeamPoints(teamPoints, teamA.name(), 1);
                updateTeamPoints(teamPoints, teamB.name(), 1);
            }

            System.out.println("Processed: " + match);
        }

        // Print team standings
        System.out.println("\nTeam Standings:");
        teamPoints.entrySet().stream()
                  .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                                   .thenComparing(Map.Entry.comparingByKey()))
                  .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue() + " pts"));
    }
}