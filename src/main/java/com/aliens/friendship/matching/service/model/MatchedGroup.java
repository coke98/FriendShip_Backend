package com.aliens.friendship.matching.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MatchedGroup {
    int memberId1;
    int memberId2;
    Integer memberId3;

}