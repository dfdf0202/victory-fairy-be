package kr.co.victoryfairy.storage.db.core.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.model.MemberModel;
import kr.co.victoryfairy.storage.db.core.repository.MemberCustomRepository;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static kr.co.victoryfairy.storage.db.core.entity.QFileEntity.fileEntity;
import static kr.co.victoryfairy.storage.db.core.entity.QFileRefEntity.fileRefEntity;
import static kr.co.victoryfairy.storage.db.core.entity.QMemberEntity.memberEntity;
import static kr.co.victoryfairy.storage.db.core.entity.QMemberInfoEntity.memberInfoEntity;
import static kr.co.victoryfairy.storage.db.core.entity.QTeamEntity.teamEntity;

@Repository
public class MemberCustomRepositoryImpl extends QuerydslRepositorySupport implements MemberCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public MemberCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        super(MemberEntity.class);
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public Optional<MemberModel.MemberInfo> findById(Long memberId) {
        return Optional.ofNullable(jpaQueryFactory
                .select(Projections.fields(MemberModel.MemberInfo.class
                        , memberEntity.id
                        , memberInfoEntity.nickNm
                        , memberInfoEntity.snsType
                        , teamEntity.id.as("teamId")
                        , teamEntity.name.as("teamName")
                        , fileEntity.id.as("fileId")
                        , fileEntity.path
                        , fileEntity.saveName
                        , fileEntity.ext
                ))
                .from(memberEntity)
                .innerJoin(memberInfoEntity).on(memberInfoEntity.memberEntity.id.eq(memberEntity.id))
                .leftJoin(teamEntity).on(memberInfoEntity.teamEntity.id.eq(teamEntity.id))
                .leftJoin(fileRefEntity).on(fileRefEntity.refType.eq(RefType.PROFILE).and(fileRefEntity.refId.eq(memberEntity.id)))
                .leftJoin(fileEntity).on(fileRefEntity.fileEntity.id.eq(fileEntity.id).and(fileEntity.isUse.eq(true)))
                .where(memberEntity.id.eq(memberId))
                .fetchOne());
    }
}
