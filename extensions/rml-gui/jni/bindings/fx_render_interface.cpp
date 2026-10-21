#include "fx_render_interface.h"

#include "fx_gl.h"

#include <RmlUi/Core/Colour.h>
#include <RmlUi/Core/Core.h>
#include <RmlUi/Core/DecorationTypes.h>
#include <RmlUi/Core/Dictionary.h>
#include <RmlUi/Core/FileInterface.h>
#include <RmlUi/Core/Log.h>
#include <RmlUi/Core/Math.h>

#include <algorithm>
#include <cmath>
#include <chrono>
#include <cstddef>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <type_traits>
#include <unordered_map>
#include <vector>

#if defined(__ANDROID__)
	#include <EGL/egl.h>
	#include <sys/system_properties.h>
#endif

namespace {

// Atributos del shader; deben coincidir con glBindAttribLocation.
constexpr unsigned int kAttributePosition = 0;
constexpr unsigned int kAttributeColour = 1;
constexpr unsigned int kAttributeTexCoord = 2;

// Máximo de paradas de gradiente. GLES 2.0 garantiza 64 vec4 de uniforms en el
// fragment shader; 8 paradas (vec4 + float) dejan margen de sobra.
constexpr int kMaxGradientStops = 64;

#if FX_GL_ES
// GL_EXT_discard_framebuffer: en GPUs por tiles (Mali, Adreno...) evita cargar el contenido anterior
// de un destino que la pasada va a sobrescribir por completo, lo que abarata cada cambio de FBO.
typedef void (*PFN_fxDiscardFramebuffer)(GLenum, GLsizei, const GLenum*);

void DiscardBoundFramebufferColor()
{
	static PFN_fxDiscardFramebuffer discard = [] {
		const char* extensions = reinterpret_cast<const char*>(glGetString(GL_EXTENSIONS));
		if (!extensions || !std::strstr(extensions, "GL_EXT_discard_framebuffer"))
			return static_cast<PFN_fxDiscardFramebuffer>(nullptr);
		return reinterpret_cast<PFN_fxDiscardFramebuffer>(eglGetProcAddress("glDiscardFramebufferEXT"));
	}();
	if (discard)
	{
		const GLenum attachment = GL_COLOR_ATTACHMENT0;
		discard(GL_FRAMEBUFFER, 1, &attachment);
	}
}
#else
inline void DiscardBoundFramebufferColor() {}
#endif

inline void BindFramebufferForOverwrite(unsigned int framebuffer)
{
	glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
	DiscardBoundFramebufferColor();
}


#if FX_GL_ES
// Índices de 32 bits: nativos en GLES 3.0+, o con GL_OES_element_index_uint en GLES 2.0.
// Máximo índice que se sube sin trocear; la propiedad debug.fxrml.chunk lo reduce para pruebas.
int GeometryChunkLimit()
{
	static const int limit = [] {
		char value[PROP_VALUE_MAX] = {0};
		if (__system_property_get("debug.fxrml.chunk", value) > 0)
		{
			const int parsed = std::atoi(value);
			if (parsed > 16)
				return parsed;
		}
		return 0xFFFF;
	}();
	return limit;
}

bool SupportsUintIndices()
{
	static const bool supported = [] {
		// Interruptor de depuración para forzar la ruta de troceado a 16 bits:
		// adb shell setprop debug.fxrml.nouint 1
		char forced[PROP_VALUE_MAX] = {0};
		if (__system_property_get("debug.fxrml.nouint", forced) > 0 && forced[0] == '1')
			return false;
		const char* version = reinterpret_cast<const char*>(glGetString(GL_VERSION));
		if (version && std::strstr(version, "OpenGL ES 3"))
			return true;
		const char* extensions = reinterpret_cast<const char*>(glGetString(GL_EXTENSIONS));
		return extensions && std::strstr(extensions, "GL_OES_element_index_uint") != nullptr;
	}();
	return supported;
}
#endif
constexpr int kBlurNumWeights = 4; // 7 muestras simétricas

// ---------------------------------------------------------------------------
//  GLSL: una sola fuente para GLSL ES 100 (Android) y GLSL 130 (desktop).
// ---------------------------------------------------------------------------

#if FX_GL_ES
	#define FX_GLSL_VERSION "#version 100\n"
	#define FX_GLSL_PRECISION \
		"#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif\n"
	#define FX_GLSL_ATTR "attribute "
	#define FX_GLSL_VERT_OUT "varying "
	#define FX_GLSL_FRAG_IN "varying "
	#define FX_GLSL_FRAG_OUT_DECL ""
	#define FX_GLSL_FRAG_OUT "gl_FragColor"
	#define FX_GLSL_TEX "texture2D"
#else
	#define FX_GLSL_VERSION "#version 130\n"
	#define FX_GLSL_PRECISION ""
	#define FX_GLSL_ATTR "in "
	#define FX_GLSL_VERT_OUT "out "
	#define FX_GLSL_FRAG_IN "in "
	#define FX_GLSL_FRAG_OUT_DECL "out vec4 fx_finalColor;\n"
	#define FX_GLSL_FRAG_OUT "fx_finalColor"
	#define FX_GLSL_TEX "texture"
#endif

std::string VertexShaderSource()
{
	return std::string(FX_GLSL_VERSION) +
		"uniform vec2 _translate;\n"
		"uniform mat4 _transform;\n" FX_GLSL_ATTR "vec2 inPosition;\n" FX_GLSL_ATTR "vec4 inColor0;\n" FX_GLSL_ATTR
		"vec2 inTexCoord0;\n" FX_GLSL_VERT_OUT "vec2 fragTexCoord;\n" FX_GLSL_VERT_OUT "vec4 fragColor;\n"
		"void main()\n"
		"{\n"
		"	fragTexCoord = inTexCoord0;\n"
		"	fragColor = inColor0;\n"
		"	gl_Position = _transform * vec4(inPosition + _translate, 0.0, 1.0);\n"
		"}\n";
}

std::string FragmentHeader()
{
	return std::string(FX_GLSL_VERSION) + FX_GLSL_PRECISION + FX_GLSL_FRAG_OUT_DECL + FX_GLSL_FRAG_IN "vec2 fragTexCoord;\n" +
		FX_GLSL_FRAG_IN "vec4 fragColor;\n";
}

std::string FragmentSimple()
{
	return FragmentHeader() + "void main()\n{\n	" FX_GLSL_FRAG_OUT " = fragColor;\n}\n";
}

std::string FragmentTexture()
{
	return FragmentHeader() +
		"uniform sampler2D _tex;\n"
		"void main()\n{\n	" FX_GLSL_FRAG_OUT " = fragColor * " FX_GLSL_TEX "(_tex, fragTexCoord);\n}\n";
}

std::string FragmentGradient()
{
	// El degradado (paradas + smoothstep) se evalua en CPU en una textura 1D (_tex): en GPUs
	// moviles el bucle por paradas a pantalla completa era el mayor coste de la UI.
	std::string source = FragmentHeader();
	source += "#define LINEAR 0\n"
			  "#define RADIAL 1\n"
			  "#define CONIC 2\n"
			  "#define REPEATING_LINEAR 3\n"
			  "#define REPEATING_RADIAL 4\n"
			  "#define REPEATING_CONIC 5\n"
			  "#define PI 3.14159265\n"
			  "uniform int _func;\n"
			  "uniform vec2 _p;\n"
			  "uniform vec2 _v;\n"
			  "uniform vec2 _range;\n" // x = posicion de la primera parada, y = 1 / (ultima - primera)
			  "uniform sampler2D _tex;\n"
			  "void main()\n"
			  "{\n"
			  "	float t = 0.0;\n"
			  "	if (_func == LINEAR || _func == REPEATING_LINEAR)\n"
			  "	{\n"
			  "		float dist_square = dot(_v, _v);\n"
			  "		vec2 V = fragTexCoord - _p;\n"
			  "		t = dot(_v, V) / dist_square;\n"
			  "	}\n"
			  "	else if (_func == RADIAL || _func == REPEATING_RADIAL)\n"
			  "	{\n"
			  "		vec2 V = fragTexCoord - _p;\n"
			  "		t = length(_v * V);\n"
			  "	}\n"
			  "	else if (_func == CONIC || _func == REPEATING_CONIC)\n"
			  "	{\n"
			  "		mat2 R = mat2(_v.x, -_v.y, _v.y, _v.x);\n"
			  "		vec2 V = R * (fragTexCoord - _p);\n"
			  "		t = 0.5 + atan(-V.x, V.y) / (2.0 * PI);\n"
			  "	}\n"
			  "	if (_func == REPEATING_LINEAR || _func == REPEATING_RADIAL || _func == REPEATING_CONIC)\n"
			  "	{\n"
			  "		float len = 1.0 / _range.y;\n"
			  "		t = _range.x + mod(t - _range.x, len);\n"
			  "	}\n"
			  "	float u = clamp((t - _range.x) * _range.y, 0.0, 1.0);\n"
			  "	" FX_GLSL_FRAG_OUT " = fragColor * " FX_GLSL_TEX "(_tex, vec2(u, 0.5));\n"
			  "}\n";
	return source;
}

std::string FragmentPassthrough()
{
	return FragmentHeader() +
		"uniform sampler2D _tex;\n"
		"uniform vec2 _uvOffset;\n"
		"uniform vec2 _uvScale;\n"
		"void main()\n{\n	" FX_GLSL_FRAG_OUT " = " FX_GLSL_TEX "(_tex, fragTexCoord * _uvScale + _uvOffset);\n}\n";
}

std::string FragmentColorMatrix()
{
	return FragmentHeader() +
		"uniform sampler2D _tex;\n"
		"uniform mat4 _color_matrix;\n"
		"void main()\n{\n"
		"	vec4 texColor = " FX_GLSL_TEX "(_tex, fragTexCoord);\n"
		"	vec3 transformedColor = vec3(_color_matrix * texColor);\n"
		"	" FX_GLSL_FRAG_OUT " = vec4(transformedColor, texColor.a);\n"
		"}\n";
}

std::string FragmentBlendMask()
{
	return FragmentHeader() +
		"uniform sampler2D _tex;\n"
		"uniform sampler2D _texMask;\n"
		"void main()\n{\n"
		"	vec4 texColor = " FX_GLSL_TEX "(_tex, fragTexCoord);\n"
		"	float maskAlpha = " FX_GLSL_TEX "(_texMask, fragTexCoord).a;\n"
		"	" FX_GLSL_FRAG_OUT " = texColor * maskAlpha;\n"
		"}\n";
}

std::string FragmentBlur()
{
	// Sin arrays de varying indexados dinámicamente: GLSL ES 1.00 exige
	// índices constantes. Se desenrollan las 7 muestras simétricas.
	return FragmentHeader() +
		"uniform sampler2D _tex;\n"
		"uniform vec2 _texelOffset;\n"
		"uniform vec2 _texCoordMin;\n"
		"uniform vec2 _texCoordMax;\n"
		"uniform float _weights[4];\n"
		"vec4 sample_tex(vec2 uv, float weight)\n"
		"{\n"
		"	vec2 in_region = step(_texCoordMin, uv) * step(uv, _texCoordMax);\n"
		"	return " FX_GLSL_TEX "(_tex, uv) * (in_region.x * in_region.y * weight);\n"
		"}\n"
		"void main()\n"
		"{\n"
		"	vec4 color = sample_tex(fragTexCoord, _weights[0]);\n"
		"	color += sample_tex(fragTexCoord + _texelOffset, _weights[1]);\n"
		"	color += sample_tex(fragTexCoord - _texelOffset, _weights[1]);\n"
		"	color += sample_tex(fragTexCoord + _texelOffset * 2.0, _weights[2]);\n"
		"	color += sample_tex(fragTexCoord - _texelOffset * 2.0, _weights[2]);\n"
		"	color += sample_tex(fragTexCoord + _texelOffset * 3.0, _weights[3]);\n"
		"	color += sample_tex(fragTexCoord - _texelOffset * 3.0, _weights[3]);\n"
		"	" FX_GLSL_FRAG_OUT " = color;\n"
		"}\n";
}

std::string FragmentDownsample()
{
	// Reduce la resolucion en una sola pasada: _taps x _taps muestras bilineales (cada una promedia
	// 2x2 texels), es decir, un promedio exacto de bloques de (2 * _taps) texels de lado.
	return FragmentHeader() +
		"uniform sampler2D _tex;\n"
		"uniform vec2 _texelOffset;\n" // tamano de un texel de la textura origen
		"uniform vec2 _texCoordMin;\n"
		"uniform vec2 _texCoordMax;\n"
		"uniform float _taps;\n"
		"void main()\n{\n"
		"	vec4 sum = vec4(0.0);\n"
		"	for (int j = 0; j < 4; j++)\n"
		"	{\n"
		"		for (int i = 0; i < 4; i++)\n"
		"		{\n"
		"			if (float(i) < _taps && float(j) < _taps)\n"
		"			{\n"
		"				vec2 offset = vec2(2.0 * float(i) + 1.0 - _taps, 2.0 * float(j) + 1.0 - _taps) * _texelOffset;\n"
		"				vec2 uv = fragTexCoord + offset;\n"
		"				vec2 in_region = step(_texCoordMin, uv) * step(uv, _texCoordMax);\n"
		"				sum += " FX_GLSL_TEX "(_tex, uv) * (in_region.x * in_region.y);\n"
		"			}\n"
		"		}\n"
		"	}\n"
		"	" FX_GLSL_FRAG_OUT " = sum / (_taps * _taps);\n"
		"}\n";
}

std::string FragmentDropShadow()
{
	return FragmentHeader() +
		"uniform sampler2D _tex;\n"
		"uniform vec2 _texCoordMin;\n"
		"uniform vec2 _texCoordMax;\n"
		"uniform vec4 _color;\n"
		"uniform vec2 _uvOffset;\n"
		"void main()\n{\n"
		"	vec2 uv = fragTexCoord + _uvOffset;\n"
		"	vec2 in_region = step(_texCoordMin, uv) * step(uv, _texCoordMax);\n"
		"	" FX_GLSL_FRAG_OUT " = " FX_GLSL_TEX "(_tex, uv).a * in_region.x * in_region.y * _color;\n"
		"}\n";
}

unsigned int CompileShaderStage(GLenum type, const std::string& source)
{
	GLuint shader = glCreateShader(type);
	if (!shader)
		return 0;

	const char* source_ptr = source.c_str();
	glShaderSource(shader, 1, &source_ptr, nullptr);
	glCompileShader(shader);

	GLint status = 0;
	glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
	if (!status)
	{
		GLint log_length = 0;
		glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &log_length);

		Rml::String log;
		if (log_length > 1)
		{
			log.resize(static_cast<size_t>(log_length));
			glGetShaderInfoLog(shader, log_length, nullptr, &log[0]);
		}

		Rml::Log::Message(Rml::Log::LT_ERROR, "fxrmlui: error al compilar shader (%s): %s",
			type == GL_VERTEX_SHADER ? "vertex" : "fragment", log.c_str());
		glDeleteShader(shader);
		return 0;
	}

