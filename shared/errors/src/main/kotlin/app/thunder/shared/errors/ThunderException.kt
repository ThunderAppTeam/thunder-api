package app.thunder.shared.errors

class ThunderException(val errorCode: ErrorCode) : RuntimeException()
