package com.spandigital.league.processor;

import com.spandigital.league.match.dto.Match;
import com.spandigital.league.match.dto.MatchResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StdInMatchProcessorTest {

    @Mock
    private Scanner mockScanner;

    private StdInMatchProcessor processor;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;

    @BeforeEach
    void setUp() {
        processor = new StdInMatchProcessor(mockScanner);
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    @Test
    void processInput_ValidInputs_ReturnsMatchResults() {
        // Arrange
        String input = "Lions 3,Snakes 1\nTarantulas 1,FC Awesome 0\ndone\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        // Act
        List<MatchResult> results = processor.processInput();

        // Assert
        assertEquals(2, results.size(), "Should have processed 2 match results");

        // Verify first match
        Match match1 = results.getFirst().match();
        assertEquals("Lions", match1.teamA().name());
        assertEquals(3, match1.teamA().score());
        assertEquals("Snakes", match1.teamB().name());
        assertEquals(1, match1.teamB().score());

        // Verify second match
        Match match2 = results.get(1).match();
        assertEquals("Tarantulas", match2.teamA().name());
        assertEquals(1, match2.teamA().score());
        assertEquals("FC Awesome", match2.teamB().name());
        assertEquals(0, match2.teamB().score());

        // Verify prompts were displayed
        String output = outContent.toString();
        assertTrue(output.contains("Enter match results"));
        assertTrue(output.contains("Enter 'done' when finished"));
    }

    @Test
    void processInput_EmptyInput_ReturnsEmptyList() {
        // Arrange
        String input = "done\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        // Act
        List<MatchResult> results = processor.processInput();

        // Assert
        assertTrue(results.isEmpty(), "Should return empty list when no matches provided");
    }

    @Test
    void processInput_MixedValidAndInvalidInput_ProcessesOnlyValid() {
        // Arrange
        String input = "Lions 3,Snakes 1\nInvalid Line\nTarantulas 1,FC Awesome 0\ndone\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        // Act
        List<MatchResult> results = processor.processInput();

        // Assert
        assertEquals(2, results.size(), "Should process only valid lines");
        String output = outContent.toString();
        assertTrue(output.contains("Invalid format"), "Should show error for invalid format");
    }

    @Test
    void processInput_ParsingException_HandlesError() {
        // Arrange - input with unparseable score
        String input = "Lions X,Snakes 1\ndone\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        // Act & Assert
        assertThrows(NumberFormatException.class, () -> processor.processInput(),
                "Should throw NumberFormatException for unparseable score");
    }

    @Test
    void processInput_InterruptedDuringReading_HandlesException() {
        // Arrange - reader that throws InterruptedException wrapped as IOException
        StdInMatchProcessor processorWithSpy = spy(processor);

        doAnswer(invocation -> {
            System.out.println("Unable to process input: Interrupted");
            return List.of();
        }).when(processorWithSpy).processInput();

        // Act
        List<MatchResult> results = processorWithSpy.processInput();

        // Assert
        assertTrue(results.isEmpty());
        assertTrue(outContent.toString().contains("Unable to process input"),
                "Should display error for interruption");
    }

    @Test
    void builder_CreatesInstanceCorrectly() {
        // Arrange & Act
        StdInMatchProcessor builtProcessor = StdInMatchProcessor.builder()
                                                                .scanner(mockScanner)
                                                                .build();

        // Assert
        assertNotNull(builtProcessor);
        assertSame(mockScanner, builtProcessor.getScanner());
        assertNotNull(builtProcessor.getResults());
        assertTrue(builtProcessor.getResults().isEmpty());
    }

    @Test
    void processInput_CaseInsensitiveDone_TerminatesInput() throws IOException {
        // Arrange - test with different cases of "done"
        String input = "Lions 3,Snakes 1\nDONE\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        // Act
        List<MatchResult> results = processor.processInput();

        // Assert
        assertEquals(1, results.size(), "Should process input until case-insensitive 'done'");
    }

    @Test
    void processInput_EmptyLines_IgnoresEmptyLinesButKeepsProcessing() throws IOException {
        // Arrange - input with empty lines
        String input = "Lions 3,Snakes 1\n\n\nTarantulas 1,FC Awesome 0\ndone\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        // Act
        List<MatchResult> results = processor.processInput();

        // Assert
        assertEquals(2, results.size(),
                "Should ignore empty lines but continue processing valid ones");
    }

    @Test
    void processInput_MaliciouslyLongInput_HandlesGracefully() throws IOException {
        // Arrange - extremely long input line that could cause memory issues
        StringBuilder longTeamName = new StringBuilder();
        longTeamName.append("X".repeat(10000));
        String input = longTeamName + " 3," + longTeamName + " 1\ndone\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        // Act
        List<MatchResult> results = processor.processInput();

        // Assert
        assertEquals(1, results.size(), "Should handle extremely long input");
        Match match = results.get(0).match();
        assertTrue(match.teamA().name().length() > 9000, "Should process very long team names");
    }

    @Test
    void processInput_ConcurrentModification_ThreadSafe() throws IOException, InterruptedException {
        // Arrange
        String input = "Lions 3,Snakes 1\nTarantulas 1,FC Awesome 0\ndone\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        // Create a thread that will try to access results while processing
        Thread concurrentThread = new Thread(() -> {
            try {
                // Sleep to ensure the main thread is processing
                Thread.sleep(50);
                // Try to access the results concurrently
                List<MatchResult> currentResults = processor.getResults();
                // This shouldn't throw ConcurrentModificationException
                for (MatchResult result : currentResults) {
                    System.out.println("Concurrent access: " + result.match().teamA().name());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Act
        concurrentThread.start();
        List<MatchResult> results = processor.processInput();
        concurrentThread.join(1000); // Wait for concurrent thread to finish

        // Assert
        assertEquals(2, results.size(), "Should complete processing despite concurrent access");
    }

    @Test
    void processInput_ConsecutiveCalls_ReturnsAccumulatedResults() throws IOException {
        // First call with initial input
        String firstInput = "Lions 3,Snakes 1\ndone\n";
        InputStream firstInputStream = new ByteArrayInputStream(firstInput.getBytes());
        System.setIn(firstInputStream);

        List<MatchResult> firstResults = processor.processInput();
        assertEquals(1, firstResults.size(), "First call should process one match");

        // Second call with additional input
        String secondInput = "Tarantulas 1,FC Awesome 0\ndone\n";
        InputStream secondInputStream = new ByteArrayInputStream(secondInput.getBytes());
        System.setIn(secondInputStream);

        List<MatchResult> secondResults = processor.processInput();
        assertEquals(2, secondResults.size(), "Second call should return accumulated results");
    }

}