	return shader;
}

bool IsPowerOfTwo(int value)
{
	return value > 0 && (value & (value - 1)) == 0;
}

Rml::Colourf ConvertToColorf(Rml::ColourbPremultiplied c0)
{
	Rml::Colourf result;
	for (int i = 0; i < 4; i++)
		result[i] = (1.f / 255.f) * float(c0[i]);
	return result;
}

// La textura de una capa contiene la UI con el eje Y invertido respecto a la
// convención de RmlUi (fila 0 de la textura = parte inferior de la UI).
Rml::Rectanglei VerticallyFlipped(Rml::Rectanglei rect, int viewport_height)
{
	Rml::Rectanglei flipped = rect;
	flipped.p0.y = viewport_height - rect.p1.y;
	flipped.p1.y = viewport_height - rect.p0.y;
	return flipped;
}

} // namespace

// ---------------------------------------------------------------------------
//  FxRenderInterface
// ---------------------------------------------------------------------------

FxRenderInterface::FxRenderInterface() = default;

FxRenderInterface::~FxRenderInterface()
{
	DestroyAllResources();
}

void FxRenderInterface::SetViewport(int width, int height)
{
	width = std::max(width, 0);
	height = std::max(height, 0);

	if (viewport_width_ != width || viewport_height_ != height)
	{
		// Si cambia el tamaño hay que recrear capas y framebuffers de posproceso.
		if (initialized_ && !frame_active_)
			DestroyLayers();
	}

	viewport_width_ = width;
	viewport_height_ = height;
}

bool FxRenderInterface::SupportsLayers() const
{
	return glGenFramebuffers && glBindFramebuffer && glDeleteFramebuffers && glCheckFramebufferStatus && glFramebufferTexture2D &&
		glFramebufferRenderbuffer && glGenRenderbuffers && glBindRenderbuffer && glRenderbufferStorage && glDeleteRenderbuffers &&
		glBlendColor && glBlendEquationSeparate;
}

bool FxRenderInterface::EnsureInitialized()
{
	if (initialized_)
		return true;

	if (!fx_gl_load())
	{
		Rml::Log::Message(Rml::Log::LT_ERROR, "fxrmlui: no se pudieron resolver las funciones OpenGL");
		return false;
	}

	if (!CreatePrograms())
		return false;

	if (!CreateFullscreenQuad())
		return false;

	if (fx_gl_has_vertex_arrays())
	{
#if !FX_GL_ES
		glGenVertexArrays(1, &vertex_array_);
#endif
	}

	layers_supported_ = SupportsLayers();
#if defined(__ANDROID__)
	// Interruptor de depuración: adb shell setprop debug.fxrml.nolayers 1
	{
		char value[PROP_VALUE_MAX] = {0};
		if (__system_property_get("debug.fxrml.nolayers", value) > 0 && value[0] == '1')
			layers_supported_ = false;
	}
#endif
	if (!layers_supported_)
	{
		Rml::Log::Message(Rml::Log::LT_WARNING,
			"fxrmlui: sin soporte de framebuffers; se desactivan capas, filtros y sombras interiores (la UI sigue funcionando).");
	}

	initialized_ = true;
	return true;
}

bool FxRenderInterface::CreatePrograms()
{
	const unsigned int vertex_shader = CompileShaderStage(GL_VERTEX_SHADER, VertexShaderSource());
	if (!vertex_shader)
		return false;

	struct ProgramDefinition
	{
		ProgramId id;
		const char* name;
		std::string (*fragment)();
	};

	const ProgramDefinition definitions[] = {
		{ProgramId::Color, "color", FragmentSimple},
		{ProgramId::Texture, "texture", FragmentTexture},
		{ProgramId::Gradient, "gradient", FragmentGradient},
		{ProgramId::Passthrough, "passthrough", FragmentPassthrough},
		{ProgramId::ColorMatrix, "color_matrix", FragmentColorMatrix},
		{ProgramId::BlendMask, "blend_mask", FragmentBlendMask},
		{ProgramId::Blur, "blur", FragmentBlur},
		{ProgramId::DropShadow, "drop_shadow", FragmentDropShadow},
		{ProgramId::Downsample, "downsample", FragmentDownsample},
	};

	bool success = true;

	for (const ProgramDefinition& definition : definitions)
	{
		const unsigned int fragment_shader = CompileShaderStage(GL_FRAGMENT_SHADER, definition.fragment());
		if (!fragment_shader)
		{
			success = false;
			continue;
		}

		GLuint program = glCreateProgram();
		glAttachShader(program, vertex_shader);
		glAttachShader(program, fragment_shader);
		glBindAttribLocation(program, kAttributePosition, "inPosition");
		glBindAttribLocation(program, kAttributeColour, "inColor0");
		glBindAttribLocation(program, kAttributeTexCoord, "inTexCoord0");
		glLinkProgram(program);

		glDeleteShader(fragment_shader);

		GLint status = 0;
		glGetProgramiv(program, GL_LINK_STATUS, &status);
		if (!status)
		{
			GLint log_length = 0;
			glGetProgramiv(program, GL_INFO_LOG_LENGTH, &log_length);

			Rml::String log;
			if (log_length > 1)
			{
				log.resize(static_cast<size_t>(log_length));
				glGetProgramInfoLog(program, log_length, nullptr, &log[0]);
			}

			Rml::Log::Message(Rml::Log::LT_ERROR, "fxrmlui: error al enlazar el programa '%s': %s", definition.name, log.c_str());
			glDeleteProgram(program);
			success = false;
			continue;
		}

		Program& target = programs_[static_cast<size_t>(definition.id)];
		target.id = program;
		target.translate = glGetUniformLocation(program, "_translate");
		target.transform = glGetUniformLocation(program, "_transform");
		target.tex = glGetUniformLocation(program, "_tex");
		target.tex_mask = glGetUniformLocation(program, "_texMask");
		target.color = glGetUniformLocation(program, "_color");
		target.color_matrix = glGetUniformLocation(program, "_color_matrix");
		target.texel_offset = glGetUniformLocation(program, "_texelOffset");
		target.taps = glGetUniformLocation(program, "_taps");
		target.uv_offset = glGetUniformLocation(program, "_uvOffset");
		target.uv_scale = glGetUniformLocation(program, "_uvScale");
		target.weights = glGetUniformLocation(program, "_weights[0]");
		target.func = glGetUniformLocation(program, "_func");
		target.p = glGetUniformLocation(program, "_p");
		target.v = glGetUniformLocation(program, "_v");
		target.range = glGetUniformLocation(program, "_range");
	}

	glDeleteShader(vertex_shader);

	if (!programs_[static_cast<size_t>(ProgramId::Color)].id || !programs_[static_cast<size_t>(ProgramId::Texture)].id ||
		!programs_[static_cast<size_t>(ProgramId::Passthrough)].id)
	{
		Rml::Log::Message(Rml::Log::LT_ERROR, "fxrmlui: faltan programas GL básicos para dibujar la UI");
		success = false;
	}

	if (!programs_[static_cast<size_t>(ProgramId::Gradient)].id)
		Rml::Log::Message(Rml::Log::LT_WARNING, "fxrmlui: sin programa de gradientes; los decoradores de gradiente no se dibujarán");

	return success;
}

bool FxRenderInterface::CreateFullscreenQuad()
{
	if (fullscreen_quad_)
		return true;

	// Quad en espacio de clip (-1..1) con coordenadas de textura 0..1.
	Rml::Vertex vertices[4];
	for (int i = 0; i < 4; i++)
	{
		vertices[i].colour = Rml::ColourbPremultiplied(255, 255, 255, 255);
	}

	vertices[0].position = Rml::Vector2f(-1.f, -1.f);
	vertices[0].tex_coord = Rml::Vector2f(0.f, 0.f);
	vertices[1].position = Rml::Vector2f(1.f, -1.f);
	vertices[1].tex_coord = Rml::Vector2f(1.f, 0.f);
	vertices[2].position = Rml::Vector2f(1.f, 1.f);
	vertices[2].tex_coord = Rml::Vector2f(1.f, 1.f);
	vertices[3].position = Rml::Vector2f(-1.f, 1.f);
	vertices[3].tex_coord = Rml::Vector2f(0.f, 1.f);

	const int indices[6] = {0, 1, 2, 0, 2, 3};

	Rml::CompiledGeometryHandle handle = CompileGeometry({vertices, 4}, {indices, 6});
	if (!handle)
		return false;

	fullscreen_quad_ = reinterpret_cast<Geometry*>(handle);
	return true;
}

void FxRenderInterface::DestroyLayers()
{
	layer_count_ = 0;

	for (LayerTarget& layer : layers_)
		DestroyLayerTarget(layer);

	layers_.clear();

	for (LayerTarget& target : postprocess_)
		DestroyLayerTarget(target);

	for (LayerTarget& target : blur_pyramid_)
		DestroyLayerTarget(target);
	DestroyLayerTarget(blur_temp_low_);

	if (shared_stencil_)
	{
		glDeleteRenderbuffers(1, &shared_stencil_);
		shared_stencil_ = 0;
	}
}

void FxRenderInterface::DestroyAllResources()
{
	if (programs_[static_cast<size_t>(ProgramId::Color)].id)
	{
		for (Program& program : programs_)
		{
			if (program.id)
				glDeleteProgram(program.id);
			program = Program();
		}
	}

	if (fullscreen_quad_)
	{
		ReleaseGeometry(reinterpret_cast<Rml::CompiledGeometryHandle>(fullscreen_quad_));
		fullscreen_quad_ = nullptr;
	}

	if (initialized_)
		DestroyLayers();

#if !FX_GL_ES
	if (vertex_array_)
	{
		glDeleteVertexArrays(1, &vertex_array_);
		vertex_array_ = 0;
	}
#endif

	initialized_ = false;
}

