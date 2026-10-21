# Relevo: replicar mecánicas de GTA SA en `rpg-car`

Archivo para **retomar el trabajo en otra sesión** (si se acaban los créditos). Léelo primero.
Plan aprobado completo: `docs/PLAN_GTASA_MECHANICS.md`. Mecánicas documentadas: `MECHANICS_GTASA.md`.
Hallazgos de ingeniería inversa en bruto: `docs/re/`.

Decisiones del usuario: lo no verificado se cierra con una **Fase 0 de ingeniería inversa** antes de implementar;
alcance del motor = **blend de animaciones + eventos + `CameraBlender`** (sin máscara de huesos, sin ANPK).
El usuario valida jugando y mandando capturas (`C:\Users\Steward\Pictures\captura.png`). No ejecutar el juego en
primer plano sin avisar; el usuario rechazó una prueba que lanzaba el juego en bucle.

## Estado por fase

| Fase | Estado |
|---|---|
| 0. RE de lo pendiente | **En curso** (3 agentes en segundo plano, escriben en `docs/re/`). Ver "Agentes" |
| 1. `MECHANICS_GTASA.md` | **Hecho (versión 1)**. Falta fusionar lo que cierren los agentes de la Fase 0 |
| 2. Motor | **Hecho** y commiteado (`93f554e`): blend, eventos, root motion, `update(dt)`, `CameraBlender` |
| 3. `rpg-car` | **En curso**: locomoción a pie hecha (sin commit aún, ver abajo). Falta todo lo demás |

### Commits
- `e4b6c69` rpg-car: AN-94, animaciones de fusil/armado, puerta sincronizada, lanzador en ventana.
- `93f554e` motor: blend, eventos, root motion, `update(dt)`, `CameraBlender`, `tools/re/dis.ps1`.

- `6497d77` rpg-car: `PedLocomotion` + `Player` usando el modelo de movimiento de SA; `MECHANICS_GTASA.md`; `docs/`;
  `tools/re/IfpRootMotion.java`. (Los archivos de `docs/re/` de los agentes se commitearon a medias; seguirán
  cambiando.)

`PedLocomotion` (ratio, estados STAND/WALK/RUN/SPRINT, blend, velocidad por root motion). `Player.java` ya no tiene
`WALK_SPEED`, `RUN_SPEED`, `ACCELERATION`, `DECELERATION`, `RUN_INPUT_THRESHOLD`, `RUN_HOLD_TIME`, `ANIM_*`,
`WALK_start` ni `Run_stop`. Sprint con Shift (`sprint_touch` para el táctil, aún sin botón en la UI). Compila.

### Pendiente de comprobar (importante)
1. **El juego se cierra solo a los ~9 s** al lanzarlo desde un script sin interacción (sin stderr, sin `hs_err`).
   Antes se asumió que el código de salida 9 era "el usuario cerró la ventana"; **no está confirmado**. Diagnosticar
   (¿crash nativo silencioso de fxcore/openpl? ¿cierra por falta de foco?) y confirmar que la nueva locomoción
   no lo provoca: comparar con `git stash` / commit `e4b6c69`.
2. Probar a mano la locomoción: caminar con joystick suave (walk), a fondo (run), Shift (sprint), soltar (para en
   ~0.25 s), apuntar quieto (pose RIFLE_fire), moverse apuntando.

## Cómo compilar y ejecutar (escritorio)

```
gradlew :examples:rpg-car:desktop:dist
cd examples/rpg-car/desktop/output
java -jar rpg-car.jar-all.jar
```
Hay que ejecutar desde `output/` (`openpl64.dll` va suelto). `fxcore.dll` va embebido y necesita `dist/desktop`
generado (`build.bat`). Java 17. La física OpenPL **no está en el repo** (solo `libopenpl.jar` + DLL).

## Ingeniería inversa: método

`gta_sa.exe` (`D:\Games\GTA San Andreas\gta_sa.exe`, v1.0 HOODLUM, base 0x400000). Símbolos con nombre en
`D:\proyectos\CppProjects\decompilaciones\gtasa-menu\docs\gta_sa_symbols.tsv` (las direcciones son correctas, pero
Ghidra no define muchas funciones). Herramientas:
- `tools/re/dis.ps1 -Va 0x680c60 -Len 0x300 [-Out ruta]`: desensambla bytes por VA con `objdump`
  (`C:\w64devkit\bin\objdump.exe`). `-Floats` vuelca floats de tablas de datos. Funciones con parche
  `push -1; jmp 0x4xxxxx`: el cuerpo real empieza 7 bytes después.
