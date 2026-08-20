package withoutc.chongchong.auth.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import withoutc.chongchong.auth.token.HashedRefreshToken;

@Converter
public class HashedRefreshTokenConverter implements AttributeConverter<HashedRefreshToken, String> {

    @Override
    public String convertToDatabaseColumn(HashedRefreshToken attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.value();
    }

    @Override
    public HashedRefreshToken convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return new HashedRefreshToken(dbData);
    }
}
