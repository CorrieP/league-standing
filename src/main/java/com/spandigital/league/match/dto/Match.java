package com.spandigital.league.match.dto;

import lombok.Builder;

@Builder
public record Match(TeamScore teamA, TeamScore teamB) {

    @Override
    public String toString() {
        return teamA.name() + " " + teamA.score() + " - " + teamB.name() + " " + teamB.score();
    }
}
