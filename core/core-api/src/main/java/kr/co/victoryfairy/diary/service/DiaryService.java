package kr.co.victoryfairy.diary.service;


import kr.co.victoryfairy.storage.db.core.dto.DiaryDto;

public interface DiaryService {

    DiaryDto writeDiary(DiaryDto diaryDto);

}
