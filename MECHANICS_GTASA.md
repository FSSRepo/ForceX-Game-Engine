# Mecánicas de GTA San Andreas (jugador y cámara)

Especificación para replicarlas 1:1 en `examples/rpg-car`. Sale de la decompilación de
`D:\Games\GTA San Andreas\gta_sa.exe` (v1.0 PC, variante HOODLUM, base 0x400000) y de los datos del juego.
Versión 1; los pendientes se cierran en `docs/re/` y se fusionan aquí (ver `docs/PROGRESS.md`).

- **[V]** verificado en el exe o medido en los datos. **[NV]** no verificado: valor provisional o estimación.
- Detalle y direcciones: `docs/re/report_ped_onfoot_initial.md`, `docs/re/report_camera_initial.md` y los informes
  de la Fase 0 (`docs/re/ped_jump_fall_climb_duck.md`, `docs/re/car_enter_exit.md`, `docs/re/camera_pending.md`).

## 0. Convenciones

- Unidad de tiempo de SA: `dt = DAT_00b7cb5c`; **1.0 = 20 ms (1/50 s)**. Un "por frame" de SA son 50 por segundo.
  Para pasar `X por frame` a `por segundo` se multiplica por 50.
- Ángulos en radianes. Cámara: alpha = horizontal, beta = vertical (positivo = cámara más alta).
- Ejes de GTA: X derecha, Y adelante, Z arriba. `IFPReader` guarda la posición de los fotogramas como `(y, x, z)`, así
  que **el avance de una animación queda en `x`** de `Vector3f` (en `ped.ifp` el avance real es el eje Y del archivo).
- Animaciones: `D:\Games\GTA San Andreas\anim\ped.ifp` (ANP3; copia en `rpg-car/core/assets/game/ped.ifp`).
  Las de armas están en `anim.img` (`rifle.ifp`, `colt45.ifp`, `uzi.ifp`...; `rifle.ifp` ya extraído a assets).

## 1. Movimiento a pie

`CTaskSimplePlayerOnFoot::ProcessPed` (0x688810) → `PlayerControlZelda` (cuerpo real 0x6883d7).

| Regla | Detalle | |
|---|---|---|
| Entrada | `m = sqrt(x²+y²)/60`, con `x,y` ∈ [−128,128]; a fondo ≈ **2.1333** | [V] |
| Dirección | rumbo del stick − rumbo de cámara (`DAT_00b6f178`): **relativo a la cámara** | [V] |
| Ratio de movimiento | sigue a `m` con paso `0.07·dt` = **3.5 unidades/s**; con `m ≤ 0` baja a 0 de golpe | [V] |
| Estado | sprint (7) si se pulsa sprint y hay energía; si no, run (6) o walk (4) según el ratio | [V] sprint/run; umbral walk↔run [NV] (provisional: ratio > 1.0 = run) |
| Salto | tecla de salto, `ped+0xFC == 0`, estado ∉ {7, 8, 0x33, 0x22, 0x2d, 0x2e} → `CTaskComplexJump` | [V] |

Animación por estado (`CPed::SetMoveAnim` 0x5e4a00): solo se reselecciona al **cambiar de estado**.

| Estado | Anim ID | Animación (ped.ifp) | Blend delta (peso/s) |
|---|---|---|---|
| idle | 3 | `IDLE_stance` | 4.0 |
| giro izq. / dcha. | 0x89 / 0x8A | [NV] | 16.0 |
| walk | 0 | `WALK_player` | 1.0 |
| run | 1 | `run_player` | 1.0 (3.0 si `ped+0x530 == 9`) |
| sprint | 2 | `sprint_civi` | 1.0 |

Los nombres de ped.ifp son [NV] contra el exe (salen del conocimiento de SA), pero existen en `ped.ifp`.
Con arma equipada (juego "armed"): `IDLE_armed`, `WALK_armed`, `run_armed` [NV].

