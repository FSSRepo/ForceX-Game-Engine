#pragma once

// ---------------------------------------------------------------------------
//  Carga mínima de OpenGL para fxrmlui.
//
//  Android: se enlaza directamente contra libGLESv2 (GLES 2.0), cuyas
//           funciones y tipos vienen de <GLES2/gl2.h>.
//  Desktop: las funciones de OpenGL 1.1 se enlazan con la librería del
//           sistema y las de OpenGL 2.0+ (shaders, VBO, VAO) se resuelven en
//           tiempo de ejecución, porque en Windows opengl32.dll sólo exporta
//           OpenGL 1.1. La carga debe ocurrir con un contexto GL activo.
// ---------------------------------------------------------------------------

#include <cstddef>

#if defined(__ANDROID__)
	#include <GLES2/gl2.h>
	#define FX_GL_ES 1
#else
	#define FX_GL_ES 0

	#if defined(_WIN32)
		#ifndef WIN32_LEAN_AND_MEAN
			#define WIN32_LEAN_AND_MEAN
		#endif
		#ifndef NOMINMAX
			#define NOMINMAX
		#endif
		#include <windows.h>
	#endif

	#include <GL/gl.h>

	// Tipos no garantizados por <GL/gl.h> en todas las plataformas.
	typedef std::ptrdiff_t FxGLsizeiptr;
	typedef std::ptrdiff_t FxGLintptr;
	typedef char FxGLchar;
#endif

// Constantes usadas por el renderer. En Android ya vienen de gl2.h.
#ifndef GL_TRIANGLES
	#define GL_TRIANGLES 0x0004
#endif
#ifndef GL_UNSIGNED_BYTE
	#define GL_UNSIGNED_BYTE 0x1401
#endif
#ifndef GL_UNSIGNED_SHORT
	#define GL_UNSIGNED_SHORT 0x1403
#endif
#ifndef GL_UNSIGNED_INT
	#define GL_UNSIGNED_INT 0x1405
#endif
#ifndef GL_FLOAT
	#define GL_FLOAT 0x1406
#endif
#ifndef GL_TEXTURE_2D
	#define GL_TEXTURE_2D 0x0DE1
#endif
#ifndef GL_RGBA
	#define GL_RGBA 0x1908
#endif
#ifndef GL_RGBA8
	#define GL_RGBA8 0x8058
#endif
#ifndef GL_TEXTURE_MIN_FILTER
	#define GL_TEXTURE_MIN_FILTER 0x2801
#endif
#ifndef GL_TEXTURE_MAG_FILTER
	#define GL_TEXTURE_MAG_FILTER 0x2800
#endif
#ifndef GL_TEXTURE_WRAP_S
	#define GL_TEXTURE_WRAP_S 0x2802
#endif
#ifndef GL_TEXTURE_WRAP_T
	#define GL_TEXTURE_WRAP_T 0x2803
#endif
#ifndef GL_LINEAR
	#define GL_LINEAR 0x2601
#endif
#ifndef GL_REPEAT
	#define GL_REPEAT 0x2901
#endif
#ifndef GL_CLAMP_TO_EDGE
	#define GL_CLAMP_TO_EDGE 0x812F
#endif
#ifndef GL_TEXTURE0
	#define GL_TEXTURE0 0x84C0
#endif
#ifndef GL_ARRAY_BUFFER
	#define GL_ARRAY_BUFFER 0x8892
#endif
#ifndef GL_ELEMENT_ARRAY_BUFFER
	#define GL_ELEMENT_ARRAY_BUFFER 0x8893
#endif
#ifndef GL_STATIC_DRAW
	#define GL_STATIC_DRAW 0x88E4
#endif
#ifndef GL_VERTEX_SHADER
	#define GL_VERTEX_SHADER 0x8B31
#endif
#ifndef GL_FRAGMENT_SHADER
	#define GL_FRAGMENT_SHADER 0x8B30
#endif
#ifndef GL_COMPILE_STATUS
	#define GL_COMPILE_STATUS 0x8B81
