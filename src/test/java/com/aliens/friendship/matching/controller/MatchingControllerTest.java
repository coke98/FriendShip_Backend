package com.aliens.friendship.matching.controller;

import com.aliens.friendship.global.config.jwt.JwtAuthenticationFilter;
import com.aliens.friendship.matching.controller.dto.ApplicantRequest;
import com.aliens.friendship.matching.controller.dto.ApplicantResponse;
import com.aliens.friendship.matching.controller.dto.PartnersResponse;
import com.aliens.friendship.matching.domain.Language;
import com.aliens.friendship.matching.service.MatchingInfoService;
import com.aliens.friendship.matching.service.MatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.*;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters=false)
@WebMvcTest(MatchingController.class)
class MatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    MatchingInfoService matchingInfoService;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    MatchingService matchingService;

    @Test
    @DisplayName("언어 목록 조회 성공")
    void GetLanguages_Success() throws Exception {
        // Given
        Map<String, Object> languageResponse = new HashMap<>();
        List<Language> languages = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            languages.add(Language.builder()
                    .id(i + 1)
                    .languageText("language" + (i + 1))
                    .build());
        }
        languageResponse.put("languages", languages);
        when(matchingInfoService.getLanguages()).thenReturn(languageResponse);

        // When
        ResultActions resultActions = mockMvc.perform(get("api/v1/matching/languages"));

        // Then
        verify(matchingInfoService, times(1)).getLanguages();
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.response.languages", hasSize(languages.size())))
                .andExpect(jsonPath("$.response.languages[*].id", containsInAnyOrder(1, 2, 3)))
                .andExpect(jsonPath("$.response.languages[*].languageText", containsInAnyOrder("language1", "language2", "language3")));
    }

    @Test
    @DisplayName("매칭 참가 신청 성공")
    void ApplyMatching_Success() throws Exception {
        // given
        ApplicantRequest request = new ApplicantRequest();
        request.setFirstPreferLanguage(1);
        request.setSecondPreferLanguage(2);

        // when
        ResultActions resultActions = mockMvc.perform(post("api/v1/matching/applicant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)));

        // then
        verify(matchingInfoService, times(1)).applyMatching(any());
        resultActions.andExpect(status().isOk());
    }

    @Test
    @DisplayName("매칭 상태 조회 성공")
    void GetMatchingStatus_Success() throws Exception {
        // Given
        Map<String, String> status = new HashMap<>();
        status.put("status", "MATCHED");
        when(matchingInfoService.getMatchingStatus()).thenReturn(status);

        // When
        ResultActions resultActions = mockMvc.perform(get("api/v1/matching/status"));

        // Then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value("MATCHED"));
    }


    @Test
    @DisplayName("파트너 목록 조회 성공")
    void GetPartners_Success() throws Exception {
        // Given
        PartnersResponse partnersResponse = new PartnersResponse();
        List<PartnersResponse.Member> partners = PartnersFixture.createPartners(4);
        partnersResponse.setPartners(partners);
        when(matchingInfoService.getPartnersResponse()).thenReturn(partnersResponse);

        // When
        ResultActions resultActions = mockMvc.perform(get("api/v1/matching/partners"));

        // Then
        resultActions.andExpect(status().isOk());

        for (int i = 0; i < partners.size(); i++) {
            PartnersResponse.Member partnerDto = partners.get(i);
            resultActions.andExpect(jsonPath("$.response.partners[" + i + "].memberId").value(partnerDto.getMemberId()))
                    .andExpect(jsonPath("$.response.partners[" + i + "].name").value(partnerDto.getName()))
                    .andExpect(jsonPath("$.response.partners[" + i + "].mbti").value(partnerDto.getMbti()))
                    .andExpect(jsonPath("$.response.partners[" + i + "].gender").value(partnerDto.getGender()))
                    .andExpect(jsonPath("$.response.partners[" + i + "].countryImage").value(partnerDto.getCountryImage()))
                    .andExpect(jsonPath("$.response.partners[" + i + "].nationality").value(partnerDto.getNationality()))
                    .andExpect(jsonPath("$.response.partners[" + i + "].profileImage").value(partnerDto.getProfileImage()));
        }

        verify(matchingInfoService).getPartnersResponse();
    }


    @Test
    @DisplayName("신청 정보 조회 성공")
    void GetApplicant_Success() throws Exception {
        // Given
        ApplicantResponse.Member applicantDto = ApplicantResponse.Member.builder()
                .name("Ryan")
                .gender("MALE")
                .mbti("INTJ")
                .age(25)
                .nationality("American")
                .profileImage("Profile_URL")
                .countryImage("Korea_URL")
                .build();
        ApplicantResponse.PreferLanguages preferLanguagesDto = ApplicantResponse.PreferLanguages.builder()
                .firstPreferLanguage("English")
                .secondPreferLanguage("Spanish")
                .build();
        ApplicantResponse returnDto = ApplicantResponse.builder()
                .member(applicantDto)
                .preferLanguages(preferLanguagesDto)
                .build();
        when(matchingInfoService.getApplicant()).thenReturn(returnDto);

        // When
        ResultActions resultActions = mockMvc.perform(get("api/v1/matching/applicant"));

        // Then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.response.member.name").value(applicantDto.getName()))
                .andExpect(jsonPath("$.response.member.gender").value(applicantDto.getGender()))
                .andExpect(jsonPath("$.response.member.mbti").value(applicantDto.getMbti()))
                .andExpect(jsonPath("$.response.member.age").value(applicantDto.getAge()))
                .andExpect(jsonPath("$.response.member.nationality").value(applicantDto.getNationality()))
                .andExpect(jsonPath("$.response.member.profileImage").value(applicantDto.getProfileImage()))
                .andExpect(jsonPath("$.response.member.countryImage").value(applicantDto.getCountryImage()))
                .andExpect(jsonPath("$.response.preferLanguages.firstPreferLanguage").value(preferLanguagesDto.getFirstPreferLanguage()))
                .andExpect(jsonPath("$.response.preferLanguages.secondPreferLanguage").value(preferLanguagesDto.getSecondPreferLanguage()));

        verify(matchingInfoService, times(1)).getApplicant();
    }
}

//=======
//@Autowired
//private MemberService memberService;
//
//@Autowired
//private BlockingInfoService blockingInfoService;
//
//@Autowired
//private MemberRepository memberRepository;
//
//@Autowired
//private NationalityRepository nationalityRepository;
//
//        TokenDto token;
//        JoinDto blockingMember;
//        JoinDto blockedMember;
//        int idOfBlockedMember;
//
//@Test
//@DisplayName("차단 성공")
//    void Blocking_Success() throws Exception {
//
//            //회원가입
//            MultipartFile mockMultipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());
//            Nationality nationality = new Nationality(1, "South Korea");
//            nationalityRepository.save(nationality);
//            blockingMember = JoinDto.builder()
//            .password("1q2w3e4r")
//            .email("skatks1016@naver.com")
//            .name("김명준")
//            .mbti("INTJ")
//            .birthday("1998-01-01")
//            .gender("male")
//            .image(mockMultipartFile)
//            .nationality(nationality)
//            .build();
//
//            blockedMember = JoinDto.builder()
//            .password("1q2w3e4r")
//            .email("skatks1125@naver.com")
//            .name("최정은")
//            .mbti("INTJ")
//            .birthday("1998-01-01")
//            .gender("male")
//            .image(mockMultipartFile)
//            .nationality(nationality)
//            .build();
//
//            memberService.join(blockedMember);
//            memberService.join(blockingMember);
//
//            //blockedMember의 Id값 추출
//            Optional<Member> blockedMemberEntity = memberRepository.findByEmail(blockedMember.getEmail());
//        idOfBlockedMember = blockedMemberEntity.get().getId();
//
//        // given
//        LoginDto loginMember = new LoginDto("skatks1016@naver.com","1q2w3e4r");
//        TokenDto tokenResponse = memberService.login(loginMember);
//        String accessToken = tokenResponse.getAccessToken();
//
//        // when & then
//        mockMvc.perform(get("/matching/partner/" + idOfBlockedMember+ "/block")
//        .header("Authorization", "Bearer " + accessToken))
//        .andExpect(status().isOk());
//        }
//
//        }
//        >>>>>>> develop
