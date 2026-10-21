#pragma once

#include <RmlUi/Core/Matrix4.h>
#include <RmlUi/Core/RenderInterface.h>

#include <chrono>
#include <memory>
#include <vector>

/**
    Renderer de RmlUi sobre el contexto OpenGL/GLES ya activo de ForceX.

    - Pipeline con shaders (GLSL 130 en desktop, GLSL ES 100 en Android) y VBOs,
      compatible con el contexto OpenGL 3.0 core que crea el backend de escritorio.
    - No crea ventana ni contexto: dibuja en el framebuffer actual y restaura el
      estado GL que modifica (viewport, scissor, blend, depth, cull, stencil,
      framebuffer, programa y buffers).
    - Soporta gradientes (`decorator: *-gradient`), capas con FBO y filtros
      (opacity, blur, drop-shadow, color-matrix y mask-image) sin usar
      `glBlitFramebuffer`, que no existe en GLES 2.0.
    - GenerateTexture cubre las texturas de callback que usan los plugins SVG y
      Lottie, además de las imágenes TGA cargadas por RmlUi.

    BeginFrame()/EndFrame() deben invocarse en el hilo propietario del contexto GL.
*/
class FxRenderInterface final : public Rml::RenderInterface
{
public:
	FxRenderInterface();
	~FxRenderInterface() override;

	/// Actualiza el tamaño del viewport con el que se proyecta la UI.
	void SetViewport(int width, int height);

	/// Prepara el estado GL para dibujar RmlUi. Devuelve false si no se pudo inicializar.
	bool BeginFrame();

	/// Compone la capa base sobre el framebuffer del motor y restaura el estado GL.
	void EndFrame();

	// -- Rml::RenderInterface --

	Rml::CompiledGeometryHandle CompileGeometry(Rml::Span<const Rml::Vertex> vertices, Rml::Span<const int> indices) override;
	void RenderGeometry(Rml::CompiledGeometryHandle geometry, Rml::Vector2f translation, Rml::TextureHandle texture) override;
	void ReleaseGeometry(Rml::CompiledGeometryHandle geometry) override;

	Rml::TextureHandle LoadTexture(Rml::Vector2i& texture_dimensions, const Rml::String& source) override;
	Rml::TextureHandle GenerateTexture(Rml::Span<const Rml::byte> source, Rml::Vector2i source_dimensions) override;
	void ReleaseTexture(Rml::TextureHandle texture) override;

	void EnableScissorRegion(bool enable) override;
	void SetScissorRegion(Rml::Rectanglei region) override;

	void EnableClipMask(bool enable) override;
	void RenderToClipMask(Rml::ClipMaskOperation operation, Rml::CompiledGeometryHandle geometry, Rml::Vector2f translation) override;

	void SetTransform(const Rml::Matrix4f* transform) override;

	Rml::LayerHandle PushLayer() override;
	void CompositeLayers(Rml::LayerHandle source, Rml::LayerHandle destination, Rml::BlendMode blend_mode,
		Rml::Span<const Rml::CompiledFilterHandle> filters) override;
	void PopLayer() override;

	Rml::TextureHandle SaveLayerAsTexture() override;
	Rml::CompiledFilterHandle SaveLayerAsMaskImage() override;

	Rml::CompiledFilterHandle CompileFilter(const Rml::String& name, const Rml::Dictionary& parameters) override;
	void ReleaseFilter(Rml::CompiledFilterHandle filter) override;

	Rml::CompiledShaderHandle CompileShader(const Rml::String& name, const Rml::Dictionary& parameters) override;
	void RenderShader(Rml::CompiledShaderHandle shader, Rml::CompiledGeometryHandle geometry, Rml::Vector2f translation,
		Rml::TextureHandle texture) override;
	void ReleaseShader(Rml::CompiledShaderHandle shader) override;

private:
	enum class ProgramId
	{
		None = 0,
		Color,
		Texture,
		Gradient,
		Passthrough,
		ColorMatrix,
		BlendMask,
		Blur,
		DropShadow,
		Downsample,
		Count
	};

