package kr.co.victoryfairy.core.api.service.oauth;

import io.dodn.springboot.core.enums.MemberEnum;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OauthFactory {

    private final Map<String, OauthService> parserMap;

    public OauthFactory(Map<String, OauthService> parserMap) {
        this.parserMap = parserMap;
    }

    /**
     * 인터페이스를 상속받은 각자의 클래스를 인잭션
     * @param snsType
     * @return
     */
    public OauthService getService(MemberEnum.SnsType snsType) {
        var service = parserMap.get(snsType.name());
        if (service == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_SNS);
        }
        return service;
    }
}
