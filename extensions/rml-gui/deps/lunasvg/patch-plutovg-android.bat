@echo off
rem Parchea plutovg-font.c para usar pthread en Android.
rem
rem plutovg usa <threads.h> (mtx_*), que bionic solo exporta desde la API 30: un .so enlazado
rem contra la API "latest" falla con 'cannot locate symbol "mtx_init"' en Android < 11.
rem Idempotente: no hace nada si el parche ya esta aplicado.
rem
rem Uso: patch-plutovg-android.bat <plutovg-font.c>...
rem
rem El script en PowerShell va embebido al final de este mismo archivo.
for %%F in (%*) do powershell -NoProfile -ExecutionPolicy Bypass -Command "& ([scriptblock]::Create(((Get-Content -Raw -Encoding UTF8 -LiteralPath '%~f0') -split ('#'+'PS#'))[1])) '%%~fF'"
exit /b %errorlevel%
#PS#
$marker = '#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L'
$block = @'
#elif defined(__ANDROID__) /* FX_ANDROID_PTHREAD */

#include <pthread.h>

typedef pthread_mutex_t plutovg_mutex_t;

static inline void plutovg_mutex_init_recursive(pthread_mutex_t* mutex)
{
    pthread_mutexattr_t attr;
    pthread_mutexattr_init(&attr);
    pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init(mutex, &attr);
    pthread_mutexattr_destroy(&attr);
}

#define plutovg_mutex_init(mutex) plutovg_mutex_init_recursive(mutex)
#define plutovg_mutex_lock(mutex) pthread_mutex_lock(mutex)
#define plutovg_mutex_unlock(mutex) pthread_mutex_unlock(mutex)
#define plutovg_mutex_destroy(mutex) pthread_mutex_destroy(mutex)

'@
$utf8 = New-Object System.Text.UTF8Encoding($false)
foreach ($path in $args) {
    if (-not (Test-Path -LiteralPath $path)) { continue }
    $path = (Resolve-Path -LiteralPath $path).Path
    $text = [System.IO.File]::ReadAllText($path, $utf8)
    $index = $text.IndexOf($marker)
    if ($text.Contains('FX_ANDROID_PTHREAD') -or $index -lt 0) { continue }
    $eol = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
    $patched = $text.Substring(0, $index) + ($block -replace "`r?`n", $eol) + $text.Substring($index)
    [System.IO.File]::WriteAllText($path, $patched, $utf8)
    Write-Host "parcheado: $path"
}