	enum class FilterType
	{
		Invalid = 0,
		Passthrough,
		Blur,
		DropShadow,
		ColorMatrix,
		MaskImage,
	};

	enum class ShaderGradientFunction
	{
		Linear = 0,
		Radial = 1,
		Conic = 2,
		RepeatingLinear = 3,
		RepeatingRadial = 4,
		RepeatingConic = 5,
	};

	struct Program
	{
		unsigned int id = 0;
		int translate = -1;
		int transform = -1;
		int tex = -1;
		int tex_mask = -1;
		int color = -1;
		int color_matrix = -1;
		int texel_offset = -1;
		int taps = -1;
		int uv_offset = -1;
		int uv_scale = -1;
		int weights = -1;
		int func = -1;
		int p = -1;
		int v = -1;
		int range = -1;
	};

	struct Geometry
	{
		unsigned int vertex_buffer = 0;
		unsigned int index_buffer = 0;
		int index_count = 0;
		unsigned int index_type = 0x1405; // GL_UNSIGNED_INT
		// Trozos adicionales cuando el mesh excede 65535 vértices en GLES2 sin
		// GL_OES_element_index_uint.
		std::unique_ptr<Geometry> next;
	};

	struct LayerTarget
	{
		int width = 0;
		int height = 0;
		unsigned int framebuffer = 0;
		unsigned int texture = 0;

		bool Valid() const { return framebuffer != 0 && texture != 0; }
	};

	struct CompiledFilter
	{
		FilterType type = FilterType::Invalid;

		// Passthrough
		float blend_factor = 1.0f;

		// Blur / drop-shadow
		float sigma = 0.0f;

		// Drop-shadow
		Rml::Vector2f offset;
		Rml::ColourbPremultiplied color;

		// Color matrix
		Rml::Matrix4f color_matrix = Rml::Matrix4f::Identity();
	};

	struct CompiledShader
	{
		ShaderGradientFunction gradient_function = ShaderGradientFunction::Linear;
		Rml::Vector2f p;
		Rml::Vector2f v;
		Rml::Vector<float> stop_positions;
		Rml::Vector<Rml::Colourf> stop_colors;
		// Degradado precalculado en una textura 1D: el shader solo calcula t y muestrea.
		unsigned int lut_texture = 0;
		float range_start = 0.f;
		float range_inv_length = 1.f;
	};

	bool EnsureInitialized();
	bool CreatePrograms();
	bool CreateFullscreenQuad();
	bool InitLayers();
	bool SupportsLayers() const;
	void DestroyAllResources();
	void DestroyLayers();

	bool CreateLayerTarget(LayerTarget& target, int width, int height, bool with_stencil);
	void DestroyLayerTarget(LayerTarget& target, bool release_texture = true);
	bool CreatePostprocessTarget(LayerTarget& target);
	void BindTexture(unsigned int texture);
	void BindLayer(int layer_index);
	LayerTarget& TopLayer();
	LayerTarget& EnsurePostprocess(int index);

	void UseProgram(ProgramId program);
	int Uniform(ProgramId program, int Program::*member) const;
	void SubmitTransform(const Program& program, Rml::Vector2f translation);
	void DrawFullscreenQuad(ProgramId program = ProgramId::Passthrough);
	void DrawQuad(Rml::Vector2f uv_offset, Rml::Vector2f uv_scale, ProgramId program_id = ProgramId::Passthrough);
	void DrawGeometryBuffers(const Geometry& geometry);
	void DrawGeometryImpl(const Geometry& geometry, Rml::Vector2f translation, Rml::TextureHandle texture, bool use_texture_program);
	void RenderFullscreenLayer(const LayerTarget& source, const LayerTarget& destination, bool blend, bool flip_vertical = false);

