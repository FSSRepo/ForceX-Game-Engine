package com.forcex.gfx3d.effect.water;
import com.forcex.gfx3d.shader.*;
import com.forcex.io.*;
import com.forcex.*;
import com.forcex.gfx3d.*;
import com.forcex.math.*;
import com.forcex.gfx3d.effect.*;

public class WaterShader extends ShaderProgram{
	int mvp = -1,pvm = -1,mm = -1,mvf;
	int cp = -1,lp,lc,ip;
	int time = 0;
	
	public WaterShader(boolean ligthing,boolean reflection){
		String prefix = "";
		if(ligthing){
			prefix += "#define lightingFlag\n";
		}
		if(reflection){
			prefix += "#define reflectionFlag\n";
		}
		createProgram(
			prefix + FileUtils.readStringText(FX.homeDirectory+"shaders/water.vs"),
			prefix + FileUtils.readStringText(FX.homeDirectory+"shaders/water.fs"));
		attrib_position = getAttribLocation("vertexs");
		if(ligthing){
			pvm = getUniformLocation("projView");
			mm = getUniformLocation("modelMatrix");
			lp = getUniformLocation("lightPosition");
			lc = getUniformLocation("lightColor");
			cp = getUniformLocation("cameraPosition");
			start();
			setInt(getUniformLocation("uNormal"),2);
			if(reflection){
				setInt(getUniformLocation("uReflectTex"),3);
			}
			stop();
		}else{
			mvp = getUniformLocation("mvp");
		}
		mvf = getUniformLocation("moveFactor");
		time =  getUniformLocation("time");
		ip = getUniformLocation("isPiscine");
		start();
		setInt(getUniformLocation("uTexture"),0);
		setInt(getUniformLocation("uDudv"),1);
		stop();
	}
	public void setCamera(Camera cam){
		setMatrix4f(pvm,cam.getProjViewMatrix());
		setVector3(cp,cam.position);
	}
	public void setModelMatrix(Matrix4f modelMatrix){
		setMatrix4f(mm,modelMatrix);
	}
	public void setMVPMatrix(Matrix4f mvp){
		setMatrix4f(this.mvp,mvp);
	}
	public void setIsPicine(boolean z){
		setBoolean(ip,z);
	}
	public void setLight(Light light){
		setVector3(lp,light.getPosition());
		setColor3(lc,light.getColor());
	}
	
	public void setMoveFactor(float mf){
		setFloat(mvf,mf);
	}
	
	public void setTime(float tm){
		setFloat(time,tm);
	}
}
