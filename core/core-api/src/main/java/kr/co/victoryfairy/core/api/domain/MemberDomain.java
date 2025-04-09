package kr.co.victoryfairy.core.api.domain;

import io.dodn.springboot.core.enums.MemberEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

public interface MemberDomain {

    @Builder
    @Getter
    @Schema(name = "Member.MemberDto")
    class MemberDto {
        @Schema(description = "member id")
        private Long id;

        @Schema(description = "member info")
        private MemberInfoDto memberInfo;
    }

    @Builder
    @Getter
    @Schema(name = "Member.MemberInfoDto")
    class MemberInfoDto {
        @Schema(description = "sns 타입", example = "KAKAO", implementation = MemberEnum.SnsType.class)
        private MemberEnum.SnsType snsType;

        @Schema(description = "닉네임 등록 여부")
        private Boolean isNickNmAdded;

        @Schema(description = "응원하는 팀 등록 여부")
        private Boolean isTeamAdded;
    }

    record MemberSns(
            MemberEnum.SnsType type,
            String snsId,
            String email
    ) {}

    @Schema(name = "Member.MemberInfoResponse")
    record MemberInfoResponse(
            @Schema(description = "sns 타입", implementation = MemberEnum.SnsType.class)
            MemberEnum.SnsType snsType,

            @Schema(description = "sns id")
            String snsId,

            @Schema(description = "닉네임 등록 여부")
            Boolean isNickNmAdded,

            @Schema(description = "응원하는 팀 등록 여부")
            Boolean isTeamAdded
    ) {}

    @Schema(name = "Member.MemberLoginResponse")
    record MemberLoginResponse(
            @Schema(description = "member info")
            MemberInfoResponse memberInfo,
            @Schema(description = "access token")
            String accessToken,
            @Schema(description = "refresh token")
            String refreshToken
    ) {}

    @Schema(name = "Member.MemberLoginRequest")
    record MemberLoginRequest(
            @Schema(description = "code", example = "XHOtTSJ5nhn8ZXWgXD")
            String code,

            @Schema(description = "sns 타입", example = "KAKAO", implementation = MemberEnum.SnsType.class)
            MemberEnum.SnsType snsType,

            @Schema(description = "로그인 콜백 URL", example = "http://localhost:8080/auth/callbackKakao")
            String loginCallbackUrl
    ) {}
}
