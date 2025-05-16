package kr.co.victoryfairy.core.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import kr.co.victoryfairy.core.api.domain.DiaryDomain;
import kr.co.victoryfairy.core.api.service.DiaryService;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.model.CustomResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "일기 작성")
    @PostMapping("/diary")
    public CustomResponse<MessageEnum> writeDiary(@RequestBody DiaryDomain.DiaryDto request){
        diaryService.writeDiary(request);
        return CustomResponse.ok(MessageEnum.Common.SAVE);
    }
}
