package com.aliens.friendship.matching.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MatchingParticipantInfo {

    private int memberId;
    private int answer; // 1 또는 2 값
    private int language;
}