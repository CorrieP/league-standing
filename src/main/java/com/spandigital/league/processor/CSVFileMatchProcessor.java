package com.spandigital.league.processor;

import com.spandigital.league.match.dto.MatchResult;
import lombok.experimental.SuperBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

@SuperBuilder
public class CSVFileMatchProcessor extends AbstractMatchProcessor {

    public CSVFileMatchProcessor(Scanner scanner) {
        super(scanner);
    }

    @Override
    public List<MatchResult> processInput() {
        System.out.println("Enter CSV file path:");
        String filePath = getScanner().nextLine();

        try (BufferedReader fileReader = new BufferedReader(new FileReader(filePath))) {
            String csvLine;

            while ((csvLine = fileReader.readLine()) != null) {
                processLine(csvLine);
            }
        } catch (Exception e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            return Collections.emptyList();
        }
        return getResults();
    }
}
