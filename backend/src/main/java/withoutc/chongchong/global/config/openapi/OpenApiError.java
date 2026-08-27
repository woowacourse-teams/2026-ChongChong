package withoutc.chongchong.global.config.openapi;

record OpenApiError(
        String responseCode,
        String code,
        String message,
        Object example,
        String exampleKey
) {

    OpenApiError(String responseCode, String code, String message, Object example) {
        this(responseCode, code, message, example, code);
    }
}
