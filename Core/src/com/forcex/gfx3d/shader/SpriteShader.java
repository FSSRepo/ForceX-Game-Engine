package com.forcex.gfx3d.shader;
import com.forcex.utils.*;
import com.forcex.math.*;
import com.forcex.io.*;
import com.forcex.*;

public class SpriteShader extends ShaderProgram {
    public int attribute_vertex;
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
        createProgram(prefix + FileUtils.readStringText(FX.homeDirectory+"shaders/sprite.vs"),prefix + FileUtils.readStringText(FX.homeDirectory+"shaders/sprite.fs"));
        attribute_vertex = getAttribLocation("vertex");
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
		setInt(getUniformLocation("uTexture"),0);
		stop();
    }

    public void setSpriteColor(Color color) {
        if (useColor) {
			if(color == null){
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
	
	public void setSpriteTransform(Matrix2f matrix){
		if(use2d){
			setMatrix2f(u_mat2d,matrix);
		}
	}

    public void setMVPMatrix(Matrix4f m4) {
        setMatrix4f(u_MVPMatrix, m4);
    }
}