bool FxRenderInterface::BeginFrame()
{
	if (viewport_width_ <= 0 || viewport_height_ <= 0)
		return false;

	if (!EnsureInitialized())
		return false;

	UpdateBlurGovernor();

	// Guardar estado GL del motor.
	glGetIntegerv(GL_VIEWPORT, saved_.viewport);
	glGetIntegerv(GL_SCISSOR_BOX, saved_.scissor_box);
	glGetIntegerv(GL_BLEND_SRC_RGB, &saved_.blend_src_rgb);
	glGetIntegerv(GL_BLEND_DST_RGB, &saved_.blend_dst_rgb);
	glGetIntegerv(GL_BLEND_SRC_ALPHA, &saved_.blend_src_alpha);
	glGetIntegerv(GL_BLEND_DST_ALPHA, &saved_.blend_dst_alpha);
	glGetIntegerv(GL_BLEND_EQUATION_RGB, &saved_.blend_equation_rgb);
	glGetIntegerv(GL_BLEND_EQUATION_ALPHA, &saved_.blend_equation_alpha);
	glGetIntegerv(GL_CURRENT_PROGRAM, &saved_.program);
	glGetIntegerv(GL_ACTIVE_TEXTURE, &saved_.active_texture);
	glGetIntegerv(GL_TEXTURE_BINDING_2D, &saved_.texture_2d);
	glGetIntegerv(GL_ARRAY_BUFFER_BINDING, &saved_.array_buffer);
	glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING, &saved_.element_array_buffer);
	glGetIntegerv(GL_STENCIL_CLEAR_VALUE, &saved_.stencil_clear_value);
	glGetIntegerv(GL_STENCIL_FUNC, &saved_.stencil_func);
	glGetIntegerv(GL_STENCIL_REF, &saved_.stencil_ref);
	glGetIntegerv(GL_STENCIL_VALUE_MASK, &saved_.stencil_value_mask);
	glGetIntegerv(GL_STENCIL_WRITEMASK, &saved_.stencil_write_mask);
	glGetIntegerv(GL_STENCIL_FAIL, &saved_.stencil_fail);
	glGetIntegerv(GL_STENCIL_PASS_DEPTH_FAIL, &saved_.stencil_pass_depth_fail);
	glGetIntegerv(GL_STENCIL_PASS_DEPTH_PASS, &saved_.stencil_pass_depth_pass);

	if (glStencilFuncSeparate)
	{
		glGetIntegerv(GL_STENCIL_BACK_FUNC, &saved_.stencil_back_func);
		glGetIntegerv(GL_STENCIL_BACK_REF, &saved_.stencil_back_ref);
		glGetIntegerv(GL_STENCIL_BACK_VALUE_MASK, &saved_.stencil_back_value_mask);
		glGetIntegerv(GL_STENCIL_BACK_WRITEMASK, &saved_.stencil_back_write_mask);
		glGetIntegerv(GL_STENCIL_BACK_FAIL, &saved_.stencil_back_fail);
		glGetIntegerv(GL_STENCIL_BACK_PASS_DEPTH_FAIL, &saved_.stencil_back_pass_depth_fail);
		glGetIntegerv(GL_STENCIL_BACK_PASS_DEPTH_PASS, &saved_.stencil_back_pass_depth_pass);
	}

	glGetFloatv(GL_COLOR_CLEAR_VALUE, saved_.clear_color);
	glGetBooleanv(GL_COLOR_WRITEMASK, saved_.color_mask);
	glGetBooleanv(GL_DEPTH_WRITEMASK, &saved_.depth_mask);
	saved_.depth_test = glIsEnabled(GL_DEPTH_TEST) != GL_FALSE;
	saved_.cull_face = glIsEnabled(GL_CULL_FACE) != GL_FALSE;
	saved_.blend = glIsEnabled(GL_BLEND) != GL_FALSE;
	saved_.scissor = glIsEnabled(GL_SCISSOR_TEST) != GL_FALSE;
	saved_.stencil = glIsEnabled(GL_STENCIL_TEST) != GL_FALSE;

	saved_.framebuffer = 0;
	if (glBindFramebuffer)
		glGetIntegerv(GL_FRAMEBUFFER_BINDING, &saved_.framebuffer);

	saved_.vertex_array = 0;
	if (fx_gl_has_vertex_arrays())
		glGetIntegerv(GL_VERTEX_ARRAY_BINDING, &saved_.vertex_array);

	// Comprobar/recrear capas si cambió el tamaño.
	if (layers_supported_)
	{
		if (!layers_.empty() && (layers_[0].width != viewport_width_ || layers_[0].height != viewport_height_))
			DestroyLayers();
	}

	// Estado esperado por la UI.
	glDisable(GL_DEPTH_TEST);
	glDisable(GL_CULL_FACE);
	glDisable(GL_SCISSOR_TEST);
	glDepthMask(GL_FALSE);
	glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
	glEnable(GL_BLEND);
	glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
	glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
	glClearStencil(0);
	glClearColor(0.f, 0.f, 0.f, 0.f);
	glViewport(0, 0, viewport_width_, viewport_height_);

	scissor_region_ = Rml::Rectanglei::MakeInvalid();
	scissor_enabled_ = false;
	transform_enabled_ = false;
	projection_ = Rml::Matrix4f::ProjectOrtho(0, static_cast<float>(viewport_width_), static_cast<float>(viewport_height_), 0, -10000, 10000);
	active_program_ = ProgramId::None;

#if !FX_GL_ES
	if (vertex_array_)
		glBindVertexArray(vertex_array_);
#endif

	layer_count_ = 0;

	if (layers_supported_)
	{
		if (!InitLayers())
		{
			layers_supported_ = false;
			DestroyLayers();
			if (!layers_warning_logged_)
			{
				Rml::Log::Message(Rml::Log::LT_WARNING, "fxrmlui: no se pudo crear el framebuffer de la UI; se continuará sin capas ni filtros");
				layers_warning_logged_ = true;
			}
		}
	}

	if (layers_supported_)
	{
		layer_count_ = 1;
		BindLayer(0);
		glEnable(GL_STENCIL_TEST);
		glStencilFunc(GL_ALWAYS, 1, static_cast<GLuint>(-1));
		glStencilMask(static_cast<GLuint>(-1));
		glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
		glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
	}

	UseProgram(ProgramId::Color);
	frame_active_ = true;
	return true;
}

void FxRenderInterface::EndFrame()
{
	if (!frame_active_)
		return;

	// Componer la capa base sobre el framebuffer que usaba el motor.
	if (layers_supported_ && layer_count_ > 0 && layers_[0].Valid())
	{
		glBindFramebuffer(GL_FRAMEBUFFER, static_cast<GLuint>(saved_.framebuffer));
		glViewport(0, 0, viewport_width_, viewport_height_);
		glDisable(GL_SCISSOR_TEST);
		glDisable(GL_STENCIL_TEST);
		glEnable(GL_BLEND);
		glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
		glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
		glActiveTexture(GL_TEXTURE0);
		BindTexture(layers_[0].texture);
		DrawFullscreenQuad();
		layer_count_ = 0;
	}

	// Restaurar el estado del motor.
#if !FX_GL_ES
	if (fx_gl_has_vertex_arrays())
		glBindVertexArray(static_cast<GLuint>(saved_.vertex_array));
#endif

	if (glBindFramebuffer)
		glBindFramebuffer(GL_FRAMEBUFFER, static_cast<GLuint>(saved_.framebuffer));

	glBindBuffer(GL_ARRAY_BUFFER, static_cast<GLuint>(saved_.array_buffer));
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, static_cast<GLuint>(saved_.element_array_buffer));
	glUseProgram(static_cast<GLuint>(saved_.program));
	glActiveTexture(static_cast<GLenum>(saved_.active_texture));
	glBindTexture(GL_TEXTURE_2D, static_cast<GLuint>(saved_.texture_2d));

	glViewport(saved_.viewport[0], saved_.viewport[1], saved_.viewport[2], saved_.viewport[3]);
	glScissor(saved_.scissor_box[0], saved_.scissor_box[1], saved_.scissor_box[2], saved_.scissor_box[3]);
	glBlendEquationSeparate(static_cast<GLenum>(saved_.blend_equation_rgb), static_cast<GLenum>(saved_.blend_equation_alpha));
	glBlendFuncSeparate(static_cast<GLenum>(saved_.blend_src_rgb), static_cast<GLenum>(saved_.blend_dst_rgb),
		static_cast<GLenum>(saved_.blend_src_alpha), static_cast<GLenum>(saved_.blend_dst_alpha));

	glDepthMask(saved_.depth_mask);
	glColorMask(saved_.color_mask[0], saved_.color_mask[1], saved_.color_mask[2], saved_.color_mask[3]);
	glStencilFunc(static_cast<GLenum>(saved_.stencil_func), saved_.stencil_ref, static_cast<GLuint>(saved_.stencil_value_mask));
	glStencilMask(static_cast<GLuint>(saved_.stencil_write_mask));
	glStencilOp(static_cast<GLenum>(saved_.stencil_fail), static_cast<GLenum>(saved_.stencil_pass_depth_fail),
		static_cast<GLenum>(saved_.stencil_pass_depth_pass));
	glClearStencil(saved_.stencil_clear_value);
	glClearColor(saved_.clear_color[0], saved_.clear_color[1], saved_.clear_color[2], saved_.clear_color[3]);

	if (glStencilFuncSeparate && glStencilMaskSeparate && glStencilOpSeparate)
	{
		glStencilFuncSeparate(GL_FRONT, static_cast<GLenum>(saved_.stencil_func), saved_.stencil_ref, static_cast<GLuint>(saved_.stencil_value_mask));
		glStencilMaskSeparate(GL_FRONT, static_cast<GLuint>(saved_.stencil_write_mask));
		glStencilOpSeparate(GL_FRONT, static_cast<GLenum>(saved_.stencil_fail), static_cast<GLenum>(saved_.stencil_pass_depth_fail),
			static_cast<GLenum>(saved_.stencil_pass_depth_pass));

		glStencilFuncSeparate(GL_BACK, static_cast<GLenum>(saved_.stencil_back_func), saved_.stencil_back_ref,
			static_cast<GLuint>(saved_.stencil_back_value_mask));
		glStencilMaskSeparate(GL_BACK, static_cast<GLuint>(saved_.stencil_back_write_mask));
		glStencilOpSeparate(GL_BACK, static_cast<GLenum>(saved_.stencil_back_fail), static_cast<GLenum>(saved_.stencil_back_pass_depth_fail),
			static_cast<GLenum>(saved_.stencil_back_pass_depth_pass));
	}

	if (saved_.depth_test)
		glEnable(GL_DEPTH_TEST);
	else
		glDisable(GL_DEPTH_TEST);

	if (saved_.cull_face)
		glEnable(GL_CULL_FACE);
	else
		glDisable(GL_CULL_FACE);

	if (saved_.blend)
		glEnable(GL_BLEND);
	else
		glDisable(GL_BLEND);

	if (saved_.scissor)
		glEnable(GL_SCISSOR_TEST);
	else
		glDisable(GL_SCISSOR_TEST);

	if (saved_.stencil)
		glEnable(GL_STENCIL_TEST);
	else
		glDisable(GL_STENCIL_TEST);

	frame_active_ = false;
}

bool FxRenderInterface::InitLayers()
{
	if (!layers_supported_)
		return false;

	if (!layers_.empty() && layers_[0].Valid())
	{
		layer_count_ = 1;
		return true;
	}

	layers_.clear();
	layer_count_ = 0;

	LayerTarget base;
	if (!CreateLayerTarget(base, viewport_width_, viewport_height_, true))
		return false;

	layers_.push_back(base);
	layer_count_ = 1;

	// Asegurar que los framebuffers de posproceso existen con el tamaño actual.
	for (LayerTarget& target : postprocess_)
	{
		if (target.Valid() && (target.width != viewport_width_ || target.height != viewport_height_))
			DestroyLayerTarget(target);
	}

	return true;
}

