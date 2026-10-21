# Camara de GTA SA (v1.0 HOODLUM) - cierre de pendientes

Convenciones: **[V]** = leido/deducido directamente del codigo; **[NV]** = estimacion.
dt = DAT_00b7cb5c (1.0 = 20 ms). TheCamera = 0xB6F028 (`T`). Los CCam estan en `T+0x174 + idx*0x238` (idx = byte T+0x59 = 0xB6F081). [V] (ctor 0x5BC520 hace `lea ecx,[esi+0x174]`, `+0x3AC`, `+0x5E4`; modo en `T+0x180+idx*0x238`).
Herramienta auxiliar: se volco todo el .text a un asm con `objdump` (ver "Estado"); `dis.ps1` da lo mismo por rangos.

## Objetivos
1. Zoom a pie ........................ HECHO (parcial: falta tabla de valores por defecto para niveles; ver 1)
2. Offsets de beta por modo .......... HECHO
3. Colision (nivel/tabla) ............ HECHO (falta identificar 0x51A5D0 y 0x7EE1D0 en detalle)
4. StartTransition ................... PENDIENTE
5. Funciones/flags misc .............. EN CURSO (B72914, B6F080 resueltos)
6. Camara en vehiculo ................ PENDIENTE
7. Apuntar ........................... PENDIENTE
8. Extras ............................ PENDIENTE

---------------------------------------------------------------------

## 1. Zoom a pie (CCamera, proceso por frame en ~0x528A00-0x529140) [V]

Campos de TheCamera (direccion absoluta = 0xB6F028 + off):

| off | abs | significado |
|---|---|---|
| +0xB0 | B6F0D8 | quien controla la camara (lo escribe TakeControl 0x50C7F4/0x50C8CB/0x50C92C: arg 4). 2 = permite cambiar zoom |
| +0xB4 | B6F0DC | NIVEL zoom coche (1..3). Inicial 2 (0x473F79 `mov [B6F0DC],2`) |
| +0xB8 | B6F0E0 | base zoom coche del nivel (tabla 0x8CC3E0/3F4/408 indexada por tipo de vehiculo) |
| +0xBC | B6F0E4 | total (si 0 se copia de +0xB8) |
| +0xC0 | B6F0E8 | zoom coche suavizado |
| +0xC4 | B6F0EC | zoom coche por script (valor objetivo script) |
| +0xC8 | B6F0F0 | NIVEL zoom a pie (1 cerca, 2 medio, 3 lejos). Inicial 0 (memset del ctor) -> tratado como nivel 2 para la distancia pero sin offset de beta |
| +0xCC | B6F0F4 | zoom a pie OBJETIVO del nivel actual |
| +0xD0 | B6F0F8 | constante 0 (solo se lee en FollowPed 0x523214; nadie la escribe) |
| +0xD4 | B6F0FC | zoom a pie SUAVIZADO (el que lee Process_FollowPed) |
| +0xD8 | B6F100 | zoom a pie por script |
| +0x38 | B6F060 | byte: usar zoom de script a pie |
| +0x39 | B6F061 | byte: usar zoom de script en coche |
| +0x54 | B6F07C | byte = bit0 de byte 0xC87AB8 (llamada 0x72DD20), solo evaluado en el frame "ultimo cam" (0x528062-0x528072). Fuerza zoom a pie=0.5 y coche=-0.65 |
| +0x58 | B6F080 | byte "recien reiniciada/transicion": StartTransition lo pone a 1 (0x51584C, 0x515A3E); se limpia a 0 en 0x52A4A2/0x52B0DF/0x52B355. Mientras vale 1 el zoom se SALTA (snap) al objetivo (0x52A496: C0=B8, D4=CC) |

### Quien cambia el nivel (0x528D11-0x528DB3) [V]
Condiciones (todas): `CPad(0)->CycleCameraModeUp(0x5404A0) || ...Down(0x5404F0)` pulsado; `[0xA43088]!=1`; `T+0x3D==0`, `T+0x54==0`, `T+0x1F==0`; (`T+0x2B==1` o `T+0xB0==2`); `0x441390()==false`.
Accion: `nivel = nivel + (CycleCameraModeUp ? -1 : +1)` ; si `>3` -> 1 ; si `<1` -> 3. (0x5404A0/0x5404F0 = "JustDown" del boton: pad config 0/2 -> Select (0x1A) y DPadDown (0x12); config 1/3 -> DPadUp (0x10)...). El mismo handler se usa para el nivel a pie (+0xC8). El nivel de coche (+0xB4) usa `T+0xB4` con tabla propia (0x528B0A-0x528B3D).
Guardado en savegame: 0x5D1D7D (coche) y 0x5D1D89 (a pie) [V].

