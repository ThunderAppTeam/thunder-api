package app.thunder.libraries.errors

class ExternalApiException(val errorCode: ExternalErrors) : RuntimeException()
