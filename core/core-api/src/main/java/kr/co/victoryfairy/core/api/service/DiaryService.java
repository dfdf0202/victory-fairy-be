package kr.co.victoryfairy.core.api.service;


import kr.co.victoryfairy.core.api.domain.DiaryDomain;

public interface DiaryService {

    void writeDiary(DiaryDomain.DiaryDto diaryDto);

}
