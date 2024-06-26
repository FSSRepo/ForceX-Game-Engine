package com.forcex.gui.widgets;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.core.GL;
import com.forcex.gui.Drawer;
import com.forcex.gui.View;
import com.forcex.math.Maths;
import com.forcex.math.Vector2f;
import com.forcex.math.Vector3f;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;

import java.util.ArrayList;
import java.util.Iterator;

public class Radar extends View {
    // the "angle value" is the Z angle of the player
    private float angle = 0;
    private int map_texture = -1, player_icon, north_point;
    private final Vector2f worldExtents;
	private float radius = 0;
    private final Vector2f player_position;
	private final Vector2f north_position;
	private final Vector2f show_extent;
    private final RadarType type;
    private final ArrayList<MapObject> objects;
    private final Vector2f tmp = new Vector2f();
    private int vbo = -1;
    private onRadarListener listener;
    private final Color edge;
	private final Color ocean;

    public Radar(float limitX, float limitY) {
        this(limitX, limitY, 0.2f, 0.2f, RadarType.MAP_PART);
    }

    public Radar(float limitX, float limitY, float width, float height, RadarType type) {
        this.type = type;
		worldExtents = new Vector2f(limitX, limitY);
        player_position = new Vector2f();
        north_position = new Vector2f();
        show_extent = new Vector2f();
        setWidth(width);
        setHeight(height);
        objects = new ArrayList<>();
        edge = new Color(0, 0, 0);
        ocean = new Color(50, 205, 180);
    }

    public void setShowExtent(float limX, float limY) {
        show_extent.set(limX / worldExtents.x, limY / worldExtents.y);
    }

    public void add(MapObject obj) {
        objects.add(obj);
    }

    public void remove(MapObject obj) {
        objects.remove(obj);
    }

    public void setMapTexture(int texture) {
        map_texture = texture;
    }

    public void setPlayerIcon(int texture) {
        player_icon = texture;
    }

    public void setNorthIcon(int icon) {
        north_point = icon;
    }

    public boolean hasMapTexture() {
        return map_texture != -1;
    }

    public void setRadarListener(onRadarListener listener) {
        this.listener = listener;
        listener.rad = this;
    }

    public void update(Vector3f posplayer, float rotationZ) {
        angle = rotationZ;
        player_position.set(
                Maths.clamp(posplayer.x / worldExtents.x, -1, 1),
                Maths.clamp(posplayer.y / worldExtents.y, -1, 1));
    }

    @Override
    public void onDraw(Drawer drawer) {
        if (type == RadarType.MAP_ALL) {
            drawer.setScale(extent.x, extent.y);
            drawer.renderQuad(local, null, map_texture);
            Iterator<MapObject> it = objects.iterator();
            while (it.hasNext()) {
                MapObject o = it.next();
                drawer.setScale(extent.x * 0.04f, extent.y * 0.04f);
                drawer.renderQuad(o.getRadarPosition(local, extent.x, extent.y, worldExtents.x, worldExtents.y), null, o.icon);
            }
            // render north
            north_position.set(local.x, local.y + extent.y);
            drawer.setScale(extent.x * 0.08f, extent.y * 0.08f);
            drawer.renderQuad(north_position, null, north_point);
            // player icon
            drawer.setTransform(angle, width * 0.05f, height * 0.05f);
            drawer.renderQuad(getPlayerPosition(), null, player_icon);
        } else {
            radius = extent.length();
            updateVbo();
            drawer.scissorArea(local.x, local.y, extent.x, extent.y);
            drawer.setScale(extent.x, extent.y);
            drawer.renderQuad(local, ocean, -1);
            drawer.setTransform(-angle, 1, 1);
            drawer.freeRender(vbo, local, null, map_texture);
            FX.gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
            drawer.finishScissor();
            drawer.setScale(extent.x, extent.y);
            drawer.renderLineQuad(local, edge);
            // player icon
            drawer.setScale(extent.x * 0.1f, extent.y * 0.1f);
            drawer.renderQuad(local, null, player_icon);
            // render north
            drawer.setScale(extent.x * 0.08f, extent.y * 0.08f);
            drawer.renderQuad(updateNorth(), null, north_point);
            Iterator<MapObject> it = objects.iterator();
            while (it.hasNext()) {
                MapObject o = it.next();
                o.getRadarPosition(local, extent.x, extent.y, worldExtents.x, worldExtents.y);
                if (!o.isInZone(player_position, show_extent)) {
                    drawer.renderQuad(o.noZonePosition(player_position, radius, local, extent.x, extent.y, angle), null, o.icon);
                } else {
                    drawer.renderQuad(o.ZonePosition(player_position, local, show_extent, extent.x, extent.y, angle), null, o.icon);
                }
            }
        }
    }