Velocidad de animación (`SetMoveAnimSpeed` 0x5dec10) [V]:
`speed = clamp(moveSpeed, −0.3, 0.3) + 1.2 − stat·0.4/32768`, con `stat` 0..65535 (por defecto ≈ 32768 → 1.0).
Si `ped+0x484 == 2`: `moveSpeed + 1.0` con valores fijos 0.7 y 1.3.

**Velocidades reales** (root motion medido en `ped.ifp` con `tools/re/IfpRootMotion.java`, primer→último fotograma
de la raíz, a velocidad de animación 1.0) [V]:

| Animación | Duración | Avance | Velocidad |
|---|---|---|---|
| `WALK_player` | 1.200 s | 1.867 m | **1.556 m/s** |
| `WALK_armed` | 1.067 s | 1.867 m | 1.750 m/s |
| `run_player` | 0.733 s | 4.185 m | **5.706 m/s** |
| `run_armed` | 0.700 s | 4.184 m | 5.977 m/s |
| `sprint_civi` | 0.533 s | 5.100 m | **9.562 m/s** |
| `Run_stop` | 0.867 s | 1.979 m | 2.29 m/s medio |
| `WALK_start` | 0.267 s | 0.471 m | 1.78 m/s |
| `JUMP_launch` | 0.200 s | 1.036 m (y) + 0.147 (z) | 5.18 m/s |
| `JUMP_glide` | 0.500 s | ≈ 0 | — |
| `JUMP_land` | 0.233 s | 0.903 m | 3.87 m/s |
| `FALL_land` | 0.467 s | 0.355 m | 0.76 m/s |

En SA el movimiento sale del root motion mezclado: la velocidad es la suma de las velocidades de cada animación
ponderada por su peso de blend. Por eso el personaje acelera y frena con los blend deltas (arranque ≈ 1 s a
delta 1.0; parada ≈ 0.25 s a delta 4.0). **Implementación**: `PedLocomotion` (velocidad por estado = root motion de la
animación; mezcla con el mismo peso que las poses).

Sprint y energía (0x60a610, tabla 0x8d2460, stride 0x20) [V] valores, [NV] semántica: ganancia 4.0/step pulsando
sprint; pérdida 0.7 al correr sin sprint y 0.2 en reposo; umbral 5.0; máximo 10.0; multiplicador 0.3 (CJ);
devuelve `(energía/5 − 1)·mult + 1`. La velocidad de sprint solo existe mientras ese valor ≥ 1.0.

**Grupo de animaciones del jugador** [V]: CJ usa el grupo `player` (`data/peds.ide` línea 42, no está en
`animgrp.dat` sino en la tabla del exe, cadenas en 0x85d8c0..; se guardan en orden inverso): `walk_player`,
`run_player`, `SPRINT_civi`, `IDLE_STANCE`, `roadcross`, `walk_start`. Los demás peatones usan grupos de
`animgrp.dat` (p. ej. `man`: `walk_civi` 1.52 m/s, `run_civi` 4.30 m/s, `sprint_panic` 5.90 m/s). **Las velocidades
de la tabla de arriba son las del grupo `player`**; ojo con informes que citen 1.52 / 4.30 / 5.90: son del grupo `man`.

**Giro del heading** [V] (`CPed::UpdateRotation` 0x5E4C50): máximo **9°/frame = 450°/s** para el jugador, tomado de
`data/pedstats.dat` (`STAT_PLAYER`, columna C "Heading change rate"), igual en walk, run y sprint; si el ángulo
restante cabe en el límite se alinea exacto (180° = 20 frames). Estados de giro 2/3 (anims 0x89/0x8A): [NV].

## 2. Salto, caída, aterrizaje, trepar, agacharse

Detalle y direcciones: `docs/re/ped_jump_fall_climb_duck.md`. Todo [V] salvo nota. IDs de animación
(`AnimationId = índice − 2` en el array de nombres de 0x8d1160): `JUMP_launch` 116, `JUMP_launch_R` 117,
`JUMP_glide` 118, `JUMP_land` 119, `FALL_fall` 120, `FALL_glide` 121, `FALL_land` 122, `FALL_collapse` 123,
`CLIMB_jump` 128, `CLIMB_idle` 129, `CLIMB_Pull` 130, `CLIMB_Stand` 131, `CLIMB_Stand_finish` 132, `WEAPON_crouch` 55.

