package com.forcex.anim

import java.util.*
import com.forcex.utils.*

class AnimationTrack {
    internal var frames = ArrayList<Track>()
    var tackTime = 0.0f
        internal set
    var duration = 0f
        internal set
    internal var repeat = false

    internal var listener: onInterpolateListener? = null

    private// es el primer frame?
    // retornar el ultimo frame
    // retornar el primer frame
    // retornar el frame anterior
    //retornar el ultimo frame
    val previus: Track
        get() {
            for (f in 0..frames.size) {
                if (tackTime <= frames[f].time) {
                    return if (f == 0) {
                        if (frames.size != 1) {
                            frames[frames.size - 1]
                        } else {
                            frames[f]
                        }
                    } else {
                        frames[f - 1]
                    }
                }
            }
            return frames[frames.size - 1]
        }

    private//retornar el ultimo frame
    val next: Track
        get() {
            for (f in frames) {
                if (tackTime <= frames[f.toInt()].time) {
                    return frames[f.toInt()]
                }
            }
            return frames[frames.size - 1]
        }

    inner class Track(internal var time: Float, internal var `object`: Any)

    interface onInterpolateListener {
        fun interpolate(prv: Any, nxt: Any, p: Float)
    }

    fun setInterpolation(interpolation: onInterpolateListener) {
        listener = interpolation
    }

    fun add(time: Float, obj: Any) {
        frames.add(Track(time, obj))
    }

    fun updateDuration() {
        duration = 0f
        for (f in frames) {
            if (f.time > duration) {
                duration = f.time
            }
        }
    }

    fun stop() {
        listener = null
        tackTime = 0f
        frames.clear()
    }

    fun setRepeat(z: Boolean) {
        repeat = z
    }

    fun update(delta: Float) {
        if (listener != null) {
            if (repeat) {
                tackTime %= duration
            } else if (tackTime > duration) {
                return
            }
            val prv = previus
            val nxt = next
            val totalTime = nxt.time - prv.time
            val currentTime = tackTime - prv.time
            listener!!.interpolate(prv.`object`, nxt.`object`, currentTime / totalTime)
            tackTime += delta
        }
    }
}
