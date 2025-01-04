package app.thunder.api.exception

class ThunderException(val errorCode:MemberErrors) : RuntimeException()