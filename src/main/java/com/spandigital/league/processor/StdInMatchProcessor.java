package com.spandigital.league.processor;

import com.spandigital.league.match.dto.MatchResult;
import lombok.experimental.SuperBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Scanner;

@SuperBuilder
public class StdInMatchProcessor extends AbstractMatchProcessor {

    public StdInMatchProcessor(Scanner scanner) {
        super(scanner);
    }

    public List<MatchResult> processInput() {
        System.out.println("Enter match results (format: TeamA,ScoreA,TeamB,ScoreB)");
        System.out.println("Enter 'done' when finished");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while (!(line = reader.readLine()).equalsIgnoreCase("done")) {
                processLine(line);
            }
        } catch (IOException e) {
            System.out.println("Unable to process input: " + e.getMessage());
        }
        return getResults();
    }
}
