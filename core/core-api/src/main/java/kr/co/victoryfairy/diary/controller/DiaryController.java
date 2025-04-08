package kr.co.victoryfairy.diary.controller;

import kr.co.victoryfairy.diary.service.DiaryService;
import kr.co.victoryfairy.storage.db.core.dto.DiaryDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @PostMapping("/diary")
    public DiaryDto writeDiary(@RequestBody DiaryDto diaryDto){
        return diaryService.writeDiary(diaryDto);
    }
}
