package kr.co.victoryfairy.core.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.api.domain.MyPageDomain;
import kr.co.victoryfairy.core.api.service.MemberService;
import kr.co.victoryfairy.core.api.service.MyPageService;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "My Page", description = "미이 페이지")
@RestController
@RequestMapping("/my-page")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "유저 정보")
    @GetMapping("/member")
    public CustomResponse<MyPageDomain.MemberInfoForMyPageResponse> findMemberInfo() {
        var response = myPageService.findMemberInfoForMyPage();
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "유저 정보")
    @GetMapping("/victory-power")
    public CustomResponse<MyPageDomain.VictoryPowerResponse> findVictoryPower() {
        var response = myPageService.findVictoryPower();
        return CustomResponse.ok(response);
    }
}
