attribute vec3 aPosition;
attribute vec2 aTexCoord;

uniform mat4 uProjViewMatrix;
uniform mat3 uNormalMatrix;
uniform mat4 uModelMatrix;
uniform float uPointSize;
uniform vec3 uCameraPosition;

varying vec2 vTexCoord;

#ifdef colorFlag
attribute vec4 aColor;
varying vec4 vColor;
#endif

#ifdef animFlag
attribute vec4 aBoneWeights;
attribute vec4 aBoneIndices;
uniform int useSkinning;
uniform mat4 u_boneMatrices[32];
#endif

#ifdef fogFlag
uniform vec2 ufogParams;
varying float vFogEffect;
#endif

#ifdef lightingFlag
attribute vec3 aNormal;
uniform vec3 lightPosition;
varying vec3 vNormal;
varying vec3 L;
varying vec3 V;
uniform bool mat_useNormalMap;
#endif

#ifdef normalMapFlag
attribute vec3 aTangent;
attribute vec3 aBitangent;
varying mat3 TBN;
#endif

#ifdef reflectionCubeMapFlag
#ifndef lightingFlag
attribute vec3 aNormal;
#endif
varying vec3 vReflectedCoords;
#endif

#ifdef shadowMapFlag
uniform mat4 uShadowMatrix;
varying vec4 vShadowCoords;
#endif

#ifdef clipPlane
uniform vec4 uClipPlane;
varying float clip_factor;
#endif

void main(){
	#ifdef animFlag
		mat4 skin = mat4(1.0);
		if(useSkinning == 1){
			skin =		u_boneMatrices[int(aBoneIndices.x)] * aBoneWeights.x;
			skin += 	u_boneMatrices[int(aBoneIndices.y)] * aBoneWeights.y;
			skin += 	u_boneMatrices[int(aBoneIndices.z)] * aBoneWeights.z;
			skin += 	u_boneMatrices[int(aBoneIndices.w)] * aBoneWeights.w;
		}
		vec4 pos = uModelMatrix * skin * vec4(aPosition,1.0);
	#else
		vec4 pos = uModelMatrix * vec4(aPosition,1.0);
	#endif
	
	gl_PointSize = uPointSize;
	float distance = length(uCameraPosition - pos.xyz);
	vTexCoord = aTexCoord;
	
	#ifdef reflectionCubeMapFlag
	vec3 view = normalize(pos.xyz - uCameraPosition);
	vReflectedCoords = reflect(view,normalize(aNormal));
	#endif
	
	#ifdef colorFlag
		vColor = aColor;
	#endif
	
	#ifdef lightingFlag
		#ifndef normalMapFlag
			#ifdef animFlag
				vNormal = (uNormalMatrix * mat3(skin) * aNormal);
			#else
				vNormal = (uNormalMatrix * aNormal);
			#endif
		#else
			if(mat_useNormalMap){
				#ifdef animFlag
					vec3 norm = normalize(uNormalMatrix * mat3(skin) * aNormal);
					vec3 tang = normalize(uNormalMatrix * mat3(skin) * aTangent);
					vec3 bitang = normalize(uNormalMatrix * mat3(skin) * aBitangent);
				#else
					vec3 norm = normalize(uNormalMatrix * aNormal);
					vec3 tang = normalize(uNormalMatrix * aTangent);
					vec3 bitang = normalize(uNormalMatrix * aBitangent);
				#endif
				TBN = mat3(tang,bitang,norm);
			}else{
				vNormal = normalize(uNormalMatrix * aNormal);
			}
		#endif
		L = normalize(lightPosition - pos.xyz);
		V = normalize(uCameraPosition - pos.xyz);
	#endif
	
	#ifdef shadowMapFlag
		vShadowCoords = uShadowMatrix * pos;
		vShadowCoords.xyz = (vShadowCoords.xyz / vShadowCoords.w) * 0.5 + 0.5;
		vShadowCoords.z = min(vShadowCoords.z, 0.998);
	#endif
	gl_Position = uProjViewMatrix * pos;
	
	#ifdef clipPlane
		clip_factor = dot(pos.xyz,uClipPlane.xyz) + uClipPlane.w;
	#endif
	
	#ifdef fogFlag
		vFogEffect = clamp((ufogParams.x - distance) / ufogParams.y,0.0,1.0);
	#endif
}
