#include "fx_gl.h"

#if !FX_GL_ES

#include <RmlUi/Core/Log.h>

#if defined(_WIN32)
	typedef void* (*PFN_wglGetProcAddress)(const char*);
	static HMODULE g_gl_module = nullptr;
	static PFN_wglGetProcAddress g_wgl_get_proc_address = nullptr;

	static void* FxPlatformGLGetSymbol(const char* name)
	{
		void* symbol = g_wgl_get_proc_address ? reinterpret_cast<void*>(g_wgl_get_proc_address(name)) : nullptr;

		// wglGetProcAddress devuelve 1, 2 o 3 para funciones no disponibles.
		if (symbol == nullptr || symbol == reinterpret_cast<void*>(1) || symbol == reinterpret_cast<void*>(2) || symbol == reinterpret_cast<void*>(3))
			symbol = g_gl_module ? reinterpret_cast<void*>(GetProcAddress(g_gl_module, name)) : nullptr;

		return symbol;
	}
#else
	#include <dlfcn.h>
	static void* g_gl_module = nullptr;

	static void* FxPlatformGLGetSymbol(const char* name)
	{
		return g_gl_module ? dlsym(g_gl_module, name) : nullptr;
	}
#endif

PFN_fx_glCreateShader fx_glCreateShader = nullptr;
PFN_fx_glShaderSource fx_glShaderSource = nullptr;
PFN_fx_glCompileShader fx_glCompileShader = nullptr;
PFN_fx_glGetShaderiv fx_glGetShaderiv = nullptr;
PFN_fx_glGetShaderInfoLog fx_glGetShaderInfoLog = nullptr;
PFN_fx_glDeleteShader fx_glDeleteShader = nullptr;
PFN_fx_glCreateProgram fx_glCreateProgram = nullptr;
PFN_fx_glAttachShader fx_glAttachShader = nullptr;
PFN_fx_glBindAttribLocation fx_glBindAttribLocation = nullptr;
PFN_fx_glLinkProgram fx_glLinkProgram = nullptr;
PFN_fx_glGetProgramiv fx_glGetProgramiv = nullptr;
PFN_fx_glGetProgramInfoLog fx_glGetProgramInfoLog = nullptr;
PFN_fx_glUseProgram fx_glUseProgram = nullptr;
PFN_fx_glDeleteProgram fx_glDeleteProgram = nullptr;
PFN_fx_glGetUniformLocation fx_glGetUniformLocation = nullptr;
PFN_fx_glUniform1i fx_glUniform1i = nullptr;
PFN_fx_glUniform2f fx_glUniform2f = nullptr;
PFN_fx_glUniformMatrix4fv fx_glUniformMatrix4fv = nullptr;
PFN_fx_glGenBuffers fx_glGenBuffers = nullptr;
PFN_fx_glBindBuffer fx_glBindBuffer = nullptr;
PFN_fx_glBufferData fx_glBufferData = nullptr;
PFN_fx_glDeleteBuffers fx_glDeleteBuffers = nullptr;
PFN_fx_glEnableVertexAttribArray fx_glEnableVertexAttribArray = nullptr;
PFN_fx_glDisableVertexAttribArray fx_glDisableVertexAttribArray = nullptr;
PFN_fx_glVertexAttribPointer fx_glVertexAttribPointer = nullptr;
PFN_fx_glActiveTexture fx_glActiveTexture = nullptr;
PFN_fx_glGenVertexArrays fx_glGenVertexArrays = nullptr;
PFN_fx_glBindVertexArray fx_glBindVertexArray = nullptr;
PFN_fx_glDeleteVertexArrays fx_glDeleteVertexArrays = nullptr;
PFN_fx_glBlendFuncSeparate fx_glBlendFuncSeparate = nullptr;
PFN_fx_glBlendEquationSeparate fx_glBlendEquationSeparate = nullptr;
PFN_fx_glBlendColor fx_glBlendColor = nullptr;
PFN_fx_glUniform1f fx_glUniform1f = nullptr;
PFN_fx_glUniform1fv fx_glUniform1fv = nullptr;
PFN_fx_glUniform2fv fx_glUniform2fv = nullptr;
PFN_fx_glUniform4fv fx_glUniform4fv = nullptr;
PFN_fx_glStencilFuncSeparate fx_glStencilFuncSeparate = nullptr;
PFN_fx_glStencilMaskSeparate fx_glStencilMaskSeparate = nullptr;
PFN_fx_glStencilOpSeparate fx_glStencilOpSeparate = nullptr;
PFN_fx_glGenFramebuffers fx_glGenFramebuffers = nullptr;
PFN_fx_glBindFramebuffer fx_glBindFramebuffer = nullptr;
PFN_fx_glDeleteFramebuffers fx_glDeleteFramebuffers = nullptr;
PFN_fx_glCheckFramebufferStatus fx_glCheckFramebufferStatus = nullptr;
PFN_fx_glFramebufferTexture2D fx_glFramebufferTexture2D = nullptr;
PFN_fx_glFramebufferRenderbuffer fx_glFramebufferRenderbuffer = nullptr;
PFN_fx_glGenRenderbuffers fx_glGenRenderbuffers = nullptr;
PFN_fx_glBindRenderbuffer fx_glBindRenderbuffer = nullptr;
PFN_fx_glRenderbufferStorage fx_glRenderbufferStorage = nullptr;
PFN_fx_glDeleteRenderbuffers fx_glDeleteRenderbuffers = nullptr;