#endif
#ifndef GL_LINK_STATUS
	#define GL_LINK_STATUS 0x8B82
#endif
#ifndef GL_INFO_LOG_LENGTH
	#define GL_INFO_LOG_LENGTH 0x8B84
#endif
#ifndef GL_BLEND
	#define GL_BLEND 0x0BE2
#endif
#ifndef GL_ONE
	#define GL_ONE 1
#endif
#ifndef GL_ONE_MINUS_SRC_ALPHA
	#define GL_ONE_MINUS_SRC_ALPHA 0x0303
#endif
#ifndef GL_DEPTH_TEST
	#define GL_DEPTH_TEST 0x0B71
#endif
#ifndef GL_CULL_FACE
	#define GL_CULL_FACE 0x0B44
#endif
#ifndef GL_SCISSOR_TEST
	#define GL_SCISSOR_TEST 0x0C11
#endif
#ifndef GL_STENCIL_TEST
	#define GL_STENCIL_TEST 0x0B90
#endif
#ifndef GL_ALWAYS
	#define GL_ALWAYS 0x0207
#endif
#ifndef GL_EQUAL
	#define GL_EQUAL 0x0202
#endif
#ifndef GL_KEEP
	#define GL_KEEP 0x1E00
#endif
#ifndef GL_REPLACE
	#define GL_REPLACE 0x1E01
#endif
#ifndef GL_INCR
	#define GL_INCR 0x1E02
#endif
#ifndef GL_COLOR_BUFFER_BIT
	#define GL_COLOR_BUFFER_BIT 0x00004000
#endif
#ifndef GL_STENCIL_BUFFER_BIT
	#define GL_STENCIL_BUFFER_BIT 0x00000400
#endif
#ifndef GL_VIEWPORT
	#define GL_VIEWPORT 0x0BA2
#endif
#ifndef GL_SCISSOR_BOX
	#define GL_SCISSOR_BOX 0x0C10
#endif
#ifndef GL_STENCIL_REF
	#define GL_STENCIL_REF 0x0B97
#endif
#ifndef GL_STENCIL_FUNC
	#define GL_STENCIL_FUNC 0x0B92
#endif
#ifndef GL_STENCIL_VALUE_MASK
	#define GL_STENCIL_VALUE_MASK 0x0B93
#endif
#ifndef GL_STENCIL_WRITEMASK
	#define GL_STENCIL_WRITEMASK 0x0B98
#endif
#ifndef GL_COLOR_WRITEMASK
	#define GL_COLOR_WRITEMASK 0x0C23
#endif
#ifndef GL_UNPACK_ALIGNMENT
	#define GL_UNPACK_ALIGNMENT 0x0CF5
#endif
#ifndef GL_DEPTH_WRITEMASK
	#define GL_DEPTH_WRITEMASK 0x0B72
#endif
#ifndef GL_BLEND_SRC_RGB
	#define GL_BLEND_SRC_RGB 0x80C9
#endif
#ifndef GL_BLEND_DST_RGB
	#define GL_BLEND_DST_RGB 0x80C8
#endif
#ifndef GL_BLEND_SRC_ALPHA
	#define GL_BLEND_SRC_ALPHA 0x80CB
#endif
#ifndef GL_BLEND_DST_ALPHA
	#define GL_BLEND_DST_ALPHA 0x80CA
#endif
#ifndef GL_CURRENT_PROGRAM
	#define GL_CURRENT_PROGRAM 0x8B8D
#endif
#ifndef GL_ACTIVE_TEXTURE
	#define GL_ACTIVE_TEXTURE 0x84E0
#endif
#ifndef GL_TEXTURE_BINDING_2D
	#define GL_TEXTURE_BINDING_2D 0x8069
#endif
#ifndef GL_ARRAY_BUFFER_BINDING
	#define GL_ARRAY_BUFFER_BINDING 0x8894
#endif
#ifndef GL_ELEMENT_ARRAY_BUFFER_BINDING
	#define GL_ELEMENT_ARRAY_BUFFER_BINDING 0x8895
