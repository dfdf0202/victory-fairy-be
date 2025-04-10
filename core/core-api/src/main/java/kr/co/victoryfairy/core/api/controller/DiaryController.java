package kr.co.victoryfairy.core.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import kr.co.victoryfairy.core.api.domain.DiaryDomain;
import kr.co.victoryfairy.core.api.service.DiaryService;
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

    @Operation(summary = "일기 작성")
    @PostMapping("/diary")
    public CustomResponse<DiaryDomain.DiaryDto> writeDiary(@RequestBody DiaryDomain.DiaryDto request){
        return CustomResponse.ok(diaryService.writeDiary(request));
    }
}
