package com.hellostranger.chessserver.core

enum class GameResult {
    NotStarted,
    Waiting,
    WaitingPrivate,
    Aborted,
    InProgress,
    WhiteIsMated,
    BlackIsMated,
    WhiteResigned,
    BlackResigned,
    Stalemate,
    Repetition,
    FiftyMoveRule,
    InsufficientMaterial,
    DrawByArbiter,
    DrawByAgreement,
}