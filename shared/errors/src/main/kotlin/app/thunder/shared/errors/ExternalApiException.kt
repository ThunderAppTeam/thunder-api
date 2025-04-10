package app.thunder.shared.errors

class ExternalApiException(val errorCode: ExternalErrors) : RuntimeException()
