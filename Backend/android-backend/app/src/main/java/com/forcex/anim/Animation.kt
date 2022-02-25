package com.forcex.anim

import java.util.*

class Animation(name: String) {
    internal var duration = 0f
    internal var bones = ArrayList<Bone>()
    var name = ""

    val rootBone: Bone
        get() = bones[0]

    init {
        this.name = name
    }

    fun addBone(bone: Bone) {
        bones.add(bone)
    }

    fun findBone(index: Int): Bone? {
        for (i in bones.indices) {
            if (bones[i].boneID == index) {
                return bones[i]
            }
        }
        return null
    }

    fun getDuration(): Float {
        if (duration == 0f) {
            for (bone in bones) {
                for (key in bone.keyframes) {
                    if (key.time > duration) {
                        duration = key.time
                    }
                }
            }
        }
        return duration
    }
}
