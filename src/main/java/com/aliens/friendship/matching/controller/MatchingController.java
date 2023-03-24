package com.aliens.friendship.matching.controller;

import com.aliens.friendship.chatting.service.ChattingService;
import com.aliens.friendship.global.common.Response;
import com.aliens.friendship.matching.controller.dto.ApplicantResponse;
import com.aliens.friendship.matching.controller.dto.PartnersResponse;
import com.aliens.friendship.matching.service.BlockingInfoService;
import com.aliens.friendship.matching.service.MatchingInfoService;
import com.aliens.friendship.matching.controller.dto.ApplicantRequest;
import com.aliens.friendship.matching.service.MatchingService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RestController
@RequestMapping("api/v1/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingInfoService matchingInfoService;
    private final BlockingInfoService blockingInfoService;
    private final ChattingService chattingService;
    private final MatchingService matchingService;


    @GetMapping("/matching/languages")
    public Response<Map<String, Object>> getLanguages() {
        return Response.SUCCESS(matchingInfoService.getLanguages());
    }

    @PostMapping("/applicant")
    public void applyMatching(@RequestBody ApplicantRequest applicantRequest) {
        matchingInfoService.applyMatching(applicantRequest);
    }

    @GetMapping("/status")
    public Response<Map<String, String>> getStatus() {
        return Response.SUCCESS(matchingInfoService.getMatchingStatus());
    }

    @GetMapping("/partners")
    public Response<PartnersResponse> getPartners() {
        return Response.SUCCESS(matchingInfoService.getPartnersResponse());
    }

    @GetMapping("/applicant")
    public Response<ApplicantResponse> getApplicant() throws Exception {
        return Response.SUCCESS(matchingInfoService.getApplicant());
    }

    @GetMapping("/matching/partner/{memberId}/block/{roomId}")
    public Response<String> blocking(@PathVariable int memberId,Long roomId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        blockingInfoService.block(email, memberId);
        chattingService.saveChatMessage(roomId, "공지", "차단된 상대입니다.", 3);
        chattingService.updateRoomStatus(roomId, "CLOSE");
        return Response.SUCCESS("차단 성공");
    }
    @PostMapping()
    public void match() {
        matchingService.matchParticipants();
    }

    // TODO: 새로 매칭 시작 전 member의 is_applied를 none으로 변경 후 matchingParticipants 데이터 모두 삭제
    // TODO: 특정 시간이 되면 매칭 로직을 돌린다...
}