bool FxRenderInterface::CreateLayerTarget(LayerTarget& target, int width, int height, bool with_stencil)
{
	if (width <= 0 || height <= 0)
		return false;

	target = LayerTarget();
	target.width = width;
	target.height = height;

	glGenTextures(1, &target.texture);
	if (!target.texture)
		return false;

	glBindTexture(GL_TEXTURE_2D, target.texture);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
	glBindTexture(GL_TEXTURE_2D, 0);

	glGenFramebuffers(1, &target.framebuffer);
	if (!target.framebuffer)
	{
		DestroyLayerTarget(target);
		return false;
	}

	glBindFramebuffer(GL_FRAMEBUFFER, target.framebuffer);
	glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, target.texture, 0);

	if (with_stencil)
	{
		if (!shared_stencil_)
		{
			glGenRenderbuffers(1, &shared_stencil_);
			glBindRenderbuffer(GL_RENDERBUFFER, shared_stencil_);
			glRenderbufferStorage(GL_RENDERBUFFER, GL_STENCIL_INDEX8, width, height);
			glBindRenderbuffer(GL_RENDERBUFFER, 0);

			if (!shared_stencil_)
			{
				glBindFramebuffer(GL_FRAMEBUFFER, 0);
				DestroyLayerTarget(target);
				return false;
			}
		}

		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, shared_stencil_);
	}

	const GLenum status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
	glBindFramebuffer(GL_FRAMEBUFFER, 0);

	if (status != GL_FRAMEBUFFER_COMPLETE)
	{
		Rml::Log::Message(Rml::Log::LT_ERROR, "fxrmlui: framebuffer incompleto (0x%04X) de %dx%d", static_cast<unsigned>(status), width, height);
		DestroyLayerTarget(target);
		return false;
	}

	return true;
}

void FxRenderInterface::DestroyLayerTarget(LayerTarget& target, bool release_texture)
{
	if (target.framebuffer)
	{
		glDeleteFramebuffers(1, &target.framebuffer);
		target.framebuffer = 0;
	}

	if (release_texture && target.texture)
	{
		glDeleteTextures(1, &target.texture);
		target.texture = 0;
	}

	target.width = 0;
	target.height = 0;
}

bool FxRenderInterface::CreatePostprocessTarget(LayerTarget& target)
{
	return CreateLayerTarget(target, viewport_width_, viewport_height_, false);
}

FxRenderInterface::LayerTarget& FxRenderInterface::EnsurePostprocess(int index)
{
	LayerTarget& target = postprocess_[index];
	if (!target.Valid() || target.width != viewport_width_ || target.height != viewport_height_)
	{
		DestroyLayerTarget(target);
		CreatePostprocessTarget(target);
	}
	return target;
}

FxRenderInterface::LayerTarget& FxRenderInterface::TopLayer()
{
	return layers_[static_cast<size_t>(std::max(layer_count_ - 1, 0))];
}

void FxRenderInterface::BindLayer(int layer_index)
{
	if (!layers_supported_)
		return;

	layer_index = std::max(0, std::min(layer_index, static_cast<int>(layers_.size()) - 1));
	glBindFramebuffer(GL_FRAMEBUFFER, layers_[static_cast<size_t>(layer_index)].framebuffer);
}

void FxRenderInterface::BindTexture(unsigned int texture)
{
	glActiveTexture(GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, texture);
}

void FxRenderInterface::UseProgram(ProgramId program)
{
	if (program == ProgramId::None)
	{
		active_program_ = ProgramId::None;
		glUseProgram(0);
		return;
	}

	const unsigned int id = programs_[static_cast<size_t>(program)].id;
	if (!id)
		return;

	if (active_program_ != program)
	{
		glUseProgram(id);
		active_program_ = program;
	}
}

int FxRenderInterface::Uniform(ProgramId program, int Program::*member) const
{
	return programs_[static_cast<size_t>(program)].*member;
}

void FxRenderInterface::SubmitTransform(const Program& program, Rml::Vector2f translation)
{
	Rml::Matrix4f matrix = projection_;
	if (transform_enabled_)
		matrix = projection_ * transform_;

	constexpr bool transpose = std::is_same_v<Rml::Matrix4f, Rml::RowMajorMatrix4f>;
	if (program.transform >= 0)
		glUniformMatrix4fv(program.transform, 1, transpose ? GL_TRUE : GL_FALSE, matrix.data());
	if (program.translate >= 0)
		glUniform2f(program.translate, translation.x, translation.y);
}

void FxRenderInterface::DrawFullscreenQuad(ProgramId program)
{
	DrawQuad(Rml::Vector2f(0.f, 0.f), Rml::Vector2f(1.f, 1.f), program);
}

void FxRenderInterface::DrawQuad(Rml::Vector2f uv_offset, Rml::Vector2f uv_scale, ProgramId program_id)
{
	if (!fullscreen_quad_)
		return;

	// Cada pasada de posproceso dibuja con su propio programa; antes se forzaba
	// Passthrough y blur/color-matrix/drop-shadow no tenian efecto.
	UseProgram(program_id);
	const Program& program = programs_[static_cast<size_t>(program_id)];

	// El quad ya está en espacio de clip: se anula proyección y transformación.
	constexpr bool transpose = std::is_same_v<Rml::Matrix4f, Rml::RowMajorMatrix4f>;
	static const Rml::Matrix4f identity = Rml::Matrix4f::Identity();
	if (program.transform >= 0)
		glUniformMatrix4fv(program.transform, 1, transpose ? GL_TRUE : GL_FALSE, identity.data());
	if (program.translate >= 0)
		glUniform2f(program.translate, 0.f, 0.f);
	if (program.uv_offset >= 0)
		glUniform2f(program.uv_offset, uv_offset.x, uv_offset.y);
	if (program.uv_scale >= 0)
		glUniform2f(program.uv_scale, uv_scale.x, uv_scale.y);
	if (program.tex >= 0)
		glUniform1i(program.tex, 0);

	const Geometry& geometry = *fullscreen_quad_;
	glBindBuffer(GL_ARRAY_BUFFER, geometry.vertex_buffer);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, geometry.index_buffer);

	glEnableVertexAttribArray(kAttributePosition);
	glEnableVertexAttribArray(kAttributeColour);
	glEnableVertexAttribArray(kAttributeTexCoord);

	const GLsizei stride = static_cast<GLsizei>(sizeof(Rml::Vertex));
	glVertexAttribPointer(kAttributePosition, 2, GL_FLOAT, GL_FALSE, stride, reinterpret_cast<const void*>(offsetof(Rml::Vertex, position)));
	glVertexAttribPointer(kAttributeColour, 4, GL_UNSIGNED_BYTE, GL_TRUE, stride, reinterpret_cast<const void*>(offsetof(Rml::Vertex, colour)));
	glVertexAttribPointer(kAttributeTexCoord, 2, GL_FLOAT, GL_FALSE, stride, reinterpret_cast<const void*>(offsetof(Rml::Vertex, tex_coord)));

	glDrawElements(GL_TRIANGLES, geometry.index_count, static_cast<GLenum>(geometry.index_type), nullptr);

	glDisableVertexAttribArray(kAttributePosition);
	glDisableVertexAttribArray(kAttributeColour);
	glDisableVertexAttribArray(kAttributeTexCoord);

	glBindBuffer(GL_ARRAY_BUFFER, 0);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
}

void FxRenderInterface::DrawGeometryBuffers(const Geometry& geometry)
{
	for (const Geometry* chunk = &geometry; chunk; chunk = chunk->next.get())
	{
		glBindBuffer(GL_ARRAY_BUFFER, chunk->vertex_buffer);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, chunk->index_buffer);

		glEnableVertexAttribArray(kAttributePosition);
		glEnableVertexAttribArray(kAttributeColour);
		glEnableVertexAttribArray(kAttributeTexCoord);

		const GLsizei stride = static_cast<GLsizei>(sizeof(Rml::Vertex));
		glVertexAttribPointer(kAttributePosition, 2, GL_FLOAT, GL_FALSE, stride, reinterpret_cast<const void*>(offsetof(Rml::Vertex, position)));
		glVertexAttribPointer(kAttributeColour, 4, GL_UNSIGNED_BYTE, GL_TRUE, stride, reinterpret_cast<const void*>(offsetof(Rml::Vertex, colour)));
		glVertexAttribPointer(kAttributeTexCoord, 2, GL_FLOAT, GL_FALSE, stride, reinterpret_cast<const void*>(offsetof(Rml::Vertex, tex_coord)));

		glDrawElements(GL_TRIANGLES, chunk->index_count, static_cast<GLenum>(chunk->index_type), nullptr);

		glDisableVertexAttribArray(kAttributePosition);
		glDisableVertexAttribArray(kAttributeColour);
		glDisableVertexAttribArray(kAttributeTexCoord);

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}
}

void FxRenderInterface::DrawGeometryImpl(const Geometry& geometry, Rml::Vector2f translation, Rml::TextureHandle texture,
	bool use_texture_program)
{
	UseProgram(use_texture_program ? ProgramId::Texture : ProgramId::Color);

	const Program& program = programs_[static_cast<size_t>(use_texture_program ? ProgramId::Texture : ProgramId::Color)];
	SubmitTransform(program, translation);

	if (program.tex >= 0)
	{
		glUniform1i(program.tex, 0);
		if (texture)
			BindTexture(static_cast<unsigned int>(texture));
	}

	DrawGeometryBuffers(geometry);
}

void FxRenderInterface::RenderFullscreenLayer(const LayerTarget& source, const LayerTarget& destination, bool blend, bool flip_vertical)
{
	if (!source.Valid() || !destination.Valid())
		return;

	if (blend)
		glBindFramebuffer(GL_FRAMEBUFFER, destination.framebuffer);
	else
		BindFramebufferForOverwrite(destination.framebuffer);
	glViewport(0, 0, destination.width, destination.height);
	BindTexture(source.texture);

	if (blend)
		glEnable(GL_BLEND);
	else
		glDisable(GL_BLEND);

	if (flip_vertical)
		DrawQuad(Rml::Vector2f(0.f, 1.f), Rml::Vector2f(1.f, -1.f));
	else
		DrawFullscreenQuad();

	if (blend)
		glEnable(GL_BLEND);
	else
		glDisable(GL_BLEND);
}

Rml::CompiledGeometryHandle FxRenderInterface::CompileGeometry(Rml::Span<const Rml::Vertex> vertices, Rml::Span<const int> indices)
{
	if (vertices.empty() || indices.empty())
		return 0;

	Geometry* geometry = new Geometry();

	auto upload_chunk = [](Geometry& target, const Rml::Vertex* vertex_data, size_t vertex_count, const void* index_data, size_t index_size,
							int index_count, unsigned int index_type) {
		target.index_count = index_count;
		target.index_type = index_type;

		glGenBuffers(1, &target.vertex_buffer);
		glBindBuffer(GL_ARRAY_BUFFER, target.vertex_buffer);
		glBufferData(GL_ARRAY_BUFFER, static_cast<FxGLsizeiptr>(vertex_count * sizeof(Rml::Vertex)), vertex_data, GL_STATIC_DRAW);

		glGenBuffers(1, &target.index_buffer);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, target.index_buffer);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, static_cast<FxGLsizeiptr>(index_size), index_data, GL_STATIC_DRAW);
	};

	bool uploaded = false;

#if FX_GL_ES
	// GLES 2.0 sólo garantiza índices de 16 bits.
	int max_index = 0;
	for (const int index : indices)
		max_index = std::max(max_index, index);

	if (max_index <= GeometryChunkLimit())
	{
		std::vector<unsigned short> indices16(indices.size());
		for (size_t i = 0; i < indices.size(); ++i)
			indices16[i] = static_cast<unsigned short>(indices[i]);

		upload_chunk(*geometry, vertices.data(), vertices.size(), indices16.data(), indices16.size() * sizeof(unsigned short),
			static_cast<int>(indices16.size()), GL_UNSIGNED_SHORT);
		uploaded = true;
	}
	else if (!SupportsUintIndices())
	{
		// Sin GL_OES_element_index_uint: se reparten los triángulos en trozos de
		// <= 65535 vértices, remapeando los índices dentro de cada trozo.
		std::vector<Rml::Vertex> chunk_vertices;
		std::vector<unsigned short> chunk_indices;
		std::unordered_map<int, unsigned short> remap;
		Geometry* tail = geometry;
		bool first = true;

		auto flush = [&]() {
			if (chunk_indices.empty())
				return;

			Geometry* target = tail;
			if (!first)
			{
				tail->next = std::make_unique<Geometry>();
				target = tail->next.get();
				tail = target;
			}
			first = false;

			upload_chunk(*target, chunk_vertices.data(), chunk_vertices.size(), chunk_indices.data(),
				chunk_indices.size() * sizeof(unsigned short), static_cast<int>(chunk_indices.size()), GL_UNSIGNED_SHORT);

			chunk_vertices.clear();
			chunk_indices.clear();
			remap.clear();
		};

		for (size_t i = 0; i + 2 < indices.size(); i += 3)
		{
			int missing = 0;
			for (size_t k = 0; k < 3; ++k)
				if (remap.find(indices[i + k]) == remap.end())
					++missing;

			if (chunk_vertices.size() + static_cast<size_t>(missing) > static_cast<size_t>(GeometryChunkLimit()))
				flush();

			for (size_t k = 0; k < 3; ++k)
			{
				const int source_index = indices[i + k];
				auto found = remap.find(source_index);
				if (found == remap.end())
				{
					found = remap.emplace(source_index, static_cast<unsigned short>(chunk_vertices.size())).first;
					chunk_vertices.push_back(vertices[static_cast<size_t>(source_index)]);
				}
				chunk_indices.push_back(found->second);
			}
		}
		flush();
		uploaded = !first;

		static bool logged = false;
		if (!logged)
		{
			logged = true;
			Rml::Log::Message(Rml::Log::LT_INFO, "fxrmlui: mesh de %zu vertices troceado (sin indices de 32 bits en GLES2)", vertices.size());
		}
	}
