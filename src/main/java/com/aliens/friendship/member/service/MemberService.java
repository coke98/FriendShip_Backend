package com.aliens.friendship.member.service;

import com.aliens.friendship.global.config.cache.CacheKey;
import com.aliens.friendship.global.config.jwt.JwtExpirationEnums;
import com.aliens.friendship.jwt.domain.LogoutAccessToken;
import com.aliens.friendship.member.controller.dto.MemberInfoDto;
import com.aliens.friendship.member.controller.dto.WithdrawalDto;
import com.aliens.friendship.member.domain.Member;
import com.aliens.friendship.jwt.domain.RefreshToken;
import com.aliens.friendship.member.controller.dto.JoinDto;
import com.aliens.friendship.jwt.domain.dto.LoginDto;
import com.aliens.friendship.jwt.domain.dto.MemberInfo;
import com.aliens.friendship.jwt.domain.dto.TokenDto;
import com.aliens.friendship.jwt.repository.LogoutAccessTokenRedisRepository;
import com.aliens.friendship.member.repository.MemberRepository;
import com.aliens.friendship.jwt.repository.RefreshTokenRedisRepository;
import com.aliens.friendship.jwt.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final LogoutAccessTokenRedisRepository logoutAccessTokenRedisRepository;
    private final JwtTokenUtil jwtTokenUtil;

    private final ProfileImageService profileImageService;

    public void join(JoinDto joinDto) throws Exception {
        checkDuplicatedEmail(joinDto.getEmail());
        joinDto.setPassword(passwordEncoder.encode(joinDto.getPassword()));
        joinDto.setImageUrl(profileImageService.uploadProfileImage(joinDto.getImage()));
        memberRepository.save(Member.ofUser(joinDto));
    }

    public void joinAdmin(JoinDto joinDto) {
        joinDto.setPassword(passwordEncoder.encode(joinDto.getPassword()));
        memberRepository.save(Member.ofAdmin(joinDto));
    }

    public void withdrawal(WithdrawalDto withdrawalDto) throws Exception {
        Member member = memberRepository.findByEmail(withdrawalDto.getEmail()).orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));
        if (!passwordEncoder.matches(withdrawalDto.getPassword(), member.getPassword())) {
            throw new Exception("비밀번호가 일치하지 않습니다.");
        }
        memberRepository.delete(member);
    }

    private void checkDuplicatedEmail(String email) throws Exception{
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new Exception("이미 사용중인 이메일입니다.");
        }
    }

    public TokenDto login(LoginDto loginDto) {
        Member member = memberRepository.findByEmail(loginDto.getEmail()).orElseThrow(() -> new NoSuchElementException("회원이 없습니다."));
        checkPassword(loginDto.getPassword(), member.getPassword());
        String username = member.getEmail();
        String accessToken = jwtTokenUtil.generateAccessToken(username);
        RefreshToken refreshToken = saveRefreshToken(username);
        return TokenDto.of(accessToken, refreshToken.getRefreshToken());
    }

    private void checkPassword(String rawPassword, String findMemberPassword) {
        if (!passwordEncoder.matches(rawPassword, findMemberPassword)) {
            throw new IllegalArgumentException("비밀번호가 맞지 않습니다.");
        }
    }

    private RefreshToken saveRefreshToken(String username) {
        return refreshTokenRedisRepository.save(RefreshToken.createRefreshToken(username,
                jwtTokenUtil.generateRefreshToken(username), JwtExpirationEnums.REFRESH_TOKEN_EXPIRATION_TIME.getValue()));
    }

    // TODO: 만나이 반환으로 수정
    public MemberInfoDto getMemberInfo(int memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));
        return MemberInfoDto.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .mbti(member.getMbti())
                .gender(member.getGender())
                .nationality(member.getNationality().getId())
                .age(member.getAge())
                .name(member.getName())
                .build();
    }

    // 4
    @CacheEvict(value = CacheKey.USER, key = "#username")
    public void logout(TokenDto tokenDto, String username) {
        String accessToken = resolveToken(tokenDto.getAccessToken());
        long remainMilliSeconds = jwtTokenUtil.getRemainMilliSeconds(accessToken);
        refreshTokenRedisRepository.deleteById(username);
        logoutAccessTokenRedisRepository.save(LogoutAccessToken.of(accessToken, username, remainMilliSeconds));
    }

    private String resolveToken(String token) {
        return token.substring(7);
    }

    // 3
    public TokenDto reissue(String refreshToken) {
        refreshToken = resolveToken(refreshToken);
        String username = getCurrentUsername();
        RefreshToken redisRefreshToken = refreshTokenRedisRepository.findById(username).orElseThrow(NoSuchElementException::new);

        if (refreshToken.equals(redisRefreshToken.getRefreshToken())) {
            return reissueRefreshToken(refreshToken, username);
        }
        throw new IllegalArgumentException("토큰이 일치하지 않습니다.");
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        return principal.getUsername();
    }

    private TokenDto reissueRefreshToken(String refreshToken, String username) {
        if (lessThanReissueExpirationTimesLeft(refreshToken)) {
            String accessToken = jwtTokenUtil.generateAccessToken(username);
            return TokenDto.of(accessToken, saveRefreshToken(username).getRefreshToken());
        }
        return TokenDto.of(jwtTokenUtil.generateAccessToken(username), refreshToken);
    }

    private boolean lessThanReissueExpirationTimesLeft(String refreshToken) {
        return jwtTokenUtil.getRemainMilliSeconds(refreshToken) < JwtExpirationEnums.REISSUE_EXPIRATION_TIME.getValue();
    }
}