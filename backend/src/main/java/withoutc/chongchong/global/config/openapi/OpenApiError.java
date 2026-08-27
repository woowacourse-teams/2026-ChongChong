package withoutc.chongchong.global.config.openapi;

record OpenApiError(
        String responseCode,
        String code,
        String message,
        Object example
) {
}
