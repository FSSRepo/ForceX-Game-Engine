package com.forcex.gfx3d.particle;

import com.forcex.gfx3d.shader.ShaderProgram;
import com.forcex.math.Matrix4f;
import com.forcex.math.Vector4f;
import com.forcex.utils.Color;

/** Shader de billboards de particulas: textura * color (el alfa del color se usa para el desvanecido). */
public class ParticleShader extends ShaderProgram {
    private static final String VERTEX =
            "attribute vec4 vertex;\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "uniform vec4 uvRect;\n" +
            "varying vec2 texcoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * vec4(vertex.xy, 0.0, 1.0);\n" +
            "    texcoord = uvRect.xy + vertex.zw * uvRect.zw;\n" +
            "}\n";

    private static final String FRAGMENT =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec2 texcoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform vec4 color;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(uTexture, texcoord) * color;\n" +
            "}\n";

    public final int attribute_vertex;
    private final int u_MVPMatrix;
    private final int u_color;
    private final int u_uvRect;
    private final Vector4f uv_rect = new Vector4f();

    public ParticleShader() {
        createProgram(VERTEX, FRAGMENT, "");
        attribute_vertex = getAttribLocation("vertex");
        u_MVPMatrix = getUniformLocation("uMVPMatrix");
        u_color = getUniformLocation("color");
        u_uvRect = getUniformLocation("uvRect");
        start();
        setInt(getUniformLocation("uTexture"), 0);
        stop();
    }

    public void setMVPMatrix(Matrix4f matrix) {
        setMatrix4f(u_MVPMatrix, matrix);
    }

    /** Zona de la textura que cubre el sprite: origen (u, v) y tamano (du, dv). */
    public void setUvRect(float u, float v, float du, float dv) {
        setVector4(u_uvRect, uv_rect.set(u, v, du, dv));
    }

    public void setParticleColor(Color color) {
        setColor4(u_color, color);
    }
}
