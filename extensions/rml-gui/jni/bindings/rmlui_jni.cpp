// ---------------------------------------------------------------------------
//  Bindings JNI de RmlUi para ForceX (librería nativa fxrmlui).
//
//  Todas las funciones deben invocarse desde el hilo propietario del contexto
//  OpenGL/GLES del engine. Las interfaces nativas (render, sistema y archivos)
//  viven en una única instancia creada por RmlUI.init().
// ---------------------------------------------------------------------------

#include "fx_file_interface.h"
#include "fx_render_interface.h"
#include "fx_system_interface.h"

#include <RmlUi/Core.h>
#include <jni.h>

#include <memory>
#include <string>
#include <unordered_map>
#include <utility>
#include <vector>

namespace {

JavaVM* g_java_vm = nullptr;
jclass g_rmlui_class = nullptr;
jmethodID g_read_file_method = nullptr;
jmethodID g_new_event_method = nullptr;

struct FxRuntime
{
	FxSystemInterface system_interface;
	FxFileInterface file_interface;
	FxRenderInterface render_interface;
	Rml::Context* context = nullptr;
};

std::unique_ptr<FxRuntime> g_runtime;

JNIEnv* GetJniEnv();

/// Listener nativo que reenvía los eventos RML a un `RmlDocument.Listener` Java.
class FxEventListener final : public Rml::EventListener
{
public:
	FxEventListener(JNIEnv* env, jobject listener) : listener_(env->NewGlobalRef(listener))
	{
		jclass listener_class = env->GetObjectClass(listener);
		on_event_ = env->GetMethodID(listener_class, "onEvent", "(Lcom/forcex/rmlui/RmlEvent;)Z");
		env->DeleteLocalRef(listener_class);

		if (env->ExceptionCheck())
		{
			env->ExceptionClear();
			on_event_ = nullptr;
		}
	}

	~FxEventListener() override
	{
		if (JNIEnv* env = GetJniEnv(); env && listener_)
			env->DeleteGlobalRef(listener_);
	}

	bool Matches(JNIEnv* env, jobject listener) const
	{
		return env && env->IsSameObject(listener_, listener);
	}

	bool IsValid() const
	{
		return listener_ != nullptr && on_event_ != nullptr;
	}

	void ProcessEvent(Rml::Event& event) override
	{
		JNIEnv* env = GetJniEnv();
		if (!env || !listener_ || !on_event_ || !g_rmlui_class || !g_new_event_method)
			return;

		const Rml::Element* target = event.GetTargetElement();
		const Rml::Element* current = event.GetCurrentElement();

		jstring type = env->NewStringUTF(event.GetType().c_str());
		jstring target_id = env->NewStringUTF(target ? target->GetId().c_str() : "");
		jstring current_id = env->NewStringUTF(current ? current->GetId().c_str() : "");

		jobject java_event = env->CallStaticObjectMethod(g_rmlui_class, g_new_event_method, type, target_id, current_id);
		env->DeleteLocalRef(type);
		env->DeleteLocalRef(target_id);
		env->DeleteLocalRef(current_id);

		if (env->ExceptionCheck())
		{
			env->ExceptionDescribe();
			env->ExceptionClear();
			return;
		}

		if (!java_event)
			return;

		const jboolean stop_propagation = env->CallBooleanMethod(listener_, on_event_, java_event);
		env->DeleteLocalRef(java_event);

		if (env->ExceptionCheck())
		{
			env->ExceptionDescribe();
			env->ExceptionClear();
			return;
		}

		if (stop_propagation)
			event.StopPropagation();
	}

