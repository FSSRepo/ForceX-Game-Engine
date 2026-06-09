uniform mat4 uMVPMatrix;
attribute vec3 aPosition;

#ifdef filterAlpha
attribute vec2 aTexCoords;
varying vec2 vTexcoords;
#endif

#ifdef useSkinning
attribute vec4 aBoneWeights;
attribute vec4 aBoneIndices;
uniform mat4 uBoneMatrices[32];
uniform bool isAnimation;
#endif

void main() {
	#ifdef useSkinning
	mat4 skin = mat4(1.0);
	if(isAnimation){
		skin = 	uBoneMatrices[int(aBoneIndices.x)] * aBoneWeights.x;
		skin += uBoneMatrices[int(aBoneIndices.y)] * aBoneWeights.y;
		skin += uBoneMatrices[int(aBoneIndices.z)] * aBoneWeights.z;
		skin += uBoneMatrices[int(aBoneIndices.w)] * aBoneWeights.w;
	}
	gl_Position = uMVPMatrix * skin * vec4(aPosition,1.0);
	#else
	gl_Position = uMVPMatrix * vec4(aPosition,1.0);
	#endif
	
	#ifdef filterAlpha
	vTexcoords = aTexCoords;
	#endif
}
