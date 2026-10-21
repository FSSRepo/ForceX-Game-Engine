# Informe inicial: mecánicas a pie (gta_sa.exe v1.0 HOODLUM)

Origen: primer agente de exploración (solo lectura). Es el punto de partida; los pendientes se cierran en
`ped_jump_fall_climb_duck.md` y `car_enter_exit.md`. **[V]** = leído del exe, **[NV]** = no verificado.

## Por qué hay tantos [NV]

El exe es una variante ofuscada: casi todas las funciones de tareas no están definidas en Ghidra
(`CTaskSimpleJump::ProcessPed` 0x680c60, `CPlayerPed::SetMoveAnim` 0x609490, `CTaskSimpleLand`, `CTaskSimpleFall`,
`CTaskSimpleDuck`...). `procedure_pseudo_code` y `read_function_instructions` fallan en esas direcciones.
Algunas empiezan con un parche `push -1; jmp 0x4xxxxx` (p. ej. 0x6883d0 salta a 0x407c05): el cuerpo real empieza
7 bytes después (0x6883d7). Solución: `tools/re/dis.ps1` (ver `docs/PROGRESS.md`).

## 1. Movimiento a pie

`CTaskSimplePlayerOnFoot::ProcessPed` (0x688810) despacha según flags del ped (nombres de destino = hipótesis):
- flag 0x4000000 (+0x46c) activo → 0x687f30.
- arma de apuntado manual → 0x687c20 (¿apuntar/sniper?).
- si no → 0x687530 (movimiento normal) o `PlayerControlZelda`.
- termina con 0x6859a0 (arma, melee, agacharse) y 0x6872c0 (audio de interacción).

`PlayerControlZelda` (cuerpo real 0x6883d7), todo **[V]**:
- `x = GetPedWalkLeftRight`, `y = GetPedWalkUpDown` (short con signo; 0 si el pad está deshabilitado).
- `m = sqrt(x²+y²) * 0.016666668` = dividido entre 60. Con el stick a fondo en un eje (±128) da ≈ 2.13.
- si `ped+0xFC != 0` (ped en estado especial) → `m = 0`.
- si `*(short*)(pad+0x2a) == 0` o `m <= 1.0`, `m` queda igual; si `m > 1.0` y ese campo ≠ 0, `m = 1.0`
  (significado del campo **[NV]**, probablemente limita a "andar").
- si `m > 0`: rumbo objetivo `heading = CGeneral::GetRadianAngleBetweenPoints(0,0,-x,y) - cameraHeading`,
  normalizado con 0x53cb50. `cameraHeading` = global `DAT_00b6f178`; resultado en `ped+0x55c`.
  **El movimiento es relativo a la cámara.** Se calculan `-sin` y `cos` y se pasan a un test de colisión/pared
  (thunk 0x40593e, no resuelto); si falla el ratio se pone a 0.
- ratio hacia `m`: `playerData+0x14 += clamp(m - actual, -step, +step)`, `step = timeStep * 0.07`
  (`timeStep` = `DAT_00b7cb5c`, 1.0 = 20 ms). Sube y baja con la misma pendiente, 0.07 por frame de 1/50 s
  = **3.5 unidades/s**. Con `m <= 0`: `playerData+0x14 = 0` de golpe.
- sprint: 0x60a610 (energía). Si devuelve ≥ 1.0 y se pulsa sprint (0x5407f0 / 0x5407a0) → `ped+0x534 = 7`;
  si no, si el botón de correr (0x5407a0) → `ped+0x534 = 6`.
- salto: lo dispara 0x540770 con 0x540670 en falso y `ped+0xFC == 0`; estado (`DAT_00b6f1a8[...]`) distinto de
  7, 8, 0x33, 0x22, 0x2d, 0x2e. Crea `CTaskComplexJump` (0x67a030) y la mete con 0x681af0(task, 3, 0).

`CPed::SetMoveAnim` (0x5e4a00), **[V]**. Mapea `ped+0x534` (estado) a animación; solo se reselecciona si cambia
el estado:

| Estado | Animación (ID del enum de anims) | Blend delta |
|---|---|---|
| 1 | 3 (idle) | 4.0 |
| 2 | 0x89 (giro izquierda) | 16.0 |
| 3 | 0x8A (giro derecha) | 16.0 |
| 4 | 0 (walk) | 1.0 |
| 6 | 1 (run) | 1.0 (3.0 si `ped+0x530 == 9`) |
| 7 | 2 (sprint) | 1.0 |

El estado 5 (jog) no se maneja. Los nombres de ped.ifp (`WALK_player`, `run_player`, `sprint_civi`,
`IDLE_stance`) **[NV]** contra el exe; salen de conocimiento general de SA.

`SetMoveAnimSpeed` (0x5dec10), **[V]**:
- `f = clamp(ped+0x518, -0.3, 0.3)` (¿`m_fMoveSpeed`?).
- `anim.speed (+0x24) = (f + 1.2) - (ushort)(ped+0x20) * 0.4 * 3.051851e-05` (1/32768): `ped+0x20` es una
  estadística 0..65535 (¿músculo/gordura?) que resta hasta 0.4.
- si `ped+0x484 == 2`: velocidad = `ped+0x518 + 1.0`, con valores fijos 0.7 (< -0.3) y 1.3 (> 0.3).

**[NV]**: tabla que traduce el ratio `playerData+0x14` a los estados 4/6/7 (se asigna en otra función no
definida); tiempos de WALK_start y Run_stop; tablas de velocidad de giro del heading.

