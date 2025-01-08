package app.thunder.api.exception

class ExternalApiException(val errorCode: ExternalErrors) : RuntimeException()