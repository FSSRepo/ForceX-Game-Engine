# Camara de GTA SA (v1.0 HOODLUM) - cierre de pendientes

Convenciones: **[V]** = leido/deducido directamente del codigo; **[NV]** = estimacion.
dt = DAT_00b7cb5c (1.0 = 20 ms). TheCamera = 0xB6F028 (`T`). Los CCam estan en `T+0x174 + idx*0x238` (idx = byte T+0x59 = 0xB6F081). [V] (ctor 0x5BC520 hace `lea ecx,[esi+0x174]`, `+0x3AC`, `+0x5E4`; modo en `T+0x180+idx*0x238`).
Herramienta auxiliar: se volco todo el .text a un asm con `objdump` (ver "Estado"); `dis.ps1` da lo mismo por rangos.

## Objetivos
1. Zoom a pie ........................ HECHO
2. Offsets de beta por modo .......... HECHO
3. Colision (nivel/tabla) ............ HECHO (0x51A5D0 ConeCastCollisionResolve sin detallar)
4. StartTransition ................... HECHO
5. Funciones/flags misc .............. HECHO (601070/601110 NV)
6. Camara en vehiculo ................ HECHO (beta max parcial)
7. Apuntar ........................... HECHO
8. Extras ............................ PARCIAL (mirar atras hecho; cinematicas de coche y sniper NO)

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

### Tabla 0x8CC8E0 = gCamColVars (stride 0x18 = 6 floats, 28 filas, 0x8CC8E0-0x8CCB7F) [V]
Simbolos (PDB, gtasa-menu/docs/gta_sa_symbols.tsv): `0x520190 CCamera::CameraColDetAndReact`, `0x51A5D0 CCamera::ConeCastCollisionResolve`, `0x50CB60 CCamera::SetCamCollisionVarDataSet`, `0x8CCB80 gCurCamColVars`, `0x50CB90 SetNearClipBasedOnPedCollision`.
Indices de nivel: AimWeapon 0..3 (`SetColVarsAimWeapon` 0x50CBF0), Ped = `i+3` (tipo 0) o `i+6` (tipo 1) (`SetColVarsPed` 0x50CC50, lo llama FollowCar 0x52448C con DAT_B6F0F0 = nivel zoom a pie), Vehiculo = `9 + 3*tipo + i`, tipo 0..6 (`SetColVarsVehicle` 0x50CCA0, llamado desde 0x525D52 con el nivel de zoom de coche), defecto 5.
| nivel | [0] radio rel | [1] radio max | [2] fraccion min (solo vehiculo/nivel>=10) | [3] umbral | [4] near clip si frac<umbral | [5] vel. recuperacion |
|---|---|---|---|---|---|---|
| 0 | 1.0 | 3 | 0.1 | 0.75 | 0.04 | 0.2 |
| 1,2,3,5,8 | 0.65 | 3 | 0.1 | 0.3 | 0.05 | 0.2 |
| 4,7 | 0.65 | 3 | 0.1 | 0.5 | 0.05 | 0.2 |
| 6,9 | 0.65 | 3 | 0.05 | 0.3 | 0.05 | 0.2 |
| 10,11,12 | 0.65 | 0.65 | 0.1 | 0.1 | 0.05 | 0.1 |
| 13 | 0.65 | 0.65 | 0.07 | 0.3 | 0.05 | 0.1 |
| 14 | 0.65 | 0.65 | 0.05 | 0.3 | 0.05 | 0.1 |
| 15 | 0.65 | 0.65 | 0.02 | 0.3 | 0.05 | 0.1 |
| 16-21 | 0.65 | 3 | 0.1 | 0.1 | 0.05 | 0.1 |
| 22 | 0.65 | 0.25 | 0.1 | 0.1 | 0.05 | 0.1 |
| 23-27 | 0.65 | 0.65 | 0.1 | 0.1 | 0.05 | 0.1 |
Nota: (nivel 9 pertenece al tipo 0 de vehiculo, i=0; ped usa 3..8).