	/// El listener se destruye cuando RmlUi lo desvincula del elemento.
	void OnDetach(Rml::Element* /*element*/) override
	{
		delete this;
	}

private:
	jobject listener_ = nullptr;
	jmethodID on_event_ = nullptr;
};

struct FxListenerEntry
{
	Rml::String type;
	FxEventListener* listener = nullptr;
};

/// Listeners registrados desde Java, por documento, para poder desvincularlos.
std::unordered_map<Rml::ElementDocument*, std::vector<FxListenerEntry>> g_document_listeners;

JNIEnv* GetJniEnv()
{
	if (!g_java_vm)
		return nullptr;

	JNIEnv* env = nullptr;
	const jint status = g_java_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
	if (status == JNI_OK)
		return env;

	if (status == JNI_EDETACHED)
	{
		// La firma de AttachCurrentThread difiere entre el JNI estándar y el de Android.
#if defined(__ANDROID__)
		if (g_java_vm->AttachCurrentThread(&env, nullptr) != JNI_OK)
#else
		if (g_java_vm->AttachCurrentThread(reinterpret_cast<void**>(&env), nullptr) != JNI_OK)
#endif
			return nullptr;
	}

	return env;
}

std::string ToString(JNIEnv* env, jstring value)
{
	if (!value)
		return {};

	const char* chars = env->GetStringUTFChars(value, nullptr);
	std::string result = chars ? chars : "";
	if (chars)
		env->ReleaseStringUTFChars(value, chars);
	return result;
}

/// Carga el archivo completo a través de FX.fs (Java), usando el FileInterface
/// nativo como fallback si el motor no está disponible.
bool LoadAssetFromJava(const Rml::String& path, std::vector<Rml::byte>& out_data)
{
	JNIEnv* env = GetJniEnv();
	if (!env || !g_rmlui_class || !g_read_file_method)
		return false;

	jstring java_path = env->NewStringUTF(path.c_str());
	if (!java_path)
		return false;

	auto bytes = static_cast<jbyteArray>(env->CallStaticObjectMethod(g_rmlui_class, g_read_file_method, java_path));
	env->DeleteLocalRef(java_path);

	if (env->ExceptionCheck())
	{
		env->ExceptionDescribe();
		env->ExceptionClear();
		return false;
	}

	if (!bytes)
		return false;

	const jsize length = env->GetArrayLength(bytes);
	out_data.resize(static_cast<size_t>(length));
	if (length > 0)
		env->GetByteArrayRegion(bytes, 0, length, reinterpret_cast<jbyte*>(out_data.data()));

	env->DeleteLocalRef(bytes);
	return true;
}

Rml::Context* ToContext(jlong handle)
{
	return reinterpret_cast<Rml::Context*>(handle);
}

Rml::ElementDocument* ToDocument(jlong handle)
{
	return reinterpret_cast<Rml::ElementDocument*>(handle);
}

jlong LoadDocumentFromMemory(Rml::Context* context, const std::string& rml)
{
	if (!context || rml.empty())
		return 0;

	Rml::ElementDocument* document = context->LoadDocumentFromMemory(rml, "[rmlui-java]");
	return reinterpret_cast<jlong>(document);
}

jlong LoadDocumentFromFile(Rml::Context* context, const std::string& path)
{
	if (!context || path.empty())
		return 0;

	std::vector<Rml::byte> data;
	if (!LoadAssetFromJava(path, data) || data.empty())
		return 0;

	const std::string rml(reinterpret_cast<const char*>(data.data()), data.size());
	Rml::ElementDocument* document = context->LoadDocumentFromMemory(rml, path);
	return reinterpret_cast<jlong>(document);
}

void RemoveDocumentListeners(Rml::ElementDocument* document)
{
	const auto it = g_document_listeners.find(document);
	if (it == g_document_listeners.end())
		return;

	for (const FxListenerEntry& entry : it->second)
		document->RemoveEventListener(entry.type, entry.listener);

	g_document_listeners.erase(it);
}

} // namespace

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /*reserved*/)
{
	g_java_vm = vm;
	return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* /*vm*/, void* /*reserved*/)
{
	if (JNIEnv* env = GetJniEnv())
	{
		if (g_rmlui_class)
			env->DeleteGlobalRef(g_rmlui_class);
	}
	g_rmlui_class = nullptr;
	g_read_file_method = nullptr;
	g_new_event_method = nullptr;
	g_java_vm = nullptr;
}