#endif
#ifndef GL_VERTEX_ARRAY_BINDING
	#define GL_VERTEX_ARRAY_BINDING 0x85B5
#endif
#ifndef GL_TEXTURE1
	#define GL_TEXTURE1 0x84C1
#endif
#ifndef GL_ZERO
	#define GL_ZERO 0
#endif
#ifndef GL_FUNC_ADD
	#define GL_FUNC_ADD 0x8006
#endif
#ifndef GL_CONSTANT_COLOR
	#define GL_CONSTANT_COLOR 0x8001
#endif
#ifndef GL_BLEND_EQUATION_RGB
	#define GL_BLEND_EQUATION_RGB 0x8009
#endif
#ifndef GL_BLEND_EQUATION_ALPHA
	#define GL_BLEND_EQUATION_ALPHA 0x883D
#endif
#ifndef GL_STENCIL_CLEAR_VALUE
	#define GL_STENCIL_CLEAR_VALUE 0x0B91
#endif
#ifndef GL_COLOR_CLEAR_VALUE
	#define GL_COLOR_CLEAR_VALUE 0x0C22
#endif
#ifndef GL_STENCIL_FAIL
	#define GL_STENCIL_FAIL 0x0B94
#endif
#ifndef GL_STENCIL_PASS_DEPTH_FAIL
	#define GL_STENCIL_PASS_DEPTH_FAIL 0x0B95
#endif
#ifndef GL_STENCIL_PASS_DEPTH_PASS
	#define GL_STENCIL_PASS_DEPTH_PASS 0x0B96
#endif
#ifndef GL_STENCIL_BACK_FUNC
	#define GL_STENCIL_BACK_FUNC 0x8800
#endif
#ifndef GL_STENCIL_BACK_REF
	#define GL_STENCIL_BACK_REF 0x8CA3
#endif
#ifndef GL_STENCIL_BACK_VALUE_MASK
	#define GL_STENCIL_BACK_VALUE_MASK 0x8CA4
#endif
#ifndef GL_STENCIL_BACK_WRITEMASK
	#define GL_STENCIL_BACK_WRITEMASK 0x8CA5
#endif
#ifndef GL_STENCIL_BACK_FAIL
	#define GL_STENCIL_BACK_FAIL 0x8801
#endif
#ifndef GL_STENCIL_BACK_PASS_DEPTH_FAIL
	#define GL_STENCIL_BACK_PASS_DEPTH_FAIL 0x8802
#endif
#ifndef GL_STENCIL_BACK_PASS_DEPTH_PASS
	#define GL_STENCIL_BACK_PASS_DEPTH_PASS 0x8803
#endif
#ifndef GL_FRAMEBUFFER
	#define GL_FRAMEBUFFER 0x8D40
#endif
#ifndef GL_READ_FRAMEBUFFER
	#define GL_READ_FRAMEBUFFER 0x8CA8
#endif
#ifndef GL_DRAW_FRAMEBUFFER
	#define GL_DRAW_FRAMEBUFFER 0x8CA9
#endif
#ifndef GL_RENDERBUFFER
	#define GL_RENDERBUFFER 0x8D41
#endif
#ifndef GL_COLOR_ATTACHMENT0
	#define GL_COLOR_ATTACHMENT0 0x8CE0
#endif
#ifndef GL_STENCIL_ATTACHMENT
	#define GL_STENCIL_ATTACHMENT 0x8D20
#endif
#ifndef GL_DEPTH_STENCIL_ATTACHMENT
	#define GL_DEPTH_STENCIL_ATTACHMENT 0x821A
#endif
#ifndef GL_STENCIL_INDEX8
	#define GL_STENCIL_INDEX8 0x8D48
#endif
#ifndef GL_FRAMEBUFFER_COMPLETE
	#define GL_FRAMEBUFFER_COMPLETE 0x8CD5
#endif
#ifndef GL_FRAMEBUFFER_BINDING
	#define GL_FRAMEBUFFER_BINDING 0x8CA6
#endif

#if FX_GL_ES

/// En Android las funciones están enlazadas directamente contra libGLESv2.
using FxGLsizeiptr = std::ptrdiff_t;

