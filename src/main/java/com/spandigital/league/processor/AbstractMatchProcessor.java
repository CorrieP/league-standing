package com.spandigital.league.processor;

import com.spandigital.league.match.dto.Match;
import com.spandigital.league.match.dto.MatchResult;
import com.spandigital.league.match.dto.TeamScore;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@SuperBuilder
@Getter
public abstract class AbstractMatchProcessor {

    private final List<MatchResult> results = new ArrayList<>();
    private final Scanner scanner;

    protected AbstractMatchProcessor(Scanner scanner) {
        this.scanner = scanner;
    }

    public abstract List<MatchResult> processInput();

    protected void processLine(String line) {
        var parts = line.split(",");
        if (parts.length == 2) {

            var teamScoreA = extractTeamScore(parts[0]);
            var teamScoreB = extractTeamScore(parts[1]);

            Match match = new Match(
                    teamScoreA,
                    teamScoreB
            );

            results.add(new MatchResult(match));
        } else {
            System.out.println("Invalid format, please use: TeamA ScoreA, TeamB ScoreB");
        }
    }

    private TeamScore extractTeamScore(String part) {
        // Extract just the score (last set of digits in the string)
        String scoreStr = part.replaceAll(".*?(\\d+)\\s*$", "$1");
        int score;
        try {
            score = Integer.parseInt(scoreStr);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid score format: " + part);
        }

        // Extract team name (everything except the last set of digits)
        String name = part.replaceAll("\\s*\\d+\\s*$", "").trim();

        return TeamScore.builder()
                        .score(score)
                        .name(name)
                        .build();
    }
}