bool fx_gl_load()
{
	if (fx_glCreateShader && fx_glCreateProgram && fx_glGenBuffers)
		return true;

#if defined(_WIN32)
	if (!g_gl_module)
		g_gl_module = LoadLibraryA("opengl32.dll");

	if (!g_gl_module)
	{
		Rml::Log::Message(Rml::Log::LT_ERROR, "fxrmlui: no se pudo cargar opengl32.dll");
		return false;
	}

	if (!g_wgl_get_proc_address)
		g_wgl_get_proc_address = reinterpret_cast<PFN_wglGetProcAddress>(GetProcAddress(g_gl_module, "wglGetProcAddress"));
#else
	if (!g_gl_module)
		g_gl_module = dlopen("libGL.so.1", RTLD_LAZY | RTLD_LOCAL);

	if (!g_gl_module)
		g_gl_module = dlopen("libGL.so", RTLD_LAZY | RTLD_LOCAL);

	if (!g_gl_module)
	{
		Rml::Log::Message(Rml::Log::LT_ERROR, "fxrmlui: no se pudo cargar libGL");
		return false;
	}
#endif

	bool loaded = true;

#define FX_GL_RESOLVE(fn) \
	do \
	{ \
		fx_##fn = reinterpret_cast<PFN_fx_##fn>(FxPlatformGLGetSymbol(#fn)); \
		if (!fx_##fn) \
		{ \
			Rml::Log::Message(Rml::Log::LT_ERROR, "fxrmlui: símbolo GL no disponible: %s", #fn); \
			loaded = false; \
		} \
	} while (false)

	FX_GL_RESOLVE(glCreateShader);
	FX_GL_RESOLVE(glShaderSource);
	FX_GL_RESOLVE(glCompileShader);
	FX_GL_RESOLVE(glGetShaderiv);
	FX_GL_RESOLVE(glGetShaderInfoLog);
	FX_GL_RESOLVE(glDeleteShader);
	FX_GL_RESOLVE(glCreateProgram);
	FX_GL_RESOLVE(glAttachShader);
	FX_GL_RESOLVE(glBindAttribLocation);
	FX_GL_RESOLVE(glLinkProgram);
	FX_GL_RESOLVE(glGetProgramiv);
	FX_GL_RESOLVE(glGetProgramInfoLog);
	FX_GL_RESOLVE(glUseProgram);
	FX_GL_RESOLVE(glDeleteProgram);
	FX_GL_RESOLVE(glGetUniformLocation);
	FX_GL_RESOLVE(glUniform1i);
	FX_GL_RESOLVE(glUniform2f);
	FX_GL_RESOLVE(glUniformMatrix4fv);
	FX_GL_RESOLVE(glGenBuffers);
	FX_GL_RESOLVE(glBindBuffer);
	FX_GL_RESOLVE(glBufferData);
	FX_GL_RESOLVE(glDeleteBuffers);
	FX_GL_RESOLVE(glEnableVertexAttribArray);
	FX_GL_RESOLVE(glDisableVertexAttribArray);
	FX_GL_RESOLVE(glVertexAttribPointer);
	FX_GL_RESOLVE(glActiveTexture);
	FX_GL_RESOLVE(glBlendFuncSeparate);
	FX_GL_RESOLVE(glBlendEquationSeparate);
	FX_GL_RESOLVE(glBlendColor);
	FX_GL_RESOLVE(glUniform1f);
	FX_GL_RESOLVE(glUniform1fv);
	FX_GL_RESOLVE(glUniform2fv);
	FX_GL_RESOLVE(glUniform4fv);
	FX_GL_RESOLVE(glStencilFuncSeparate);
	FX_GL_RESOLVE(glStencilMaskSeparate);
	FX_GL_RESOLVE(glStencilOpSeparate);

	// Funciones de framebuffer (GL 3.0 o ARB_framebuffer_object). Se intentan
	// también los sufijos EXT/ARB de los controladores antiguos. Si no están
	// disponibles el renderer sigue funcionando sin capas ni filtros.
	fx_glGenFramebuffers = reinterpret_cast<PFN_fx_glGenFramebuffers>(FxPlatformGLGetSymbol("glGenFramebuffers"));
	fx_glBindFramebuffer = reinterpret_cast<PFN_fx_glBindFramebuffer>(FxPlatformGLGetSymbol("glBindFramebuffer"));
	fx_glDeleteFramebuffers = reinterpret_cast<PFN_fx_glDeleteFramebuffers>(FxPlatformGLGetSymbol("glDeleteFramebuffers"));
	fx_glCheckFramebufferStatus = reinterpret_cast<PFN_fx_glCheckFramebufferStatus>(FxPlatformGLGetSymbol("glCheckFramebufferStatus"));
	fx_glFramebufferTexture2D = reinterpret_cast<PFN_fx_glFramebufferTexture2D>(FxPlatformGLGetSymbol("glFramebufferTexture2D"));
	fx_glFramebufferRenderbuffer = reinterpret_cast<PFN_fx_glFramebufferRenderbuffer>(FxPlatformGLGetSymbol("glFramebufferRenderbuffer"));
	fx_glGenRenderbuffers = reinterpret_cast<PFN_fx_glGenRenderbuffers>(FxPlatformGLGetSymbol("glGenRenderbuffers"));
	fx_glBindRenderbuffer = reinterpret_cast<PFN_fx_glBindRenderbuffer>(FxPlatformGLGetSymbol("glBindRenderbuffer"));
	fx_glRenderbufferStorage = reinterpret_cast<PFN_fx_glRenderbufferStorage>(FxPlatformGLGetSymbol("glRenderbufferStorage"));
	fx_glDeleteRenderbuffers = reinterpret_cast<PFN_fx_glDeleteRenderbuffers>(FxPlatformGLGetSymbol("glDeleteRenderbuffers"));

	if (!fx_glGenFramebuffers)
	{
		fx_glGenFramebuffers = reinterpret_cast<PFN_fx_glGenFramebuffers>(FxPlatformGLGetSymbol("glGenFramebuffersEXT"));
		fx_glBindFramebuffer = reinterpret_cast<PFN_fx_glBindFramebuffer>(FxPlatformGLGetSymbol("glBindFramebufferEXT"));
		fx_glDeleteFramebuffers = reinterpret_cast<PFN_fx_glDeleteFramebuffers>(FxPlatformGLGetSymbol("glDeleteFramebuffersEXT"));
		fx_glCheckFramebufferStatus = reinterpret_cast<PFN_fx_glCheckFramebufferStatus>(FxPlatformGLGetSymbol("glCheckFramebufferStatusEXT"));
		fx_glFramebufferTexture2D = reinterpret_cast<PFN_fx_glFramebufferTexture2D>(FxPlatformGLGetSymbol("glFramebufferTexture2DEXT"));
		fx_glFramebufferRenderbuffer = reinterpret_cast<PFN_fx_glFramebufferRenderbuffer>(FxPlatformGLGetSymbol("glFramebufferRenderbufferEXT"));
		fx_glGenRenderbuffers = reinterpret_cast<PFN_fx_glGenRenderbuffers>(FxPlatformGLGetSymbol("glGenRenderbuffersEXT"));
		fx_glBindRenderbuffer = reinterpret_cast<PFN_fx_glBindRenderbuffer>(FxPlatformGLGetSymbol("glBindRenderbufferEXT"));
		fx_glRenderbufferStorage = reinterpret_cast<PFN_fx_glRenderbufferStorage>(FxPlatformGLGetSymbol("glRenderbufferStorageEXT"));
		fx_glDeleteRenderbuffers = reinterpret_cast<PFN_fx_glDeleteRenderbuffers>(FxPlatformGLGetSymbol("glDeleteRenderbuffersEXT"));
	}

	// Los VAO sólo son imprescindibles en perfiles core de OpenGL; en contextos
	// de compatibilidad se pueden omitir.
	fx_glGenVertexArrays = reinterpret_cast<PFN_fx_glGenVertexArrays>(FxPlatformGLGetSymbol("glGenVertexArrays"));
	fx_glBindVertexArray = reinterpret_cast<PFN_fx_glBindVertexArray>(FxPlatformGLGetSymbol("glBindVertexArray"));
	fx_glDeleteVertexArrays = reinterpret_cast<PFN_fx_glDeleteVertexArrays>(FxPlatformGLGetSymbol("glDeleteVertexArrays"));

#undef FX_GL_RESOLVE

	return loaded;
}

#endif // !FX_GL_ES
