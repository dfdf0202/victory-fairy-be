package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.model.MemberModel;

import java.util.Optional;

public interface MemberCustomRepository {

    Optional<MemberModel.MemberInfo> findById(Long memberId);

}
