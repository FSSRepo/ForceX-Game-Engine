# Informe inicial: cámara (gta_sa.exe v1.0 HOODLUM)

Origen: primer agente de cámara (solo lectura; pseudocódigo de Ghidra, `read_bytes` y desensamblado propio con
capstone). Los pendientes se cierran en `camera_pending.md`. **[V]** = leído, **[NV]** = no verificado.

Unidad de tiempo: `dt = DAT_00b7cb5c`, **1.0 = 20 ms (1/50 s)**. Ángulos en radianes. `TheCamera` = 0xB6F028.
`CCam` = objeto por cámara. alpha = ángulo horizontal, beta = ángulo vertical (positivo = cámara más alta).

## 0. Funciones (las direcciones sugeridas no coinciden en todo)

| Dirección | Función |
|---|---|
| 0x522D4C | `CCam::Process_FollowPed_SA` (Ghidra lo define aquí; 0x5231A0 cae a mitad del cuerpo) |
| 0x5245B0–0x525E48 | `Process_FollowCar_SA` (CAM_ON_A_STRING, modo 18). 0x524AE0 es interior. Ghidra no lo desensambla (`jmp 0x407e47`): leer con dis.ps1 |
| 0x521500–0x522C80 | `Process_AimWeapon` (modo 53 por inferencia, NV) |
| 0x520690 | gestor de los modos 3, 18, 22 y 16 con vehículo y ped como entidad (BehindCar/boat; papel exacto NV) |
| 0x520190 | colisión de cámara |
| 0x516B20 | plano de recorte cercano (near clip) según la colisión |
| 0x50C7C0 | `TakeControl` |
| ≈0x5155xx | `StartTransition` (inicio no localizado). Constantes en 0x51567A–0x515847 |
| 0x50C160 / 0x50C1B2 | zoom por script (peatón / vehículo) |
| 0x50A3B0 / 0x50A480 | `InitCameraVehicleTweaks` / `ApplyVehicleCameraTweaks` |

## 1. Seguimiento a pie (0x522D4C)

Tabla de parámetros en **0x8CC548**, stride 0x3C. La fila se elige con `DAT_00b72914 != 0` (significado NV).
Fila 0 **[V]**:

| Campo | Valor | Uso |
|---|---|---|
| +0x00 | 0.6 | altura del punto de mira: pos. ped + 0.6 m en Z |
| +0x04 | 2.0 | se suma al zoom actual |
| +0x08 | 0.15 | base del offset de beta |
| +0x0C | 2.0 | distancia mínima (comparada con la distancia final) |
| +0x10 | 4.0 | distancia lejana mínima para la fuente secundaria (rol parcial, NV) |
| +0x18 | 0.1 | paso máximo de beta, × dt |
| +0x20 | 0.8 | inercia de la velocidad de alpha y beta: factor `0.8^dt` por frame |
| +0x24 | 0.1 | límite de la velocidad angular (rad/frame) |
| +0x28 | 0.1 | ganancia de auto-seguimiento de alpha |
| +0x2C | 0.02 | paso máximo de auto-seguimiento (rad por dt) |
| +0x34 | 0.7854 | beta máxima (45°) |
| +0x38 | 1.48353 | beta mínima (−85°) |

Fila 1 (0x8CC584) difiere: distancia lejana 3.0; auto-seguimiento 0.3 / 0.05; beta mínima 0.7854.

- **Distancia**: `dist = zoom(0xB6F0FC) + 2.0` (se duplica en un caso concreto, flags NV). Zoom por script
  (`SetZoomValueFollowPedScript`) fija +0xD8 en 0.25, 1.5 o 2.9 (0x50C160). Quién escribe los zooms
  cercano/medio/lejano (0xB6F0F4/F8) y con qué valores: **[NV]**.
- **Beta**: límites [−1.48353, +0.7854]. Posición = `target − dist·(cosβ·cosα, cosβ·sinα, sinβ)`.
  Beta en reposo: `−(0.15 + offset del CCam por modo de zoom)` (offsets CCam+0xE8/EC/F0/F4, valor NV).
  Offsets adicionales: −0.349 rad (−20°) si `FUN_00601110() != 0`; −0.262 rad (−15°) si `FUN_00601070() != 0`
  (flags del ped no identificados). Si el ped corre hacia la cámara (Δ > 90° y velocidad² > 0.002), el límite de
  |β| baja de 90° a 20°: `1.5708 − (1.5708 − 0.349)·min(1, (|Δ| − 90°)·1.2/90°)`.
