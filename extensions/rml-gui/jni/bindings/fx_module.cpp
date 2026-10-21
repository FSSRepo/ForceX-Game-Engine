#include "fx_file_interface.h"
#include "fx_render_interface.h"
#include "fx_system_interface.h"

#include <RmlUi/Core.h>

#if defined(_WIN32)
	#define FX_RMLUI_EXPORT __declspec(dllexport)
#else
	#define FX_RMLUI_EXPORT __attribute__((visibility("default")))
#endif

// Punto de enlace mínimo de la librería nativa: fuerza la inclusión de RmlUi y
// permite comprobar que fxrmlui enlaza correctamente con RmlUi + FreeType.
extern "C" FX_RMLUI_EXPORT const char* fxrmlui_version(void)
{
	static const Rml::String version = Rml::GetVersion();
	return version.c_str();
}

namespace {
Rml::String EscapeAttribute(const char* value)
{
	Rml::String escaped;
	for (const char* c = value; c && *c; ++c)
	{
		if (*c == '&')
			escaped += "&amp;";
		else if (*c == '"')
			escaped += "&quot;";
		else if (*c == '<')
			escaped += "&lt;";
		else if (*c == '>')
			escaped += "&gt;";
		else
			escaped += *c;
	}
	return escaped;
}
} // namespace

/**
    Prueba de humo de las interfaces nativas (fase 5).

    Carga un documento RML embebido con un SVG inline y, opcionalmente, una
    animación Lottie, y lo actualiza y dibuja durante unos frames. Debe
    invocarse en el hilo con el contexto OpenGL/GLES activo, porque SVG, Lottie
    y los glifos crean texturas mediante el RenderInterface.

    @param width, height  Dimensiones del viewport en píxeles.
    @param lottie_path    Ruta a un .json de Bodymovin; puede ser nullptr.
    @return 0 si el documento se cargó y dibujó sin errores.
*/
extern "C" FX_RMLUI_EXPORT int fxrmlui_selftest(int width, int height, const char* lottie_path)
{
	if (width <= 0 || height <= 0)
		return -1;

	FxSystemInterface system_interface;
	FxFileInterface file_interface;
	FxRenderInterface render_interface;

	Rml::SetSystemInterface(&system_interface);
	Rml::SetFileInterface(&file_interface);
	Rml::SetRenderInterface(&render_interface);

	if (!Rml::Initialise())
		return -2;

	render_interface.SetViewport(width, height);

	Rml::Context* context = Rml::CreateContext("fxrmlui_selftest", Rml::Vector2i(width, height));
	if (!context)
	{
		Rml::Shutdown();
		return -3;
	}

	Rml::String document_rml = R"(
<rml>
	<head>
		<style>
			body { width: 100%; height: 100%; }
			svg { display: block; width: 64px; height: 64px; }
			lottie { display: block; width: 64px; height: 64px; }
		</style>
	</head>
	<body>
		<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64">
			<circle cx="32" cy="32" r="28" fill="#e33" stroke="#222" stroke-width="4" />
		</svg>
)";

	if (lottie_path && *lottie_path)
	{
		document_rml += "<lottie src=\"";
		document_rml += EscapeAttribute(lottie_path);
		document_rml += "\" />";
	}

	document_rml += R"(
	</body>
</rml>
)";

	Rml::ElementDocument* document = context->LoadDocumentFromMemory(document_rml, "fxrmlui_selftest.rml");
	if (!document)
	{
		Rml::Shutdown();
		return -4;
	}

	document->Show();

	int result = 0;
	for (int frame = 0; frame < 4; ++frame)
	{
		context->Update();

		if (!render_interface.BeginFrame())
		{
			result = -5;
			break;
		}

		context->Render();
		render_interface.EndFrame();
	}

	Rml::Shutdown();
	return result;
}
