package kr.co.victoryfairy.storage.db.core.model;

import io.dodn.springboot.core.enums.MemberEnum;
import lombok.Getter;

public interface MemberModel {

    @Getter
    class MemberInfo {
        private Long id;
        private String nickNm;
        private MemberEnum.SnsType snsType;

        private Long teamId;
        private String teamName;

        private Long fileId;
        private String path;
        private String saveName;
        private String ext;
    }

}