- **Auto-seguimiento (alpha)**: objetivo = rumbo del ped − π/2. Solo si |Δα| < 2.9645 rad (≈170°) o bandera de
  forzado. Giro por frame `Δ·min(1, |v_ped|·dt·0.1)`, limitado a ±dt·0.02. Mientras `DAT_00b6ec54` (retorno forzado
  detrás, tras mirar atrás) la tasa se multiplica por 0.5 (tope 1.0) y el paso máximo por 2.0. Sin velocidad del
  ped no hay seguimiento.
- **Control manual**: stick `rate = (FOV·0.0125)·K·|x|·x·0.007²` (eje X usa `0.0714`, eje Y `0.0429`). Ratón (anula
  la inercia): `Δα = mouseX·(−2.5)·(FOV·0.0125)·sensX`, `Δβ = mouseY·(+2.5)·(FOV·0.0125)·sensY`. Al soltar el
  stick la velocidad decae con `0.8^dt`; por debajo de 1e-4 → 0. Con ratón no hay recentrado automático de beta (NV).
- **Colisión (0x520190)**: esfera de barrido (`FUN_0051A5D0`) del objetivo a la cámara. Radio
  `dist · b6fe88[0] · 0.2939`, limitado por `b6fe88[1]` (`b6fe88` = puntero a tabla de runtime, NV). Suelo del
  radio 0.65 (parece alto, NV). Margen del barrido: `0.18/dist` (nivel < 4), `0.3/dist` (4–9) o el valor de la
  tabla (≥ 10); nivel = `DAT_008ccb80` (valor en juego NV). Con impacto, la fracción (`DAT_008ccb84`) salta al
  valor del impacto; sin impacto vuelve a 1.0 gradualmente: `(hit − frac)·dt·b6fe88[5]`, máx +0.05 por frame, solo
  si la cámara se movió > 0.01 m. Moto/bici (`[0x590]==9`): paso 0.05. Near clip típico 0.05 (0x3D4CCCCD) vía
  `FUN_007EE1D0`.
- **FOV a pie [V]**: 70° por defecto. Si `DAT_00b6f080 == 0`, el FOV se acerca a 70 a ±1.0° por dt; si no, se fija
  en 70. No varía con la velocidad.

## 2. Seguimiento en vehículo (0x5245B0)

Categoría de vehículo (índice): 0 coche (por defecto); 1 moto/bici/quad (`[0x590]==9` o tipo 2); 2 tipo 3 (heli);
3 tipo 4 (avión; 2 si modelo 0x208 con contador sobre umbral); 4 tipo 5 (barco); 5 modelos 441, 464, 564, 594 (RC);
6 modelos 465, 501 (RC). La asociación exacta con tipos de vehículo es NV.

Tabla de parámetros en **0x8CC600**, stride 0x3C. Coche (fila 0), con otras filas entre paréntesis:

| Campo | Coche | Notas |
|---|---|---|
| +0x00 | 1.3 | multiplicador de la altura del bounding box (otras: 1.1) |
| +0x04 | 1.0 | distancia base (avión: 3.5) |
| +0x08 | 0.4 | se resta de la altura del objetivo (moto 0.1, otras 0.2) |
| +0x0C | 10.0 | todas las filas |
| +0x10 | 15.0 | distancia máxima (moto 11, avión 25, RC 5) |
| +0x14 | 0.5 | base de la suavidad de beta: `1 − 0.5^dt` por frame (heli 0.05) |
| +0x18 | 1.0 | tope del paso de beta por dt |
| +0x20 | 0.85 | inercia de velocidad angular (avión y barco 0.9; heli 0.9; otros 0.75) |
| +0x24 | 0.2 | velocidad angular máxima (otros 0.05–0.1) |
| +0x28 | 0.075 | ganancia de auto-seguimiento de alpha |
| +0x2C | 0.05 | paso máximo de alpha × dt |
| +0x30 | 0.8 | multiplica la entrada del stick/ratón (moto 0.75; heli, avión y barco 1.0) |
| +0x34 | 0.7854 | beta máxima (heli 0.1745, avión 1.5533, barco 0.3491, RC 0.7854 o 0.3491) |
| +0x38 | 1.5533 | beta mínima (heli y barco 1.2217) |

- **Distancia**: `dist = zoom_suavizado(TheCamera+0xC0) + fila[+0x04] + tweak·L`, tope `fila[+0x10]`. `L` = longitud
  del vehículo = 2·|bbox.min.y| (+ término de remolque, NV). Tweaks por defecto: 1.0 (0xB6FD70, `TheCamera+0xD48`),
  1.0 (0xB6FD74, multiplica Z del objetivo), 0.0 (0xB6FD78, offset aditivo de beta). Modelo 501: offset de beta
  0.179. Vista media (`b6f0dc`==2) en coche/moto: el máximo × 0.65.
