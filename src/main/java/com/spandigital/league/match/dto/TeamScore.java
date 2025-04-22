package com.spandigital.league.match.dto;

import lombok.Builder;

@Builder
public record TeamScore(String name, Integer score) {

}
