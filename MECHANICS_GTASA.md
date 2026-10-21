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

Giro del heading: [NV] (provisional 480°/s). Estados de giro 2/3 (anims 0x89/0x8A): [NV].

## 2. Salto, caída, aterrizaje, trepar, agacharse

Pendiente de la Fase 0 (`docs/re/ped_jump_fall_climb_duck.md`). Lo verificado:
- Lanzamiento del salto: ver tabla de la sección 1.
- `CTaskSimpleClimb::TestForClimb` (0x6803a0): dos tests de línea de visión; el segundo se desplaza 0.4 m hacia atrás
  a lo largo del ángulo de la pared y un offset Z global [V].
- Animaciones existentes en `ped.ifp`: `JUMP_launch`, `JUMP_launch_R`, `JUMP_glide`, `JUMP_land`, `FALL_fall`,
  `FALL_land`, `FALL_glide`, `FALL_front`, `FALL_back`, `FALL_collapse`, `FALL_skyDive`.

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
- **Puerta** [V]: el ángulo lo marca el **tiempo de la animación del ped** (`CTaskSimpleCarOpenDoorFromOutside::ProcessPed`
  0x64d710 → `vehicle->ProcessOpenDoor`). Antes del tiempo crítico (`CVehicleAnimGroup::ComputeCriticalBlendTime`
  0x6e3c80) la puerta se mantiene/decae; después sigue el tiempo de la animación. Valores de tiempo crítico: [NV].
- **Root motion de las animaciones del coche** (medido, [V]): `CAR_getin_LHS` desplaza al ped (0.951, 0.486, −0.375) m;
  `CAR_getout_LHS` (−0.949, −0.489, 0.374) m; `CAR_open_LHS`, `CAR_closedoor_*` y `CAR_close_*` ≈ 0.
- Distancias de activación, tolerancia de llegada e interpolación de alineación: pendiente (`docs/re/car_enter_exit.md`).

Estado actual en `rpg-car`: abrir → subir → cerrar desde dentro → conducir, y bajar con la puerta ligada a la fase
de la animación (`Player.updateEntering/updateExiting/updateSeated`, `ramp()`). Falta el cierre desde fuera al bajar.

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
