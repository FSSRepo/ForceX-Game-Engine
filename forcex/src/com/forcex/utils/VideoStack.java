package com.forcex.utils;

import com.forcex.app.threading.Task;
import com.forcex.io.VideoClip;

import java.util.ArrayList;
import java.util.ListIterator;

public class VideoStack implements Task {
    ArrayList<VideoClip> clips = new ArrayList<>();
    VideoClip add;
    VideoClip remove;
    boolean running = false;

    public void add(VideoClip clip) {
        if (running) {
            add = clip;
            return;
        }
        clips.add(clip);
    }

    public void update() {
        if (add != null && !running) {
            clips.add(add);
            add = null;
        }
        if (remove != null) {
            clips.remove(remove);
            remove = null;
        }
        for (VideoClip c : clips) {
            c.update();
        }
    }

    @Override
    public boolean execute() {
        ListIterator<VideoClip> it = clips.listIterator();
        VideoClip queue = null;
        running = true;
        while (it.hasNext()) {
            VideoClip c = it.next();
            if (c.perform()) {
                queue = c;
            }
        }
        running = false;
        remove = queue;
        while (remove != null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        queue = null;
        return clips.size() == 0;
    }
}