**Salto**
- Cadena: `ComplexJump` → `SimpleJump` → (`HitHead` | `Climb` | `ComplexInAirAndLand` → `InAir` → `Land` / `Fall`).
- El botón crea `ComplexJump(0)`; requiere no estar en el aire, botón de agacharse suelto y ningún salto activo.
- **El impulso se aplica al terminar `JUMP_launch` (0.2 s)**, no al pulsar.
- Velocidad horizontal H (m por frame de 20 ms): 0.10 parado o andando, 0.10–0.17 corriendo, 0.17–0.22 en sprint,
  interpolando con el blend de la animación (×50 para m/s: 5 m/s parado, hasta 11 m/s en sprint).
- Impulso vertical: fuerza 8.5 (jugador) / 4.5 (NPC); `dV = F/masa`, masa del ped 70 → **Vz = 0.1214 m/frame = 6.07 m/s**.
- Gravedad de ped: `vz −= 0.008·dt` por frame (0.4 m/s² por frame ×50 → 20 m/s²). Apex ≈ 0.92 m; ≈ 30 frames (0.6 s) de aire a nivel.
- H y Vz se multiplican por `GetFatAndMuscleModifier(2)` ≈ 1.0 con stats por defecto (mínimo 0.8, [NV]).
- Con obstáculo delante H = 0 y se usa `CLIMB_jump`; si el test de techo choca, `HitHead`.

**Caída y aterrizaje**
- `FALL_fall` arranca con `vz ≤ −0.1` m/frame (5 m/s). El grito sale tras 500 ms en el aire con `vz < −0.4` (20 m/s).
- Si `FALL_fall` suena al aterrizar: `minVz ≤ −0.4` → `CTaskSimpleFall(KO_skid_back, 700 ms)`; si no, `Land` con `FALL_collapse`.
- Si no, `Land` con `JUMP_land` o `FALL_land` según el jugador y su stat. `Land` usa blend 100 (instantáneo);
  los pies caen en t = 0.1 s (derecho) y 0.2 s (izquierdo). Daño real de caída: **[NV]** (`CPedDamageResponseCalculator` 0x5E05B0).

**Trepar** [V parcial]
- 22 esferas de radio 0.4 en coordenadas locales del ped: y = 0.5/1.0/1.5 m delante, z = 0.4…2.9, más 4 pegadas al cuerpo.
- `TestForClimb` hace dos `ScanToGrab`; el segundo mira 0.4 m detrás del borde y, si encuentra algo, anula el climb.
- Rechazos por altura según el nivel de stat L y el flag de `Jump`: siempre se rechaza el nivel z = 2.9; z = 2.4 salvo
  (flag y L < 1); z = 1.9 salvo (flag y L < 2). La pared debe cumplir `dot(normal, front) > 0.3` (≈ 72°).
- Climb tipo 2 si `grabZ − pedZ ≤ 1.4`, tipo 1 si no. Secuencia: `CLIMB_jump` 0.567 s → `CLIMB_idle` (cuelgue, espera
  pulsar salto) → `CLIMB_Pull` 0.867 s (z +1.075) → `CLIMB_Stand` 0.8 s (z +0.943) → `CLIMB_Stand_finish` 0.2 s.
  El vault bajo usa `CLIMB_jump_B` (0.967 s). Offsets runtime en bss 0xC18F78/0xC18F7C: [NV].

**Agacharse**
- `DuckToggle` crea `CTaskSimpleDuck(type 4)` en el slot secundario 1. No se puede agachar en run/sprint (`moveState ≥ 6`).
- `WEAPON_crouch` (0.8 s) con blend 4.0; salir con stick > 0.5 pasa a walk (4) o run (6). Root motion: `GunCrouchFwd`
  3.74 m/s, `GunCrouchBwd` 1.87 m/s, `Crouch_Roll_*` ±2.8 m en 0.933 s. No se puede saltar con el botón de agacharse pulsado.

