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

private:
	std::chrono::steady_clock::time_point start_time_;
};