- **Zoom por tipo** (tablas 0x8CC3E0 / 0x8CC3F4 / 0x8CC408; orden coche, moto, heli, avión, barco):

  | Nivel | Valores |
  |---|---|
  | Cercano (0) | −1.0, −0.2, −3.2, 0.05, −2.41 |
  | Medio (1) | 1.0, 1.4, 0.65, 1.9, 6.49 |
  | Lejano (2) | 6.0, 6.0, 15.9, 15.9, 15.0 |

  Se escriben en `TheCamera+0xC4` y se suavizan hacia `+0xC0` (suavización NV).
- **Objetivo**: centro del vehículo + `altura_bbox·f0 − f2` en Z (≈1.42 m para un coche de 1.4; estimación).
  Offset de beta extra: `0.3/(dist + z)·z`.
- **Offsets de beta por modo y tipo** (0x8CC41C, 0x8CC430, 0x8CC444): modo 1 = 0.08, 0.08, 0.15, 0.08, 0.08;
  modo 2 = 0.07, 0.08, 0.3, 0.08, 0.08; modo 3 = 0.055, 0.05, 0.15, 0.06, 0.08. El zoom medio también usa el
  array del modo 2. Orden por tipo NV.
- **Beta**: `β += clamp((β_obj − β)·(1 − f5^dt), ±dt·f6)`, límites [−f14, +f13].
- **Alpha**: objetivo = rumbo del vector **velocidad** − π/2, solo si la velocidad XY > 0.02 (≈1 m/s). Marcha atrás
  y derrape: el objetivo sigue la velocidad, no el morro (adaptación en marcha atrás NV; hay un producto escalar
  morro/cámara que modifica la beta máxima, sin interpretar). Velocidad angular
  `ω = (1 − f8^dt)·(entrada + auto) + f8^dt·ω_anterior`, `f8 = 0.85` en coche, recortada a ±f9 y 0 si |ω| < 1e-4.
  Auto-seguimiento: Δ/`max(dt,1)` · `dt·0.075`, recortado a ±dt·0.05 (fórmula completa con factores de velocidad
  lateral: aproximada, NV). Entrada manual arma un temporizador `0xB70118` = 50.0 (1.0 s): durante ese tiempo no hay
  auto-seguimiento.
- **Entrada manual**: stick `(FOV·0.0125)·0.0714·|x|·x·0.007²` (Y: 0.0429); ratón `mouseX·(−2.0)·(FOV·0.0125)·sens`
  (Y: doble de `b73428`). Avión/heli leen flags distintos (0xC1CC02 / 0xC1CC03), NV.
- **FOV (solo coches y motos) [V]**: base 70°; si la velocidad hacia delante > 0.4 (≈ 20 m/s), el FOV sube
  `(v_fwd − 0.4)·dt` por frame; tope 100° (0x42C80000); al bajar la velocidad decae `70 + (fov − 70)·0.98^dt`.
  Otros vehículos: 70°.
- Colisión: la misma rutina 0x520190 + 0x516B20 (near clip) + 0x50CC50 (modo de zoom).

## 3. Apuntar con arma (0x521500)

Tipo de configuración (`local_50`; significado de los dos primeros NV): 0 por defecto; 1 si `FUN_00601110() != 0`;
2 en vehículo; 3 si `FUN_0073B1C0` verdadero (NV). Tabla **0x8CC4C0**, stride 0x1C **[V]**:

| Tipo | Dist. base | Amplitud | Factor | Beta inicial | Z sobre cabeza | Beta máx | Beta mín |
|---|---|---|---|---|---|---|---|
| 0 | 1.0 | 1.6 | 1.0 | −0.12 | 0.0 | 0.7854 | 1.5533 |
| 1 | 3.5 | 0.7 | 1.0 | −0.16 | 0.2 | 0.6109 | 1.2217 |
| 2 | 6.0 | 0.7 | 1.0 | −0.16 | 0.4 | 0.6109 | 1.2217 |
| 3 | 2.5 | 0.7 | 1.0 | −0.12 | 0.15 | 0.7854 | 0.7941 |