### Algoritmo [V]
```
d = Source - Target ; L = |d|
R = L * T[0] * 0.2939 (0x8CCB90)          // radio de la esfera
R = min(R, T[1]) ; R = max(R, 0.65 (0x8CCE18))   // 0.65 = radio MINIMO global, no es erroneo
minFrac = T[2]
si nivel<=9: minFrac = (nivel>3 ? 0.3 : 0.18) / L     // 0x8CCE14 / 0x8CCE10
si nivel>=10 y ped en estado 9 (T[0xB7CD68]+0x590==9): minFrac=0.05 (0x8CCE0C)
col = 0x51A5D0(TheCamera, Source, Target, outPos, R, minFrac, &frac)   // devuelve fraccion libre 0..1
si col y frac < T[3]: RwCameraSetNearClipPlane(RwCamera(0xC1703C), T[4])   // 0x7EE1D0 = _RwCameraSetNearClipPlane [V, simbolo PDB]
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
`0x7EE1D0` = `RwCameraSetNearClipPlane` (PDB). `0x50CB90 SetNearClipBasedOnPedCollision(f)` tambien lo usa. Si ademas nivel>=10, hay bici (veh+0x590==9, BIKE) y frac<0.5 (0x8CCE00) -> near clip = 0.05 (0x8CCDFC).

---------------------------------------------------------------------

---------------------------------------------------------------------

## 4. StartTransition (0x515200..0x515B60) y mezcla en Process (0x52B9A0-0x52C3D9) [V]

Campos TheCamera: `+0x78` = tiempo inicio (ms, CTimer 0xB7CB84), `+0x7C` = duracion total (ms), `+0x80` = duracion de mezcla (copia de +0x7C al final), `+0xC10` = fraccion A, `+0xC14` = fraccion B, `+0xC18/+0xC1C` copias de C10/C14 (para la fase "snapshot"), `+0x57`=1 transicion activa, `+0x58`=1 (reset/snap, ver seccion 1).

Valores por defecto al empezar (0x515222 y 0x515679): **C10 = 0.25, C14 = 0.75, duracion = 1350 ms (0x546)**.
Excepciones segun (modo_nuevo `bp`, modo_viejo `ax`); modos = enum eCamMode de SA (4 FOLLOWPED, 11 SYPHON, 15 FIXED, 18 CAM_ON_A_STRING, 22 BEHINDBOAT, 28 SYPHON_CRIM_IN_FRONT, 35 SPECIAL_FIXED_FOR_SYPHON, 39-43/46/52 runabout/sniper/etc, 53 AIMWEAPON):

| condicion (nuevo <- viejo) | duracion ms | C10 | C14 | direccion |
|---|---|---|---|---|
| 11 <- 28 | 1800 | (def 0.25) | (def 0.75) | 0x51568F |
| 18 <- {28,4,11,35,53} | 750 | 0.1 | 0.9 | 0x5156BF |
| *viejo==35 (cualquier nuevo salvo los anteriores)* | `[8CCCC4]`=**1000** | `[8CCCCC]`=**0.2** | `[8CCCC8]`=**0.8** | 0x5156E5 |
| viejo==15 | 1350 | 0.05 | 0.95 | 0x515710 |
| 4 <- 18 con `T+0x27` != 0 | 800 | 0.02 | 0.98 | 0x515740 |
| {18,22} <- {39,40,52,41,42,43,46} (salir de sniper/lanzacohetes/1a persona en vehiculo) | **1 (corte)** | 0 | 1.0 | 0x51582C |
| **nuevo==53 (AIMWEAPON)** | `[8CCCBC]`=**400** | `[B70044]` (bss; =0 al arrancar, **[NV]**; nadie lo escribe en el exe, la constante vecina `[8CCCB8]`=0.4 se usa en 0x513D61) | `[8CCCC0]`=**1.0** | 0x5157B4 (ademas bl=1) |
| {4,28,11,35} <- {28,4,11,53} | 350 | 0.1 | 0.9 | 0x51580D (bl=1) |
| resto | 1350 | 0.25 | 0.75 | |

Caso `T+0x2B != 0` (camara controlada por script) al final (0x515B33): C18=0, C1C=1.0, duracion mezcla = **600 ms** (350 si bl, es decir si se entra en 53 o en el grupo 4/28/11/35). Si `T+0x50==1` se restauran C10/C14/duracion de `T+0xC24/C28/C2C` (0x515AEB).
**Caso modo 35 (0x23) como viejo:** 1000 ms, 0.2/0.8. **Caso modo 53 como nuevo:** 400 ms, C10=[B70044]~0, C14=1.0 (es decir, practicamente toda la mezcla es la fase B, curva coseno en 0.4 s).

### Interpolacion [V] (0x52BA84-0x52C340)
```
t = min(ahora - T+0x78, T+0x7C) / T+0x7C            // [esp+1c], 0..1
// fase A (t <= C10):  s=(C10-t)/C10  (C10==0 -> 0)
//     w = 0.5 - 0.5*cos(pi*s)     // curva coseno 1 -> 0
//     Source = base(+0x890) + w*offset(+0x8B4) ; mira(+0x8A8)+w*(+0x8CC) ; fov = fov_nuevo(+0x170) + w*(+0x114)
// fase B (C10 < t <= 1):  u=(t-C10)/C14 (C14==0 -> 1)
//     w2 = 0.5 - 0.5*cos(pi*u)       // 0 -> 1
//     pos = lerp(pos_intermedia, pos_nueva_camara(CCam+0x310), w2) via 0x40FE60/0x40FEC0 (a + (b-a)*w2)
// fin: t>1 -> 0x52C340 (cierra, T+0x57 = 0)
```
Es decir: posicion (Source), punto de mira (Target) y FOV (+0x110) se mezclan con la misma curva coseno ("smoothstep" 0.5-0.5cos(pi*x)); la orientacion se reconstruye a partir de front = mira - source y up (0x59C910 normaliza). Con `T+0x2B` (script) la distancia horizontal source-target se fuerza a >= 1.3 (`0x8CCF20`) durante la mezcla (0x52BDBF y 0x52C10E). La mezcla de orientacion exacta (slerp vs lerp+normalize) no se examino en detalle **[NV]**.

---------------------------------------------------------------------

## 6. Camara en vehiculo: Process_FollowCar_SA (0x5245B0-0x525E48) [V salvo marcas]
Simbolo PDB: `CCam::Process_FollowCar_SA(CVector const&, float, float, float, bool)`. `esi` = CCam, `edi` = vehiculo, `ebx*0x3C` = fila de CARCAM_SET (0x8CC600).

### Fila de la tabla (0x8CC600 + fila*0x3C, 15 floats) [V valores; semantica por uso en codigo]
Valores (`dis.ps1 -Va 0x8cc600 -Len 0x1d4 -Floats`):
| fila | [0] | [1] | [2] | [3] | [4] | [5] | [6] | [7] | [8] | [9] | [10] | [11] | [12] | [13] | [14] |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 0 coche | 1.3 | 1 | 0.4 | 10 | 15 | 0.5 | 1 | 1 | 0.85 | 0.2 | 0.075 | 0.05 | 0.8 | 0.7854 | 1.5533 |
| 1 moto/quad | 1.1 | 1 | 0.1 | 10 | 11 | 0.5 | 1 | 1 | 0.85 | 0.2 | 0.075 | 0.05 | 0.75 | 0.7854 | 1.5533 |
| 2 heli/VTOL | 1.1 | 1 | 0.2 | 10 | 15 | 0.05 | 0.05 | 0 | 0.9 | 0.05 | 0.01 | 0.05 | 1 | 0.1745 | 1.2217 |
| 3 avion | 1.1 | 3.5 | 0.2 | 10 | 25 | 0.5 | 1 | 1 | 0.75 | 0.1 | 0.005 | 0.2 | 1 | 1.5533 | 1.5533 |
| 4 barco | 1.3 | 1 | 0.4 | 10 | 15 | 0.5 | 1 | 0 | 0.9 | 0.05 | 0.005 | 0.05 | 1 | 0.3491 | 1.2217 |
| 5 RC coche/avion | 1.1 | 1 | 0.2 | 10 | 5 | 0.5 | 1 | 1 | 0.75 | 0.1 | 0.005 | 0.2 | 1 | 0.7854 | 1.5533 |
| 6 RC heli | 1.1 | 1 | 0.2 | 10 | 5 | 0.5 | 1 | 1 | 0.75 | 0.1 | 0.005 | 0.2 | 1 | 0.3491 | 1.2217 |

Seleccion de fila (0x524624-0x5246C6): modelos 441,464,564,594 (RC Bandit/Baron/Tiger/Cam) -> 5; modelos 465,501 (RC Raider/Goblin) -> 6; `veh+0x590==9` o `veh+0x594==2` -> 1; `veh+0x594==3` -> 2; `veh+0x594==4` -> (modelo 520 con `veh+0x86C >= word[8D33C8]` -> 2; modelo 539 -> 0; otros -> 3); `veh+0x594==5` -> 4; resto 0. Los numeros son [V]; los nombres de tipo (moto, heli, avion, barco) son [NV].

Uso por campo [V]: `[1]` se suma al zoom suavizado `T+0xC0` (0x5246D8); `[0]*altura - [2]` offset de altura (0x524A43); `[3]` multiplica `B6FD70*alto` (0x5249C9); `[4]` distancia minima (x0.65 si nivel de zoom 1 y apariencia 0/1, 0x524AB2); `[5]`,`[6]`: retorno de beta (`k2 = 1 - [5]^dt`, paso max `dt*[6]`, 0x5253A6-0x5253D9); `[8]` base de la suavizacion exponencial `k = [8]^dt` de las velocidades de alpha y beta; `[9]` velocidad max de alpha/beta; `[10]` ganancia del auto-seguimiento de alpha; `[11]` paso max del auto-seguimiento (rad por unidad dt); `[12]` multiplicador del stick vertical; `[13]` beta maxima por defecto; `[14]` beta minima = -`[14]`.

### Offset de beta base por nivel de zoom de coche (0x524712-0x52475A) [V]
Indexado por apariencia (0x50AF00): nivel1 `0x8CC41C` {0.08,0.08,0.15,0.08,0.08}; nivel2 `0x8CC430` {0.07,0.08,0.3,0.08,0.08}; nivel3 `0x8CC444` {0.055,0.05,0.15,0.06,0.08}. Para entidades tipo moto (`(veh+0x36)&0xF8==0x40`) siempre la de nivel 2.

### Distancia (0x5249B3-0x524AEA) [V]
`dist = (T+0xC0 + fila[1]) + ajustes de ApplyVehicleCameraTweaks (B6FD70/74/78)`, con minimo `fila[4]` (x0.65 en nivel 1 y apariencia 0/1). Se guarda `CCam+0xD0 = dist`, `CCam+0xCC = 3.5`. Para apariencia 3 (no moto) el target se desplaza `0.6 * veh.top * altura` (const 0x8CC53C).

### Contador de sensibilidad `CCam+0xB4` (0x524AFB-0x524BE5) [V]
Si el vehiculo es de tipo 0 o 9: `v = dot(vel, forward)`; si `v > 0.4 (0x8CC540)`: `B4 += (v-0.4)*dt`. Si `B4 > 70`: `B4 = 70 + (B4-70)*0.98^dt` (0x8CC544; pow = 0x822130); `B4 = min(B4,100)`. En reset `B4 = 70`. Se usa como factor de sensibilidad del stick: `B4*0.0125` (0.875 a 70, 1.25 a 100). No hay logica especial de marcha atras (solo cuenta velocidad hacia delante).

### Auto-seguimiento de alpha ("derrape") (0x524DA9-0x525002) [V]
```
F  = normalize(target - source)          // front actual (CCam+0x190)
A  = atan2(-F.x, F.y) - pi/2              // alpha de la camara
si |vel2D| > 0.02 (0x858B38): H = atan2(-vx, vy) - pi/2  sino H = A
H' = H normalizado a (A-pi, A+pi]
L  = | vel - (vel.F)*F |                  // velocidad lateral 3D respecto al eje de la camara
m  = min(1, dt*fila[10]*L)
paso = clamp( m*(H'-A), -dt*fila[11], +dt*fila[11] )
A' = A + paso  (normalizado a (alpha-pi, alpha+pi])
delta_alpha_auto = (A' - alpha_actual) / max(1, dt)        // [esp+44]
```
Solo empuja la camara hacia el rumbo de la velocidad cuando hay velocidad lateral respecto a la vista (derrape/giro); ganancia 0.075 y tope 0.05 rad por unidad dt (coche). Con el coche yendo recto (incluida marcha atras recta) L=0 y la camara no gira.

### Suavizacion de velocidades alpha/beta (0x525850-0x525B0E) [V]
```
k  = fila[8]^dt                                          // 0.85^dt coche; 0.9 heli/barco; 0.75 avion/RC
entrada_alpha = x*|x| * (B4*0.0125) * 0.0714286 * 0.007^2   // x = stick camara X (0x541040 negado)
entrada_beta  = y*|y| * (B4*0.0125) * 0.0428571 * 0.007^2   // y = stick camara Y (0x5410C0)
  (constantes 0x85A5C4, 0x859B50, 0x8631AC, 0x8CC4A0)
  (x,y = 0 si gCameraDirection(0x8CC384)!=3; beta=0 en modelos 443,486,406,524,592,520,525,530,531; alpha=beta=0 en modelo 564 o (veh+0x590==0 y veh+0x38C&0x20000))
v_alpha = clamp(entrada_alpha + delta_alpha_auto, +-fila[9])
alphaSpeed(+0xC0) = (1-k)*v_alpha + k*alphaSpeed ; |alphaSpeed|<1e-4 -> 0
alpha(+0xBC) += dt*alphaSpeed                 // (modo mirar 'bl': += entrada_alpha directo)
v_beta  = entrada_beta*fila[12] (+ retorno a beta base: (-base - beta)*0.05 si |base+beta|>0.05 y sin stick; en motos (+0x804>1) +0.075*(objetivo-beta))
betaSpeed(+0xB0) = (1-k)*v_beta + k*betaSpeed ; clamp +-fila[9] (x0.5 si v_beta>0); |.|<1e-4 -> 0
beta(+0xAC) += dt*betaSpeed + paso_retorno     // paso_retorno = clamp((beta_obj-beta)*(1-fila[5]^dt), +-dt*fila[6])
beta = clamp(beta, -fila[14], betaMax)
```
Alpha se normaliza con 0x509C50; si `0xB6F050` ("camara directamente detras") alpha = atan2(front)+pi.

### Beta maxima ("producto escalar morro/camara") (0x5250DE-0x525364) [V parcial, resto NV]
`betaMax = fila[13]` por defecto. Solo se recalcula si `|vel|^2 > 0.04` (0x863244) y el vehiculo no es heli (`594!=3`), avion con `+0x960!=0`, ni moto con `+0x804<4`. Se construye un vector horizontal por producto vectorial (0x59C730) + normalizacion (0x59C910) a partir del morro (`veh.matrix+0x10`) y se exige `dot(vector, F) > 0` (0x5251A5); si no, queda `fila[13]`. Si se cumple:
```
phi = asin(|sin(heading_coche - alpha_camara)|)          // 0x821E70 = asin
betaMax = pitch_coche(veh.top.z) * cos(heading_coche - alpha) + atan2( altura_relativa , caja_colision * 1.2 (0x8CCED0) )   // 0x8CCED4=1.5, 0x8CCED8=1.2 en la rama alterna
(motos/aviones con +0x960>1: se suma un segundo termino con el heading desplazado pi/2)
```
La geometria exacta del termino con la caja de colision (`GetColModel`, campos +4 y +0xC) no se cerro **[NV]**: acota la beta segun el cabeceo del coche y el angulo relativo morro/camara para que la camara no baje dentro del vehiculo.

### Otros [V]
- Front/posicion final: `front = (-cos(alpha)cos(beta), -sin(alpha)cos(beta), sin(beta))`; `Source = target - front*dist`; luego `SetColVarsVehicle(apariencia, nivel_zoom_coche)` (0x525D52), `CameraColDetAndReact` (0x525D98), `ImproveNearClip` (0x525DAA), y el vector `front` se suaviza con `0x50A0A0(v,4)` x3 [funcion de suavizado, semantica exacta NV].
- Zoom de coche suavizado: ver seccion 1 (paso lineal 0.12*dt hacia `T+0xB8`).

---------------------------------------------------------------------

## 7. Apuntar: CCam::Process_AimWeapon (0x521500-0x522C80) [V salvo marcas]
(Simbolo PDB `CCam::Process_AimWeapon(CVector const&, float, float, float)`.) `FUN_00822130` = `pow(base, exp)` (CRT; `fld base; fld dt; call`), `0x821E70` = `asin`, `0x420800(a,b)` = `max(a,b)`.

### Suavizado del stick (0x52230C-0x5223F0) [V]
```
x = -pad.GetCamStickX (0x541040) ; y = pad.GetCamStickY (0x5410C0)         // enteros, +-128
entrada_alpha = x*|x| * (T+0xB4*0.0125) * 0.0714286 * dt * 0.007^2          // 0x85A5C4, 0x859B50, 0x8CC4A0
entrada_beta  = y*|y| * 0.0428571 * dt * 0.007^2                          // 0x8631AC (sin factor T+0xB4 en beta)
base = 0.8 (0x8CCE5C)                                                      // por defecto
si |x| < 2.0 (0x858CA0) y |y| < 2.0 : base = 0.5 (0x8CCE58)               // stick en zona muerta: frena mas rapido
k = base^dt
alphaSpeed(+0xC0) = k*alphaSpeed + (1-k)*entrada_alpha
betaSpeed (+0xB0) = k*betaSpeed  + (1-k)*entrada_beta
```
(los clamps y la integracion `alpha += alphaSpeed`, `beta += betaSpeed` y el limite de beta vienen despues; el limite superior de beta viene de `tabla_apuntar[+0x14]` en `0x8CC4D4` = 0.7854 para el 1er tipo, ver tabla 0x8CC4C0 stride 0x1C).

### Otros suavizados con pow [V]
- 0x521915-0x52195E (solo si `T+0x58==0`, o sea no recien reiniciada): `k = 0.96^dt` (0x862F30); `B70100 = k'*... ` : filtro paso bajo de los offsets de mira `B700FC/B70100/B700F4` con constante 0.96 por unidad dt (`nuevo = (1-k)*entrada + k*antiguo`).
- 0x521F3A-0x521F46: si el ped tiene tarea de arma (`0x600F30` != 0): `k = 0.9^dt` (0x8CC39C) para otro filtro paso bajo.

### Cuando el ped adopta el rumbo de la camara (0x522B69-0x522C48) [V logica; nombres de flags NV]
Al final de cada frame de apuntado (ped con `tipo==3` y `IsPlayer`):
```
wi = CWeaponInfo: de la tarea UseGun (0x600F70 -> task+0x30) o del arma activa (0x743C60(tipo, skill=1))
si (wi.flags>>1)&1:  ejecuta SOLO si ped.flags(+0x46C) & 0x4000000      // [NV] "bCanAimWithArm" y flag de ped de apuntado
si wi.fireType == 0 (cuerpo a cuerpo): no hace nada
si ped.flags(+0x46C) & 0x100 (p.ej. en vehiculo/estado especial): no hace nada
si arma activa == 41 (spray) :                     rumbo = atan2(-front.x, front.y) - [esp+0xC]
sino si ped tiene objetivo (ped+0x71C):            rumbo = atan2(-(obj.x-ped.x), obj.y-ped.y)       // hacia el objetivo, 0x40FE60 resta
sino si flag B6EC44 (apuntando con stick libre):   rumbo = atan2(-front.x, front.y) - [esp+0xC]
sino: no cambia
si rumbo > -100 (0x859014):  ped.rot_actual(+0x558) = ped.rot_objetivo(+0x55C) = rumbo - 0.05 (0x862F18)
                             -> 0x43E0C0 / 0x446F90 (actualiza matriz)
```
Es un snap instantaneo cada frame (sin suavizado) del rumbo del ped al de la camara menos 0.05 rad. Hay otra via (0x522641-0x5226B3) con memoria `8CC530` (-1001 inicial = "no valido"): si no hay objetivo y se pulsa disparo, el ped se orienta a `rot_actual - pi/2 (0x858FE4)`... `rot = memorizado + pi/2` [NV de detalle].

## 5. Identificaciones [V salvo marca]
- `DAT_00B72914` = `CGame::bMissionPackGame` (0 en juego normal; escrito en 0x440601, 0x4422D6, 0x53BD8C, 0x5BF880=0, load 0x5D204F; guardado en savegame 0x5D1DA0). En FollowPed (0x522DA8-0x522DD1) elige la FILA de la tabla de seguimiento a pie: `fila = (B72914 != 0)`; fila 0 = 0x8CC548 (juego base), **fila 1 = 0x8CC584 = variante para mission packs** (difiere: [4]=3 en vez de 4, [5]=0.9, [7]=1, [10]=0.3, [11]=0.05, [14]=0.7854; y usa `CCam+0xF0` en vez de `+0xEC` como offset de beta del nivel 2).
- `DAT_00B6F080` = `T+0x58`: flag "camara recien reiniciada/transicion" (ver seccion 1/4).
- `FUN_00601070` = `CPedIntelligence::GetTaskSwim`-like **[NV]**: devuelve la tarea de la ranura primaria si su tipo (`vtbl+0x10`) es 0x518; el WORD `task+0xA` es el estado de nado (4 = nado submarino a toda velocidad). Efecto en FollowPed (0x522EBF-0x522EE8): `[esp+38]=1.0` y, salvo estado 4, `[esp+44]=0.5` (0x862F34). El beta objetivo asociado se carga en 0x523A1B (`0x862F38` = -0.2618 rad = -15 grados).
- `FUN_00601110` = `GetTaskJetPack`-like **[NV]** (tipo 0x517, exige `IsPlayer` 0x5DF8F0): `[esp+38]=0.5` y, si el ped NO esta en vehiculo (`ped+0x46C` bit0 = 0), `[esp+44]=3.0` (0x862F3C). Beta objetivo en 0x523A01: `0x862F40` = -0.3491 rad = -20 grados.
- Con `[esp+38] != 0` (nado/jetpack) FollowPed fuerza el seguimiento de alpha hacia el ped aunque no haya movimiento (0x523638); `[esp+44]` pondera el retorno de beta (0x523AD5-0x523B3C, tope 4*`0x8CCE80`...).
- `FUN_006011B0(ped intel)`: devuelve true si el arma activa (slot `ped+0x718`, tipo en `ped+slot*0x1C+0x5A0`) es 46 (0x2E, paracaidas) y el ped no esta en vehiculo ni flag 0x100; en nivel de zoom 3, FollowPed duplica `fila[1]` (`0x8CC54C`, valor 2 -> 4; 0x522DF7-0x522E06) y lo suma al zoom (0x522E1C) -> **con paracaidas en zoom lejano la distancia base crece +2**.
- `DAT_0xB6EC54` byte: ped "en modo seguimiento libre", `DAT_0xB6EC50` float: zoom previo (FollowPed lo compara con el actual: si el zoom crece, `[esp+34]` toma el zoom actual).

## 8. Extras
### Mirar atras (CCam::LookBehind 0x520690) [V parcial]
- Aplica a modos 3/18/22 con vehiculo, 16 (1a persona) con vehiculo, o ped (tipo 3).
- Vehiculo: la camara se coloca DELANTE del vehiculo: `Source = target + dir * dist` con `dir = veh.forward (matrix+0x10)` con z += 0.2 (0x858CC4) sin renormalizar, `dist = CCam+0xD0` (distancia de FollowCar) si modo 18, 15.0 en otros modos; `target = vector guardado en B6F018/1C/20` (salida de FollowCar). Despues `CameraColDetAndReact`, `ImproveNearClip`.
- Ped: `dist = max(0.6 (0x858CC8), 2.0 (0x8CCE4C) + zoom_a_pie(B6F0FC))`; `dir` = adelante del ped normalizado con `z = 0.3 + ...` (0x858C24); `Source = pos_ped + dir*dist`; si nada de nivel de zoom: desplazamiento extra por nivel (tablas 0x8CCE1C: {0,1,1}, 0x8CCE28: {-1,-1,-1}, 0x8CCE34: {0.6,0.6,0.6}, 0x8CCE40: {0.6,0,0}; solo si hay tarea de nado `0x601070`). Fuerza flags de colision de camara (`8A5B14/15/17` = colisionar con vehiculos/objetos/peds) durante la colision. `LookRight` en 0x520E40.
### No resuelto
Camara cinematica al entrar al coche (modos 39-43, 46, 52), camara de sniper/rocket (modo 7/8/51), Process_FollowPedWithMouse: no se analizaron (sin tiempo). Simbolos utiles del PDB (`D:\proyectos\CppProjects\decompilaciones\gtasa-menu\docs\gta_sa_symbols.tsv`): `0x511B50 Process_Rocket`, `0x517EA0 Process_1stPerson`, `0x50EB70 Process_1rstPersonPedOnPC`, `0x5105C0 Process_M16_1stPerson`, `0x517500 Process_SpecialFixedForSyphon`, `0x51E560 TryToStartNewCamMode`, `0x526FC0 CCam::Process`, `0x52B730 CCamera::Process`, `0x50CD30/50CD80/50CDA0/50CDE0 Camera*SpecialCases`.

## Estado
**Resuelto** (con direcciones en cada seccion): 1 (zoom a pie: campos, niveles, valores -0.55/1.5/3.6, cambio con boton Select/DPad, suavizado lineal 0.12*dt), 2 (offsets de beta 0.0598/-0.1/0/-0.07), 3 (colision: tabla gCamColVars de 28 filas, algoritmo, nivel por defecto 5, quien lo cambia; 0.65 = radio minimo legitimo; DAT_008CCB84 = fraccion suavizada persistente, inicial 1.0), 4 (StartTransition tabla completa + curva coseno), 5 (identificaciones; las de 601070/601110 son [NV] por semantica), 6 (FollowCar: tabla de filas, auto-seguimiento de alpha, suavizado exp, contador B4; beta maxima solo parcial), 7 (stick de apuntar, bases pow 0.8/0.5/0.96/0.9, snap del rumbo del ped).
**No resuelto / NV**: geometria exacta del termino de beta maxima con la caja de colision (0x5252A0-0x52535C); funcion `0x50A0A0(v,4)` (suavizado del front); `0x51A5D0 ConeCastCollisionResolve` interior; bit0 de `C87AB8` (0x72DD20); `B70044` (bss); modo 8 extras (cinematicas de entrada al coche, sniper); mezcla de orientacion en transicion (slerp vs lerp).
**Siguientes pasos**:
1. Volcar todo el `.text` con objdump para greps: bytes `0x400..0x456400` del exe -> `objdump -D -b binary -mi386 -M intel --adjust-vma=0x401000`.
2. Beta maxima: `powershell -NoProfile -File tools
e\dis.ps1 -Va 0x5250DE -Len 0x290` y seguir las pilas x87 (referencias: 0x8CCED0/D4/D8, `GetColModel` 0x535300 campos +4 y +0xC).
3. Camara de sniper/rocket: `dis.ps1 -Va 0x511B50 -Len 0x600` (Process_Rocket) y `0x517EA0` (1stPerson); entrada de coche: buscar llamadas a `TryToStartNewCamMode` 0x51E560 y `TakeControl` 0x50C7C0 con modos 39..46.
4. Verificar en runtime el valor de B70044 y de ped zoom inicial (CCamera ctor deja `T+0xC8=0`).
