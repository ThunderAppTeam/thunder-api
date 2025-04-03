package app.thunder.api.exception

class ThunderException(val errorCode: ErrorCode) : RuntimeException()