#endif

	if (!uploaded)
	{
		upload_chunk(*geometry, vertices.data(), vertices.size(), indices.data(), indices.size() * sizeof(int), static_cast<int>(indices.size()),
			GL_UNSIGNED_INT);
	}

	glBindBuffer(GL_ARRAY_BUFFER, 0);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

	return reinterpret_cast<Rml::CompiledGeometryHandle>(geometry);
}

void FxRenderInterface::RenderGeometry(Rml::CompiledGeometryHandle handle, Rml::Vector2f translation, Rml::TextureHandle texture)
{
	if (!handle)
		return;

	const Geometry& geometry = *reinterpret_cast<const Geometry*>(handle);
	DrawGeometryImpl(geometry, translation, texture, texture != 0);
}

void FxRenderInterface::ReleaseGeometry(Rml::CompiledGeometryHandle handle)
{
	if (!handle)
		return;

	Geometry* geometry = reinterpret_cast<Geometry*>(handle);

	for (Geometry* chunk = geometry; chunk; chunk = chunk->next.get())
	{
		if (chunk->vertex_buffer)
			glDeleteBuffers(1, &chunk->vertex_buffer);
		if (chunk->index_buffer)
			glDeleteBuffers(1, &chunk->index_buffer);
	}

	delete geometry;
}

// TGA empaquetado a 1 byte para leer el header directamente del archivo.
#pragma pack(1)
struct TGAHeader
{
	char idLength;
	char colourMapType;
	char dataType;
	short int colourMapOrigin;
	short int colourMapLength;
	char colourMapDepth;
	short int xOrigin;
	short int yOrigin;
	short int width;
	short int height;
	char bitsPerPixel;
	char imageDescriptor;
};
#pragma pack()

Rml::TextureHandle FxRenderInterface::LoadTexture(Rml::Vector2i& texture_dimensions, const Rml::String& source)
{
	Rml::FileInterface* file_interface = Rml::GetFileInterface();
	if (!file_interface)
		return 0;

	Rml::FileHandle file_handle = file_interface->Open(source);
	if (!file_handle)
		return 0;

	file_interface->Seek(file_handle, 0, SEEK_END);
	const size_t buffer_size = file_interface->Tell(file_handle);
	file_interface->Seek(file_handle, 0, SEEK_SET);

	if (buffer_size <= sizeof(TGAHeader))
	{
		Rml::Log::Message(Rml::Log::LT_ERROR, "fxrmlui: el archivo de textura es menor que una cabecera TGA");
		file_interface->Close(file_handle);
		return 0;
	}

	using Rml::byte;
	Rml::UniquePtr<byte[]> buffer(new byte[buffer_size]);
	file_interface->Read(buffer.get(), buffer_size, file_handle);
	file_interface->Close(file_handle);

	TGAHeader header;
	memcpy(&header, buffer.get(), sizeof(TGAHeader));

	const int color_mode = header.bitsPerPixel / 8;
	const size_t image_size = static_cast<size_t>(header.width) * static_cast<size_t>(header.height) * 4;

	if (header.dataType != 2)
	{
		Rml::Log::Message(Rml::Log::LT_ERROR, "fxrmlui: sólo se soportan TGA sin compresión");
		return 0;
	}

	if (color_mode < 3)
	{
		Rml::Log::Message(Rml::Log::LT_ERROR, "fxrmlui: sólo se soportan TGA de 24 y 32 bits");
		return 0;
	}

	const byte* image_src = buffer.get() + sizeof(TGAHeader);
	Rml::UniquePtr<byte[]> image_dest_buffer(new byte[image_size]);
	byte* image_dest = image_dest_buffer.get();
	const bool top_to_bottom_order = ((header.imageDescriptor & 32) != 0);

	// TGA es BGR: se convierte a RGBA, se voltea el eje Y si hace falta y se
	// premultiplica el alfa (formato que espera RmlUi).
	for (long y = 0; y < header.height; y++)
	{
		long read_index = y * header.width * color_mode;
		long write_index = top_to_bottom_order ? (y * header.width * 4) : (header.height - y - 1) * header.width * 4;

		for (long x = 0; x < header.width; x++)
		{
			image_dest[write_index] = image_src[read_index + 2];
			image_dest[write_index + 1] = image_src[read_index + 1];
			image_dest[write_index + 2] = image_src[read_index];

			if (color_mode == 4)
			{
				const byte alpha = image_src[read_index + 3];
				for (size_t j = 0; j < 3; j++)
					image_dest[write_index + j] = byte((image_dest[write_index + j] * alpha) / 255);
				image_dest[write_index + 3] = alpha;
			}
			else
			{
				image_dest[write_index + 3] = 255;
			}

			write_index += 4;
			read_index += color_mode;
		}
	}

	texture_dimensions.x = header.width;
	texture_dimensions.y = header.height;

	return GenerateTexture({image_dest, image_size}, texture_dimensions);
}

Rml::TextureHandle FxRenderInterface::GenerateTexture(Rml::Span<const Rml::byte> source, Rml::Vector2i source_dimensions)
{
	if (!source.data() || source_dimensions.x <= 0 || source_dimensions.y <= 0)
		return 0;

	GLuint texture_id = 0;
	glGenTextures(1, &texture_id);
	if (!texture_id)
	{
		Rml::Log::Message(Rml::Log::LT_ERROR, "fxrmlui: no se pudo generar la textura");
		return 0;
	}

	glActiveTexture(GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, texture_id);
	glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, source_dimensions.x, source_dimensions.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, source.data());
	glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

	// GLES 2.0 exige CLAMP_TO_EDGE en texturas NPOT (las de SVG/Lottie lo son).
#if FX_GL_ES
	const GLenum wrap = (IsPowerOfTwo(source_dimensions.x) && IsPowerOfTwo(source_dimensions.y)) ? GL_REPEAT : GL_CLAMP_TO_EDGE;
#else
	const GLenum wrap = GL_REPEAT;
#endif
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);

	glBindTexture(GL_TEXTURE_2D, 0);

	return static_cast<Rml::TextureHandle>(texture_id);
}

void FxRenderInterface::ReleaseTexture(Rml::TextureHandle texture)
{
	if (!texture)
		return;

	GLuint texture_id = static_cast<GLuint>(texture);
	glDeleteTextures(1, &texture_id);
}

void FxRenderInterface::EnableScissorRegion(bool enable)
{
	scissor_enabled_ = enable;

	if (enable)
	{
		glEnable(GL_SCISSOR_TEST);
	}
	else
	{
		glDisable(GL_SCISSOR_TEST);
		scissor_region_ = Rml::Rectanglei::MakeInvalid();
	}
}

void FxRenderInterface::SetScissorRegion(Rml::Rectanglei region)
{
	scissor_region_ = region;

	if (!region.Valid())
	{
		glDisable(GL_SCISSOR_TEST);
		scissor_enabled_ = false;
		return;
	}

	const int x = region.Left();
	const int y = viewport_height_ - region.Bottom();
	const int width = std::max(region.Width(), 0);
	const int height = std::max(region.Height(), 0);

	glEnable(GL_SCISSOR_TEST);
	scissor_enabled_ = true;
	glScissor(x, y, width, height);
}

void FxRenderInterface::EnableClipMask(bool enable)
{
	if (enable)
		glEnable(GL_STENCIL_TEST);
	else
		glDisable(GL_STENCIL_TEST);
}

void FxRenderInterface::RenderToClipMask(Rml::ClipMaskOperation operation, Rml::CompiledGeometryHandle handle, Rml::Vector2f translation)
{
	if (!handle)
		return;

	using Rml::ClipMaskOperation;

	const bool clear_stencil = (operation == ClipMaskOperation::Set || operation == ClipMaskOperation::SetInverse);
	if (clear_stencil)
		glClear(GL_STENCIL_BUFFER_BIT);

	GLint stencil_test_value = 0;
	glGetIntegerv(GL_STENCIL_REF, &stencil_test_value);

	glColorMask(GL_FALSE, GL_FALSE, GL_FALSE, GL_FALSE);
	glStencilFunc(GL_ALWAYS, 1, static_cast<GLuint>(-1));

	switch (operation)
	{
	case ClipMaskOperation::Set:
		glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
		stencil_test_value = 1;
		break;
	case ClipMaskOperation::SetInverse:
		glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
		stencil_test_value = 0;
		break;
	case ClipMaskOperation::Intersect:
		glStencilOp(GL_KEEP, GL_KEEP, GL_INCR);
		stencil_test_value += 1;
		break;
	}

	RenderGeometry(handle, translation, {});

	glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
	glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
	glStencilFunc(GL_EQUAL, stencil_test_value, static_cast<GLuint>(-1));
}

void FxRenderInterface::SetTransform(const Rml::Matrix4f* transform)
{
	if (transform)
	{
		transform_ = *transform;
		transform_enabled_ = true;
	}
	else
	{
		transform_enabled_ = false;
	}
}

// ---------------------------------------------------------------------------
//  Capas y filtros
// ---------------------------------------------------------------------------

Rml::LayerHandle FxRenderInterface::PushLayer()
{
	if (!layers_supported_ || !frame_active_)
		return 0;

	if (layer_count_ == static_cast<int>(layers_.size()))
	{
		LayerTarget target;
		if (!CreateLayerTarget(target, viewport_width_, viewport_height_, true))
		{
			Rml::Log::Message(Rml::Log::LT_WARNING, "fxrmlui: no se pudo crear una capa nueva");
			return static_cast<Rml::LayerHandle>(std::max(layer_count_ - 1, 0));
		}
		layers_.push_back(target);
	}

	++layer_count_;
	BindLayer(layer_count_ - 1);

	// La nueva capa debe nacer transparente dentro del scissor activo.
	glClearColor(0.f, 0.f, 0.f, 0.f);
	glClear(GL_COLOR_BUFFER_BIT);

	return static_cast<Rml::LayerHandle>(layer_count_ - 1);
}

void FxRenderInterface::PopLayer()
{
	if (!layers_supported_ || layer_count_ <= 1)
		return;

	--layer_count_;
	BindLayer(layer_count_ - 1);
}

void FxRenderInterface::SwapPostprocessPrimarySecondary()
{
	std::swap(postprocess_[0], postprocess_[1]);
}

void FxRenderInterface::CopyLayerToPostprocessPrimary(Rml::LayerHandle layer)
{
	if (!layers_supported_ || layer >= layers_.size())
		return;

	LayerTarget& destination = EnsurePostprocess(0);
	RenderFullscreenLayer(layers_[layer], destination, false);
	glBindFramebuffer(GL_FRAMEBUFFER, TopLayer().framebuffer);
}

void FxRenderInterface::CompositeLayers(Rml::LayerHandle source, Rml::LayerHandle destination, Rml::BlendMode blend_mode,
	Rml::Span<const Rml::CompiledFilterHandle> filters)
{
	if (!layers_supported_ || !frame_active_)
		return;

	if (source >= layers_.size() || destination >= layers_.size())
		return;

	CopyLayerToPostprocessPrimary(source);

	RenderFilters(filters);


	LayerTarget& destination_layer = layers_[static_cast<size_t>(destination)];
	LayerTarget& primary = EnsurePostprocess(0);

	glBindFramebuffer(GL_FRAMEBUFFER, destination_layer.framebuffer);
	glViewport(0, 0, viewport_width_, viewport_height_);
	BindTexture(primary.texture);

	if (blend_mode == Rml::BlendMode::Replace)
		glDisable(GL_BLEND);
	else
		glEnable(GL_BLEND);

	DrawFullscreenQuad();

	if (blend_mode == Rml::BlendMode::Replace)
		glEnable(GL_BLEND);


	// Volver a la capa superior activa.
	glBindFramebuffer(GL_FRAMEBUFFER, TopLayer().framebuffer);
}

