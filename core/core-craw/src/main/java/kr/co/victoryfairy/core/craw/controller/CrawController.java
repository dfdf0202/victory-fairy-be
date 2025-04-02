package kr.co.victoryfairy.core.craw.controller;

import io.dodn.springboot.core.enums.MessageEnum;
import io.swagger.v3.oas.annotations.Operation;
import kr.co.victoryfairy.core.craw.service.CrawService;
import kr.co.victoryfairy.core.craw.support.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/craw")
public class CrawController {

    private final CrawService crawService;

    public CrawController(CrawService crawService) {
        this.crawService = crawService;
    }

    @Operation(summary = "경기 일정 일괄 불러오기")
    @GetMapping("/match-list")
    public ApiResponse<String> getMatchList(@RequestParam(name = "sYear") String sYear, @RequestParam(name = "sMonth", required = false) String sMonth) {
        crawService.crawMatchList(sYear, sMonth);
        return ApiResponse.success(MessageEnum.Common.REQUEST.getDescKr());
    }

    @Operation(summary = "경기 내용 불러오기")
    @GetMapping("/match-detail")
    public ApiResponse<String> getMatchDetail(@RequestParam(name = "sYear") String sYear) {
        crawService.crawMatchDetail(sYear);
        return ApiResponse.success(MessageEnum.Common.REQUEST.getDescKr());
    }
}