## 3. Subir y bajar del vehículo

Flujo de tareas [V] (`CTaskComplexEnterCar::CreateSubTask` 0x63e040, `CTaskComplexLeaveCar`):

1. Ir al punto de la puerta (`CTaskComplexGoToCarDoorAndStandStill`) y alinear (`CAR_align_LHS/RHS`, `CAR_alignHI_*`).
2. Abrir puerta desde fuera (`CAR_open_LHS/RHS`, 1.1 s). Puerta bloqueada: `CAR_doorlocked_*`.
3. Subir (`CAR_getin_LHS/RHS`, 1.0 s; `CAR_getinL_*` para coches bajos).
4. Poner al ped como conductor (`CTaskSimpleCarSetPedInAsDriver` 0x64b950).
5. Cerrar la puerta desde dentro (`CAR_closedoor_LHS/RHS`, 0.6 s / 0.4 s).
6. Conducir (`CAR_sit`, anims de volante).

Al bajar: `CAR_getout_*` (1.133 s) y cierre desde fuera (`CAR_close_LHS/RHS`, 0.733 s).

- **Punto de la puerta** (`CCarEnterExit::GetPositionToOpenCarDoor` 0x64e740) [V]: offsets locales de
  `CVehicleAnimGroup::ComputeAnimDoorOffsets` (0x6e3d10) = **último fotograma** de la animación (IDs 0x167, 0x169,
  0x16b, 0x175, 0x177, 0x17a, 0x17b); puertas 8/9 delanteras, 10/11 traseras; se transforma con la matriz del vehículo.
- **Puerta** [V] (`CVehicle::ProcessOpenDoor` 0x6d56c0; `CDoor::Open(ratio)`): el ángulo lo marca el **tiempo del clip
  del ped** `t` (segundos). Ratio de apertura entre un `start` y un `stop`:
  - open / getout: 0 antes de `start`, `(t−start)/(stop−start)` entre ambos (solo crece), 1 después.
  - closedoor / close: 1 antes de `start`, `1 − (t−start)/(stop−start)` (solo decrece), 0 después.
  - jacked / fallout: 0.1→0.4 con factor 3.333; rollout 0.01→0.1; rolldoor abre 0.05–0.2 y cierra 0.3–0.475.
  - Ángulo = `ratio × openAngle`: puertas delanteras **1.2566 rad (72°)** (π/2 con flag de handling); capó y maletero 0.9448 rad.
  - Antes de `start` de OpenOut la puerta decae: `(1 − t/start)·ratioInicial`.
- **Ventanas por grupo** (se leen de `data/handling.cfg`, líneas que empiezan por `^`; índice = columna `aj` de `tHandlingData+0xde`;
  parser `cHandlingDataMgr::LoadHandlingData` 0x5bd830; tabla `m_vehicleAnimGroups` 0xc1cdc0, stride 0x94). Grupo 0
  [V, verificado también contra el archivo]: `OpenOut` 0.41–0.80 s, `CloseIn` 0.30–0.45 s, `OpenIn` 0.06–0.43 s,
  `CloseOut` 0.20–0.43 s. Tiempos críticos del grupo 0: 0.5 / 0 / −0.5 / −0.3 / 0.3 (GetIn, JumpOut, GetOut, Jacked, Fall).
  `ComputeCriticalBlendTime` por animación: 0x15f–0x16b → GetIn (+0xc), getout 0x175–0x178 → +0x14, jacked 0x17a/b → +0x18,
  rollout 0x180/1 → +0x10, fallout 0x183/4 → +0x1c; el resto 0. Los tiempos críticos |Tc| ≥ 10 se codifican como Tc − 11.
- **Alineación** [V] (`CTaskUtilityLineUpPedWithCar` 0x6513a0): cada frame **fuerza** posición y rumbo del ped. Posición =
  punto de la puerta + root motion de la animación (último fotograma × progreso); rumbo = giro suave con
  `frac = (timestamp − ahora)/600`. El ped **no** sigue el root motion crudo. `GoToCarDoorAndStandStill` elige entre
  `SimpleGoToPoint` (0x384) y `MoveFollowPointRoute` (0x389) según colisión del punto con las cajas del coche (0x651210).