Rml::TextureHandle FxRenderInterface::SaveLayerAsTexture()
{
	if (!layers_supported_ || !scissor_region_.Valid())
		return 0;

	Rml::Rectanglei bounds = scissor_region_;
	bounds.p0.x = std::max(bounds.p0.x, 0);
	bounds.p0.y = std::max(bounds.p0.y, 0);
	bounds.p1.x = std::min(bounds.p1.x, viewport_width_);
	bounds.p1.y = std::min(bounds.p1.y, viewport_height_);

	if (!bounds.Valid() || bounds.Width() == 0 || bounds.Height() == 0)
		return 0;

	LayerTarget target;
	if (!CreateLayerTarget(target, bounds.Width(), bounds.Height(), false))
		return 0;

	GLint previous_framebuffer = 0;
	glGetIntegerv(GL_FRAMEBUFFER_BINDING, &previous_framebuffer);

	GLint previous_viewport[4];
	glGetIntegerv(GL_VIEWPORT, previous_viewport);

	float previous_clear_color[4];
	glGetFloatv(GL_COLOR_CLEAR_VALUE, previous_clear_color);

	const bool scissor_was_enabled = glIsEnabled(GL_SCISSOR_TEST) != GL_FALSE;
	const bool stencil_was_enabled = glIsEnabled(GL_STENCIL_TEST) != GL_FALSE;
	const bool blend_was_enabled = glIsEnabled(GL_BLEND) != GL_FALSE;

	glBindFramebuffer(GL_FRAMEBUFFER, target.framebuffer);
	glViewport(0, 0, bounds.Width(), bounds.Height());
	glDisable(GL_SCISSOR_TEST);
	glDisable(GL_STENCIL_TEST);
	glDisable(GL_BLEND);
	glClearColor(0.f, 0.f, 0.f, 0.f);
	glClear(GL_COLOR_BUFFER_BIT);

	// La capa se muestrea en coordenadas de UI. El quad de salida se voltea en
	// vertical (v crece hacia arriba), de modo que la textura resultante respeta
	// la convención de RmlUi (fila 0 = parte superior).
	const float u_scale = static_cast<float>(bounds.Width()) / static_cast<float>(viewport_width_);
	const float v_scale = -static_cast<float>(bounds.Height()) / static_cast<float>(viewport_height_);
	const Rml::Vector2f uv_offset(static_cast<float>(bounds.Left()) / static_cast<float>(viewport_width_),
		1.f - static_cast<float>(bounds.Top()) / static_cast<float>(viewport_height_));

	BindTexture(TopLayer().texture);
	DrawQuad(uv_offset, Rml::Vector2f(u_scale, v_scale));

	glBindFramebuffer(GL_FRAMEBUFFER, static_cast<GLuint>(previous_framebuffer));
	glViewport(previous_viewport[0], previous_viewport[1], previous_viewport[2], previous_viewport[3]);
	glClearColor(previous_clear_color[0], previous_clear_color[1], previous_clear_color[2], previous_clear_color[3]);

	if (scissor_was_enabled)
		glEnable(GL_SCISSOR_TEST);
	else
		glDisable(GL_SCISSOR_TEST);

	if (stencil_was_enabled)
		glEnable(GL_STENCIL_TEST);
	else
		glDisable(GL_STENCIL_TEST);

	if (blend_was_enabled)
		glEnable(GL_BLEND);
	else
		glDisable(GL_BLEND);

	// Se conserva la textura y se libera sólo el framebuffer temporal.
	DestroyLayerTarget(target, false);
	target.framebuffer = 0;

	return static_cast<Rml::TextureHandle>(target.texture);
}

Rml::CompiledFilterHandle FxRenderInterface::SaveLayerAsMaskImage()
{
	if (!layers_supported_ || layer_count_ <= 0)
		return 0;

	LayerTarget& mask = EnsurePostprocess(3);
	if (!mask.Valid())
		return 0;

	RenderFullscreenLayer(TopLayer(), mask, false);
	glBindFramebuffer(GL_FRAMEBUFFER, TopLayer().framebuffer);

	CompiledFilter* filter = new CompiledFilter();
	filter->type = FilterType::MaskImage;
	return reinterpret_cast<Rml::CompiledFilterHandle>(filter);
}

Rml::CompiledFilterHandle FxRenderInterface::CompileFilter(const Rml::String& name, const Rml::Dictionary& parameters)
{
	if (!layers_supported_)
		return 0;

	CompiledFilter filter;

	if (name == "opacity")
	{
		filter.type = FilterType::Passthrough;
		filter.blend_factor = Rml::Get(parameters, "value", 1.0f);
	}
	else if (name == "blur")
	{
		filter.type = FilterType::Blur;
		filter.sigma = Rml::Get(parameters, "sigma", 1.0f);
	}
	else if (name == "drop-shadow")
	{
		filter.type = FilterType::DropShadow;
		filter.sigma = Rml::Get(parameters, "sigma", 0.f);
		filter.color = Rml::Get(parameters, "color", Rml::Colourb()).ToPremultiplied();
		filter.offset = Rml::Get(parameters, "offset", Rml::Vector2f(0.f));
	}
	else if (name == "brightness")
	{
		filter.type = FilterType::ColorMatrix;
		const float value = Rml::Get(parameters, "value", 1.0f);
		filter.color_matrix = Rml::Matrix4f::Diag(value, value, value, 1.f);
	}
	else if (name == "contrast")
	{
		filter.type = FilterType::ColorMatrix;
		const float value = Rml::Get(parameters, "value", 1.0f);
		const float grayness = 0.5f - 0.5f * value;
		filter.color_matrix = Rml::Matrix4f::Diag(value, value, value, 1.f);
		filter.color_matrix.SetColumn(3, Rml::Vector4f(grayness, grayness, grayness, 1.f));
	}
	else if (name == "invert")
	{
		filter.type = FilterType::ColorMatrix;
		const float value = Rml::Math::Clamp(Rml::Get(parameters, "value", 1.0f), 0.f, 1.f);
		const float inverted = 1.f - 2.f * value;
		filter.color_matrix = Rml::Matrix4f::Diag(inverted, inverted, inverted, 1.f);
		filter.color_matrix.SetColumn(3, Rml::Vector4f(value, value, value, 1.f));
	}
	else if (name == "grayscale")
	{
		filter.type = FilterType::ColorMatrix;
		const float value = Rml::Get(parameters, "value", 1.0f);
		const float rev_value = 1.f - value;
		const Rml::Vector3f gray = value * Rml::Vector3f(0.2126f, 0.7152f, 0.0722f);
		filter.color_matrix = Rml::Matrix4f::FromRows({gray.x + rev_value, gray.y, gray.z, 0.f}, {gray.x, gray.y + rev_value, gray.z, 0.f},
			{gray.x, gray.y, gray.z + rev_value, 0.f}, {0.f, 0.f, 0.f, 1.f});
	}
	else if (name == "sepia")
	{
		filter.type = FilterType::ColorMatrix;
		const float value = Rml::Get(parameters, "value", 1.0f);
		const float rev_value = 1.f - value;
		const Rml::Vector3f r_mix = value * Rml::Vector3f(0.393f, 0.769f, 0.189f);
		const Rml::Vector3f g_mix = value * Rml::Vector3f(0.349f, 0.686f, 0.168f);
		const Rml::Vector3f b_mix = value * Rml::Vector3f(0.272f, 0.534f, 0.131f);
		filter.color_matrix = Rml::Matrix4f::FromRows({r_mix.x + rev_value, r_mix.y, r_mix.z, 0.f}, {g_mix.x, g_mix.y + rev_value, g_mix.z, 0.f},
			{b_mix.x, b_mix.y, b_mix.z + rev_value, 0.f}, {0.f, 0.f, 0.f, 1.f});
	}
	else if (name == "hue-rotate")
	{
		filter.type = FilterType::ColorMatrix;
		const float value = Rml::Get(parameters, "value", 1.0f);
		const float s = Rml::Math::Sin(value);
		const float c = Rml::Math::Cos(value);
		filter.color_matrix = Rml::Matrix4f::FromRows(
			{0.213f + 0.787f * c - 0.213f * s, 0.715f - 0.715f * c - 0.715f * s, 0.072f - 0.072f * c + 0.928f * s, 0.f},
			{0.213f - 0.213f * c + 0.143f * s, 0.715f + 0.285f * c + 0.140f * s, 0.072f - 0.072f * c - 0.283f * s, 0.f},
			{0.213f - 0.213f * c - 0.787f * s, 0.715f - 0.715f * c + 0.715f * s, 0.072f + 0.928f * c + 0.072f * s, 0.f},
			{0.f, 0.f, 0.f, 1.f});
	}
	else if (name == "saturate")
	{
		filter.type = FilterType::ColorMatrix;
		const float value = Rml::Get(parameters, "value", 1.0f);
		filter.color_matrix = Rml::Matrix4f::FromRows(
			{0.213f + 0.787f * value, 0.715f - 0.715f * value, 0.072f - 0.072f * value, 0.f},
			{0.213f - 0.213f * value, 0.715f + 0.285f * value, 0.072f - 0.072f * value, 0.f},
			{0.213f - 0.213f * value, 0.715f - 0.715f * value, 0.072f + 0.928f * value, 0.f},
			{0.f, 0.f, 0.f, 1.f});
	}

	if (filter.type == FilterType::Invalid)
	{
		Rml::Log::Message(Rml::Log::LT_WARNING, "fxrmlui: filtro no soportado '%s'", name.c_str());
		return 0;
	}

	return reinterpret_cast<Rml::CompiledFilterHandle>(new CompiledFilter(std::move(filter)));
}

void FxRenderInterface::ReleaseFilter(Rml::CompiledFilterHandle filter)
{
	delete reinterpret_cast<CompiledFilter*>(filter);
}