	void CopyLayerToPostprocessPrimary(Rml::LayerHandle layer);
	void RenderFilters(Rml::Span<const Rml::CompiledFilterHandle> filters);
	void RenderBlur(float sigma, LayerTarget& source_destination, LayerTarget& temp);
	void RenderBlurPasses(float sigma, LayerTarget& source_destination, LayerTarget& temp, int width, int height, Rml::Vector2f uv_min,
		Rml::Vector2f uv_max);
	int ChooseBlurDownscale(float sigma) const;
	void UpdateBlurGovernor();
	LayerTarget& EnsureLowResTarget(LayerTarget& target, int width, int height);
	void SwapPostprocessPrimarySecondary();

	void SetupScissor(bool enable, Rml::Rectanglei region);

	int viewport_width_ = 0;
	int viewport_height_ = 0;

	bool initialized_ = false;
	bool frame_active_ = false;
	bool layers_supported_ = false;
	bool layers_warning_logged_ = false;

	Program programs_[static_cast<size_t>(ProgramId::Count)];
	ProgramId active_program_ = ProgramId::None;

	unsigned int vertex_array_ = 0;

	Geometry* fullscreen_quad_ = nullptr;

	std::vector<LayerTarget> layers_;
	int layer_count_ = 0;
	LayerTarget postprocess_[4]; // primary, secondary, tertiary, blend mask
	LayerTarget blur_pyramid_[6]; // niveles 1..5 (1/2 ... 1/32) del blur a resolucion reducida
	LayerTarget blur_temp_low_;

	// Regulador adaptativo: sube la reduccion del blur si los frames con blur son lentos.
	std::chrono::steady_clock::time_point last_frame_time_;
	bool has_last_frame_time_ = false;
	bool blur_used_this_frame_ = false;
	bool blur_used_last_frame_ = false;
	float blur_frame_ms_ = 16.f;
	int blur_extra_levels_ = 0;
	int blur_fast_frames_ = 0;
	int blur_recover_wait_ = 300;
	int blur_cooldown_ = 0;
	unsigned int shared_stencil_ = 0;

	Rml::Rectanglei scissor_region_ = Rml::Rectanglei::MakeInvalid();
	bool scissor_enabled_ = false;

	Rml::Matrix4f projection_ = Rml::Matrix4f::Identity();
	Rml::Matrix4f transform_ = Rml::Matrix4f::Identity();
	bool transform_enabled_ = false;

	struct SavedState
	{
		int viewport[4]{};
		int scissor_box[4]{};
		int blend_src_rgb = 0;
		int blend_dst_rgb = 0;
		int blend_src_alpha = 0;
		int blend_dst_alpha = 0;
		int blend_equation_rgb = 0;
		int blend_equation_alpha = 0;
		int program = 0;
		int active_texture = 0;
		int texture_2d = 0;
		int array_buffer = 0;
		int element_array_buffer = 0;
		int framebuffer = 0;
		int vertex_array = 0;
		int stencil_clear_value = 0;
		int stencil_func = 0;
		int stencil_ref = 0;
		int stencil_value_mask = 0;
		int stencil_write_mask = 0;
		int stencil_fail = 0;
		int stencil_pass_depth_fail = 0;
		int stencil_pass_depth_pass = 0;
		int stencil_back_func = 0;
		int stencil_back_ref = 0;
		int stencil_back_value_mask = 0;
		int stencil_back_write_mask = 0;
		int stencil_back_fail = 0;
		int stencil_back_pass_depth_fail = 0;
		int stencil_back_pass_depth_pass = 0;
		float clear_color[4]{};
		unsigned char color_mask[4]{1, 1, 1, 1};
		unsigned char depth_mask = 1;
		bool depth_test = false;
		bool cull_face = false;
		bool blend = false;
		bool scissor = false;
		bool stencil = false;
	} saved_;
};