- **Posición**: `dist = base + amp·cos(β)` si β ≤ 0; para β > 0 `cos(factor·β)` con tope 1.5708. Objetivo: cabeza + 0.5 m
  + "Z sobre la cabeza"; con FOV < 70 se suma `0.1·(70 − FOV)/(70 − FOVzoom)`. Desplazamiento lateral (hombro) a lo
  largo del eje derecho del ped: **0.2 m** (0x3E4CCCCD), 0.3 si `FUN_005A7970(1)` y el flag de arma coinciden.
  Offset del punto de mira: 20° horizontal y 3° vertical sin arma a distancia; con arma sale del retículo
  (`DAT_00b6ec10/14`): `atan((cx − 0.5)·2·tan(FOV/2))` y la vertical análoga entre el aspecto. Con lock-on el offset
  horizontal tiende a 0.
- **FOV**: 70° por defecto; armas ID 30 y 31 (AK-47, M4): **50°** (`DAT_008CC4B4`); arma 33 (rifle de caza): 35°
  (`DAT_008CC4B8`); otras 70°. Se acerca al objetivo a 1.0°/dt. El sniper es otro modo (no leído).
- **Entrada**: ratón sin inercia: `Δα = mouseX·(−2.5)·(FOV·0.0125)·sensX`, `Δβ = mouseY·(+4.0)·(FOV·0.0125)·sensY`.
  Stick con las fórmulas cuadráticas de a pie, suavizado con `pow(base, dt)` (base de `FUN_00822130`, NV). Límite de
  giro hacia el objetivo (`DAT_008CC4A4`) 0.1 rad/dt; seguimiento del rumbo del ped 0.1·dt (`DAT_008CC4A8`), × 0.25
  en vehículo (`DAT_008CC4AC`).
- **Rotación del ped respecto a la cámara**: `ped+0x558 = atan2(−dirx, diry) − offset_horizontal − 0.05` (y `+0x55C`),
  salvo sniper, vehículo, etc. (condiciones NV). La cámara sigue el rumbo del ped sin entrada manual
  (`DAT_00B6EC44 = 1` con entrada → cámara libre). Tras 2000 ms (`DAT_008CC538`) sin entrada, y hasta 5000 ms
  (`DAT_008CC534`), la cámara se realinea al rumbo del ped si la diferencia es < 30° (0.5236 rad).
- Near clip: baja a 0.05 al apuntar a pie.

## 4. Transiciones y `TakeControl`

`TakeControl(entidad, modo, tipoSwitch, …)` (0x50C7C0) **[V]**: con `tipoSwitch == 2` (interpolación) se admite
aunque `[+0xCF0]` sea 0; guarda tipo en `TheCamera+0xB0`, entidad en `+0x958`, modo en `+0xC38`; 2º argumento en
`+0xC3C` (NV); `+0x2B = 0`, `+0x32 = 1`. Sin modo: ped → 4 (seguir ped), vehículo → 18. Termina llamando 0x56E210(-1).

`StartTransition` (constantes 0x51567A–0x515847; modos 4 = seguir ped, 18 = coche, 53 = apuntar). Duración por
defecto 1350 ms (`+0x7C = 0x546`); fracciones (`+0xC10` / `+0xC14`) = `m_fFractionInterToStopMoving` / `…CatchUp`:

| Caso | Duración | Fracciones |
|---|---|---|
| Por defecto | 1350 ms | no se sobrescriben (valores previos NV) |
| Entre modos de ped (4, 28, 11, 35, 53) | 350 ms | 0.1 / 0.9 |
| Nuevo = 18 (coche) desde 28, 4, 11, 35 o 53 | 750 ms | 0.1 / 0.9 |
| Nuevo = 11 desde 28 | 1800 ms | NV |
| Nuevo = 4 desde 18, con flag `[+0x27]` | 800 ms | 0.02 / 0.98 |
| Nuevo = 18 o 22 desde 39, 40, 41, 42, 43, 46 o 52 (vistas en vehículo) | 1 ms (corte seco) | 0 / 1 |

Modos 35 (0x23) y 53 (0x35) tienen casos propios (datos en 0x8CCCC4 y 0xB70044): NV.

## 5. Otros

Cinemática al entrar al coche y "mirar atrás": no investigados. Los modos 39, 40, 41, 42, 43, 46 y 52 hacen corte
seco al volver a 18/22. Modelos 441, 464, 564, 594, 465 y 501 (RC) tienen filas propias (índices 5 y 6).

## Resumen de lo no verificado

zoom a pie en 0xB6F0F4/F8/FC; bases de `pow` del suavizado del stick al apuntar; fórmula exacta del auto-seguimiento
de alpha en coche y comportamiento en marcha atrás/derrape; offsets de beta por zoom a pie (CCam+0xE8..0xF4);
`DAT_008CCB80` y tabla `0xb6fe88`; fracciones de transición por defecto y casos 35/53; qué identifican
`FUN_00601070`, `FUN_00601110` y `DAT_00B72914`.
