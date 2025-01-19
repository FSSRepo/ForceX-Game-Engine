package com.forcex.gfx3d.shader;

import com.forcex.math.Matrix2f;
import com.forcex.math.Matrix4f;
import com.forcex.math.Vector2f;
import com.forcex.utils.Color;

public class SpriteShader extends ShaderProgram {
    public int attribute_vertex;
    public int attribute_colors;
    Vector2f tmp = new Vector2f();
    Color temp_color;
    int u_MVPMatrix;
    int u_Position;
    int u_TexInfo;
    int u_TexOffset1;
    int u_TexOffset2;
    int u_blend;
    int u_color;
    int u_mat2d;
    boolean use2d;
    boolean useBlendTextured;
    boolean useColor;

    public SpriteShader(boolean use2d, boolean useBlendTex, boolean useColor) {
        this.use2d = use2d;
        this.useBlendTextured = useBlendTex;
        this.useColor = useColor;
        temp_color = new Color(Color.WHITE);
        String prefix = "";
        if (use2d) {
            prefix += "#define useIn2dFlag\n";
        }
        if (useBlendTex) {
            prefix += "#define useBlendTextured\n";
        }
        if (useColor) {
            prefix += "#define useColor\n";
        }
        createProgram("shaders/sprite.vs", "shaders/sprite.fs", prefix);
        attribute_vertex = getAttribLocation("vertex");
        attribute_colors = getAttribLocation("aColor");
        if (use2d) {
            u_Position = getUniformLocation("position");
            u_mat2d = getUniformLocation("mat2d");
        } else {
            u_MVPMatrix = getUniformLocation("uMVPMatrix");
        }
        if (useBlendTex) {
            u_TexOffset1 = getUniformLocation("texoffset1");
            u_TexOffset2 = getUniformLocation("texoffset2");
            u_TexInfo = getUniformLocation("texinfo");
        }
        if (useColor) {
            u_color = getUniformLocation("color");
        }
        start();
        setInt(getUniformLocation("uTexture"), 0);
        stop();
    }

    public void setSpriteColor(Color color) {
        if (useColor) {
            if (color == null) {
                setColor4(u_color, temp_color);
                return;
            }
            setColor4(u_color, color);
        }
    }

    public void setSpriteTexInfo(float numRows, float blend) {
        if (useBlendTextured) {
            setVector2(u_TexInfo, tmp.set(numRows, blend));
        }
    }

    public void setSpriteTexOffsets(Vector2f ofs1, Vector2f ofs2) {
        if (useBlendTextured) {
            setVector2(u_TexOffset1, ofs1);
            setVector2(u_TexOffset2, ofs2);
        }
    }

    public void setSpriteTransform(Vector2f v, Matrix2f matrix) {
        if (use2d) {
            setVector2(u_Position, v);
            setMatrix2f(u_mat2d, matrix);
        }
    }

    public void setSpriteTransform(Matrix2f matrix) {
        if (use2d) {
            setMatrix2f(u_mat2d, matrix);
        }
    }

    public void setMVPMatrix(Matrix4f m4) {
        setMatrix4f(u_MVPMatrix, m4);
    }
}
