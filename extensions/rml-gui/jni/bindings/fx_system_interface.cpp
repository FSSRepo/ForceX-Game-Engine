#include "fx_system_interface.h"

#include <cstdio>

#if defined(__ANDROID__)
	#include <android/log.h>
#endif

FxSystemInterface::FxSystemInterface() : start_time_(std::chrono::steady_clock::now()) {}

double FxSystemInterface::GetElapsedTime()
{
	const auto elapsed = std::chrono::steady_clock::now() - start_time_;
	return std::chrono::duration<double>(elapsed).count();
}

void FxSystemInterface::JoinPath(Rml::String& translated_path, const Rml::String& document_path, const Rml::String& path)
{
	Rml::SystemInterface::JoinPath(translated_path, document_path, path);
	if (translated_path.rfind("assets/", 0) == 0)
		translated_path.erase(0, 7);
}

bool FxSystemInterface::LogMessage(Rml::Log::Type type, const Rml::String& message)
{
	const char* level = "INFO";
	switch (type)
	{
	case Rml::Log::LT_ERROR:
		level = "ERROR";
		break;
	case Rml::Log::LT_ASSERT:
		level = "ASSERT";
		break;
	case Rml::Log::LT_WARNING:
		level = "WARNING";
		break;
	case Rml::Log::LT_DEBUG:
		level = "DEBUG";
		break;
	default:
		break;
	}

#if defined(__ANDROID__)
	int priority = ANDROID_LOG_INFO;
	if (type == Rml::Log::LT_ERROR || type == Rml::Log::LT_ASSERT)
		priority = ANDROID_LOG_ERROR;
	else if (type == Rml::Log::LT_WARNING)
		priority = ANDROID_LOG_WARN;
	else if (type == Rml::Log::LT_DEBUG)
		priority = ANDROID_LOG_DEBUG;
	__android_log_print(priority, "fxrmlui", "[%s] %s", level, message.c_str());
#endif
	std::fprintf(stderr, "fxrmlui [%s] %s\n", level, message.c_str());
	std::fflush(stderr);
	return true;
}