// ---------------------------------------------------------------- RmlUI ----

extern "C" JNIEXPORT jstring JNICALL Java_com_forcex_rmlui_RmlUI_nativeGetVersion(JNIEnv* env, jclass)
{
	const Rml::String version = Rml::GetVersion();
	return env->NewStringUTF(version.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_forcex_rmlui_RmlUI_nativeInit(JNIEnv* env, jclass clazz, jint width, jint height)
{
	if (width <= 0 || height <= 0)
		return JNI_FALSE;

	if (g_runtime)
		return JNI_TRUE;

	if (!g_rmlui_class)
	{
		g_rmlui_class = static_cast<jclass>(env->NewGlobalRef(clazz));
		g_read_file_method = env->GetStaticMethodID(clazz, "readFile", "(Ljava/lang/String;)[B");
		g_new_event_method = env->GetStaticMethodID(clazz, "newEvent",
			"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lcom/forcex/rmlui/RmlEvent;");

		if (env->ExceptionCheck() || !g_read_file_method || !g_new_event_method)
		{
			env->ExceptionClear();
			g_read_file_method = nullptr;
			g_new_event_method = nullptr;
			return JNI_FALSE;
		}
	}

	auto runtime = std::make_unique<FxRuntime>();
	runtime->file_interface.SetAssetLoader(LoadAssetFromJava);

	Rml::SetSystemInterface(&runtime->system_interface);
	Rml::SetFileInterface(&runtime->file_interface);
	Rml::SetRenderInterface(&runtime->render_interface);

	if (!Rml::Initialise())
		return JNI_FALSE;

	runtime->render_interface.SetViewport(width, height);

	Rml::Context* context = Rml::CreateContext("main", Rml::Vector2i(width, height));
	if (!context)
	{
		Rml::Shutdown();
		return JNI_FALSE;
	}

	runtime->context = context;
	g_runtime = std::move(runtime);
	return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlUI_nativeShutdown(JNIEnv*, jclass)
{
	if (!g_runtime)
		return;

	// Los elementos destruyen sus listeners (OnDetach) durante el Shutdown.
	Rml::Shutdown();
	g_document_listeners.clear();
	g_runtime.reset();
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlUI_nativeResize(JNIEnv*, jclass, jint width, jint height)
{
	if (!g_runtime)
		return;

	if (width <= 0 || height <= 0)
		return;

	g_runtime->render_interface.SetViewport(width, height);
	if (g_runtime->context)
		g_runtime->context->SetDimensions(Rml::Vector2i(width, height));
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_forcex_rmlui_RmlUI_nativeLoadFont(JNIEnv* env, jclass, jstring path)
{
	if (!g_runtime || !path)
		return JNI_FALSE;

	const std::string font_path = ToString(env, path);
	if (font_path.empty())
		return JNI_FALSE;

	return Rml::LoadFontFace(font_path) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlong JNICALL Java_com_forcex_rmlui_RmlUI_nativeGetContextHandle(JNIEnv*, jclass)
{
	return reinterpret_cast<jlong>(g_runtime ? g_runtime->context : nullptr);
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlUI_nativeUpdate(JNIEnv*, jclass)
{
	if (g_runtime && g_runtime->context)
		g_runtime->context->Update();
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlUI_nativeRender(JNIEnv*, jclass)
{
	if (g_runtime && g_runtime->context)
	{
		g_runtime->render_interface.SetViewport(g_runtime->context->GetDimensions().x, g_runtime->context->GetDimensions().y);

		if (g_runtime->render_interface.BeginFrame())
		{
			g_runtime->context->Render();
			g_runtime->render_interface.EndFrame();
		}
	}
}

// ------------------------------------------------------------- RmlContext ----

extern "C" JNIEXPORT jstring JNICALL Java_com_forcex_rmlui_RmlContext_nativeGetName(JNIEnv* env, jclass, jlong handle)
{
	Rml::Context* context = ToContext(handle);
	return context ? env->NewStringUTF(context->GetName().c_str()) : nullptr;
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlContext_nativeSetDimensions(JNIEnv*, jclass, jlong handle, jint width, jint height)
{
	Rml::Context* context = ToContext(handle);
	if (!context || width <= 0 || height <= 0)
		return;

	context->SetDimensions(Rml::Vector2i(width, height));
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlContext_nativeSetDensityIndependentPixelRatio(JNIEnv*, jclass, jlong handle, jfloat ratio)
{
	Rml::Context* context = ToContext(handle);
	if (!context || ratio <= 0.f)
		return;

	context->SetDensityIndependentPixelRatio(ratio);
}

extern "C" JNIEXPORT jlong JNICALL Java_com_forcex_rmlui_RmlContext_nativeLoadDocument(JNIEnv* env, jclass, jlong handle, jstring rml)
{
	if (!rml)
		return 0;

	return LoadDocumentFromMemory(ToContext(handle), ToString(env, rml));
}

extern "C" JNIEXPORT jlong JNICALL Java_com_forcex_rmlui_RmlContext_nativeLoadDocumentFromFile(JNIEnv* env, jclass, jlong handle, jstring path)
{
	if (!path)
		return 0;

	return LoadDocumentFromFile(ToContext(handle), ToString(env, path));
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlContext_nativeSetInnerRml(
	JNIEnv* env, jclass, jlong handle, jstring element_id, jstring rml)
{
	Rml::Context* context = ToContext(handle);
	if (!context || !element_id || !rml)
		return;

	const std::string id = ToString(env, element_id);
	const std::string markup = ToString(env, rml);
	if (Rml::Element* element = context->GetRootElement()->GetElementById(id.c_str()))
		element->SetInnerRML(markup);
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlContext_nativeSetElementClass(
	JNIEnv* env, jclass, jlong handle, jstring element_id, jstring class_name, jboolean enable)
{
	Rml::Context* context = ToContext(handle);
	if (!context || !element_id || !class_name)
		return;

	const std::string id = ToString(env, element_id);
	const std::string name = ToString(env, class_name);
	if (Rml::Element* element = context->GetRootElement()->GetElementById(id.c_str()))
		element->SetClass(name.c_str(), enable == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlContext_nativeProcessMouseMove(
	JNIEnv*, jclass, jlong handle, jint x, jint y, jint modifiers)
{
	if (Rml::Context* context = ToContext(handle))
		context->ProcessMouseMove(x, y, modifiers);
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlContext_nativeProcessMouseButton(
	JNIEnv*, jclass, jlong handle, jint button, jboolean down, jint modifiers)
{
	Rml::Context* context = ToContext(handle);
	if (!context || button < 0 || button > 2)
		return;

	if (down)
		context->ProcessMouseButtonDown(button, modifiers);
	else
		context->ProcessMouseButtonUp(button, modifiers);
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlContext_nativeProcessMouseWheel(
	JNIEnv*, jclass, jlong handle, jfloat x, jfloat y, jint modifiers)
{
	if (Rml::Context* context = ToContext(handle))
		context->ProcessMouseWheel(Rml::Vector2f(x, y), modifiers);
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlContext_nativeProcessKey(
	JNIEnv*, jclass, jlong handle, jint key, jboolean down, jint modifiers)
{
	Rml::Context* context = ToContext(handle);
	if (!context || key <= Rml::Input::KI_UNKNOWN || key > Rml::Input::KI_LAST_CUSTOM_KEY)
		return;

	const auto identifier = static_cast<Rml::Input::KeyIdentifier>(key);
	if (down)
		context->ProcessKeyDown(identifier, modifiers);
	else
		context->ProcessKeyUp(identifier, modifiers);
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlContext_nativeProcessText(
	JNIEnv*, jclass, jlong handle, jint code_point)
{
	if (Rml::Context* context = ToContext(handle))
		context->ProcessTextInput(static_cast<Rml::Character>(code_point));
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlContext_nativeUpdate(JNIEnv*, jclass, jlong handle)
{
	if (Rml::Context* context = ToContext(handle))
		context->Update();
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlContext_nativeRender(JNIEnv*, jclass, jlong handle)
{
	Rml::Context* context = ToContext(handle);
	if (!context || !g_runtime)
		return;

	const Rml::Vector2i dimensions = context->GetDimensions();
	g_runtime->render_interface.SetViewport(dimensions.x, dimensions.y);

	if (g_runtime->render_interface.BeginFrame())
	{
		context->Render();
		g_runtime->render_interface.EndFrame();
	}
}

// ------------------------------------------------------------ RmlDocument ----

extern "C" JNIEXPORT jstring JNICALL Java_com_forcex_rmlui_RmlDocument_nativeGetId(JNIEnv* env, jclass, jlong handle)
{
	Rml::ElementDocument* document = ToDocument(handle);
	return document ? env->NewStringUTF(document->GetId().c_str()) : nullptr;
}

extern "C" JNIEXPORT jstring JNICALL Java_com_forcex_rmlui_RmlDocument_nativeGetSourceUrl(JNIEnv* env, jclass, jlong handle)
{
	Rml::ElementDocument* document = ToDocument(handle);
	return document ? env->NewStringUTF(document->GetSourceURL().c_str()) : nullptr;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_forcex_rmlui_RmlDocument_nativeIsVisible(JNIEnv*, jclass, jlong handle)
{
	Rml::ElementDocument* document = ToDocument(handle);
	return document && document->IsVisible(true) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlDocument_nativeShow(JNIEnv*, jclass, jlong handle)
{
	if (Rml::ElementDocument* document = ToDocument(handle))
		document->Show();
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlDocument_nativeHide(JNIEnv*, jclass, jlong handle)
{
	if (Rml::ElementDocument* document = ToDocument(handle))
		document->Hide();
}

extern "C" JNIEXPORT void JNICALL Java_com_forcex_rmlui_RmlDocument_nativeClose(JNIEnv*, jclass, jlong handle)
{
	Rml::ElementDocument* document = ToDocument(handle);
	if (!document)
		return;

	RemoveDocumentListeners(document);
	document->Close();
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_forcex_rmlui_RmlDocument_nativeAddEventListener(
	JNIEnv* env, jclass, jlong handle, jstring type, jobject listener)
{
	Rml::ElementDocument* document = ToDocument(handle);
	if (!document || !type || !listener)
		return JNI_FALSE;

	auto* native_listener = new FxEventListener(env, listener);
	if (!native_listener->IsValid())
	{
		delete native_listener;
		return JNI_FALSE;
	}

	const Rml::String event_type = ToString(env, type);
	document->AddEventListener(event_type, native_listener);
	g_document_listeners[document].push_back({event_type, native_listener});
	return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_forcex_rmlui_RmlDocument_nativeRemoveEventListener(
	JNIEnv* env, jclass, jlong handle, jstring type, jobject listener)
{
	Rml::ElementDocument* document = ToDocument(handle);
	if (!document || !type || !listener)
		return JNI_FALSE;

	const Rml::String event_type = ToString(env, type);

	const auto it = g_document_listeners.find(document);
	if (it == g_document_listeners.end())
		return JNI_FALSE;

	for (auto entry = it->second.begin(); entry != it->second.end(); ++entry)
	{
		if (entry->type != event_type || !entry->listener->Matches(env, listener))
			continue;

		// RemoveEventListener invoca OnDetach, que destruye el listener nativo.
		document->RemoveEventListener(entry->type, entry->listener);
		it->second.erase(entry);
		return JNI_TRUE;
	}

	return JNI_FALSE;
}