    private void updateVbo() {
        float[] vertices = new float[]{
                -1, 1, 0, 0, 1, 1, 1,
                -1, -1, 0, 1, 1, 1, 1,
                1, 1, 1, 0, 1, 1, 1,
                1, -1, 1, 1, 1, 1, 1
        };
        float map_x = extent.x / show_extent.x;
        float map_y = extent.y / show_extent.y;
        for (byte i = 0; i < vertices.length; i += 7) {
            vertices[i] = map_x * (vertices[i] - player_position.x);
            vertices[i + 1] = map_y * (vertices[i + 1] - player_position.y);
        }
        if (vbo == -1) {
            vbo = Drawer.createBuffer(vertices, false, true);
        } else {
            Drawer.updateBuffer(vbo, vertices);
        }
    }

    private Vector2f updateNorth() {
        float north = (angle + 90.0f) * Maths.toRadians;
        float cx = radius * Maths.cos(north);
        float cy = radius * Maths.sin(north);
        north_position.set(local.x + Maths.clamp(cx, -extent.x, extent.x), local.y + Maths.clamp(cy, -extent.y, extent.y));
        return north_position;
    }

    private Vector2f getPlayerPosition() {
        tmp.set(player_position).multLocal(extent.x, extent.y).addLocal(local);
        return tmp;
    }

    @Override
    public void onTouch(float x, float y, byte type) {
        if (type == EventType.TOUCH_PRESSED) {
            Vector3f player = new Vector3f(
                    ((x - local.x) * worldExtents.x) / extent.x,
                    ((y - local.y) * worldExtents.y) / extent.y,
                    10
            );
            if (listener != null) {
                listener.onPressPoint(player);
            }
        }
    }

    @Override
    public boolean testTouch(float x, float y) {
        return isVisible() && GameUtils.testRect(x, y, local, getExtentWidth(), getExtentHeight());
    }

    public enum RadarType {
        MAP_PART,
        MAP_ALL
    }

    public static class MapObject {
        public Vector3f pos_world;
        public Vector2f pos_radar;
        public int icon;
        boolean dynamic;

        public MapObject(boolean dynamic, int icon, Vector3f world) {
            this.dynamic = dynamic;
            this.icon = icon;
            pos_world = world;
        }

        boolean isInZone(Vector2f player_pos, Vector2f limit) {
            return
                    pos_radar.x <= (player_pos.x + limit.x) &&
                            pos_radar.x >= (player_pos.x - limit.x) &&
                            pos_radar.y <= (player_pos.y + limit.y) &&
                            pos_radar.y >= (player_pos.y - limit.y);
        }

        Vector2f getRadarPosition(Vector2f pos, float w, float h, float wx, float wy) {
            if (dynamic || pos_radar == null) {
                if (pos_radar == null) {
                    pos_radar = new Vector2f();
                }
                pos_radar.set(Maths.clamp(pos_world.x / wx, -1, 1), Maths.clamp(pos_world.y / wy, -1, 1));
            }
            return pos_radar.mult(w, h).addLocal(pos);
        }

        Vector2f noZonePosition(
                Vector2f player_pos, float radius, Vector2f pos, float w, float h, float angle) {
            float ang = (angle + getAngleFrom(player_pos)) * Maths.toRadians;
            float cx = radius * Maths.cos(ang);
            float cy = radius * Maths.sin(ang);
            return new Vector2f(
                    Maths.clamp(cx, -w, w),
                    Maths.clamp(cy, -h, h)).addLocal(pos);
        }

        Vector2f ZonePosition(
                Vector2f player_pos, Vector2f pos, Vector2f limit, float w, float h, float angle) {
            Vector2f into = pos_radar.sub(player_pos);
            into.set(into.x / limit.x, into.y / limit.y);
            into.multLocal(w, h);
            float cs = Maths.cos(angle * Maths.toRadians);
            float sn = Maths.sin(angle * Maths.toRadians);
            into.set((into.x * cs) + (-sn * into.y), (into.x * sn) + (cs * into.y));
            return pos.add(into.set(Maths.clamp(into.x, -w, w), Maths.clamp(into.y, -h, h)));
        }

        float getAngleFrom(Vector2f player) {
            Vector2f dir = pos_radar.sub(player).normalize();
            if (dir.x >= 0 && dir.y >= 0)
                return Maths.atan(dir.y / dir.x) * Maths.toDegrees;
            else if (dir.x < 0 && dir.y >= 0)
                return (Maths.atan(dir.y / dir.x) * Maths.toDegrees) + 180;
            else if (dir.x < 0 && dir.y < 0)
                return (Maths.atan(dir.y / dir.x) * Maths.toDegrees) + 180;
            else if (dir.x >= 0 && dir.y < 0)
                return (Maths.atan(dir.y / dir.x) * Maths.toDegrees) + 360;
            return 0;
        }
    }

    public static abstract class onRadarListener {
        Radar rad;

        public abstract void onPressPoint(Vector3f world_position);

        public void addCheckPoint(MapObject point) {
            rad.add(point);
        }
    }
}