inline bool fx_gl_load() { return true; }
inline bool fx_gl_has_vertex_arrays() { return false; }

#else

typedef GLuint (*PFN_fx_glCreateShader)(GLenum);
typedef void (*PFN_fx_glShaderSource)(GLuint, GLsizei, const FxGLchar* const*, const GLint*);
typedef void (*PFN_fx_glCompileShader)(GLuint);
typedef void (*PFN_fx_glGetShaderiv)(GLuint, GLenum, GLint*);
typedef void (*PFN_fx_glGetShaderInfoLog)(GLuint, GLsizei, GLsizei*, FxGLchar*);
typedef void (*PFN_fx_glDeleteShader)(GLuint);
typedef GLuint (*PFN_fx_glCreateProgram)();
typedef void (*PFN_fx_glAttachShader)(GLuint, GLuint);
typedef void (*PFN_fx_glBindAttribLocation)(GLuint, GLuint, const FxGLchar*);
typedef void (*PFN_fx_glLinkProgram)(GLuint);
typedef void (*PFN_fx_glGetProgramiv)(GLuint, GLenum, GLint*);
typedef void (*PFN_fx_glGetProgramInfoLog)(GLuint, GLsizei, GLsizei*, FxGLchar*);
typedef void (*PFN_fx_glUseProgram)(GLuint);
typedef void (*PFN_fx_glDeleteProgram)(GLuint);
typedef GLint (*PFN_fx_glGetUniformLocation)(GLuint, const FxGLchar*);
typedef void (*PFN_fx_glUniform1i)(GLint, GLint);
typedef void (*PFN_fx_glUniform2f)(GLint, GLfloat, GLfloat);
typedef void (*PFN_fx_glUniformMatrix4fv)(GLint, GLsizei, GLboolean, const GLfloat*);
typedef void (*PFN_fx_glGenBuffers)(GLsizei, GLuint*);
typedef void (*PFN_fx_glBindBuffer)(GLenum, GLuint);
typedef void (*PFN_fx_glBufferData)(GLenum, FxGLsizeiptr, const void*, GLenum);
typedef void (*PFN_fx_glDeleteBuffers)(GLsizei, const GLuint*);
typedef void (*PFN_fx_glEnableVertexAttribArray)(GLuint);
typedef void (*PFN_fx_glDisableVertexAttribArray)(GLuint);
typedef void (*PFN_fx_glVertexAttribPointer)(GLuint, GLint, GLenum, GLboolean, GLsizei, const void*);
typedef void (*PFN_fx_glActiveTexture)(GLenum);
typedef void (*PFN_fx_glGenVertexArrays)(GLsizei, GLuint*);
typedef void (*PFN_fx_glBindVertexArray)(GLuint);
typedef void (*PFN_fx_glDeleteVertexArrays)(GLsizei, const GLuint*);
typedef void (*PFN_fx_glBlendFuncSeparate)(GLenum, GLenum, GLenum, GLenum);
typedef void (*PFN_fx_glBlendEquationSeparate)(GLenum, GLenum);
typedef void (*PFN_fx_glBlendColor)(GLfloat, GLfloat, GLfloat, GLfloat);
typedef void (*PFN_fx_glUniform1f)(GLint, GLfloat);
typedef void (*PFN_fx_glUniform1fv)(GLint, GLsizei, const GLfloat*);
typedef void (*PFN_fx_glUniform2fv)(GLint, GLsizei, const GLfloat*);
typedef void (*PFN_fx_glUniform4fv)(GLint, GLsizei, const GLfloat*);
typedef void (*PFN_fx_glStencilFuncSeparate)(GLenum, GLenum, GLint, GLuint);
typedef void (*PFN_fx_glStencilMaskSeparate)(GLenum, GLuint);
typedef void (*PFN_fx_glStencilOpSeparate)(GLenum, GLenum, GLenum, GLenum);
typedef void (*PFN_fx_glGenFramebuffers)(GLsizei, GLuint*);
typedef void (*PFN_fx_glBindFramebuffer)(GLenum, GLuint);
typedef void (*PFN_fx_glDeleteFramebuffers)(GLsizei, const GLuint*);
typedef GLenum (*PFN_fx_glCheckFramebufferStatus)(GLenum);
typedef void (*PFN_fx_glFramebufferTexture2D)(GLenum, GLenum, GLenum, GLuint, GLint);
typedef void (*PFN_fx_glFramebufferRenderbuffer)(GLenum, GLenum, GLenum, GLuint);
typedef void (*PFN_fx_glGenRenderbuffers)(GLsizei, GLuint*);
typedef void (*PFN_fx_glBindRenderbuffer)(GLenum, GLuint);
typedef void (*PFN_fx_glRenderbufferStorage)(GLenum, GLenum, GLsizei, GLsizei);
typedef void (*PFN_fx_glDeleteRenderbuffers)(GLsizei, const GLuint*);

