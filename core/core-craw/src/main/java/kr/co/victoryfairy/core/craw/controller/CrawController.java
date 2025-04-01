package kr.co.victoryfairy.core.craw.controller;

import io.dodn.springboot.core.enums.MessageEnum;
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

    @GetMapping("/match-list")
    public ApiResponse<String> getMatchList(@RequestParam(name = "sYear") String sYear, @RequestParam(name = "sMonth", required = false) String sMonth) {
        crawService.crawMatchList(sYear, sMonth);
        return ApiResponse.success(MessageEnum.Common.REQUEST.getDescKr());
    }
}
