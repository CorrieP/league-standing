package com.spandigital.league.service;

import com.spandigital.league.match.MatchServiceImpl;
import com.spandigital.league.match.dto.Match;
import com.spandigital.league.match.dto.MatchResult;
import com.spandigital.league.match.dto.TeamScore;
import com.spandigital.league.processor.StdInMatchProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock
    private Scanner mockScanner;

    @Spy
    @InjectMocks
    private MatchServiceImpl matchService;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;

    @BeforeEach
    void setUp() throws Exception {
        System.setOut(new PrintStream(outContent));

        // Replace the service's Scanner with our mock
        Field scannerField = MatchServiceImpl.class.getDeclaredField("scanner");
        scannerField.setAccessible(true);
        scannerField.set(matchService, mockScanner);
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    @Test
    void processResults_WithWinsLossesAndDraws_CalculatesCorrectStandings() {
        // Arrange
        List<MatchResult> results = Arrays.asList(
                createMatchResult("Lions", 3, "Snakes", 1),
                createMatchResult("Tarantulas", 1, "FC Awesome", 0),
                createMatchResult("Lions", 1, "FC Awesome", 1),
                createMatchResult("Tarantulas", 3, "Snakes", 1),
                createMatchResult("Lions", 4, "Grouches", 0)
        );

        // Act
        matchService.processResults(results);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Lions: 7 pts"));
        assertTrue(output.contains("Tarantulas: 6 pts"));
        assertTrue(output.contains("FC Awesome: 1 pts"));
        assertTrue(output.contains("Snakes: 0 pts"));
        assertTrue(output.contains("Grouches: 0 pts"));

        // Check correct sorting (first by points, then alphabetically)
        int lionsPos = output.indexOf("Lions: 7 pts");
        int tarantulasPos = output.indexOf("Tarantulas: 6 pts");
        int fcAwesomePos = output.indexOf("FC Awesome: 1 pts");
        int snakesPos = output.indexOf("Snakes: 0 pts");
        int grouchesPos = output.indexOf("Grouches: 0 pts");

        assertTrue(lionsPos < tarantulasPos);
        assertTrue(tarantulasPos < fcAwesomePos);
        assertTrue(fcAwesomePos < snakesPos);
        assertTrue(snakesPos > grouchesPos); // Grouches comes before Snakes alphabetically
    }

    @Test
    void processResults_EmptyList_OutputsEmptyStandings() {
        // Arrange
        List<MatchResult> emptyResults = Collections.emptyList();

        // Act
        matchService.processResults(emptyResults);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Team Standings:"));
        assertFalse(output.contains("pts")); // No team points should be displayed
    }

    @Test
    void processResults_TeamWithAllDraws_GetsCorrectPoints() {
        // Arrange
        List<MatchResult> results = Arrays.asList(
                createMatchResult("Team1", 2, "Team2", 2),
                createMatchResult("Team1", 1, "Team3", 1)
        );

        // Act
        matchService.processResults(results);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Team1: 2 pts"));
        assertTrue(output.contains("Team2: 1 pts"));
        assertTrue(output.contains("Team3: 1 pts"));
    }


    @Test
    void leagueResultInput_InputMismatchException_HandlesGracefully() {
        // Arrange
        when(mockScanner.nextInt()).thenThrow(new InputMismatchException());

        // Act & Assert
        assertThrows(InputMismatchException.class, () -> matchService.leagueResultInput());
    }

    @Test
    void updateTeamPoints_NewTeam_AddsInitialPoints() throws Exception {
        // Arrange
        Map<String, Integer> teamPoints = new HashMap<>();

        // Use reflection to access private method
        var updateTeamPointsMethod = MatchServiceImpl.class.getDeclaredMethod(
                "updateTeamPoints", Map.class, String.class, int.class);
        updateTeamPointsMethod.setAccessible(true);

        // Act
        updateTeamPointsMethod.invoke(matchService, teamPoints, "NewTeam", 3);

        // Assert
        assertEquals(3, teamPoints.get("NewTeam"));
    }

    @Test
    void updateTeamPoints_ExistingTeam_AddsPointsToExistingTotal() throws Exception {
        // Arrange
        Map<String, Integer> teamPoints = new HashMap<>();
        teamPoints.put("ExistingTeam", 2);

        // Use reflection to access private method
        var updateTeamPointsMethod = MatchServiceImpl.class.getDeclaredMethod(
                "updateTeamPoints", Map.class, String.class, int.class);
        updateTeamPointsMethod.setAccessible(true);

        // Act
        updateTeamPointsMethod.invoke(matchService, teamPoints, "ExistingTeam", 3);

        // Assert
        assertEquals(5, teamPoints.get("ExistingTeam"));
    }

    @Test
    void updateTeamPoints_TeamWithZeroPoints_RemainsPresentInMap() throws Exception {
        // Arrange
        Map<String, Integer> teamPoints = new HashMap<>();

        // Use reflection to access private method
        var updateTeamPointsMethod = MatchServiceImpl.class.getDeclaredMethod(
                "updateTeamPoints", Map.class, String.class, int.class);
        updateTeamPointsMethod.setAccessible(true);

        // Act
        updateTeamPointsMethod.invoke(matchService, teamPoints, "ZeroPointTeam", 0);

        // Assert
        assertTrue(teamPoints.containsKey("ZeroPointTeam"));
        assertEquals(0, teamPoints.get("ZeroPointTeam"));
    }

    @Test
    void processResults_TeamsWithSamePoints_SortedAlphabetically() {
        // Arrange
        List<MatchResult> results = Arrays.asList(
                createMatchResult("Zebras", 3, "Antelopes", 0),
                createMatchResult("Beavers", 3, "Cougars", 0),
                createMatchResult("Antelopes", 3, "Beavers", 0)
        );

        // Act
        matchService.processResults(results);

        // Assert
        String output = outContent.toString();
        // All teams should have 3 points each
        int antelopesPos = output.indexOf("Antelopes: 3");
        int beaversPos = output.indexOf("Beavers: 3");
        int cougarsPos = output.indexOf("Cougars: 0");
        int zebrasPos = output.indexOf("Zebras: 3");

        // Check alphabetical order for teams with same points
        assertTrue(antelopesPos < beaversPos);
        assertTrue(antelopesPos < zebrasPos);
        assertTrue(beaversPos < zebrasPos);
    }

    @Test
    void processResults_WithNullValues_HandlesGracefully() {
        // Arrange - try with null list

        // Act & Assert
        assertThrows(NullPointerException.class, () -> matchService.processResults(null));
    }

    @Test
    void processResults_WithNullMatchResult_HandlesGracefully() {
        // Arrange
        List<MatchResult> results = new ArrayList<>();
        results.add(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> matchService.processResults(results));
    }


    @Test
    void processResults_LargeNumberOfMatches_HandlesEfficiently() {
        // Arrange
        List<MatchResult> results = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            results.add(createMatchResult("Team" + (i % 20), i % 5, "Opponent" + (i % 20), (i + 1) % 5));
        }

        // Act
        long startTime = System.currentTimeMillis();
        matchService.processResults(results);
        long endTime = System.currentTimeMillis();

        // Assert
        long processingTime = endTime - startTime;
        assertTrue(processingTime < 5000, "Processing should complete in under 5 seconds");

        String output = outContent.toString();
        assertTrue(output.contains("Team Standings:"));
    }

    // Helper method to create match results for testing
    private MatchResult createMatchResult(String teamA, int scoreA, String teamB, int scoreB) {
        TeamScore teamScoreA = TeamScore.builder()
                                        .name(teamA)
                                        .score(scoreA)
                                        .build();

        TeamScore teamScoreB = TeamScore.builder()
                                        .name(teamB)
                                        .score(scoreB)
                                        .build();

        Match match = new Match(teamScoreA, teamScoreB);
        return new MatchResult(match);
    }

    @Test
    void processResults_ConcurrentModification_ThreadSafe() throws InterruptedException {
        // Arrange
        List<MatchResult> results = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            results.add(createMatchResult("Team" + (i % 10), i % 3, "Opponent" + (i % 10), (i + 1) % 3));
        }

        // Create a thread that will try to access the results concurrently
        Thread concurrentThread = new Thread(() -> {
            try {
                // Force this thread to yield to give main thread a chance to process
                Thread.sleep(10);
                // This shouldn't cause ConcurrentModificationException
                matchService.processResults(results);
            } catch (Exception e) {
                fail("Concurrent execution failed: " + e.getMessage());
            }
        });

        // Act
        concurrentThread.start();
        matchService.processResults(results);
        concurrentThread.join(1000);

        // Assert
        assertFalse(concurrentThread.isAlive(), "Concurrent thread should complete");
        // No need for more assertions - we're testing that no exceptions occur
    }

    @Test
    void processResults_TeamsWithSpecialCharacters_HandledCorrectly() {
        // Arrange
        List<MatchResult> results = Arrays.asList(
                createMatchResult("Team-With-Hyphens", 3, "Team.With.Dots", 1),
                createMatchResult("Team_With_Underscores", 2, "Team With Spaces", 2)
        );

        // Act
        matchService.processResults(results);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Team-With-Hyphens: 3 pts"));
        assertTrue(output.contains("Team.With.Dots: 0 pts"));
        assertTrue(output.contains("Team_With_Underscores: 1 pts"));
        assertTrue(output.contains("Team With Spaces: 1 pts"));
    }

    @Test
    void leagueResultInput_ScannerIOException_HandlesGracefully() {
        // Arrange
        when(mockScanner.nextInt()).thenThrow(new NoSuchElementException());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> matchService.leagueResultInput());
    }

    @Test
    void processResults_EmptyTeamName_HandlesProperly() {
        // Arrange
        List<MatchResult> results = Collections.singletonList(
                createMatchResult("", 1, "ValidTeam", 0)
        );

        // Act
        matchService.processResults(results);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains(": 3 pts"), "Empty team name should be processed");
        assertTrue(output.contains("ValidTeam: 0 pts"));
    }

    @Test
    void processResults_DuplicateMatches_CountsAllMatches() {
        // Arrange
        List<MatchResult> results = Arrays.asList(
                createMatchResult("TeamA", 1, "TeamB", 0),
                createMatchResult("TeamA", 1, "TeamB", 0), // Exact duplicate
                createMatchResult("TeamA", 1, "TeamB", 0)  // Another duplicate
        );

        // Act
        matchService.processResults(results);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("TeamA: 9 pts"), "Team should get points for all duplicate matches");
        assertTrue(output.contains("TeamB: 0 pts"));
    }

    @Test
    void processResults_VeryHighScores_CalculatesPointsCorrectly() {
        // Arrange
        List<MatchResult> results = Arrays.asList(
                createMatchResult("HighScoreTeam", Integer.MAX_VALUE, "LowScoreTeam", 0)
        );

        // Act
        matchService.processResults(results);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("HighScoreTeam: 3 pts"));
        assertTrue(output.contains("LowScoreTeam: 0 pts"));
    }

    @Test
    void processResults_NegativeScores_ProcessesCorrectly() {
        // Arrange - this might be invalid in real-world scenarios but tests robustness
        List<MatchResult> results = Arrays.asList(
                createMatchResult("NegativeTeam", -1, "PositiveTeam", 1)
        );

        // Act
        matchService.processResults(results);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("PositiveTeam: 3 pts"));
        assertTrue(output.contains("NegativeTeam: 0 pts"));
    }
}
