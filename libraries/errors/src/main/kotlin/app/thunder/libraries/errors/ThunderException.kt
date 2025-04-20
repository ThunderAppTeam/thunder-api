package app.thunder.libraries.errors

class ThunderException(val errorCode: ErrorCode) : RuntimeException()