extern PFN_fx_glCreateShader fx_glCreateShader;
extern PFN_fx_glShaderSource fx_glShaderSource;
extern PFN_fx_glCompileShader fx_glCompileShader;
extern PFN_fx_glGetShaderiv fx_glGetShaderiv;
extern PFN_fx_glGetShaderInfoLog fx_glGetShaderInfoLog;
extern PFN_fx_glDeleteShader fx_glDeleteShader;
extern PFN_fx_glCreateProgram fx_glCreateProgram;
extern PFN_fx_glAttachShader fx_glAttachShader;
extern PFN_fx_glBindAttribLocation fx_glBindAttribLocation;
extern PFN_fx_glLinkProgram fx_glLinkProgram;
extern PFN_fx_glGetProgramiv fx_glGetProgramiv;
extern PFN_fx_glGetProgramInfoLog fx_glGetProgramInfoLog;
extern PFN_fx_glUseProgram fx_glUseProgram;
extern PFN_fx_glDeleteProgram fx_glDeleteProgram;
extern PFN_fx_glGetUniformLocation fx_glGetUniformLocation;
extern PFN_fx_glUniform1i fx_glUniform1i;
extern PFN_fx_glUniform2f fx_glUniform2f;
extern PFN_fx_glUniformMatrix4fv fx_glUniformMatrix4fv;
extern PFN_fx_glGenBuffers fx_glGenBuffers;
extern PFN_fx_glBindBuffer fx_glBindBuffer;
extern PFN_fx_glBufferData fx_glBufferData;
extern PFN_fx_glDeleteBuffers fx_glDeleteBuffers;
extern PFN_fx_glEnableVertexAttribArray fx_glEnableVertexAttribArray;
extern PFN_fx_glDisableVertexAttribArray fx_glDisableVertexAttribArray;
extern PFN_fx_glVertexAttribPointer fx_glVertexAttribPointer;
extern PFN_fx_glActiveTexture fx_glActiveTexture;
extern PFN_fx_glGenVertexArrays fx_glGenVertexArrays;
extern PFN_fx_glBindVertexArray fx_glBindVertexArray;
extern PFN_fx_glDeleteVertexArrays fx_glDeleteVertexArrays;
extern PFN_fx_glBlendFuncSeparate fx_glBlendFuncSeparate;
extern PFN_fx_glBlendEquationSeparate fx_glBlendEquationSeparate;
extern PFN_fx_glBlendColor fx_glBlendColor;
extern PFN_fx_glUniform1f fx_glUniform1f;
extern PFN_fx_glUniform1fv fx_glUniform1fv;
extern PFN_fx_glUniform2fv fx_glUniform2fv;
extern PFN_fx_glUniform4fv fx_glUniform4fv;
extern PFN_fx_glStencilFuncSeparate fx_glStencilFuncSeparate;
extern PFN_fx_glStencilMaskSeparate fx_glStencilMaskSeparate;
extern PFN_fx_glStencilOpSeparate fx_glStencilOpSeparate;
extern PFN_fx_glGenFramebuffers fx_glGenFramebuffers;
extern PFN_fx_glBindFramebuffer fx_glBindFramebuffer;
extern PFN_fx_glDeleteFramebuffers fx_glDeleteFramebuffers;
extern PFN_fx_glCheckFramebufferStatus fx_glCheckFramebufferStatus;
extern PFN_fx_glFramebufferTexture2D fx_glFramebufferTexture2D;
extern PFN_fx_glFramebufferRenderbuffer fx_glFramebufferRenderbuffer;
extern PFN_fx_glGenRenderbuffers fx_glGenRenderbuffers;
extern PFN_fx_glBindRenderbuffer fx_glBindRenderbuffer;
extern PFN_fx_glRenderbufferStorage fx_glRenderbufferStorage;
extern PFN_fx_glDeleteRenderbuffers fx_glDeleteRenderbuffers;

