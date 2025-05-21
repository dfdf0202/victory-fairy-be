package kr.co.victoryfairy.core.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.api.domain.TeamDomain;
import kr.co.victoryfairy.core.api.service.TeamService;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Common", description = "공통")
@RestController
@RequestMapping("/common")
@RequiredArgsConstructor
public class CommonController {

    private final TeamService teamService;

    @Operation(summary = "팀 전체 목록 불러오기")
    @GetMapping("/team")
    public CustomResponse<List<TeamDomain.TeamListResponse>> findAll() {
        var response = teamService.findAll();
        return CustomResponse.ok(response);
    }

}
