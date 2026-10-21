#!/usr/bin/env bash
#
# Parchea plutovg-font.c para usar pthread en Android.
#
# plutovg usa <threads.h> (mtx_*), que bionic solo exporta desde la API 30: un .so enlazado
# contra la API "latest" falla con 'cannot locate symbol "mtx_init"' en Android < 11.
# Idempotente: no hace nada si el parche ya está aplicado.
#
# Uso: patch-plutovg-android.sh <plutovg-font.c>...

MARKER='#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L'

read -r -d '' BLOCK <<'BLOCK_EOF'
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

BLOCK_EOF

for path in "$@"; do
    [[ -f "$path" ]] || continue
    grep -q "FX_ANDROID_PTHREAD" "$path" && continue
    line="$(grep -n -F -m1 -- "$MARKER" "$path" | cut -d: -f1)"
    [[ -n "$line" ]] || continue

    block="$BLOCK"$'\n'
    # Conserva los finales de línea originales (CRLF).
    if grep -q $'\r' "$path"; then block="${block//$'\n'/$'\r\n'}"; fi

    tmp="$path.tmp"
    { head -n "$((line - 1))" "$path"; printf '%s' "$block"; tail -n "+$line" "$path"; } > "$tmp" && mv "$tmp" "$path"
    echo "parcheado: $path"
done