#define glCreateShader fx_glCreateShader
#define glShaderSource fx_glShaderSource
#define glCompileShader fx_glCompileShader
#define glGetShaderiv fx_glGetShaderiv
#define glGetShaderInfoLog fx_glGetShaderInfoLog
#define glDeleteShader fx_glDeleteShader
#define glCreateProgram fx_glCreateProgram
#define glAttachShader fx_glAttachShader
#define glBindAttribLocation fx_glBindAttribLocation
#define glLinkProgram fx_glLinkProgram
#define glGetProgramiv fx_glGetProgramiv
#define glGetProgramInfoLog fx_glGetProgramInfoLog
#define glUseProgram fx_glUseProgram
#define glDeleteProgram fx_glDeleteProgram
#define glGetUniformLocation fx_glGetUniformLocation
#define glUniform1i fx_glUniform1i
#define glUniform2f fx_glUniform2f
#define glUniformMatrix4fv fx_glUniformMatrix4fv
#define glGenBuffers fx_glGenBuffers
#define glBindBuffer fx_glBindBuffer
#define glBufferData fx_glBufferData
#define glDeleteBuffers fx_glDeleteBuffers
#define glEnableVertexAttribArray fx_glEnableVertexAttribArray
#define glDisableVertexAttribArray fx_glDisableVertexAttribArray
#define glVertexAttribPointer fx_glVertexAttribPointer
#define glActiveTexture fx_glActiveTexture
#define glGenVertexArrays fx_glGenVertexArrays
#define glBindVertexArray fx_glBindVertexArray
#define glDeleteVertexArrays fx_glDeleteVertexArrays
#define glBlendFuncSeparate fx_glBlendFuncSeparate
#define glBlendEquationSeparate fx_glBlendEquationSeparate
#define glBlendColor fx_glBlendColor
#define glUniform1f fx_glUniform1f
#define glUniform1fv fx_glUniform1fv
#define glUniform2fv fx_glUniform2fv
#define glUniform4fv fx_glUniform4fv
#define glStencilFuncSeparate fx_glStencilFuncSeparate
#define glStencilMaskSeparate fx_glStencilMaskSeparate
#define glStencilOpSeparate fx_glStencilOpSeparate
#define glGenFramebuffers fx_glGenFramebuffers
#define glBindFramebuffer fx_glBindFramebuffer
#define glDeleteFramebuffers fx_glDeleteFramebuffers
#define glCheckFramebufferStatus fx_glCheckFramebufferStatus
#define glFramebufferTexture2D fx_glFramebufferTexture2D
#define glFramebufferRenderbuffer fx_glFramebufferRenderbuffer
#define glGenRenderbuffers fx_glGenRenderbuffers
#define glBindRenderbuffer fx_glBindRenderbuffer
#define glRenderbufferStorage fx_glRenderbufferStorage
#define glDeleteRenderbuffers fx_glDeleteRenderbuffers

/// Resuelve las funciones de OpenGL 2.0+. Debe llamarse con un contexto activo.
bool fx_gl_load();

/// Indica si están disponibles los VAO (necesarios en perfiles core de desktop).
inline bool fx_gl_has_vertex_arrays()
{
	return fx_glGenVertexArrays && fx_glBindVertexArray && fx_glDeleteVertexArrays;
}

#endif // FX_GL_ES