- **IDs de tarea** [V]: 0x320 GoToDoor, 0x321 Align, 0x322 Open, 0x323 OpenLocked, 0x324 PickUpBike, 0x325 CloseFromInside,
  0x326 CloseFromOutside, 0x327 GetIn, 0x328 Shuffle, 0x329 WaitToSlowDown, 0x32a WaitDoor, 0x32b/0x32c SetPedInAsPassenger/Driver,
  0x32d GetOut, 0x32e JumpOut, 0x32f ForcePedOut, 0x330 SetPedOut, 0x334 SlowDragPedOut, 0x2c0 LeaveCar, 0x2cf CarDriveTimed.
  `LeaveCar`: CarDriveTimed → elige puerta con espacio (0x6504c0) → GetOut → CloseDoorFromOutside → SetPedOut;
  JumpOut si no hay hueco o el coche está volcado.
- **Enganche al asiento** [V]: `GetIn` entra con blend 4.0; `GetOut` con 1000 (instantáneo). `SetPedInAsDriver` (0x64b950)
  engancha una sola vez (SetDriver, `bInVehicle`, quita la colisión del ped, crea `CTaskSimpleCarDrive`); después
  `CPed::SetPedPositionInCar` (0x5df910) fija posición = matriz del coche × `seatOffset` y rumbo = el del coche.
- **Animaciones al conducir** (grupo por defecto) [V]: 60 `CAR_sit`, 61 `CAR_Lsit`, 66–73 `Drive_L/R` (+LO/weak/pro),
  74–77 driveby, 78–80 `CAR_LB`, 81–84 barco, 91–94 camión, 95–98 kart. `AddInCarAnim` aplica blend 1000.
- **Offsets de puerta** (`ComputeAnimDoorOffsets` 0x6e3d10) = traslación del **último fotograma de la raíz** de `CAR_getin`
  (0x167/0x169/0x16b) o `CAR_getout` (0x175/0x177) o jacked (0x17a/b), cacheada. Valores numéricos: se leen de `ped.ifp`
  (`CAR_getin_LHS` → (0.951, 0.486, −0.375) m; `CAR_getout_LHS` → (−0.949, −0.489, 0.374) m). [NV] los de `seatPos`
  (salen de `vehicles.dff`).
- **Root motion de las animaciones del coche** (medido, [V]): `CAR_getin_LHS` desplaza al ped (0.951, 0.486, −0.375) m;
  `CAR_getout_LHS` (−0.949, −0.489, 0.374) m; `CAR_open_LHS`, `CAR_closedoor_*` y `CAR_close_*` ≈ 0.
- Distancias de activación, tolerancia de llegada e interpolación de alineación: pendiente (`docs/re/car_enter_exit.md`).

Estado actual en `rpg-car` (`Player.updateEntering/updateExiting/updateSeated`, `ramp()`): abrir → subir → cerrar desde
dentro → conducir; bajar (`CAR_getout`) → cerrar desde fuera (`CAR_close`, cortable moviéndose). La puerta sigue las
ventanas del grupo 0 de `handling.cfg` en segundos del clip y abre 72°. Pendiente: punto de la puerta desde el último
fotograma (offsets), alineación forzada por frame, puerta bloqueada, `CAR_getinL`/`CAR_align`, tiempo crítico.

## 4. Armas

`weapon.dat` (AK47, `data/weapon.dat` línea 114–116) [V]:
- grupo de animación `rifle` (animaciones en `rifle.ifp`: `RIFLE_fire`, `RIFLE_fire_poor`, `RIFLE_load`,
  `RIFLE_crouchfire`, `RIFLE_crouchload`).
- `6 10 7`: fotogramas a 30 fps del **inicio del bucle (6), fin del bucle (10) y fotograma del disparo (7)** → bucle
  de 0.2 a 0.333 s, disparo en 0.233 s; cadencia ≈ 7.5 tiros/s. Segunda animación igual; breakout 99.
