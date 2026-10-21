# Replicar 1:1 las mecánicas de jugador y cámara de GTA SA en `rpg-car`

## Context

El jugador de `examples/rpg-car` hoy se mueve con constantes inventadas (`WALK_SPEED` 3, `RUN_SPEED` 6.5, `TURN_SPEED` 480, cámara a 14 m...) y el motor ForceX no puede reproducir lo que hace GTA SA: el `Animator` no mezcla animaciones, no tiene eventos ni tiempo normalizado, `IFPReader` descarta el root motion, y la cámara cambia de golpe entre a pie y coche. Se decompiló `D:\Games\GTA San Andreas\gta_sa.exe` (v1.0, REA/Ghidra + desensamblado) para sacar las reglas reales. El resultado:

- Entregable 1: `MECHANICS_GTASA.md` en la raíz del repo, con las mecánicas, fórmulas y constantes, cada dato marcado **[V]** verificado en el exe o **[NV]** no verificado.
- Entregable 2: la implementación en `rpg-car` y en el motor, por fases.

Decisiones del usuario: lo no verificado se cierra en una **Fase 0 de ingeniería inversa** antes de implementar, y el alcance del motor es **blend de animaciones + eventos + controlador de cámara con transiciones** (sin máscara de huesos, sin ANPK).

Antes de empezar: el árbol tiene cambios sin commit (`Player.java`, `RPGCarGame.java`, `controls.yaml`, `Weapon.java`, `AN-94/`, `rifle.ifp`, `DesktopLauncher.java`). Hacer un commit del estado actual para poder comparar.

## Hallazgos verificados que guían el diseño

Unidad de tiempo de SA: `dt = DAT_00b7cb5c`, **1.0 = 20 ms (1/50 s)**. Los ángulos están en radianes.

**A pie** (`CTaskSimplePlayerOnFoot::PlayerControlZelda`, 0x6883d7):
- `m = sqrt(x²+y²)/60` con `x,y` de `CPad::GetPedWalkLeftRight/UpDown` (short, ±128); `m` se limita a 1.0.
- Rumbo objetivo = `atan2` del stick menos el rumbo de cámara (`DAT_00b6f178`): el movimiento es relativo a la cámara.
- El ratio de movimiento (`playerData+0x14`) sube o baja hacia `m` a `0.07·dt` por frame, es decir **3.5 unidades/s** (de 0 a 1 en ~0.29 s). Con `m <= 0` el ratio se pone a 0 de golpe.
- Estado de movimiento (`ped+0x534`): sprint = 7 si la energía de sprint ≥ 1.0 y se pulsa sprint; si no, 6 (run) si se pulsa correr; el resto idle o walk.
- Animación por estado (`CPed::SetMoveAnim`, 0x5e4a00), solo se vuelve a elegir al cambiar de estado: idle blend 4.0, giro izq. y dcha. 16.0, walk 1.0, run 1.0 (3.0 si `ped+0x530 == 9`), sprint 1.0.
- Velocidad de animación (`SetMoveAnimSpeed`, 0x5dec10): `speed = clamp(moveSpeed, -0.3, 0.3) + 1.2 - stat·0.4/32768`, con `stat` de 0..65535 (gordura o músculo).
- Energía de sprint (0x60a610, tabla en 0x8d2460, stride 0x20): ganancia 4.0/step con sprint, pérdida 0.7 al correr sin sprint, 0.2 en reposo, umbral 5.0, máximo 10.0, multiplicador 0.3 (CJ); devuelve `(energía/5 - 1)·mult + 1`.
- Salto: lo lanza `CTaskComplexJump` (0x67a030) con la tecla de salto y estado distinto de 7, 8, 0x33, 0x22, 0x2d, 0x2e. El resto **[NV]**.

