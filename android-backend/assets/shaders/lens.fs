#ifdef GL_ES
precision mediump float;
#endif

varying vec2 texcoords;
uniform sampler2D texture;
uniform float brightness;

void main(){
	gl_FragColor = texture2D(texture,texcoords);
	gl_FragColor.a *= brightness;
}
