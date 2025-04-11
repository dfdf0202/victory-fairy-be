package kr.co.victoryfairy.support.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisHandler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void setString(String key, String value) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key, value);
        System.out.println(valueOperations.get(key));
    }

    public void setMap(String key, Map<String, String> map) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        hashOperations.putAll(key, map);
        /*map.entrySet().forEach(entry -> {
            hashOperations.put(key, entry.getKey(), entry.getValue());
        });*/
    }

    public void put(String hashKey, String key, Object value) {
        redisTemplate.opsForHash().put(hashKey, key, value);
    }

    public String get(String key, String hashKey) {
        return (String) redisTemplate.opsForHash().get(key, hashKey);
    }

    public String get(String key) {
        Object o = redisTemplate.opsForValue().get(key);
        if (o == null) {
            return null;
        }
        return (String) o;
    }

    public Map<String, String> getMap(String key) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        return hashOperations.entries(key);
    }

    public void delete(String key, String hashKey) {
        redisTemplate.opsForHash().delete(key, hashKey);
    }

    public void deleteMap(String key) {
        redisTemplate.delete(key); // 전체 삭제
    }

    /**
     * hash 구조로 저장된 데이터에서 key 값으로 데이터 조회
     */
    public String getHashValue(String hashKey, String key) {
        return get(hashKey, key);
    }

    /**
     * hash 구조로 저장된 데이터에서 key 값으로 역직렬화 해서 조회
     */
    public <T> T getHashValue(String hashKey, String key, Class<T> clazz) {
        Object json = redisTemplate.opsForHash().get(hashKey, key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json.toString(), clazz);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new CustomException(MessageEnum.Common.REQUEST_FAIL);
        }
    }

    /**
     * hash 구조에 객체 Json 직렬화 저장
     */
    public void setHashValue(String hashKey, String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForHash().put(hashKey, key, json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new CustomException(MessageEnum.Common.REQUEST_FAIL);
        }
    }

    /**
     * hash 데이터 삭제
     */
    public void deleteHashValue(String hashKey, String key) {
        redisTemplate.opsForHash().delete(hashKey, key);
    }

}