**Subir al coche** (`CCarEnterExit`):
- `GetPositionToOpenCarDoor` (0x64e740) calcula el punto de la puerta con `CVehicleAnimGroup::ComputeAnimDoorOffsets` (0x6e3d10), leyendo el último fotograma de la animación (IDs 0x167, 0x169, 0x16b, 0x175, 0x177, 0x17a, 0x17b).
- `ComputeCriticalBlendTime` (0x6e3c80) devuelve el tiempo crítico por ID de animación (0x15f..0x16b, 0x175..0x178, 0x17a/b, 0x180/1, 0x183/4). Los valores salen del grupo de animación del vehículo, y no se leyeron.
- Orden de tareas: align → abrir puerta (`CAR_open`) → subir (`CAR_getin`) → poner al ped en el asiento → cerrar desde dentro (`CAR_closedoor`) → conducir. Al bajar: `CAR_getout` → cerrar desde fuera (`CAR_close`).
- La puerta se mueve según el tiempo de la animación del ped: antes del tiempo crítico se mantiene o decae hacia 0; después sigue el tiempo de la animación (`ProcessOpenDoor`).

**Cámara a pie** (`CCam::Process_FollowPed_SA`, 0x522D4C):
- Distancia = `zoom + 2.0`. Altura del punto de mira = cabeza + 0.6 m.
- Beta máxima 45° y mínima −85° (cae a 20° si el ped corre hacia la cámara).
- Inercia angular `0.8^dt`, velocidad angular máxima 0.1 rad/frame, paso máximo de beta 0.1·dt.
- Auto-seguimiento de alpha: ganancia 0.1, paso máximo 0.02·dt. Solo actúa con velocidad del ped > 0, y se suspende con entrada manual.
- Ratón: `Δα = mouseX·(−2.5)·(FOV·0.0125)·sens`. Colisión por esfera de barrido con radio `dist·0.2939·...`. FOV fijo 70°.

**Cámara en coche** (`Process_FollowCar_SA`, 0x5245B0):
- Altura = 1.3·bbox menos 0.4. Distancia base 1.0 + zoom + longitud, con tope 15.
- Suavidad de beta `1 − 0.5^dt`, inercia angular 0.85. Beta de −89° a +45°.
- Alpha sigue la dirección de la velocidad (no del morro) si la velocidad es > ~1 m/s.
- Entrada manual: suspende el auto-seguimiento 1.0 s (`0xB70118 = 50`).
- FOV: 70° base, sube `(v_fwd − 0.4)·dt` por frame hasta 100°, y decae con `0.98^dt`.
- Zoom cercano / medio / lejano para coche: −1.0 / 1.0 / 6.0.

**Apuntar** (`Process_AimWeapon`, 0x521500): tabla de tipos, tipo 0 = distancia base 1.0, amplitud 1.6, beta inicial −0.12; desplazamiento de hombro 0.2 m; FOV 50° para AK-47/M4 y 35° para el rifle de caza. Con arma de fuego el ped adopta el rumbo de la cámara.

**Transiciones** (`StartTransition`, ≈0x5155xx): 1350 ms por defecto; entre modos de ped 350 ms (0.1/0.9); a coche 750 ms; coche → ped 800 ms (0.02/0.98); modos de vista en vehículo → seguimiento: corte seco (1 ms).

## Fase 0 — Cerrar lo pendiente en el exe

Ghidra no define muchas funciones (parches `push -1; jmp 0x4xxxxx` al inicio; el cuerpo real empieza 7 bytes después). Método: leer los bytes del exe con PowerShell, mapear `raw = 0x400 + (VA − 0x401000)` para el primer `.text`, y desensamblar con `C:\w64devkit\bin\objdump.exe -D -b binary -mi386 -M intel --adjust-vma=<VA>`. Dejar los scripts en `tools/re/` y las salidas en `docs/re/`.

