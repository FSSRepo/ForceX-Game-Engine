attribute vec2 vertexs;
uniform vec4 transform;
varying vec2 texcoords;

void main(){
	texcoords = vertexs * 0.5 + 0.5;
	vec2 sp = (vertexs * transform.zw) + transform.xy;
	sp.x = sp.x * 2.0 - 1.0;
	sp.y = sp.y * -2.0 + 1.0;
	gl_Position = vec4(sp,0.0,1.0);
}
