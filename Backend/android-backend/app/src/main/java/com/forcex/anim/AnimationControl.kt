package com.forcex.anim

class AnimationControl {
    internal var loop = false
    var isRunning = true
        internal set

    var time = 0.0f
    var speed = 1.0f

    fun putCommand(cmd: Byte) {
        when (cmd) {
            CMD_RESET -> {
                time = 0.0f
                speed = 1.0f
                isRunning = true
                loop = false
            }
            CMD_PLAY -> isRunning = true
            CMD_PAUSE -> isRunning = false
            CMD_LOOP -> loop = true
            CMD_NO_LOOP -> loop = false
        }
    }

    companion object {

        val CMD_RESET: Byte = 0
        val CMD_PLAY: Byte = 1
        val CMD_PAUSE: Byte = 2
        val CMD_LOOP: Byte = 3
        val CMD_NO_LOOP: Byte = 4
    }
}
