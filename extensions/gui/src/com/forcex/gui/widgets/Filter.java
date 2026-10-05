package com.forcex.gui.widgets;

import com.forcex.FX;
import com.forcex.app.threading.Task;

import java.util.ArrayList;

public class Filter {
    protected ListAdapter adapter;
    private ArrayList<Object> items = new ArrayList<>();
    private ArrayList<Object> filtering = new ArrayList<>();
    private final OnFilteringListener listener;

    public Filter(OnFilteringListener listener) {
        this.listener = listener;
    }

    public void add(Object item) {
        items.add(item);
        filter(false, "");
    }

    protected void update() {
        for (int i = 0; i < adapter.getNumItem(); i++) {
            items.add(adapter.getItem(i));
        }
    }

    protected int getOriginalPosition(Object item) {
        return items.indexOf(item);
    }

    public void filter(boolean multi_thread, Object... args) {
        filtering.clear();
        for (Object o : items) {
            if (listener.filter(o, args)) {
                filtering.add(o);
            }
        }
        if (!multi_thread) {
            updateList();
        } else {
            FX.gpu.queueTask(new Task() {
                @Override
                public boolean execute() {
                    updateList();
                    return true;
                }
            });
        }
    }

    private void updateList() {
        adapter.removeAll();
        for (Object o : filtering) {
            adapter.add(o);
        }
    }

    public void delete() {
        items.clear();
        filtering.clear();
        filtering = null;
        items = null;
    }

    public interface OnFilteringListener {
        boolean filter(Object item, Object[] args);
    }
}