### Objetivo por nivel (0x528F8B-0x528FDD) [V]
```
objetivo(+0xCC) = CCam[activo]+{0xDC: nivel1, 0xE4: nivel3, 0xE0: nivel2/otro}
```
Valores iniciales (CCamera ctor 0x5BC5BB, solo para CCam[0] = `T+0x250..0x26C`) [V]:
| CCam+off | valor | uso |
|---|---|---|
| +0xDC | **-0.55** (0xBF0CCCCD) | zoom a pie nivel 1 (cerca) |
| +0xE0 | **1.5** (0x3FC00000) | zoom a pie nivel 2 (medio) |
| +0xE4 | **3.6** (0x40666666) | zoom a pie nivel 3 (lejos) |
| +0xE8 | **0.0598** (0x3D75C28F) | offset beta nivel 1 |
| +0xEC | **-0.1** (0xBDCCCCCD) | offset beta nivel 2 |
| +0xF0 | **0.0** | offset beta nivel 2 cuando fila de tabla = 1 (mission pack) |
| +0xF4 | **-0.07** (0xBD8F5C29) | offset beta nivel 3 |
| +0xF8 | 0.9065 (0x3F668113) | (no usado en FollowPed visto; sin determinar) |

(Item 2 resuelto: ver tabla. En FollowPed 0x522E57-0x522EA5: `beta_base = fila[+8] + {nivel1: edi+0xE8, nivel2: (fila==1? edi+0xF0 : edi+0xEC), nivel3: edi+0xF4}`; nivel 0 no suma nada.)

### Suavizacion (0x528FE3-0x529130) [V]
`paso = dt * 0.12` (constante 0x85A6F0 = 0.12) por frame, movimiento LINEAL con clamp (no exponencial):
```
destino = script_a_pie(+0x38) ? T+0xD8 : (T+0x54 ? 0.5 (0x8CCF14) : T+0xCC)
D4 += clamp-hacia(destino, paso)           // sube o baja, sin pasarse
si nivel==3 y T+0xCC==0 : D4 = CC          // 0x529117
```
Equivale a 6 unidades/s. En coche (0x528B7F-0x528CD9) idem sobre `+0xC0` hacia `+0xB8` (o `+0xC4` si script, o -0.65 (0x863274) si `+0x54`), mismo paso 0.12*dt.
Reset (T+0x58): D4=CC, C0=B8 instantaneo.

En FollowPed (0x523207-0x523222), en el ramal de reinicio (`bl!=0` / ResetStatics): si `D4 <= CC` entonces `D4 = [B6F0F8] = 0` (zoom a 0). [V] pero rol exacto [NV].

### Valores zoom de coche (item 1/6, tablas) [V]
Indexadas por `0x6D1080(...)` (categoria de vehiculo, 5 entradas):
- nivel1 (0x8CC3E0): -1, -0.2, -3.2, 0.05, -2.41
- nivel2 (0x8CC3F4): 1, 1.4, 0.65, 1.9, 6.49
- nivel3 (0x8CC408): 6, 6, 15.9, 15.9, 15
- +0xB4==4 y `T+0x26==0` -> +0xB8 = 1.0.

---------------------------------------------------------------------

## 3. Colision de camara (0x520190) [V]

`firma: f(ebp=pSource(in/out), edi=pTarget)` (stdcall ret 8). Nivel = byte `DAT_008CCB80` (inicial **5**, estatica `.data`; tambien se pone a 5 en 0x513231 y 0x5265A2). Se cambia con:
- `0x50CB60 SetColLevel(n)`: `nivel=n; DAT_008CCB84=1.0; DAT_00B6FE88 = 0x8CC8E0 + n*0x18`.
- `0x50CBF0` (mapea 0..3 -> 0..3), `0x50CC50(tipo,x)`: tipo0 -> nivel x+3, tipo1 -> x+6 (lo llama 0x52448C en FollowCar con `0xB6F0F0`), 0x522A40 (Aim).
- `0x84D750` inicializador estatico: `B6FE88 = 0x8CC8E0 + nivel*0x18`.

