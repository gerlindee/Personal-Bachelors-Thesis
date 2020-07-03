package com.example.quizzicat.Model

import java.util.*

class MultiPlayerGame(
    val gid: String,
    var active: Boolean,
    var progress: Boolean,
    val created_on: String,
    val created_by: String,
    val game_pin: String,
    val tid: Long
): ModelEntity