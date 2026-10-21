#pragma once

#include <RmlUi/Core/FileInterface.h>

#include <cstdint>
#include <functional>
#include <memory>
#include <unordered_map>
#include <vector>

/**
    FileInterface con dos fuentes:

    - Opcionalmente un *asset loader* inyectado (los bindings JNI lo conectan a
      `FX.fs`, de modo que documentos, estilos, fuentes, imágenes y animaciones
      se cargan desde recursos del jar, paquetes FX o assets de Android).
    - Fallback a stdio sobre un directorio base, útil en pruebas nativas y en
      ejecuciones de escritorio con archivos sueltos.

    Los handles de memoria usan el bit más alto para no colisionar con los
    punteros de `FILE*` que devuelve el fallback.
*/
class FxFileInterface final : public Rml::FileInterface
{
public:
	/// Carga el contenido completo de un archivo. Devuelve false si no existe.
	using AssetLoader = std::function<bool(const Rml::String& path, std::vector<Rml::byte>& out_data)>;

	explicit FxFileInterface(Rml::String base_directory = {});

	void SetBaseDirectory(Rml::String directory);
	void SetAssetLoader(AssetLoader loader);

	Rml::FileHandle Open(const Rml::String& path) override;
	void Close(Rml::FileHandle file) override;
	size_t Read(void* buffer, size_t size, Rml::FileHandle file) override;
	bool Seek(Rml::FileHandle file, long offset, int origin) override;
	size_t Tell(Rml::FileHandle file) override;

private:
	struct MemoryFile
	{
		std::vector<Rml::byte> data;
		size_t position = 0;
	};

	MemoryFile* FindMemoryFile(Rml::FileHandle file) const;
	Rml::String Resolve(const Rml::String& path) const;

	static constexpr Rml::FileHandle MemoryHandleFlag = Rml::FileHandle(1) << (sizeof(Rml::FileHandle) * 8 - 1);

	Rml::String base_directory_;
	AssetLoader asset_loader_;
	std::unordered_map<Rml::FileHandle, std::unique_ptr<MemoryFile>> memory_files_;
	uintptr_t next_memory_id_ = 1;
};
