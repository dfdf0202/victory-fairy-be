package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.model.DiaryModel;

import java.time.LocalDate;
import java.util.List;

public interface DiaryCustomRepository {

    List<DiaryModel.DiaryDto> findList(DiaryModel.ListRequest request);
    List<DiaryModel.DiaryDto> findDailyList(DiaryModel.DailyListRequest request);
}
