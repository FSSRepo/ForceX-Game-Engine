attribute vec2 positions;
#ifdef blurx4
varying vec2 blurTexcoords[7];
#else
varying vec2 blurTexcoords[3];
#endif

void main(){
	gl_Position = vec4(positions,0.0,1.0);
	vec2 centerTex = positions * 0.5 + 0.5;
	#ifdef blurx4
		#ifdef blurVertical
			for(float i = -3.0;i <= 3.0;i++){
				blurTexcoords[int(i) + 3] = centerTex + vec2(0.0,pixelSize * i);
			}
		#else
			for(float i = -3.0;i <= 3.0;i++){
				blurTexcoords[int(i) + 3] = centerTex + vec2(pixelSize * i,0.0);
			}
		#endif
	#else
		#ifdef blurVertical
			for(float i = -1.0;i <= 1.0;i++){
				blurTexcoords[int(i) + 1] = centerTex + vec2(0.0,pixelSize * i);
			}
		#else
			for(float i = -1.0;i <= 1.0;i++){
				blurTexcoords[int(i) + 1] = centerTex + vec2(pixelSize * i,0.0);
			}
		#endif
	#endif
}
