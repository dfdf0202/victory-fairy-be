package io.dodn.springboot.core.enums;

public interface MessageEnum {
    enum Common {
        REQUEST_PARAMETER("파라미터를 확인해주세요."),
        REQUEST("요청이 완료되었습니다."),
        REQUEST_FAIL("요청이 실패 하였습니다."),
        MODIFY("수정이 완료되었습니다."),
        SAVE("등록이 완료되었습니다."),
        UPDATE("수정이 완료되었습니다."),
        DELETE("삭제가 완료되었습니다."),
        APPROVE("승인이 완료되었습니다."),
        FAIL_APPROVE_FAIL("승인이 취소되었습니다.");

        private String descKr;

        Common(String descKr) {
            this.descKr = descKr;
        }

        public String getDescKr() {
            return descKr;
        }
    }
}