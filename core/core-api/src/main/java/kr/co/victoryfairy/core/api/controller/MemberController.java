package kr.co.victoryfairy.core.api.controller;

import io.dodn.springboot.core.enums.MemberEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.co.victoryfairy.core.api.domain.MemberDomain;
import kr.co.victoryfairy.core.api.service.MemberService;
import kr.co.victoryfairy.support.model.CustomResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @Operation(summary = "sns 별 인증 주소 불러오기")
    @GetMapping("/auth-path")
    public CustomResponse<String> authPath(
            @RequestParam @Validated
            @Schema(description = "인증 로그인 타입", example = "KAKAO", implementation = MemberEnum.SnsType.class)
            MemberEnum.SnsType snsType
    ) {
        return CustomResponse.ok(memberService.getOauthPath(snsType));
    }

    @Operation(summary = "로그인")
    @GetMapping("/login")
    public CustomResponse<MemberDomain.MemberLoginResponse> login(@Validated @ParameterObject MemberDomain.MemberLoginRequest request) {
        var response = memberService.login(request);
        return CustomResponse.ok(response);
    }
}