## 2. Sprint, stamina y velocidades

Energía de sprint (0x60a610), tabla de 0x20 bytes por entrada en 0x8d2460, **[V]** las 3 primeras entradas:

| Campo | Valor | Uso |
|---|---|---|
| +0 | 4.0 | ganancia por step pulsando sprint |
| +4 | 0.7 | pérdida por step al pulsar correr sin llegar a sprint |
| +8 | 0.2 | pérdida por step en reposo |
| +0xC | 5.0 | umbral de energía |
| +0x10 | 10.0 | máximo |
| +0x14 | 1.0, 0.0, 1.0 | desconocido (se pasa a 0x60a550 como "coste", **[NV]**) |
| +0x18 | 0.5, 0.4, 0.3 | desconocido (idem) |
| +0x1C | 0.3 (entrada 0), 1.0 (entrada 1), 0.3 (entrada 2) | multiplicador del ratio |

- energía en `playerData+0x1c`. Valor devuelto: `(ratio-1)*mult + 1`, con `ratio = energía/5` si la energía supera 5;
  con la entrada 0 el máximo es 1.3.
- si no se cumple la rama de stamina devuelve 0 y la energía pasa a 0.
- con la energía bajo el umbral y el botón pulsado, el ratio queda en 1.0 y se llama a 0x60a550(1, coste).
- **[NV]** velocidades m/s: salen del root motion de `ped.ifp`. Medidas después en `tools/re/IfpRootMotion.java`
  (ver `MECHANICS_GTASA.md`).

## 3. Salto, trepar, caída — casi todo [NV]

Solo se obtuvo: el lanzamiento del salto desde `PlayerControlZelda` (arriba) y `CTaskSimpleClimb::TestForClimb`
(0x6803a0), **[V]**:
- primer test tipo `ProcessLineOfSight` (thunk 0x40103c).
- si el objeto golpeado está en un grupo de entidades móviles (`+0x36 & 7` entre 2 y 4), ajusta posición y ángulo
  con su matriz.
- segundo test desplazado: `x - sin(ang)*(-0.4)`, `y + cos(ang)*(-0.4)` (0.4 m hacia atrás a lo largo del ángulo
  de la pared) más un offset Z global (`DAT_00c18f78`, valor en runtime).
- si el segundo test falla devuelve la entidad; si colisiona devuelve 0 y `*heading = -9999.9`.

## 4. Subir y bajar de vehículo (parcial)

`CCarEnterExit::GetPositionToOpenCarDoor` (0x64e740), **[V]**, devuelve el punto objetivo en coordenadas de mundo:
- puerta (3er parámetro): 8 = delantera izq., 9 = delantera dcha., 10 = trasera izq., 11 = trasera dcha.,
  0x12 = maletero/otro (nombres propios).
- offsets locales de `CVehicleAnimGroup::ComputeAnimDoorOffsets` (0x6e3d10, cacheados), leyendo el último
  fotograma de las animaciones (IDs 0x167 para índice 0, 0x169, 0x16b, 0x175, 0x177, 0x17a, 0x17b para 1..6);
  valores (x,y,z) de `+0x14,+0x18,+0x1c` del keyframe final.
- puertas 8/9: lado `x ± doorOffsetFromModel(+0xd4)`; 10/11 el contrario (`-(modelX + extra)`), misma Y y Z.
- vehículos con `vehicle+0x590 == 9` (bici): usa 0x6bf230. Con `+0x594 == 1` o `flags & 8` y tipo ≠ 4:
  ajusta Z con `0.95 - virtual(+0xd4)()`.
- transforma con la matriz del vehículo (0x59c790) y suma la posición.

`CVehicleAnimGroup::ComputeCriticalBlendTime` (0x6e3c80), **[V]**: devuelve un tiempo por ID de animación desde
campos del grupo del vehículo:

| IDs de animación | Campo |
|---|---|
| 0x15f..0x16b | `+0xc` |
| 0x180, 0x181 | `+0x10` |
| 0x175..0x178 | `+0x14` |
| 0x17a, 0x17b | `+0x18` |
| 0x183, 0x184 | `+0x1c` |
| otro | 0.0 |

Los valores de esos campos no se leyeron. **[NV]** (a cerrar en `car_enter_exit.md`): `CTaskUtilityLineUpPedWithCar`
(0x6513a0), `CTaskSimpleCarAlign` (0x64d6d0), `CTaskSimpleCarGetOut` (0x64dc70), `CTaskSimpleCarSetPedInAsDriver`
(0x64b950), distancias de activación, tolerancias, interpolación, animaciones de conducción, cierre desde fuera.

## 5. Otras mecánicas (de `ProcessPed` y 0x6859a0), [V] salvo nota

- agacharse: `CTaskSimpleDuck::ProcessPed` 0x694390, `CTaskSimpleDuckToggle` 0x62f6e0 (no abiertos).
- flag 0x4000000 (+0x46c) cambia el control a 0x687f30 (¿nadar/estado especial?).
- 0x6859a0: ramas por tipo de arma (`weapon type == 0x22,0x23,0x24,0x2b`...) que lanzan la tarea de disparo (0x681af0).
- `ped+0x71c` = objetivo fijado. ≠ 0 → apuntado "lock-on"; 0 → "free aim" con el flag 0x540670.
- detalles de correr y disparar: **[NV]**.
