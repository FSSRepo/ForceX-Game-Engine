#include "fx_file_interface.h"

#include <algorithm>
#include <cstdio>
#include <cstring>
#include <utility>

FxFileInterface::FxFileInterface(Rml::String base_directory) : base_directory_(std::move(base_directory)) {}

void FxFileInterface::SetBaseDirectory(Rml::String directory)
{
	base_directory_ = std::move(directory);
}

void FxFileInterface::SetAssetLoader(AssetLoader loader)
{
	asset_loader_ = std::move(loader);
}

Rml::String FxFileInterface::Resolve(const Rml::String& path) const
{
	if (base_directory_.empty() || path.empty())
		return path;

	// Rutas absolutas (POSIX, Windows con unidad o UNC) se dejan intactas.
	const bool is_absolute = path[0] == '/' || path[0] == '\\' ||
		(path.size() > 1 && path[1] == ':');
	if (is_absolute)
		return path;

	Rml::String resolved = base_directory_;
	if (resolved.back() != '/' && resolved.back() != '\\')
		resolved += '/';
	resolved += path;
	return resolved;
}

FxFileInterface::MemoryFile* FxFileInterface::FindMemoryFile(Rml::FileHandle file) const
{
	if ((file & MemoryHandleFlag) == 0)
		return nullptr;

	const auto it = memory_files_.find(file);
	return it == memory_files_.end() ? nullptr : it->second.get();
}

Rml::FileHandle FxFileInterface::Open(const Rml::String& path)
{
	if (asset_loader_)
	{
		std::vector<Rml::byte> data;
		if (asset_loader_(path, data))
		{
			Rml::FileHandle handle = MemoryHandleFlag | Rml::FileHandle(next_memory_id_++);
			auto file = std::make_unique<MemoryFile>();
			file->data = std::move(data);
			memory_files_[handle] = std::move(file);
			return handle;
		}
	}

	std::FILE* file = std::fopen(Resolve(path).c_str(), "rb");
	return static_cast<Rml::FileHandle>(reinterpret_cast<uintptr_t>(file));
}

void FxFileInterface::Close(Rml::FileHandle file)
{
	if (FindMemoryFile(file))
	{
		memory_files_.erase(file);
		return;
	}

	if (file)
		std::fclose(reinterpret_cast<std::FILE*>(file));
}

size_t FxFileInterface::Read(void* buffer, size_t size, Rml::FileHandle file)
{
	if (MemoryFile* memory = FindMemoryFile(file))
	{
		const size_t available = memory->data.size() - memory->position;
		const size_t to_read = std::min(size, available);
		if (to_read > 0)
			std::memcpy(buffer, memory->data.data() + memory->position, to_read);
		memory->position += to_read;
		return to_read;
	}

	if (!file)
		return 0;

	return std::fread(buffer, 1, size, reinterpret_cast<std::FILE*>(file));
}

bool FxFileInterface::Seek(Rml::FileHandle file, long offset, int origin)
{
	if (MemoryFile* memory = FindMemoryFile(file))
	{
		const long long total = static_cast<long long>(memory->data.size());
		long long target = 0;

		switch (origin)
		{
		case SEEK_SET:
			target = offset;
			break;
		case SEEK_CUR:
			target = static_cast<long long>(memory->position) + offset;
			break;
		case SEEK_END:
			target = total + offset;
			break;
		default:
			return false;
		}

		if (target < 0 || target > total)
			return false;

		memory->position = static_cast<size_t>(target);
		return true;
	}

	if (!file)
		return false;

	return std::fseek(reinterpret_cast<std::FILE*>(file), offset, origin) == 0;
}

size_t FxFileInterface::Tell(Rml::FileHandle file)
{
	if (MemoryFile* memory = FindMemoryFile(file))
		return memory->position;

	if (!file)
		return 0;

	const long position = std::ftell(reinterpret_cast<std::FILE*>(file));
	return position < 0 ? 0 : static_cast<size_t>(position);
}
