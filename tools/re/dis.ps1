# Desensambla bytes de gta_sa.exe (v1.0 HOODLUM) por direccion virtual, sin pasar por Ghidra.
# Uso: powershell -File tools/re/dis.ps1 -Va 0x680c60 -Len 0x300 [-Out docs/re/jump.asm]
# Mapeo del primer .text: raw = 0x400 + (VA - 0x401000). Para .data/.rdata (VA 0x458000..0x89e000) se usa -Data.
# Nota: algunas funciones empiezan con "push -1; jmp 0x4xxxxx"; el cuerpo real empieza 7 bytes despues.
param(
    [Parameter(Mandatory)][string]$Va,
    [int]$Len = 0x200,
    [string]$Exe = 'D:\Games\GTA San Andreas\gta_sa.exe',
    [string]$Out,
    [switch]$Floats,
    [switch]$Data
)

$addr = [Convert]::ToInt32($Va, 16)
$b = [IO.File]::ReadAllBytes($Exe)

# secciones del PE para mapear VA -> offset de archivo
$pe = [BitConverter]::ToInt32($b, 0x3c)
$ns = [BitConverter]::ToUInt16($b, $pe + 6)
$opt = [BitConverter]::ToUInt16($b, $pe + 20)
$sec = $pe + 24 + $opt
$raw = $null
for ($i = 0; $i -lt $ns; $i++) {
    $s = $sec + $i * 40
    $vs = [BitConverter]::ToUInt32($b, $s + 8)
    $sva = [BitConverter]::ToUInt32($b, $s + 12) + 0x400000
    $rs = [BitConverter]::ToUInt32($b, $s + 16)
    $rp = [BitConverter]::ToUInt32($b, $s + 20)
    if ($addr -ge $sva -and $addr -lt $sva + [Math]::Max($vs, $rs)) {
        $raw = $rp + ($addr - $sva)
        break
    }
}
if ($null -eq $raw) { throw "VA $Va no cae en ninguna seccion" }

if ($Floats) {
    # vuelca floats consecutivos (tablas de parametros)
    $lines = for ($o = 0; $o -lt $Len; $o += 4) {
        '{0:x}: {1}  (0x{2:x8})' -f ($addr + $o), [BitConverter]::ToSingle($b, $raw + $o), [BitConverter]::ToUInt32($b, $raw + $o)
    }
} else {
    $tmp = Join-Path ([IO.Path]::GetTempPath()) ('dis_' + [Guid]::NewGuid().ToString('N') + '.bin')
    $a = New-Object byte[] $Len
    [Array]::Copy($b, $raw, $a, 0, $Len)
    [IO.File]::WriteAllBytes($tmp, $a)
    $lines = & objdump -D -b binary -mi386 -M intel "--adjust-vma=$addr" $tmp |
        Select-Object -Skip 7 |
        ForEach-Object { $_ -replace '^\s*([0-9a-f]+):\s+(?:[0-9a-f]{2} )+\s*', '$1  ' } |
        Where-Object { $_ -notmatch '^\s*[0-9a-f]+\s*$' }
    Remove-Item $tmp -ErrorAction SilentlyContinue
}

if ($Out) {
    New-Item -ItemType Directory -Force (Split-Path -Parent $Out) | Out-Null
    $lines | Set-Content -Encoding utf8 $Out
    "escrito $Out ($($lines.Count) lineas)"
} else {
    $lines
}