void FxRenderInterface::RenderFilters(Rml::Span<const Rml::CompiledFilterHandle> filters)
{
	for (const Rml::CompiledFilterHandle handle : filters)
	{
		const CompiledFilter& filter = *reinterpret_cast<const CompiledFilter*>(handle);

		switch (filter.type)
		{
		case FilterType::Passthrough:
		{
			LayerTarget& primary = EnsurePostprocess(0);
			LayerTarget& secondary = EnsurePostprocess(1);

			BindTexture(primary.texture);
			BindFramebufferForOverwrite(secondary.framebuffer);
			glViewport(0, 0, viewport_width_, viewport_height_);
			glEnable(GL_BLEND);
			glBlendFunc(GL_CONSTANT_COLOR, GL_ZERO);
			glBlendColor(filter.blend_factor, filter.blend_factor, filter.blend_factor, filter.blend_factor);

			DrawFullscreenQuad();

			glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
			SwapPostprocessPrimarySecondary();
		}
		break;

		case FilterType::Blur:
		{
			glDisable(GL_BLEND);
			LayerTarget& primary = EnsurePostprocess(0);
			LayerTarget& secondary = EnsurePostprocess(1);
			RenderBlur(filter.sigma, primary, secondary);
			glEnable(GL_BLEND);
		}
		break;

		case FilterType::DropShadow:
		{
			LayerTarget& primary = EnsurePostprocess(0);
			LayerTarget& secondary = EnsurePostprocess(1);

			glDisable(GL_BLEND);

			// Sombra en el secundario.
			UseProgram(ProgramId::DropShadow);
			const Program& drop_program = programs_[static_cast<size_t>(ProgramId::DropShadow)];
			constexpr bool transpose = std::is_same_v<Rml::Matrix4f, Rml::RowMajorMatrix4f>;
			static const Rml::Matrix4f identity = Rml::Matrix4f::Identity();
			if (drop_program.transform >= 0)
				glUniformMatrix4fv(drop_program.transform, 1, transpose ? GL_TRUE : GL_FALSE, identity.data());
			if (drop_program.translate >= 0)
				glUniform2f(drop_program.translate, 0.f, 0.f);
			if (drop_program.tex >= 0)
				glUniform1i(drop_program.tex, 0);

			const Rml::Colourf color = ConvertToColorf(filter.color);
			if (drop_program.color >= 0)
				glUniform4fv(drop_program.color, 1, &color[0]);

			SetScissorRegion(scissor_region_);

			const Rml::Rectanglei flipped = scissor_region_.Valid() ? VerticallyFlipped(scissor_region_, viewport_height_) : Rml::Rectanglei::MakeInvalid();
			const Rml::Vector2f viewport_size(static_cast<float>(viewport_width_), static_cast<float>(viewport_height_));
			const Rml::Vector2f uv_min = (Rml::Vector2f(flipped.p0) + Rml::Vector2f(0.5f)) / viewport_size;
			const Rml::Vector2f uv_max = (Rml::Vector2f(flipped.p1) - Rml::Vector2f(0.5f)) / viewport_size;
			const int min_location = glGetUniformLocation(drop_program.id, "_texCoordMin");
			const int max_location = glGetUniformLocation(drop_program.id, "_texCoordMax");
			if (min_location >= 0)
				glUniform2f(min_location, uv_min.x, uv_min.y);
			if (max_location >= 0)
				glUniform2f(max_location, uv_max.x, uv_max.y);

			BindTexture(primary.texture);
			BindFramebufferForOverwrite(secondary.framebuffer);
			glViewport(0, 0, viewport_width_, viewport_height_);

			const Rml::Vector2f uv_offset = filter.offset / Rml::Vector2f(-static_cast<float>(viewport_width_), static_cast<float>(viewport_height_));
			DrawQuad(uv_offset, Rml::Vector2f(1.f, 1.f), ProgramId::DropShadow);

			if (filter.sigma >= 0.5f)
			{
				LayerTarget& tertiary = EnsurePostprocess(2);
				RenderBlur(filter.sigma, secondary, tertiary);
			}

			// Componer el original sobre la sombra.
			UseProgram(ProgramId::Passthrough);
			const Program& passthrough = programs_[static_cast<size_t>(ProgramId::Passthrough)];
			if (passthrough.transform >= 0)
				glUniformMatrix4fv(passthrough.transform, 1, transpose ? GL_TRUE : GL_FALSE, identity.data());
			if (passthrough.translate >= 0)
				glUniform2f(passthrough.translate, 0.f, 0.f);

			BindTexture(primary.texture);
			glBindFramebuffer(GL_FRAMEBUFFER, secondary.framebuffer);
			glEnable(GL_BLEND);
			glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
			DrawFullscreenQuad();

			SwapPostprocessPrimarySecondary();
		}
		break;

		case FilterType::ColorMatrix:
		{
			LayerTarget& primary = EnsurePostprocess(0);
			LayerTarget& secondary = EnsurePostprocess(1);

			UseProgram(ProgramId::ColorMatrix);
			const Program& program = programs_[static_cast<size_t>(ProgramId::ColorMatrix)];
			constexpr bool transpose = std::is_same_v<Rml::Matrix4f, Rml::RowMajorMatrix4f>;
			static const Rml::Matrix4f identity = Rml::Matrix4f::Identity();
			if (program.transform >= 0)
				glUniformMatrix4fv(program.transform, 1, transpose ? GL_TRUE : GL_FALSE, identity.data());
			if (program.translate >= 0)
				glUniform2f(program.translate, 0.f, 0.f);
			if (program.tex >= 0)
				glUniform1i(program.tex, 0);
			if (program.color_matrix >= 0)
				glUniformMatrix4fv(program.color_matrix, 1, transpose ? GL_TRUE : GL_FALSE, filter.color_matrix.data());

			glDisable(GL_BLEND);
			BindTexture(primary.texture);
			BindFramebufferForOverwrite(secondary.framebuffer);
			glViewport(0, 0, viewport_width_, viewport_height_);

			DrawFullscreenQuad(ProgramId::ColorMatrix);

			glEnable(GL_BLEND);
			SwapPostprocessPrimarySecondary();
		}
		break;

		case FilterType::MaskImage:
		{
			LayerTarget& primary = EnsurePostprocess(0);
			LayerTarget& mask = EnsurePostprocess(3);
			LayerTarget& secondary = EnsurePostprocess(1);

			UseProgram(ProgramId::BlendMask);
			const Program& program = programs_[static_cast<size_t>(ProgramId::BlendMask)];
			constexpr bool transpose = std::is_same_v<Rml::Matrix4f, Rml::RowMajorMatrix4f>;
			static const Rml::Matrix4f identity = Rml::Matrix4f::Identity();
			if (program.transform >= 0)
				glUniformMatrix4fv(program.transform, 1, transpose ? GL_TRUE : GL_FALSE, identity.data());
			if (program.translate >= 0)
				glUniform2f(program.translate, 0.f, 0.f);
			if (program.tex >= 0)
				glUniform1i(program.tex, 0);
			if (program.tex_mask >= 0)
				glUniform1i(program.tex_mask, 1);

			glDisable(GL_BLEND);

			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, primary.texture);
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, mask.texture);
			glActiveTexture(GL_TEXTURE0);

			BindFramebufferForOverwrite(secondary.framebuffer);
			glViewport(0, 0, viewport_width_, viewport_height_);

			DrawFullscreenQuad(ProgramId::BlendMask);

			glEnable(GL_BLEND);
			SwapPostprocessPrimarySecondary();
		}
		break;

		case FilterType::Invalid:
			break;
		}
	}

	glBindFramebuffer(GL_FRAMEBUFFER, TopLayer().framebuffer);
	glViewport(0, 0, viewport_width_, viewport_height_);
}

namespace {
// Sigma objetivo por pasada una vez reducida la resolucion: un kernel de 7 muestras (+-3 texels)
// solo representa bien una gaussiana con sigma <= ~2; con mas se trunca y el blur se queda corto.
constexpr float kBlurTargetSigma = 2.f;
constexpr int kBlurMaxDownscale = 32;
constexpr float kBlurSlowFrameMs = 24.f;
constexpr float kBlurFastFrameMs = 18.5f;
} // namespace

int FxRenderInterface::ChooseBlurDownscale(float sigma) const
{
#if defined(__ANDROID__)
	// Interruptor de depuracion: adb shell setprop debug.fxrml.blurscale 4 (1 = sin reduccion)
	{
		char value[PROP_VALUE_MAX] = {0};
		if (__system_property_get("debug.fxrml.blurscale", value) > 0)
		{
			const int forced = std::atoi(value);
			if (forced >= 1)
				return std::min(forced, kBlurMaxDownscale);
		}
	}
#endif

	// Cuanto mayor es el sigma, mas se puede reducir la resolucion sin que se note.
	int factor = 1;
	while (sigma / static_cast<float>(factor) > kBlurTargetSigma && factor < kBlurMaxDownscale)
		factor *= 2;

	// El regulador anade reduccion cuando el dispositivo no llega a los fps objetivo.
	factor = std::min(factor << blur_extra_levels_, kBlurMaxDownscale);

	// No reducir tanto que el sigma resultante pierda sentido.
	while (factor > 1 && sigma / static_cast<float>(factor) < 1.f)
		factor /= 2;

	return factor;
}

void FxRenderInterface::UpdateBlurGovernor()
{
	const auto now = std::chrono::steady_clock::now();
	if (has_last_frame_time_ && blur_used_last_frame_)
	{
		const float frame_ms = std::chrono::duration<float, std::milli>(now - last_frame_time_).count();
		blur_frame_ms_ += (std::min(frame_ms, 250.f) - blur_frame_ms_) * 0.15f;

		if (blur_cooldown_ > 0)
			--blur_cooldown_;

		if (blur_frame_ms_ > kBlurSlowFrameMs && blur_extra_levels_ < 2 && blur_cooldown_ == 0)
		{
			// Si vuelve a ir lento poco despues de recuperar, se espera mas antes de reintentar.
			if (blur_fast_frames_ > 0 && blur_fast_frames_ < blur_recover_wait_ * 2)
				blur_recover_wait_ = std::min(blur_recover_wait_ * 2, 7200);
			++blur_extra_levels_;
			blur_fast_frames_ = 0;
			blur_cooldown_ = 20;
		}
		else if (blur_frame_ms_ < kBlurFastFrameMs && blur_extra_levels_ > 0)
		{
			if (++blur_fast_frames_ >= blur_recover_wait_)
			{
				--blur_extra_levels_;
				blur_fast_frames_ = 0;
				blur_cooldown_ = 20;
			}
		}
		else if (blur_frame_ms_ >= kBlurFastFrameMs)
		{
			blur_fast_frames_ = 0;
		}
	}

	last_frame_time_ = now;
	has_last_frame_time_ = true;
	blur_used_last_frame_ = blur_used_this_frame_;
	blur_used_this_frame_ = false;
}

FxRenderInterface::LayerTarget& FxRenderInterface::EnsureLowResTarget(LayerTarget& target, int width, int height)
{
	if (!target.Valid() || target.width != width || target.height != height)
	{
		DestroyLayerTarget(target);
		CreateLayerTarget(target, width, height, false);
	}
	return target;
}

void FxRenderInterface::RenderBlur(float sigma, LayerTarget& source_destination, LayerTarget& temp)
{
	if (!source_destination.Valid() || !temp.Valid())
		return;

	blur_used_this_frame_ = true;

	// Limites de textura para que las muestras fuera del elemento no aporten.
	const Rml::Rectanglei flipped = scissor_region_.Valid() ? VerticallyFlipped(scissor_region_, viewport_height_) : Rml::Rectanglei::MakeInvalid();
	const Rml::Vector2f viewport_size(static_cast<float>(viewport_width_), static_cast<float>(viewport_height_));
	const Rml::Vector2f uv_min = (Rml::Vector2f(flipped.p0) + Rml::Vector2f(0.5f)) / viewport_size;
	const Rml::Vector2f uv_max = (Rml::Vector2f(flipped.p1) - Rml::Vector2f(0.5f)) / viewport_size;

	const int factor = ChooseBlurDownscale(sigma);
	if (factor <= 1)
	{
		RenderBlurPasses(sigma, source_destination, temp, viewport_width_, viewport_height_, uv_min, uv_max);
		return;
	}

	// Reduccion en pocas pasadas: cada paso promedia bloques de hasta 8x8 texels (16 muestras
	// bilineales). Factor 8 = 1 pasada, 16 = 2 (8 y 2), 32 = 2 (8 y 4).
	int step_factor[4] = {0, 0, 0, 0};
	int num_steps = 0;
	for (int remaining = factor; remaining > 1 && num_steps < 4;)
	{
		const int step = std::min(remaining, 8);
		step_factor[num_steps++] = step;
		remaining /= step;
	}

	int level_width[5] = {viewport_width_, 0, 0, 0, 0};
	int level_height[5] = {viewport_height_, 0, 0, 0, 0};
	bool targets_ok = true;
	for (int i = 1; i <= num_steps && targets_ok; ++i)
	{
		const int divisor = step_factor[i - 1];
		level_width[i] = std::max((level_width[i - 1] + divisor - 1) / divisor, 1);
		level_height[i] = std::max((level_height[i - 1] + divisor - 1) / divisor, 1);
		targets_ok = EnsureLowResTarget(blur_pyramid_[i], level_width[i], level_height[i]).Valid();
	}
	if (targets_ok)
		targets_ok = EnsureLowResTarget(blur_temp_low_, level_width[num_steps], level_height[num_steps]).Valid();
	if (!targets_ok)
	{
		RenderBlurPasses(sigma, source_destination, temp, viewport_width_, viewport_height_, uv_min, uv_max);
		return;
	}
	LayerTarget& lowest = blur_pyramid_[num_steps];

	const bool scissor_was_enabled = glIsEnabled(GL_SCISSOR_TEST) != GL_FALSE;
	glDisable(GL_SCISSOR_TEST);
	glDisable(GL_BLEND);

	const Program& down = programs_[static_cast<size_t>(ProgramId::Downsample)];
	const int down_min_location = glGetUniformLocation(down.id, "_texCoordMin");
	const int down_max_location = glGetUniformLocation(down.id, "_texCoordMax");

	const LayerTarget* from = &source_destination;
	for (int i = 1; i <= num_steps; ++i)
	{
		// DrawQuad fija transformacion, traslacion y textura; aqui solo van los parametros propios.
		UseProgram(ProgramId::Downsample);
		if (down.taps >= 0)
			glUniform1f(down.taps, static_cast<float>(step_factor[i - 1]) * 0.5f);
		if (down.texel_offset >= 0)
			glUniform2f(down.texel_offset, 1.f / static_cast<float>(level_width[i - 1]), 1.f / static_cast<float>(level_height[i - 1]));
		if (down_min_location >= 0)
			glUniform2f(down_min_location, uv_min.x, uv_min.y);
		if (down_max_location >= 0)
			glUniform2f(down_max_location, uv_max.x, uv_max.y);

		BindFramebufferForOverwrite(blur_pyramid_[i].framebuffer);
		glViewport(0, 0, level_width[i], level_height[i]);
		BindTexture(from->texture);
		DrawFullscreenQuad(ProgramId::Downsample);
		from = &blur_pyramid_[i];
	}

	RenderBlurPasses(sigma / static_cast<float>(factor), lowest, blur_temp_low_, level_width[num_steps], level_height[num_steps], uv_min, uv_max);

	// Volver a la resolucion completa dentro del scissor del elemento con el programa Blur y pesos
	// {1,0,0,0} (copia bilineal respetando la region). RenderBlurPasses deja el blend activo: hay que
	// sustituir el contenido original, no mezclarlo con el.
	glDisable(GL_BLEND);
	UseProgram(ProgramId::Blur);
	const Program& program = programs_[static_cast<size_t>(ProgramId::Blur)];
	constexpr bool transpose = std::is_same_v<Rml::Matrix4f, Rml::RowMajorMatrix4f>;
	static const Rml::Matrix4f identity = Rml::Matrix4f::Identity();
	if (program.transform >= 0)
		glUniformMatrix4fv(program.transform, 1, transpose ? GL_TRUE : GL_FALSE, identity.data());
	if (program.translate >= 0)
		glUniform2f(program.translate, 0.f, 0.f);
	if (program.tex >= 0)
		glUniform1i(program.tex, 0);
	const float copy_weights[kBlurNumWeights] = {1.f, 0.f, 0.f, 0.f};
	if (program.weights >= 0)
		glUniform1fv(program.weights, kBlurNumWeights, copy_weights);
	if (program.texel_offset >= 0)
		glUniform2f(program.texel_offset, 0.f, 0.f);
	const int min_location = glGetUniformLocation(program.id, "_texCoordMin");
	const int max_location = glGetUniformLocation(program.id, "_texCoordMax");
	if (min_location >= 0)
		glUniform2f(min_location, uv_min.x, uv_min.y);
	if (max_location >= 0)
		glUniform2f(max_location, uv_max.x, uv_max.y);

	if (scissor_was_enabled)
		glEnable(GL_SCISSOR_TEST);
	BindFramebufferForOverwrite(source_destination.framebuffer);
	glViewport(0, 0, viewport_width_, viewport_height_);
	BindTexture(lowest.texture);
	DrawFullscreenQuad(ProgramId::Blur);

	glEnable(GL_BLEND);
}