Objetivos, por prioridad:
1. Salto, caída, aterrizaje: `CTaskSimpleJump::ProcessPed` 0x680c60, `CTaskComplexJump` 0x67fc00/0x67fd10, `CTaskSimpleFall` 0x67faf0, `CTaskSimpleLand` 0x67d380 (impulsos, alturas, animaciones `JUMP_*`, `FALL_*`).
2. Trepar: `CTaskSimpleClimb::ScanToGrab` 0x67fd30 y `ScanToGrabSectorList` 0x67de10 (alturas de vault y escalada).
3. Coche: `CTaskUtilityLineUpPedWithCar` 0x6513a0, `CTaskSimpleCarAlign` 0x64d6d0, `CTaskSimpleCarGetOut` 0x64dc70, `CTaskSimpleCarSetPedInAsDriver` 0x64b950, `CVehicleAnimGroup` (valores de tiempo crítico y offsets de puerta), distancias de activación y animaciones de conducción.
4. Cámara: zoom a pie (quién escribe 0xB6F0F4/F8/FC), offsets de beta `CCam+0xE8..0xF4`, nivel de colisión `DAT_008ccb80` y tabla `0xb6fe88`, casos 35 y 53 de `StartTransition`, y qué son `FUN_00601070`, `FUN_00601110` y `DAT_00b72914`.
5. Agacharse (`CTaskSimpleDuck::ProcessPed` 0x694390), tablas de giro del heading y tiempos de `WALK_start` / `Run_stop`.
6. Velocidades m/s de walk, run y sprint, midiendo el root motion de `ped.ifp` (ya está en `core/assets/game/ped.ifp`).

Cada resultado se anota en `MECHANICS_GTASA.md` con su dirección y se pasa de **[NV]** a **[V]**. Lo que no se logre cerrar queda **[NV]** con un valor provisional de SA documentado.

## Fase 1 — `MECHANICS_GTASA.md`

Raíz del repo. Secciones: convenciones (unidad `dt` de 1/50 s, ángulos, cómo leer [V]/[NV]) · entrada y movimiento a pie · animaciones y blend deltas · sprint y energía · salto/caída/trepar/agacharse · subir y bajar del coche (flujo, puerta, offsets) · armas y apuntar (`weapon.dat` AK47: `6 10 7`, fuego en fotograma 7) · cámara a pie · cámara en coche · cámara de apuntar · transiciones · tabla de funciones con direcciones · método de RE.

## Fase 2 — Motor ForceX

Archivos en `forcex/src/com/forcex/`:
- `anim/Animator.java`: **blend entre animaciones**. Guardar la animación saliente y su tiempo, y mezclar poses por hueso (slerp de rotación, lerp de posición) con un peso que sube a `blendDelta` por segundo (semántica de SA: 4.0 → 0.25 s, 16.0 → ~0.06 s, 1.0 → 1 s). Hoy `doAnimation` salta de golpe (l.42).
- `anim/Animator.java`: aceptar `update(float dt)` explícito en lugar del reloj global `FX.gpu.getDeltaTime()` (l.67-77), para poder pausar o simular de forma determinista.
- `anim/AnimationControl.java` y `anim/Animation.java`: tiempo normalizado (`getPhase()`), **eventos por tiempo** (callbacks, p. ej. fotograma de disparo, aterrizaje, puerta) y velocidad por animación.
- `anim/Bone.java`: quitar las asignaciones por hueso y frame (`new KeyFrame` y matrices) para no generar basura en Android.
- `extensions/gtasdk/.../IFPReader.java`: opción para conservar el root motion y exponer su delta por frame, y no mutar `anim.name` en `getAnimation`.
- `gfx3d/` (clase nueva, p. ej. `CameraBlender`): interpola posición, objetivo y FOV entre dos estados con duración y fracciones (`InterToStopMoving` / `CatchUp`), o corte seco. `Camera.java` ya permite cambiar `setFieldOfView` cada frame.
- La física OpenPL **no está en el repo** (solo `libopenpl.jar` y las DLL). No se toca. Salto, caída y aterrizaje se derivan con `PL_CHAR_CAN_JUMP`, `PL_CHAR_JUMP` y la altura del `PL_TRANSFORM`. Si hiciera falta gravedad o impulso configurable, se documenta como limitación.

## Fase 3 — `rpg-car` (`examples/rpg-car/core/src/com/fss/rpg/`)

