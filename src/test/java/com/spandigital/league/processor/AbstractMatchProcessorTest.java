package com.spandigital.league.processor;

import com.spandigital.league.match.dto.Match;
import com.spandigital.league.match.dto.MatchResult;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AbstractMatchProcessorTest {

    @Mock
    private Scanner mockScanner;

    // Concrete implementation for testing the abstract class
    @SuperBuilder
    private static class TestMatchProcessor extends AbstractMatchProcessor {
        public TestMatchProcessor(Scanner scanner) {
            super(scanner);
        }

        @Override
        public List<MatchResult> processInput() {
            // Simple implementation for testing
            return getResults();
        }
    }

    private AbstractMatchProcessor processor;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        processor = new TestMatchProcessor(mockScanner);
        // Redirect System.out to capture output messages
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void processLine_ValidFormat_CreatesMatchResult() {
        // Arrange
        String validLine = "Lions 3,Snakes 1";

        // Act
        processor.processLine(validLine);

        // Assert
        List<MatchResult> results = processor.getResults();
        assertEquals(1, results.size(), "Should have one match result");

        MatchResult matchResult = results.getFirst();
        Match match = matchResult.match();

        assertEquals("Lions", match.teamA().name(), "Team A name should be Lions");
        assertEquals(3, match.teamA().score(), "Team A score should be 3");
        assertEquals("Snakes", match.teamB().name(), "Team B name should be Snakes");
        assertEquals(1, match.teamB().score(), "Team B score should be 1");
    }

    @Test
    void processLine_HandlesWhitespace_CreatesMatchResultCorrectly() {
        // Arrange
        String lineWithWhitespace = "  Lions  3 , Snakes  1  ";

        // Act
        processor.processLine(lineWithWhitespace);

        // Assert
        List<MatchResult> results = processor.getResults();
        assertEquals(1, results.size());

        MatchResult matchResult = results.getFirst();
        Match match = matchResult.match();

        assertEquals("Lions", match.teamA().name(), "Team A name should be trimmed");
        assertEquals("Snakes", match.teamB().name(), "Team B name should be trimmed");
    }

    @Test
    void processLine_MultipleLines_AccumulatesResults() {
        // Arrange
        String line1 = "Lions 3,Snakes 1";
        String line2 = "Tarantulas 1,FC Awesome 0";

        // Act
        processor.processLine(line1);
        processor.processLine(line2);

        // Assert
        List<MatchResult> results = processor.getResults();
        assertEquals(2, results.size(), "Should have two match results");

        MatchResult result1 = results.getFirst();
        assertEquals("Lions", result1.match().teamA().name());
        assertEquals(3, result1.match().teamA().score());
        assertEquals("Snakes", result1.match().teamB().name());

        MatchResult result2 = results.get(1);
        assertEquals("Tarantulas", result2.match().teamA().name());
        assertEquals("FC Awesome", result2.match().teamB().name());
    }

    @Test
    void processLine_TeamNameWithNumbers_HandlesCorrectly() {
        // Arrange
        String lineWithNumbersInName = "Team99 3,FC123 1";

        // Act
        processor.processLine(lineWithNumbersInName);

        // Assert
        List<MatchResult> results = processor.getResults();
        assertEquals(1, results.size());

        Match match = results.getFirst().match();
        assertEquals("Team99", match.teamA().name());
        assertEquals(3, match.teamA().score());
        assertEquals("FC123", match.teamB().name());
        assertEquals(1, match.teamB().score());
    }

    // Negative Tests

    @Test
    void processLine_InvalidFormat_DoesNotAddResult() {
        // Arrange
        String invalidLine = "Lions 3,Snakes 1,Tigers 2";

        // Act
        processor.processLine(invalidLine);

        // Assert
        List<MatchResult> results = processor.getResults();
        assertTrue(results.isEmpty(), "No results should be added for invalid format");
        assertTrue(outputStream.toString().contains("Invalid format"),
                "Should display error message for invalid format");
    }

    @Test
    void processLine_MissingScore_ThrowsNumberFormatException() {
        // Arrange
        String missingScore = "Lions,Snakes 1";

        // Act & Assert
        assertThrows(NumberFormatException.class, () -> processor.processLine(missingScore),
                "Should throw NumberFormatException when score is missing");
    }
    @Test
    void processLine_NonNumericScore_ThrowsNumberFormatException() {
        // Arrange
        String nonNumericScore = "Lions ABC,Snakes 1";

        // Act & Assert
        assertThrows(NumberFormatException.class, () -> processor.processLine(nonNumericScore),
                "Should throw NumberFormatException for non-numeric score");
    }

    @Test
    void processLine_EmptyString_PrintsInvalidFormat() {
        // Arrange
        String emptyLine = "";

        // Act
        processor.processLine(emptyLine);

        // Assert
        List<MatchResult> results = processor.getResults();
        assertTrue(results.isEmpty());
        assertTrue(outputStream.toString().contains("Invalid format"),
                "Should indicate invalid format for empty line");
    }

    @Test
    void processLine_NullInput_ThrowsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> processor.processLine(null),
                "Should throw NullPointerException for null input");
    }

    @Test
    void getScanner_ReturnsInjectedScanner() {
        // Act & Assert
        assertSame(mockScanner, processor.getScanner(),
                "getScanner() should return the scanner that was injected");
    }

    @Test
    void superBuilder_CreatesInstanceCorrectly() {
        // Arrange & Act
        AbstractMatchProcessor builtProcessor = TestMatchProcessor.builder()
                                                                  .scanner(mockScanner)
                                                                  .build();

        // Assert
        assertNotNull(builtProcessor);
        assertSame(mockScanner, builtProcessor.getScanner());
        assertNotNull(builtProcessor.getResults());
        assertTrue(builtProcessor.getResults().isEmpty());
    }
}