void FxRenderInterface::RenderBlurPasses(float sigma, LayerTarget& source_destination, LayerTarget& temp, int width, int height, Rml::Vector2f uv_min,
	Rml::Vector2f uv_max)
{
	if (!source_destination.Valid() || !temp.Valid())
		return;

	// Se reparte el desenfoque en pasadas de sigma <= 2 para mantener el kernel
	// de 7 muestras. La composición gaussiana es sqrt(s1² + s2²).
	int passes = static_cast<int>(std::ceil(sigma / kBlurTargetSigma));
	passes = std::max(1, std::min(passes, 6));
	const float pass_sigma = sigma / std::sqrt(static_cast<float>(passes));

	float weights[kBlurNumWeights];
	float normalization = 0.f;
	for (int i = 0; i < kBlurNumWeights; i++)
	{
		weights[i] = std::exp(-static_cast<float>(i * i) / (2.f * pass_sigma * pass_sigma)) /
			(std::sqrt(2.f * 3.14159265f) * pass_sigma);
		normalization += (i == 0 ? 1.f : 2.f) * weights[i];
	}
	for (float& weight : weights)
		weight /= normalization;

	UseProgram(ProgramId::Blur);
	const Program& program = programs_[static_cast<size_t>(ProgramId::Blur)];
	constexpr bool transpose = std::is_same_v<Rml::Matrix4f, Rml::RowMajorMatrix4f>;
	static const Rml::Matrix4f identity = Rml::Matrix4f::Identity();
	if (program.transform >= 0)
		glUniformMatrix4fv(program.transform, 1, transpose ? GL_TRUE : GL_FALSE, identity.data());
	if (program.translate >= 0)
		glUniform2f(program.translate, 0.f, 0.f);
	if (program.tex >= 0)
		glUniform1i(program.tex, 0);
	if (program.weights >= 0)
		glUniform1fv(program.weights, kBlurNumWeights, weights);

	const int min_location = glGetUniformLocation(program.id, "_texCoordMin");
	const int max_location = glGetUniformLocation(program.id, "_texCoordMax");
	if (min_location >= 0)
		glUniform2f(min_location, uv_min.x, uv_min.y);
	if (max_location >= 0)
		glUniform2f(max_location, uv_max.x, uv_max.y);

	glDisable(GL_BLEND);

	for (int pass = 0; pass < passes; pass++)
	{
		// Pasada horizontal: source -> temp.
		BindFramebufferForOverwrite(temp.framebuffer);
		glViewport(0, 0, width, height);
		BindTexture(source_destination.texture);
		if (program.texel_offset >= 0)
			glUniform2f(program.texel_offset, 1.f / static_cast<float>(width), 0.f);
		DrawFullscreenQuad(ProgramId::Blur);

		// Pasada vertical: temp -> source.
		BindFramebufferForOverwrite(source_destination.framebuffer);
		glViewport(0, 0, width, height);
		BindTexture(temp.texture);
		if (program.texel_offset >= 0)
			glUniform2f(program.texel_offset, 0.f, 1.f / static_cast<float>(height));
		DrawFullscreenQuad(ProgramId::Blur);
	}

	glEnable(GL_BLEND);
}

// ---------------------------------------------------------------------------
//  Shaders (gradientes)
// ---------------------------------------------------------------------------

Rml::CompiledShaderHandle FxRenderInterface::CompileShader(const Rml::String& name, const Rml::Dictionary& parameters)
{
	if (!programs_[static_cast<size_t>(ProgramId::Gradient)].id)
		return 0;

	CompiledShader shader;

	auto apply_color_stops = [&shader](const Rml::Dictionary& shader_parameters) {
		auto it = shader_parameters.find("color_stop_list");
		if (it == shader_parameters.end() || it->second.GetType() != Rml::Variant::COLORSTOPLIST)
			return;

		const Rml::ColorStopList& color_stop_list = it->second.GetReference<Rml::ColorStopList>();
		const int num_stops = Rml::Math::Min(static_cast<int>(color_stop_list.size()), kMaxGradientStops);

		shader.stop_positions.resize(static_cast<size_t>(num_stops));
		shader.stop_colors.resize(static_cast<size_t>(num_stops));
		for (int i = 0; i < num_stops; i++)
		{
			const Rml::ColorStop& stop = color_stop_list[static_cast<size_t>(i)];
			if (stop.position.unit != Rml::Unit::NUMBER)
				continue;
			shader.stop_positions[static_cast<size_t>(i)] = stop.position.number;
			shader.stop_colors[static_cast<size_t>(i)] = ConvertToColorf(stop.color);
		}
	};

	if (name == "linear-gradient")
	{
		const bool repeating = Rml::Get(parameters, "repeating", false);
		shader.gradient_function = repeating ? ShaderGradientFunction::RepeatingLinear : ShaderGradientFunction::Linear;
		shader.p = Rml::Get(parameters, "p0", Rml::Vector2f(0.f));
		shader.v = Rml::Get(parameters, "p1", Rml::Vector2f(0.f)) - shader.p;
		apply_color_stops(parameters);
	}
	else if (name == "radial-gradient")
	{
		const bool repeating = Rml::Get(parameters, "repeating", false);
		shader.gradient_function = repeating ? ShaderGradientFunction::RepeatingRadial : ShaderGradientFunction::Radial;
		shader.p = Rml::Get(parameters, "center", Rml::Vector2f(0.f));
		shader.v = Rml::Vector2f(1.f) / Rml::Get(parameters, "radius", Rml::Vector2f(1.f));
		apply_color_stops(parameters);
	}
	else if (name == "conic-gradient")
	{
		const bool repeating = Rml::Get(parameters, "repeating", false);
		shader.gradient_function = repeating ? ShaderGradientFunction::RepeatingConic : ShaderGradientFunction::Conic;
		shader.p = Rml::Get(parameters, "center", Rml::Vector2f(0.f));
		const float angle = Rml::Get(parameters, "angle", 0.f);
		shader.v = Rml::Vector2f(Rml::Math::Cos(angle), Rml::Math::Sin(angle));
		apply_color_stops(parameters);
	}
	else
	{
		Rml::Log::Message(Rml::Log::LT_WARNING, "fxrmlui: shader no soportado '%s'", name.c_str());
		return 0;
	}

	if (shader.stop_positions.empty())
		return 0;

	// Misma formula que usaba el shader: mix + smoothstep entre paradas consecutivas.
	constexpr int kLutSize = 256;
	const size_t num_stops = shader.stop_positions.size();
	const float first = shader.stop_positions.front();
	const float length = std::max(shader.stop_positions.back() - first, 1e-6f);
	shader.range_start = first;
	shader.range_inv_length = 1.f / length;

	std::vector<unsigned char> lut(static_cast<size_t>(kLutSize) * 4);
	for (int texel = 0; texel < kLutSize; ++texel)
	{
		const float t = first + (static_cast<float>(texel) + 0.5f) / static_cast<float>(kLutSize) * length;

		Rml::Colourf color = shader.stop_colors[0];
		for (size_t k = 1; k < num_stops; ++k)
		{
			const float e0 = shader.stop_positions[k - 1];
			const float e1 = shader.stop_positions[k];
			float weight = t >= e1 ? 1.f : 0.f;
			if (e1 > e0)
			{
				const float x = std::min(std::max((t - e0) / (e1 - e0), 0.f), 1.f);
				weight = x * x * (3.f - 2.f * x);
			}
			for (int c = 0; c < 4; ++c)
				color[c] += (shader.stop_colors[k][c] - color[c]) * weight;
		}

		for (int c = 0; c < 4; ++c)
			lut[static_cast<size_t>(texel) * 4 + static_cast<size_t>(c)] =
				static_cast<unsigned char>(std::min(std::max(color[c], 0.f), 1.f) * 255.f + 0.5f);
	}

	glGenTextures(1, &shader.lut_texture);
	if (!shader.lut_texture)
		return 0;
	BindTexture(shader.lut_texture);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, kLutSize, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, lut.data());
	glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

	return reinterpret_cast<Rml::CompiledShaderHandle>(new CompiledShader(std::move(shader)));
}

void FxRenderInterface::RenderShader(Rml::CompiledShaderHandle shader_handle, Rml::CompiledGeometryHandle geometry_handle,
	Rml::Vector2f translation, Rml::TextureHandle /*texture*/)
{
	if (!shader_handle || !geometry_handle)
		return;

	const CompiledShader& shader = *reinterpret_cast<const CompiledShader*>(shader_handle);
	const Geometry& geometry = *reinterpret_cast<const Geometry*>(geometry_handle);

	UseProgram(ProgramId::Gradient);
	const Program& program = programs_[static_cast<size_t>(ProgramId::Gradient)];
	SubmitTransform(program, translation);

	if (program.func >= 0)
		glUniform1i(program.func, static_cast<int>(shader.gradient_function));
	if (program.p >= 0)
		glUniform2f(program.p, shader.p.x, shader.p.y);
	if (program.v >= 0)
		glUniform2f(program.v, shader.v.x, shader.v.y);
	if (program.range >= 0)
		glUniform2f(program.range, shader.range_start, shader.range_inv_length);
	if (program.tex >= 0)
		glUniform1i(program.tex, 0);
	BindTexture(shader.lut_texture);

	DrawGeometryBuffers(geometry);
}

void FxRenderInterface::ReleaseShader(Rml::CompiledShaderHandle shader_handle)
{
	CompiledShader* shader = reinterpret_cast<CompiledShader*>(shader_handle);
	if (shader && shader->lut_texture)
		glDeleteTextures(1, &shader->lut_texture);
	delete shader;
}