### Tabla 0x8CC8E0 (stride 0x18 = 6 floats) [V]
| nivel | [0] radio rel | [1] radio max | [2] frac min (nivel>=10) | [3] umbral frac | [4] | [5] vel recuperacion |
|---|---|---|---|---|---|---|
| 0 | 1.0 | 3 | 0.1 | 0.75 | 0.04 | 0.2 |
| 1,2,3 | 0.65 | 3 | 0.1 | 0.3 | 0.05 | 0.2 |
| 4 | 0.65 | 3 | 0.1 | 0.5 | 0.05 | 0.2 |
| 5 (defecto) | 0.65 | 3 | 0.1 | 0.3 | 0.05 | 0.2 |
| 6 | 0.65 | 3 | 0.05 | 0.3 | 0.05 | 0.2 |
| 7 | 0.65 | 3 | 0.1 | 0.5 | 0.05 | 0.2 |
| 8 | 0.65 | 3 | 0.1 | 0.3 | 0.05 | 0.2 |
| 9 | 0.65 | 3 | 0.05 | 0.3 | 0.05 | 0.2 |
| 10,11,12 | 0.65 | 0.65 | 0.1 | 0.1 | 0.05 | 0.1 |
| 13 | 0.65 | 0.65 | 0.07 | 0.3 | 0.05 | 0.1 |
(14 filas, 0x8CC8E0-0x8CCA2F; verificado con `dis.ps1 -Va 0x8cc8e0 -Len 0x150 -Floats`)

### Algoritmo [V]
```
d = Source - Target ; L = |d|
R = L * T[0] * 0.2939 (0x8CCB90)          // radio de la esfera
R = min(R, T[1]) ; R = max(R, 0.65 (0x8CCE18))   // 0.65 = radio MINIMO global, no es erroneo
minFrac = T[2]
si nivel<=9: minFrac = (nivel>3 ? 0.3 : 0.18) / L     // 0x8CCE14 / 0x8CCE10
si nivel>=10 y ped en estado 9 (T[0xB7CD68]+0x590==9): minFrac=0.05 (0x8CCE0C)
col = 0x51A5D0(TheCamera, Source, Target, outPos, R, minFrac, &frac)   // devuelve fraccion libre 0..1
si col y frac < T[3]: 0x7EE1D0(0xC1703C, T[4])     // efecto secundario (vibracion/audio) [NV]
// suavizado persistente F = DAT_008CCB84 (inicial 1.0):
si frac < F : F = frac                             // se acerca de golpe
si no (frac >= F):
    si |Source - SourcePrevio|^2 > 0.01^2 (0x8CCE08):     // solo relaja si la camara se mueve
        F += min( dt*T[5]*(frac - F), 0.05 (0x8CCE04) )
    SourcePrevio = Source
F = min(F, 1)
Source = Target + d * F
```
`0x51A5D0` = CCamera::ColDetAndReact-like (raycast/esfera contra mundo): no desensamblado a fondo.
`0x7EE1D0(obj,float)`: guarda float en obj+0x80 y actualiza (parece parametro de vibracion/shake o audio) [NV].

---------------------------------------------------------------------

## 5. Identificaciones [V]
- `DAT_00B72914` = `CGame::bMissionPackGame` (indice mission pack; 0 normal; lo escribe 0x440601,0x4422D6,0x53BD8C,0x5BF880=0, load 0x5D204F; se guarda en savegame 0x5D1DA0). En FollowPed selecciona la FILA de la tabla de seguimiento (0 -> 0x8CC548, !=0 -> 0x8CC584 = fila 1): la fila 1 es la variante para mision packs.
- `DAT_00B6F080` = `T+0x58`, flag de camara recien reiniciada (arriba). En FollowPed (0x522F12) si es 1 se salta la suavizacion del `CCam+0xB4`.
- `0x601070` / `0x601110` (CPedIntelligence/ped task tests): pendiente.

## Estado
(en construccion; ver secciones. Se escribira resumen final aqui.)
Comandos para retomar: `powershell -NoProfile -File tools\re\dis.ps1 -Va 0x<VA> -Len 0x<N>`; para greps masivos: objdump de .text completo a un txt (bytes 0x400..0x456400 del exe, `--adjust-vma=0x401000`).
