package com.forcex.gfx3d.effect;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.gfx3d.Camera;
import com.forcex.gfx3d.shader.SpriteShader;
import com.forcex.math.Matrix4f;
import com.forcex.math.Vector3f;
import com.forcex.utils.BufferUtils;

public class BillboardRenderer {
    private Camera camera;
    private final GL gl = FX.gl;
    private SpriteShader shader;
    private final int vbo;
    private final Matrix4f temp = new Matrix4f();

    public BillboardRenderer() {
        shader = new SpriteShader(false, false, true);
        vbo = gl.glGenBuffer();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        float[] vertices = {
                -1, 1, 0, 0,
                -1, -1, 0, 1,
                1, 1, 1, 0,
                1, -1, 1, 1
        };
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertices.length * 4, BufferUtils.createFloatBuffer(vertices), GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    public void begin(Camera camera) {
        this.camera = camera;
        shader.start();
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDepthMask(false);
    }

    public void end() {
        gl.glDepthMask(true);
        gl.glDisable(GL.GL_BLEND);
        shader.stop();
    }

    public void render(BillboardObject vtc) {
        shader.setMVPMatrix(updateMVP(vtc.position, vtc.scale, camera));
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        gl.glVertexAttribPointer(shader.attribute_vertex, 4, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(shader.attribute_vertex);
        shader.setSpriteColor(vtc.color);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, vtc.texture);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
    }

    private Matrix4f updateMVP(Vector3f position, float scale, Camera cam) {
        Matrix4f vm = cam.getViewMatrix();
        Matrix4f mm = new Matrix4f();
        mm.setLocation(position);
        mm.data[Matrix4f.M00] = vm.data[Matrix4f.M00];
        mm.data[Matrix4f.M01] = vm.data[Matrix4f.M10];
        mm.data[Matrix4f.M02] = vm.data[Matrix4f.M20];

        mm.data[Matrix4f.M10] = vm.data[Matrix4f.M01];
        mm.data[Matrix4f.M11] = vm.data[Matrix4f.M11];
        mm.data[Matrix4f.M12] = vm.data[Matrix4f.M21];

        mm.data[Matrix4f.M20] = vm.data[Matrix4f.M02];
        mm.data[Matrix4f.M21] = vm.data[Matrix4f.M12];
        mm.data[Matrix4f.M22] = vm.data[Matrix4f.M22];
        vm.mult(temp, mm);
        temp.multLocal(Matrix4f.scale(scale));
        return cam.getProjectionMatrix().mult(null, temp);
    }

    public void delete() {
        gl.glDeleteBuffer(vbo);
        shader.cleanUp();
        shader = null;
    }
}
