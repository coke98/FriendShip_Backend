package com.aliens.friendship.jwt;

import com.aliens.friendship.jwt.domain.dto.JoinDto;
import com.aliens.friendship.jwt.domain.dto.LoginDto;
import com.aliens.friendship.jwt.domain.dto.TokenDto;
import com.aliens.friendship.jwt.util.JwtTokenUtil;
import com.aliens.friendship.member.domain.Nationality;
import com.aliens.friendship.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
public class JwtAPITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @Transactional
    public void setupMember() {
        Nationality nationality = Nationality.builder().id(1).natinalityText("korean").build();
        JoinDto memberJoinRequest = JoinDto.builder()
                .password("1q2w3e4r")
                .email("skatks1016@naver.com")
                .name("김명준")
                .mbti("INTJ")
                .age(21)
                .gender("male")
                .nationality(nationality)
                .build();
        memberService.join(memberJoinRequest);
    }


    @DisplayName("로그인 시도 /login 접근")
    @Test
    public void MemberAPIAccessToLogin() throws Exception {
        //given
        final String url = "/login";
        LoginDto loginMember = new LoginDto("skatks1016@naver.com","1q2w3e4r");

        //when
        ResultActions resultActions = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginMember))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // then
        resultActions.andExpect(status().isOk());

    }

    @DisplayName("로그인 유지 /health 접근(with토큰)")
    @Test
    public void MemberAPIAccessWithToken() throws Exception {
        //given
        final String url = "/health";
        LoginDto loginMember = new LoginDto("skatks1016@naver.com","1q2w3e4r");
        TokenDto tokenResponse = memberService.login(loginMember);

        //when
        ResultActions resultActions = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + tokenResponse.getAccessToken()))
                .andExpect(status().isOk());

        // then
        resultActions.andExpect(status().isOk());

    }

    @DisplayName("로그인 유지 /health 접근(without 토큰)")
    @Test
    public void MemberAPIAccessWithoutToken() throws Exception {
        //given
        final String url = "/health";
        LoginDto loginMember = new LoginDto("skatks1016@naver.com","1q2w3e4r");

        //when
        ResultActions resultActions = mockMvc.perform(get(url)).andExpect(status().is4xxClientError());

        // then
        resultActions.andExpect(status().is4xxClientError());

    }

    @DisplayName("로그 아웃 /logout 접근(with 토큰)")
    @Test
    public void MemberAPIAccessToLogout() throws Exception {
        //given
        final String url = "/logout";
        LoginDto loginMember = new LoginDto("skatks1016@naver.com","1q2w3e4r");
        TokenDto tokenResponse = memberService.login(loginMember);

        //when
        ResultActions resultActions = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokenResponse.getAccessToken())
                        .header("RefreshToken",tokenResponse.getRefreshToken()))
                .andExpect(status().isOk());

        // then
        resultActions.andExpect(status().isOk());

    }


}

