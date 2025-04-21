package kr.co.victoryfairy.core.api.service;


import kr.co.victoryfairy.core.api.domain.DiaryDomain;

public interface DiaryService {

    DiaryDomain.DiaryDto writeDiary(DiaryDomain.DiaryDto diaryDto);

}
