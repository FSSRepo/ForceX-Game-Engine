#pragma once

#include <RmlUi/Core/SystemInterface.h>

#include <chrono>

/**
    SystemInterface nativo de fxrmlui: reloj monótono para animaciones y log a
    stderr. En fases posteriores el log se reenviará a la API Java.
*/
class FxSystemInterface final : public Rml::SystemInterface
{
public:
	FxSystemInterface();

	double GetElapsedTime() override;
	bool LogMessage(Rml::Log::Type type, const Rml::String& message) override;
	void JoinPath(Rml::String& translated_path, const Rml::String& document_path, const Rml::String& path) override;

	/// RmlUi avisa cuando un campo de texto gana o pierde el foco para que la
	/// plataforma muestre su teclado virtual (IME en Android).
	void ActivateKeyboard(Rml::Vector2f caret_position, float line_height) override;
	void DeactivateKeyboard() override;

private:
	std::chrono::steady_clock::time_point start_time_;
};

/// Notifica a la capa Java que muestre u oculte el teclado virtual.
void FxNotifySoftKeyboard(bool visible);
