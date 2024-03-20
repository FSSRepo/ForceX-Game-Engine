#ifdef GL_ES
precision mediump float;
#endif

#ifdef blurx4
varying vec2 blurTexcoords[7];
#else
varying vec2 blurTexcoords[3];
#endif
uniform sampler2D texture;

void main(){
	vec4 color = vec4(0.0);
	#ifdef blurx4
		color += texture2D(texture,blurTexcoords[0]) * 0.081;
		color += texture2D(texture,blurTexcoords[1]) * 0.140;
		color += texture2D(texture,blurTexcoords[2]) * 0.199;
		color += texture2D(texture,blurTexcoords[3]) * 0.221;
		color += texture2D(texture,blurTexcoords[4]) * 0.199;
		color += texture2D(texture,blurTexcoords[5]) * 0.140;
		color += texture2D(texture,blurTexcoords[6]) * 0.081;
	#else
		color += texture2D(texture,blurTexcoords[0]) * 0.280;
		color += texture2D(texture,blurTexcoords[1]) * 0.442;
		color += texture2D(texture,blurTexcoords[2]) * 0.280;
	#endif
	gl_FragColor = vec4(color.rgb,1.0);
}
