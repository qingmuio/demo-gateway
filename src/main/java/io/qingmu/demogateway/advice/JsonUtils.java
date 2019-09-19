package io.qingmu.demogateway.advice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
public final class JsonUtils {

    public static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
    }

    public static String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static <T> T fromJson(String jsonString, Class<T> valueType) {
        Assert.notNull(valueType, "valueType is null ");
        if (jsonString == null || "".equals(jsonString)) {
            return null;
        }
        try {
            return mapper.readValue(jsonString, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static <T> T fromJsonNoEx(String jsonString, Class<T> valueType) {
        Assert.notNull(valueType, "valueType is null ");
        if (jsonString == null || "".equals(jsonString)) {
            return null;
        }
        try {
            return mapper.readValue(jsonString, valueType);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static <T> T fromJson(InputStream is, Class<T> valueType) {
        Assert.notNull(valueType, "valueType is null ");
        Assert.notNull(is, "inputStream is null");
        try {
            return mapper.readValue(is, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static <T extends Collection<S>, S> T fromJson(String jsonString, Class<T> collectionType, Class<S> elementType) {
        Assert.notNull(collectionType, "collectionType is null");
        Assert.notNull(elementType, "elementType is null");
        if (jsonString == null || "".equals(jsonString)) {
            return null;
        }
        try {
            return mapper.readValue(jsonString, mapper.getTypeFactory().constructCollectionType(collectionType, elementType));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static <T> T fromJson(String jsonString, TypeReference<T> typeReference) {
        Assert.notNull(typeReference, "typeReference is null");
        if (jsonString == null || "".equals(jsonString)) {
            return null;
        }
        try {
            return mapper.readValue(jsonString, typeReference);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    public static void main(String[] args) {
        P p = JsonUtils.fromJson("{\"date\": \"2017-01-01 10:10:04\"}", P.class);
        System.out.println(p);
    }

    public static <T> T fromJson(String jsonString, JavaType javaType) {
        Assert.notNull(javaType, "typeReference is null");
        if (jsonString == null || "".equals(jsonString)) {
            return null;
        }
        try {
            return mapper.readValue(jsonString, javaType);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Getter
    @Setter
    static class P {
        Date date;
    }

}