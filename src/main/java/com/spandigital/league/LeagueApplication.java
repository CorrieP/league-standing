package com.spandigital.league;

import com.spandigital.league.match.MatchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class LeagueApplication implements CommandLineRunner {

	private final MatchService matchService;

    public LeagueApplication(MatchService matchService) {
        this.matchService = matchService;
    }

    public static void main(String[] args) {
		SpringApplication.run(LeagueApplication.class, args);
	}

	@Override
	public void run(String... args) {
		matchService.leagueResultInput(args);
	}
}
