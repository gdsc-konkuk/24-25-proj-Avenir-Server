package juton113.Avenir.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import juton113.Avenir.domain.dto.AccessTokenDto;
import juton113.Avenir.domain.dto.RefreshTokenDto;
import juton113.Avenir.service.AuthService;
import juton113.Avenir.service.MemberService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@AllArgsConstructor
public class CustomOauth2SuccessHandler implements AuthenticationSuccessHandler {
    private final MemberService memberService;
    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");
        String memberId = String.valueOf(memberService.findMemberByEmail(email).getMemberId());

        AccessTokenDto accessTokenDto = authService.issueAccessToken(memberId);
        RefreshTokenDto refreshTokenDto = authService.issueRefreshToken(memberId);

        response.setHeader("Authorization", "Bearer " + accessTokenDto.getAccessToken());
        response.setHeader("X-Refresh-Token", "Bearer " + refreshTokenDto.getRefreshToken());

        response.setStatus(HttpServletResponse.SC_OK);

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("Login success");
    }
}