- desplazamiento de disparo `(0.78, −0.06, 0.13)`.

Arma en la mano [V] (`CVisibilityPlugins::RenderWeaponPedsForPC` 0x732f30): el modelo toma **tal cual** la matriz del
hueso de la mano derecha (id 24; paracaídas hueso 3) sin rotación ni offset adicional. Si lleva dos armas
(flag 0x800), la segunda va en el hueso 34 (mano izquierda), girada 180°.

## 5. Cámara

Tablas y fórmulas completas en `docs/re/report_camera_initial.md`. Resumen:

### 5.1 A pie (`CCam::Process_FollowPed_SA` 0x522D4C; tabla 0x8CC548, fila 0) [V]
Distancia = `zoom + 2.0`; punto de mira = cabeza + 0.6 m; beta ∈ [−85°, +45°] (baja a 20° si el ped corre hacia la
cámara); inercia angular `0.8^dt`; velocidad angular máx. 0.1 rad/frame; paso máx. de beta `0.1·dt`; auto-seguimiento
de alpha con ganancia 0.1 y paso máx. `0.02·dt` (solo con velocidad del ped > 0; suspendido con entrada manual);
ratón `Δα = mouseX·(−2.5)·(FOV·0.0125)·sens`; FOV fijo 70°. Colisión con esfera de barrido, recuperación gradual
(+0.05/frame hacia 1.0, salto inmediato al impacto). Zoom (cercano/medio/lejano) y offsets de beta por zoom: [NV].

### 5.2 En vehículo (`Process_FollowCar_SA` 0x5245B0; tabla 0x8CC600) [V]
Coche: altura = 1.3·bbox − 0.4; distancia = zoom + 1.0 + longitud (tope 15); suavidad de beta `1 − 0.5^dt`; inercia
angular 0.85; beta ∈ [−89°, +45°]; alpha sigue la **dirección de la velocidad** (no el morro) si v > ~1 m/s; la entrada
manual suspende el auto-seguimiento 1.0 s; zoom cercano/medio/lejano −1.0 / 1.0 / 6.0; FOV 70° que sube
`(v_fwd − 0.4)·dt` hasta 100° y decae con `0.98^dt`.

### 5.3 Apuntar (`Process_AimWeapon` 0x521500; tabla 0x8CC4C0) [V]
Tipo 0: distancia base 1.0, amplitud 1.6, beta inicial −0.12; hombro **0.2 m**; FOV **50°** con AK-47/M4 y 35° con
rifle de caza; el ped adopta el rumbo de la cámara; tras 2 s sin entrada la cámara se realinea con el ped.

### 5.4 Transiciones (`StartTransition` ≈0x5155xx; `TakeControl` 0x50C7C0) [V parcial]
1350 ms por defecto; entre modos de ped 350 ms (0.1/0.9); a coche 750 ms (0.1/0.9); coche → ped 800 ms (0.02/0.98);
vistas en vehículo → seguimiento: corte seco. Curva de interpolación: [NV] (en el motor: `CameraBlender`, suavizado
por defecto `SMOOTHSTEP`).

## 6. Mapa a código (`rpg-car` y motor)

| Mecánica | Código |
|---|---|
| Locomoción a pie | `rpg-car/.../PedLocomotion.java`, `Player.java` |
| Blend, eventos, root motion | `forcex/.../anim/Animator.java`, `Animation.java`, `Bone.java`, `AnimationEventListener.java` |
| Transición de cámara | `forcex/.../gfx3d/CameraBlender.java` |
| Cámaras (pendiente) | `rpg-car/.../cam/` (`CameraManager`, `FollowPedCam`, `FollowCarCam`, `AimCam`) |
| Arma en la mano | `Weapon.java` (`personaje × hueso de la mano`) |
| Medir root motion | `tools/re/IfpRootMotion.java` |
| Desensamblar el exe | `tools/re/dis.ps1` |
