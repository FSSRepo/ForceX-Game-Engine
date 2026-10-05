package com.forcex.gui.widgets;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.gui.Drawer;
import com.forcex.gui.UIContext;
import com.forcex.gui.View;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;

public class Table extends View {
    private final GL gl = FX.gl;
    String[][] data;
    TextView tv;
    float cell_width, cell_height;
    float[] widths;
    private short[] dimens;
    private int lines_vbo, ibo, length;
    private Color background_color, edge_color;

    public Table(float width, float height) {
        setWidth(width);
        setHeight(height);
        background_color = new Color(Color.WHITE);
        edge_color = new Color(0, 0, 0);
        dimens = new short[2];
    }

    public void setContent(String[][] data, float... width) {
        this.data = data;
        dimens[0] = (short) data.length;
        for (String[] c : data) {
            if (dimens[1] < c.length) {
                dimens[1] = (short) c.length;
            }
        }
        if (dimens[1] == width.length) {
            float sum = 0;
            for (float w : width) {
                sum += w;
            }
            if (sum == 1) {
                this.widths = width;
            }
        }
    }

    @Override
    public void onCreate(Drawer drawer) {
        float[] vertices = new float[28 + ((dimens[0] - 1) * 14) + ((dimens[1] - 1) * 14)];
        short[] indices = new short[8 + ((dimens[0] - 1) * 2) + ((dimens[1] - 1) * 2)];
        length = indices.length;
        put(vertices, 0, -extent.x, extent.y);
        put(vertices, 1, -extent.x, -extent.y);
        put(vertices, 2, extent.x, -extent.y);
        put(vertices, 3, extent.x, extent.y);
        int i = 0;
        for (i = 0; i < 4; i++) {
            if (i != 3) {
                put(indices, i, i, i + 1);
            } else {
                put(indices, i, i, 0);
            }
        }
        cell_width = (extent.x * 2f) / dimens[1];
        cell_height = (extent.y * 2f) / dimens[0];
        tv = new TextView(UIContext.default_font);
        tv.setTextSize(cell_height * 0.5f);
        tv.setTextColor(0, 0, 0);
        int vec = 4;
        for (short x = 1; x < dimens[0]; x++) {
            float offset_height = (extent.y - (cell_height * x));
            put(vertices, vec, -extent.x, offset_height);
            put(vertices, vec + 1, extent.x, offset_height);
            put(indices, i, vec, vec + 1);
            i++;
            vec += 2;
        }
        float offset_x = -extent.x;
        for (short y = 1; y < dimens[1]; y++) {
            if (widths != null) {
                offset_x += widths[y - 1] * extent.x * 2f;
            } else {
                offset_x += cell_width;
            }
            put(vertices, vec, offset_x, -extent.y);
            put(vertices, vec + 1, offset_x, extent.y);
            put(indices, i, vec, vec + 1);
            i++;
            vec += 2;
        }
        lines_vbo = Drawer.createBuffer(vertices, false, false);
        ibo = Drawer.createBuffer(indices, true, false);
        cell_height *= 0.5f;
        cell_width *= 0.5f;
    }

    private void put(float[] a, int vec, float x, float y) {
        a[vec * 7] = x;
        a[vec * 7 + 1] = y;
        a[vec * 7 + 2] = 0;
        a[vec * 7 + 3] = 0;
        a[vec * 7 + 4] = 1;
        a[vec * 7 + 5] = 1;
        a[vec * 7 + 6] = 1;
    }

    private void put(short[] a, int vec, int sidx, int eidx) {
        a[vec * 2] = (short) sidx;
        a[vec * 2 + 1] = (short) eidx;
    }

    @Override
    public void onDraw(Drawer drawer) {
        drawer.setScale(extent.x, extent.y);
        drawer.renderQuad(local, background_color, -1);
        drawer.setScale(1, 1);
        drawer.freeRender(lines_vbo, local, edge_color, -1);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
        gl.glDrawElements(GL.GL_LINES, length);
        float oy = (local.y + extent.y) - cell_height;
		for (String[] row : data) {
			float ox = (local.x - extent.x) + (widths == null ?
					cell_width : 0);
			for (byte x = 0; x < row.length; x++) {
				if (widths != null) {
					ox += widths[x] * extent.x;
				}
				tv.local.set(ox, oy);
				tv.setText(row[x]);
				tv.onDraw(drawer);
				if (widths == null) {
					ox += cell_width * 2;
				} else if (x < (row.length - 1)) {
					ox += widths[x] * extent.x;
				}
			}
			oy -= cell_height * 2;
		}
    }

    @Override
    public boolean testTouch(float x, float y) {
        return isVisible() && GameUtils.testRect(x, y, local, getExtentWidth(), getExtentHeight());
    }

    @Override
    public void onDestroy() {
        setVisibility(INVISIBLE);
        dimens = null;
        gl.glDeleteBuffer(lines_vbo);
        gl.glDeleteBuffer(ibo);
        data = null;
        tv.onDestroy();
        tv = null;
        background_color = null;
        edge_color = null;
    }
}
