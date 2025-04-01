package io.dodn.springboot.core.enums;

public interface MatchEnum {

    enum MatchType {
        EXHIBITION("시범경기", "1"),
        REGULAR("정규시즌", "0,9,6"),
        POST("포스트시즌", "3,4,5,7"),
        TIEBREAKER("타이브레이커", "6")
        ;

        private String desc;
        private String value;

        MatchType(String desc, String value) {
            this.desc = desc;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public String getDesc() {
            return desc;
        }
    }

    enum MatchStatus {
        READY("예정"),
        END("종료"),
        CANCELED("취소")
        ;

        private String desc;

        MatchStatus(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }
    }

}
