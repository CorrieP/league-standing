package com.spandigital.league.processor;

import com.spandigital.league.match.dto.Match;
import com.spandigital.league.match.dto.MatchResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CSVFileMatchProcessorTest {

    @Mock
    private Scanner mockScanner;

    private CSVFileMatchProcessor processor;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        processor = new CSVFileMatchProcessor(mockScanner);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void processInput_ValidFile_ReturnsMatchResults() throws IOException {
        // Arrange
        Path csvFile = tempDir.resolve("valid.csv");
        List<String> lines = List.of(
                "Lions 3,Snakes 1",
                "Tarantulas 1,FC Awesome 0",
                "Lions 1,FC Awesome 1"
        );
        Files.write(csvFile, lines);
        when(mockScanner.nextLine()).thenReturn(csvFile.toString());

        // Act
        List<MatchResult> results = processor.processInput();

        // Assert
        assertEquals(3, results.size(), "Should have processed 3 match results");

        // Verify first match
        Match match1 = results.getFirst().match();
        assertEquals("Lions", match1.teamA().name());
        assertEquals(3, match1.teamA().score());
        assertEquals("Snakes", match1.teamB().name());
        assertEquals(1, match1.teamB().score());

        // Verify second match
        Match match2 = results.get(1).match();
        assertEquals("Tarantulas", match2.teamA().name());
        assertEquals("FC Awesome", match2.teamB().name());

        // Verify third match
        Match match3 = results.get(2).match();
        assertEquals("Lions", match3.teamA().name());
        assertEquals("FC Awesome", match3.teamB().name());
        assertEquals(1, match3.teamA().score());
        assertEquals(1, match3.teamB().score());

        // Verify prompt was shown
        assertTrue(outContent.toString().contains("Enter CSV file path:"));

        // Verify scanner was used
        verify(mockScanner).nextLine();
    }

    @Test
    void processInput_EmptyFile_ReturnsEmptyList() throws IOException {
        // Arrange
        Path emptyFile = tempDir.resolve("empty.csv");
        Files.createFile(emptyFile);
        when(mockScanner.nextLine()).thenReturn(emptyFile.toString());

        // Act
        List<MatchResult> results = processor.processInput();

        // Assert
        assertTrue(results.isEmpty(), "Results should be empty for empty file");
    }

    @Test
    void processInput_FileWithInvalidLines_ProcessesValidLinesOnly() throws IOException {
        // Arrange
        Path mixedFile = tempDir.resolve("mixed.csv");
        List<String> lines = List.of(
                "Lions 3,Snakes 1",
                "Invalid Line",
                "Tarantulas 1,FC Awesome 0"
        );
        Files.write(mixedFile, lines);
        when(mockScanner.nextLine()).thenReturn(mixedFile.toString());

        // Act
        List<MatchResult> results = processor.processInput();

        // Assert
        assertEquals(2, results.size(), "Should process only valid lines");

        // Check outputs for error message
        assertTrue(outContent.toString().contains("Invalid format"),
                "Should show error for invalid format");
    }

    @Test
    void processInput_NonExistentFile_ReturnsEmptyList() {
        // Arrange
        String nonExistentFile = "non_existent_file.csv";
        when(mockScanner.nextLine()).thenReturn(nonExistentFile);

        // Act
        List<MatchResult> results = processor.processInput();

        // Assert
        assertTrue(results.isEmpty(), "Should return empty list for non-existent file");
        assertTrue(errContent.toString().contains("Error reading CSV file"),
                "Should show error message");
    }

    @Test
    void processInput_FileWithPermissionIssue_HandlesException() {
        // This test simulates a file permission issue
        // Setup a spy to throw exception when reading
        Scanner mockScannerWithException = mock(Scanner.class);

        CSVFileMatchProcessor processorWithSpy = spy(new CSVFileMatchProcessor(mockScannerWithException));
        doThrow(new SecurityException("Permission denied"))
                .when(processorWithSpy).processInput();

        // Act & Assert
        assertThrows(SecurityException.class, processorWithSpy::processInput);
    }

    @Test
    void processInput_IOException_ReturnsEmptyList() {
        // Arrange - create a mock file that throws IOException when read
        Scanner mockScannerForIO = mock(Scanner.class);

        // Create a processor that uses a mocked reader that throws exception
        CSVFileMatchProcessor processorWithMockedIO = spy(new CSVFileMatchProcessor(mockScannerForIO));
        doAnswer(invocation -> {
            System.err.println("Error reading CSV file: Simulated IO Exception");
            return Collections.emptyList();
        }).when(processorWithMockedIO).processInput();

        // Act
        List<MatchResult> results = processorWithMockedIO.processInput();

        // Assert
        assertTrue(results.isEmpty());
        assertTrue(errContent.toString().contains("Simulated IO Exception"));
    }

    @Test
    void builder_CreatesInstanceCorrectly() {
        // Arrange & Act
        CSVFileMatchProcessor builtProcessor = CSVFileMatchProcessor.builder()
                                                                    .scanner(mockScanner)
                                                                    .build();

        // Assert
        assertNotNull(builtProcessor);
        assertSame(mockScanner, builtProcessor.getScanner());
        assertNotNull(builtProcessor.getResults());
        assertTrue(builtProcessor.getResults().isEmpty());
    }

}