- `tools/re/IfpRootMotion.java`: `java tools/re/IfpRootMotion.java "D:\Games\GTA San Andreas\anim\ped.ifp" run_player`
  mide el desplazamiento de la raíz (velocidad real por animación).
- MCP `rea`: `procedure_pseudo_code` con `"0x..."` (solo para funciones definidas en Ghidra; salida enorme).
  Permiso ya concedido: regla `mcp__rea` en `/permissions`.

## Agentes de la Fase 0 (lanzados en esta sesión)

Escriben (incrementalmente, y con sección `## Estado` al final) en:
- `docs/re/ped_jump_fall_climb_duck.md` — salto, caída, aterrizaje, trepar, agacharse, giro del heading.
- `docs/re/car_enter_exit.md` — subir/bajar del coche, puerta, `CVehicleAnimGroup`, línea de alineación.
- `docs/re/camera_pending.md` — valores [NV] de cámara (zoom a pie, colisión, transiciones...).

Si un archivo no existe o su `## Estado` dice "en curso", ese agente no terminó: repetir su objetivo con
`dis.ps1` (los objetivos están en el plan, sección Fase 0). Informes iniciales ya guardados:
`docs/re/report_ped_onfoot_initial.md`, `docs/re/report_camera_initial.md`.

## Siguiente trabajo (en orden)

1. Commit de lo pendiente (ver arriba) y diagnosticar el cierre a los 9 s.
2. Fusionar los resultados de `docs/re/*.md` en `MECHANICS_GTASA.md` (pasar [NV] → [V]).
3. Fase 3, en este orden:
   a. Movimiento a pie: pendiente fijar giro del heading, tabla ratio→estado, estados de giro (anims 0x89/0x8A),
      energía de sprint (0x60a610; semántica dudosa), botón de sprint táctil (`sprint_touch` en `Player`,
      `controls.yaml`/`RPGCarGame.handleTouchControls`).
   b. Cámara a pie: nuevo paquete `com.fss.rpg.cam` con `CameraManager`, `FollowPedCam`, `FollowCarCam`, `AimCam`
      usando las tablas de `MECHANICS_GTASA.md`; quitar el código de cámara de `Player.update` (≈l.231-273) y
      `Vehicle.update` (≈l.263-309); transiciones con `CameraBlender` (350/750/800/1350 ms).
   c. Colisión de cámara: 5 rayos en `CameraCollision.java` (OpenPL no tiene barrido de esfera).
   d. Coche: puerta ligada a la animación, punto de la puerta desde la última pose de `CAR_open/getin`, tiempo
      crítico, `CAR_close_LHS` al bajar (ya hay `CAR_closedoor_LHS` al subir).
   e. Armas: FOV 50° al apuntar con AK, hombro 0.2 m, giro al rumbo de la cámara.
   f. Salto, caída, aterrizaje, trepar, agacharse (tras cerrar la Fase 0).
   g. HUD de depuración (estado, ratio, alpha/beta/dist/FOV, modo de cámara).
4. Probar en Android (`examples/rpg-car/android`) al terminar el motor: el blend asigna objetos (`Animator.blendMatrices`,
   `Bone.interpolate`); vigilar el GC.

## Estado del código relevante (resumen)

- `Player.java`: sigue siendo un monolito con booleanos (`entering`, `exiting`, `closing_door`, `aim_animation`, `armed`).
  Cámara a pie dentro de `update` (`rot_x`, `rot_y`, `distance`, `updateAutoFollow`). Enter/exit con `ramp()` para la
  puerta. Aim con `shot_event` (fotograma 7 de `RIFLE_fire`, bucle 6–10 a 30 fps, de `weapon.dat`).
- `Weapon.java`: el arma toma `personaje × matriz del hueso de la mano` tal cual, como GTA
  (`CVisibilityPlugins::RenderWeaponPedsForPC` 0x732f30, hueso 24). Si el AN-94 sale girado respecto a la mano, hay
  que corregir la orientación del **modelo**, no la mano.
- Motor: `Animator.blendTo(anim, inverse, blendDelta)`; `getPhase()`, `getBlendWeight()`, `isBlending()`;
  `update(dt)`/`updateTime(dt)`; `setRootMotion(true)` + `getRootDelta()` (en ejes de la animación: el avance está en `x`
  porque `IFPReader` guarda la posición como `(y, x, z)`); `Animation.addEvent(time, listener)`;
  `Bone.samplePosition`; `CameraBlender.start/update`.
- Convenciones: comentarios en español; llaves en la misma línea; constantes `MAYUSCULAS`, campos `snake_case`,
  métodos `camelCase`. No hay CLAUDE.md ni tests automáticos (se prueba jugando).
