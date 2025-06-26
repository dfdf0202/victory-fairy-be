package kr.co.victoryfairy.core.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AppleResponseWrapper {

    @JsonProperty("access_token")
    private String access_token;
    @JsonProperty("refresh_token")
    private String refresh_token;
    @JsonProperty("id_token")
    private String id_token;
    @JsonProperty("expires_in")
    private Integer expires_in;
    @JsonProperty("token_type")
    private String token_type;
}