1. **Movimiento a pie** (`Player.java`, o clase `PedMovement` aparte). Máquina de estados explícita con enum (stand, walk, run, sprint, turn L/R, jump, fall, land, duck, aim, enter/exit car). Replicar `PlayerControlZelda`: `m = |input|` normalizado, rumbo relativo a la cámara, ratio con subida y bajada de 3.5/s, parada inmediata al soltar. Elegir animación solo al cambiar de estado, con los blend deltas de la tabla anterior. Velocidad de animación con la fórmula `SetMoveAnimSpeed`. Sprint con la energía de 0x60a610. Sustituir `WALK_SPEED`, `RUN_SPEED`, `ACCELERATION`, `DECELERATION` y `TURN_SPEED`.
2. **Salto, caída, aterrizaje, trepar, agacharse**: según lo cerrado en la Fase 0, con animaciones de `ped.ifp`.
3. **Cámara**: nuevo paquete `cam/` con `CameraManager` y los modos `FollowPedCam`, `FollowCarCam`, `AimCam`, cada uno con su tabla de parámetros de arriba. Reemplazar el código de cámara de `Player.update` (l.231-273), `Vehicle.update` (l.263-309) y `updateAutoFollow`. Las transiciones usan `CameraBlender` con los tiempos de `StartTransition`.
4. **Colisión de cámara** (`CameraCollision.java`): sin API de barrido en OpenPL, aproximar la esfera con 5 rayos (centro y 4 desplazados un radio) y la regla de recuperación gradual de SA (+0.05/frame hacia 1.0, salto inmediato al impacto).
5. **Coche**: `startEnterVehicle` y `startExitVehicle` en `Player.java`, con puerta ligada al tiempo de animación, punto de la puerta calculado desde el último fotograma de `CAR_open/getin` (offsets), tiempo crítico, y cierre de puerta desde fuera (`CAR_close`).
6. **Armas** (`Weapon.java`, `RPGCarGame.java`): FOV 50° al apuntar con AK, hombro 0.2 m, el personaje gira al rumbo de la cámara al apuntar, y cadencia ligada al bucle de la animación.
7. **Depuración**: HUD opcional con estado de movimiento, ratio, energía de sprint, alpha/beta/distancia/FOV y modo de cámara, para compararlos con `MECHANICS_GTASA.md`.

## Orden y riesgos

Orden: Fase 0 y 1 (en paralelo) → Fase 2 (blend + eventos primero, luego `CameraBlender`) → Fase 3 en este orden: movimiento, cámara a pie, coche y cámara de coche, apuntar, salto y resto. Commits pequeños por mecánica.

Riesgos:
- Sin código fuente de OpenPL no se puede exigir física 1:1 (salto y caída se aproximan).
- La esfera de barrido de la cámara se aproxima con rayos.
- Las constantes [NV] pueden cambiar tras la Fase 0, así que cada parámetro vive en una tabla con nombre y se ajusta jugando.
- El blend cambia el coste por frame en Android; hay que revisar la basura del `Animator` y probar en un dispositivo.

## Verificación

- Build y arranque en escritorio: `gradlew :examples:rpg-car:desktop:dist` y `java -jar rpg-car.jar-all.jar` desde `examples/rpg-car/desktop/output`.
- Por cada mecánica, escenario manual con el HUD de depuración: caminar y correr con el stick a distintas magnitudes (el ratio sube a 3.5/s y cae a 0 al soltar), sprint hasta agotar energía, cámara a pie sin tocar el ratón (se recoloca) y con él (se suspende), entrar y salir del coche (orden de animaciones, puerta ligada al tiempo, cámara con transición de 750/800 ms), apuntar con AK (FOV 50°, hombro, giro del personaje).
- Blend: log o prueba de humo con dos animaciones y comprobar que el peso sube a `blendDelta` por segundo.
- Cada tabla de parámetros se compara con `MECHANICS_GTASA.md`.
- El usuario valida con capturas, como en las iteraciones anteriores, y en Android con `examples/rpg-car/android` al terminar las fases del motor.
