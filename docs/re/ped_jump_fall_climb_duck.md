# GTA SA 1.0 (HOODLUM) - Salto, caida, aterrizaje, trepar, agacharse

Fuente: `D:\Games\GTA San Andreas\gta_sa.exe` (base 0x400000). Herramienta: `tools\re\dis.ps1`.
Marcas: **[V]** leido/deducido directamente del codigo, **[NV]** estimacion.
Unidades: velocidades del ped en **m por frame de 20 ms** (dt=1.0 = 1/50 s). Para pasar a m/s multiplicar por 50.
`dt = DAT_00b7cb5c`; `now_ms = DAT_00b7cb84` (CTimer::m_snTimeInMilliseconds).

## Objetivos (estado)
1. SALTO - HECHO (secuencia, anims, impulso, gravedad, condiciones de disparo, duraciones). Pendiente menor: formula exacta del modificador de stats (aprox. 1.0 con stats por defecto).
2. CAIDA/ATERRIZAJE - HECHO a nivel de tareas y umbrales (vz<=-0.1 FALL_fall, minVz<=-0.4 derribo, tipos de Land). NO resuelto: formula final de dano real (CPedDamageResponseCalculator / dano por colision fisica).
3. TREPAR/VAULT - PARCIAL: sondas de escaneo (22 esferas), rechazos por altura, tipo 1/2 (umbral 1.4 m), secuencia de anims y root motion. Falta: offsets exactos por fase (dos floats runtime en bss 0xC18F78/0xC18F7C) y condiciones de abort.
4. AGACHARSE - HECHO (activacion, CanPedDuck, anims, control, salida).
5. HEADING - HECHO (9 deg/frame jugador, igual en walk/run/sprint). WALK_start/Run_stop: duraciones y root motion de ped.ifp obtenidas; logica que las dispara (SetMoveAnim 0x5E4A00) NO leida.

## 0. Tabla de IDs de animacion (AnimationId) [V]
La tabla de nombres esta en el exe en `0x8d1160` (punteros a strings, stride 4). El primer nombre `Walk_civi` esta en idx 2 de un array que empieza en
`0x8d1160 - 118*4`; por tanto **AnimationId = idx - 2**. Verificado porque `Launch` usa assoc 0/1/2 = walk/run/sprint.
Volcado completo en tabla: ver el array (script: leer dwords desde `0x8d1160-0x1d8`).

| ID dec (hex) | nombre ped.ifp |
|---|---|
| 0 / 1 / 2 | walk_civi / run_civi / sprint_panic (via animgrp.dat grupo `man`; cambia por grupo de ped) |
| 3 / 4 / 5 | idle_stance / roadcross / walk_start |
| 6 / 7 | run_stop / run_stopR |
| 55..59 (0x37..0x3b) | WEAPON_crouch / GunCrouchFwd / Crouch_Roll_L / GunCrouchBwd / Crouch_Roll_R |
| 112..115 | Getup, Getup, Getup, Getup_front (idx 114..117 - 2; los 3 primeros nombres repetidos) |
| 116 (0x74) | JUMP_launch |
| 117 (0x75) | JUMP_launch_R |
| 118 (0x76) | JUMP_glide |
| 119 (0x77) | JUMP_land |
| 120 (0x78) | FALL_fall |
| 121 (0x79) | FALL_glide |
| 122 (0x7a) | FALL_land |
| 123 (0x7b) | FALL_collapse |
| 124 (0x7c) / 125 (0x7d) | FALL_back / FALL_front |
| 126 (0x7e) / 127 (0x7f) | EV_step / EV_dive |
| 128 (0x80) | CLIMB_jump |
| 129 (0x81) | CLIMB_idle |
| 130 (0x82) | CLIMB_Pull |
| 131 (0x83) | CLIMB_Stand |
| 132 (0x84) | CLIMB_Stand_finish |
| 133 (0x85) | CLIMB_jump_B |
| 134 (0x86) | CLIMB_jump2fall |
| 135.. | XPRESSscratch, TURN_180(136), Turn_L, Turn_R, ARRESTgun, DROWN, DUCK_cower(141) ... |
| 26 (0x1a) | KO_skid_back (usada por CTaskSimpleFall tras caida muy rapida) |

(ID = idx-2 con idx tomado del array; p.ej. idx 118 `JUMP_launch` -> ID 116.)

## Tipos de tarea (GetTaskType) [V]
Fall 0xCF(207), SimpleJump 0xD2(210), ComplexJump 0xD3(211), ComplexInAirAndLand 0xF0(240), SimpleInAir 0xF1(241), SimpleLand 0xF2(242),
SimpleClimb 0xFE(254), HitHead 0x1F4, SimpleDuck 0x19F(415).

## 1. SALTO

### Tarea ComplexJump (ctor 0x67a030, campo +0xC = `launchMode`, 1 = fuerza de jugador)  [V]
- `CreateFirstSubTask` (0x67fd10) -> `CreateJumpSubtask(0xD2)` = `CTaskSimpleJump(bPlayerLaunch = (this+0xC == 1))`.
- `CreateNextSubTask` (0x67fc00), segun la subtarea que acaba:
  - SimpleJump terminado, si `jump.animStarted(+0x23)==0` -> fin (limpia flag ped 0x400).
  - si `jump.hitHead(+0x21)` -> pone flag ped 0x400 y crea `HitHead` (0x1F4, ctor 0x653060) -> luego fin.
  - si hay entidad de trepar `jump.climbEntity(+0x1C)` y `this+0xC != -1` -> flag ped 0x200 y crea `SimpleClimb` (0xFE).
  - si no -> flag 0x200 y crea `ComplexInAirAndLand` (0xF0, con `bUsingJumpGlide=1, bUsingFallGlide=0`).
  - tras Climb: si flag ped 0x200 sigue puesto -> InAirAndLand; si no -> fin.  tras InAirAndLand/HitHead -> fin (limpia 0x200/0x400).
- `ControlSubTask` (0x67a100) solo devuelve la subtarea actual. `MakeAbortable` (0x67a070) solo permite abortar al saltar si la tarea nueva es Climb 0x36...(no relevante).
- `CreateJumpSubtask` (0x67d980) para 0xFE: tipo de climb `= 2` si `(grabPos.z - ped.z) <= 1.4` (const 0x8d2f34), si no `= 1`. [V]

### CTaskSimpleJump (vtable 0x87054c, size 0x2C) [V]
Campos: +0x08 vec3 grabPos, +0x14 float grabHeading, +0x18 byte surface, +0x1C CEntity* climbEntity, +0x20 `animFinished`, +0x21 `hitHead`,
+0x22 `blockedAhead`, +0x23 `animStarted`, +0x24 `playerLaunch`, +0x25 `highJump?`, +0x28 anim assoc.

Flujo `ProcessPed` (0x680c60):
1. Si `animFinished==0`: si no hay anim -> `StartLaunchAnim` (0x67d7a0). Si falla -> la tarea termina (return true, `animStarted=0`). Si arranca, `animStarted=1`.
2. Si `animFinished==1` (se pone en el callback `0x67a020` cuando JUMP_launch termina):
   - si (ped es jugador o `playerLaunch`) y `[0xB72914]==0`: `climbEntity = TestForClimb(ped,&grabPos,&grabHeading,&surface,1)`.
   - si no hay climbEntity: `0x67d590` test de espacio: sube temporalmente al ped 0.75 m, reduce sus esferas de colision de radio 0.35 a 0.30 y llama `ped->TestCollision(false)`; si choca -> `hitHead=1`. Ademas test de esfera (r=0.25) delante del ped -> `blockedAhead`.
   - si `!hitHead` -> `Launch(ped)` (0x679b80) = el IMPULSO. Devuelve true (tarea terminada) -> ver CreateNextSubTask.
   => **El impulso ocurre al FINAL de la animacion JUMP_launch(_R)** (no al pulsar), duracion = duracion de esa anim / speed.

`StartLaunchAnim` 0x67d7a0 [V]: no inicia si ya suena anim 0x74/0x75; aborta (y marca flag ped 0x528|=8) segun comprobaciones de suelo/obstaculo (0x55e770 sobre ped+0xBA, direccion frente, `ProcessVerticalLine`-like 0x441e10 con 5.0 m para jugador). Elige JUMP_launch (0x74) o JUMP_launch_R (0x75) segun la fase del ciclo de andar/correr: `t = assoc.currentTime/hierarchy.totalTime + 0.367` (const 0x870634), `if t>1: t-=1`; si el ped esta quieto (ninguna assoc 0/1/2 con blend>0.3) se usa `t=0` -> comparacion con 0 (double 0x859060) elige R/L. Blend delta 8.0; speed de la anim = `GetFatAndMuscleModifier(2)` si el ped tiene stats. Fija `ped.desiredRot = ped.curRot` (no gira durante el salto).

`Launch` 0x679b80 [V] - velocidad horizontal H (m/frame) segun la anim de movimiento activa:
```
H = 0.10                                   // quieto/walk (const 0x8703b0)
if (assoc sprint(id2) existe)  H = 0.17 + (0.22-0.17)*sprint.blend   // 0x8703b4 / 0x8703b8
else if (assoc run(id1) existe) H = 0.10 + (0.17-0.10)*run.blend
Fz_impulso = 4.5   (0x8703bc)  //  8.5 (0x8703c0) si el ped es jugador o task.+0x25
if (ped.stats) Fz *= GetFatAndMuscleModifier(2)  (dos veces: Fz = mod*Fz*mod ? -> ver nota)   [NV el detalle exacto]
if (cheat 0x96916C && jugador) Fz *= 10
ped.ApplyMoveForce(0,0,Fz)                 // 0x5429F0: dV = F / mass (CPhysical)   
if (blockedAhead): moveSpeed.xy = 0 ; anim CLIMB_jump(0x80) con flag 8
else if (task.climbEntity==0): moveSpeed.xy = (-sin(heading)*H, cos(heading)*H)  (heading = ped+0x558 curRot)
anim JUMP_glide (0x76) blend 8.0 (si no hay climbEntity y no blockedAhead)
ped.flags(0x46c): &= ~1 (deja de estar de pie), |= 0x200 (en el aire)
```
- Impulso vertical del jugador: `Vz = 8.5 / mass`. Masa de ped por defecto 70 (CPed ctor, [NV: no verificado en este informe]) -> **Vz ~= 0.1214 m/frame = 6.07 m/s** [NV masa].
- Con gravedad de ped 0.008*dt por frame (const 0x863984 = 0.008, usada en el codigo de InAir) -> altura de apex `Vz^2/(2*0.008) ~= 0.92 m` y tiempo hasta apex `~15 frames = 0.30 s` [NV por masa, formula V].
- H max: 0.22 m/frame = 11 m/s (sprint), 0.17 = 8.5 m/s (run), 0.10 = 5 m/s (quieto/walk). Alcance aprox = H * (2*Vz/0.008 frames) ~ 0.22*30 = 6.6 m (sprint, a nivel) [NV].
- Condicion de los NPC: Fz=4.5 (Vz 0.064, apex ~0.26 m). Solo jugador/`+0x25` usa 8.5.

### ComplexInAirAndLand (0xF0) (CreateFirstSubTask 0x67cc30, Next 0x67ccb0, Control 0x67d230) [V]
- Primera subtarea: `CTaskSimpleInAir(bUsingJumpGlide=this.0xC, bUsingFallGlide=this.0xD, bUsingClimbJump=0)`.
- Next tras InAir (prev type 0xF1): ver seccion 2 (landing).  Next tras Fall(0xCF) crea tarea 0x677f50 (get-up). Tras Land(0xF2)/Climb(0xFE)/0xCD -> NULL.
- Control (0x67d230): si no era fallGlide y la InAir termino con `climbTarget` (InAir+0x38) valido -> crea SimpleClimb con el objetivo (tipo 1 o 2 por la misma regla de 1.4 - 0.3 = 1.1 m: `h <= 1.4-0.3` -> 2 [V], const 0x8d2f34 y 0x858c24).

### CTaskSimpleInAir (0x678cd0 ctor, ProcessPed 0x680600) [V]
Campos: +0x1C anim, +0x20 `minVz` (velocidad vertical mas negativa vista), +0x24 flags (bit0 JumpGlide, bit1 FallGlide, bit2 ClimbJump), +0x28 contador de frames,
+0x2C inicio ms, +0x30 = 500 (ms), +0x34 flag grito, +0x38 CEntity* climbTarget.
- Flag ped 0x200 siempre puesto mientras dura.
- Con JumpGlide: anim JUMP_glide (0x76) (o CLIMB_jump 0x80 si existe), blend 4.0. Con FallGlide: si `vz<=0` anim FALL_glide (0x79) blend 4.0, speed 1.0, time 0.5. Si no: busca 0x80 o FALL_glide.
- **FALL_fall (0x78) arranca cuando `vz <= -0.1` (const 0x858ef4)** (= -5 m/s) y no esta ya sonando (modo no FallGlide), blend 4.0.
- Grito de caida (speech 0x166 jugador / 0x15a otros): tras >=500 ms en el aire y `vz*50 < -20` (consts 0x858b40=50, 0x870644=-20) => `vz < -0.4`/frame (=-20 m/s). [V]
- `minVz = min(minVz, vz)` cada frame (para el dano al aterrizar).
- Aterrizaje: termina cuando el ped tiene flag `standing` (0x46c bit0); para FallGlide hay un contador de 10 frames (`0x8d2ef8 = 10`) con `-0.2 < vz <= 0` ... (heuristica anti-atasco, [NV]).
- Si detecta pared (CLIMB/grab) mientras salta: `TestForClimb(ped,&pos,&heading,&surface,0)` se llama si `vz > -0.2` (0x858fdc), velocidad horizontal > 0.05 (0x858c28) y no estado 0x36/0x37 -> guarda `climbTarget` y termina para pasar a Climb. [V parcial]

## 2. CAIDA / ATERRIZAJE

### Decision de aterrizaje (ComplexInAirAndLand::CreateNextSubTask tras InAir, 0x67ccf6..0x67d19b) [V]
Entrada: ultima `InAir` (anim, `minVz`), ped. Se limpia el anim de InAir (blendDelta = -8, callback de borrado).
- Caso `bUsingFallGlide` (this+0xD != 0): si `ped.vz < -0.1` -> `Land(anim 0x1A=KO_skid_back)`; si no -> `Land(anim -1)` (sin anim, termina al instante).
- Caso normal (`[0xD]==0`):
  a) **Si la anim activa de InAir es FALL_fall (0x78)** (el ped llego a vz<=-0.1):
     - `minVz <= -0.4` (const 0x858f0c): `CTaskSimpleFall(anim 0x1A KO_skid_back, group 0, 700 ms (0x2BC))`  (caida dura: derribo)
     - si no: `Land(anim 0x7B FALL_collapse)`.
     - En ambos: speech 0x3B (grito) y, si `ped.0x480 != 0` (jugador/stats): evento de dano `CPedDamageResponseCalculator(ped, damage, weapon -1)` con **damage = 60.0 (0x42700000), o 75.0 (0x42960000) si `0x5a7950(...)` es true** (modelo/flag, [NV cual]). (0x67ce7c..0x67cf31)
     [NV: tengo dudas de si el dano 60/75 se aplica tambien en caida normal de salto; falta verificar `0x5a7950` y el filtro posterior.]
  b) Si NO era FALL_fall y el ped es jugador (0x5df8f0): `intel=0x609560(ped)`:
     - si la anim activa es 9 (id 9 = idle_hbhb?) -> Land(anim 0x0A);
     - si `ped.0x480->+0x14 (stat float) > 1.5 (0x858ce8)` y el ped `0x53fc90/0x53fd30` (velocidad pad X/Y !=0, pad con direccion) -> `Land(JUMP_land 0x77)`;
     - si no -> `Land(FALL_land 0x7A)`.
     Luego speech 0x3A y dano `0x425c0000 = 55.0` (o `0x428c0000 = 70.0` si 0x5a7950) via evento (0x67d0e2..0x67d199). [V codigo / NV significado]
  c) Si no es jugador: `Land(FALL_land 0x7A)` + mismo evento 0x3A/55.
(La relacion exacta entre estos "damage" y el dano real requiere ver CPedDamageResponseCalculator; pueden ser parametros de intensidad de CEventDamage. [NV])

### CTaskSimpleLand (ctor 0x678e90, ProcessPed 0x67d380, callback 0x678fa0) [V]
- Campos: +0x8 anim, +0xC animId, +0x10 flags (bit0 terminado, bit1 sin anim (id==-1), bit2 "primera vez").
- Primer frame: `BlendAnimation(clump, 0, animId, blendDelta=100.0)` (blend instantaneo) + callback fin. Si `ped.matrix[+0x28]==1`(upright) y hay velocidad angular, la pone a 0 y fija heading; pone flag ped 0x400; si jugador y `ped.0x534==7` (y 0x5407a0) -> speed de la anim 2.0; si id != 0xA: `anim.speed *= GetFatAndMuscleModifier(2)`.
- JUMP_land (0x77) y id 0xA se detienen con blend delta -100 en el callback (`0x678fa0`).
- Al terminar: limpia flag ped 0x400 y reactiva walk/run/sprint (ids 0,1,2).
- `RightFootLanded` (0x678fe0): true si `anim.currentTime >= 0.1` y `prevTime < 0.1` (cruce 0.1 s); `LeftFootLanded` (0x679010): cruce de **0.2 s** (0x858cc4). [V]

### CTaskSimpleFall (ctor 0x6782c0 (anim, group, ms), ProcessPed 0x67faf0) [V parcial]
- Campos +0x8 started, +0xC animId, +0x10 group, +0x14 anim, +0x18/+0x1C tiempo restante (ms).
- Mientras ped en suelo (flag standing 0x46c bit0) o flag 0x470&0x80000: `timeLeft -= ftol(dt*0.02*1000)` = `dt*20` ms; al llegar a <=0 termina (return true).
- Marca `ped.0x528 |= 8`, `ped.0x474 |= 0x100000`.

## 3. TREPAR / VAULT (parcial)

### TestForClimb 0x6803a0 (ped, outPos, outHeading, outSurface, flag) [V]
1. `e = ScanToGrab(ped, outPos, outHeading, outSurface, flag, 0,0,0)`; si NULL -> NULL.
2. Si `e` es movil (tipo 2/3/4: vehiculo/ped/objeto) se transforma la posicion y se suma su heading.
3. **Segundo escaneo** `ScanToGrab(ped, pos', ..., 0,1,0, &pos)` desde `pos' = pos + (sin(h)*0.4, -cos(h)*0.4, dz)` (const 0x8d2f24 = -0.4, dz=`[0xC18F78]`):
   si este segundo scan encuentra algo (no libre) -> `outHeading = -10000` y devuelve NULL (hueco insuficiente detras del borde). Si esta libre -> OK devuelve `e`.
   => "dos tests desplazados 0.4 m" son dos ScanToGrab, no LOS.
- Variantes 0x6804d0 y 0x680570: test de espacio libre equivalente (devuelven bool).

### ScanToGrab 0x67fd37 (cuerpo) [V a alto nivel]
- Inicializa 4 CColModel estaticos (0x67a890): **modelo principal 0xC19518 = 22 esferas de radio 0.4** en el sistema local del ped (y = adelante, z = arriba desde el origen del ped):
  - y=-0.06: z = 1.2, 1.7, 2.2, 2.5 (indices 0..3)
  - y=0.5 : z = 2.9, 2.4, 1.9, 1.4, 0.9, 0.4 (indices 4..9)
  - y=1.0 : idem (10..15)
  - y=1.5 : idem (16..21)
  Otros modelos: 0xC19548 (7 esferas, flag1), 0xC19578 (6 esferas, flag2), 0xC195A8 (16 esferas, verificacion final). No volcados completos.
- Busca en la rejilla de sectores (50 m) alrededor de `ped.pos + front*10`; para cada entidad llama `ScanToGrabSectorList` (0x67de10) con la lista de edificios/objetos.
- Se usa un contador de visita (0xB7CD78) para no repetir entidades.

### ScanToGrabSectorList 0x67de10 [V]
- Para cada entidad (edificio tipo 1, objeto tipo 4 solida, vehiculo/ped en ciertos casos) con `dist < radioBound + ped.colRadius(+0x24) + extra`: `CCollision::ProcessColModels` (0x4185C0) del modelo de sondas contra el col del objeto.
- Se toma el primer punto de colision (`0xB9ACD0` pos, `0xB9ACE0` normal, `0xB9ACF1` = indice de esfera/pieza, `0xB9ACF3` = surface).
- Se rechaza (devuelve 1 = "bloqueado", ScanToGrab devuelve NULL) si:
  - la esfera golpeada es una de **0,1,2,3** (esferas internas del cuerpo) o
  - el punto queda detras (z <= centro y `(p-c).front >= 0`...) o la superficie no es trepable (`0x55EA10`), o
  - por **altura** (esferas altas): nivel `L` = `ftol(GetFatAndMuscleModifier(0))` (0 si no hay stats) y flag `a5` (el 1 que pasa Jump):
    - siempre rechaza esferas {4,10,16} (z=2.9)
    - si NOT(a5 && L<1): rechaza {5,11,17} (z=2.4)
    - si L>=1 y NOT(a5 && L<2): rechaza {6,12,18} (z=1.9)
  => alturas maximas (borde mas alto que sondas libres) crecen con la estadistica de musculo/grasa (L).
- Aceptacion: si la normal del contacto es casi vertical (`|nx|<0.05 && |ny|<0.05`) -> el punto es la parte superior del borde; heading = heading actual del ped (`+0x558`). Si la normal es horizontal: requiere `dot(n,front) > 0.3` (const 0x858c24; ~72 grados max de desalineacion) y `heading = atan2(-nx, ny)`. Luego un segundo `ProcessColModels` (col 0xC195A8) para refinar el punto en la cima del borde (exige normal casi vertical |n|<0.05).
- Salida `outPos` = punto de agarre; `outSurface` = material. Altura relativa al origen del ped (que esta ~1.0 m sobre el suelo): sondas cubren z local 0.0..3.3 -> bordes entre ~1.0 m y ~4.3 m sobre el suelo [NV: asume origen a 1.0 m].

### Tipo de climb [V]
`CreateJumpSubtask(0xFE)`: `type = (grabZ - pedZ <= 1.4) ? 2 : 1` (const 0x8d2f34 = 1.4).

### CTaskSimpleClimb (ctor 0x67a110: (CEntity*, CVector&, heading, surface, climbType, bForce)); ProcessPed 0x680dc0; helpers 0x67b5c0, 0x67e7b0, 0x67ef20, 0x67dbe0, 0x67a320
Campos de ctor: +0x8,+0x9,+0xA,+0xC flags=0, +0xB, +0xD=climbType, +0xE=0, +0xF, +0x10=0xFF (fase), +0x14 heading, +0x18 vec3 pos, +0x24 entidad, +0x28 short, +0x2C anim.
**PENDIENTE leer ProcessPed y tabla de offsets (0x8d2f1c..0x8d2f30: -0.4, -1.1, -0.4, 0.15, 1.0, 0.5 son offsets segun fase de climb y 0x8d2f38..58: 0.8, 0.6, 0.3, 0.5, 1.309, 0.02, -0.05, 0.9, 0.785).**

## 4. AGACHARSE [V salvo indicado]

### Activacion (CTaskSimpleDuckToggle 0x62F690/0x62F6E0, tipo 0x51A; arg +0x8 = 1 / -1 / 0)
- `ProcessPed` (0x62F6E0): arg==0 (o -1 con duck ya existente) -> aborta la `CTaskSimpleDuck` secundaria (`GetTaskDuck(0)`->virtual +0x18). arg==1 o -1: si no hay duck secundario, crea
  `CTaskSimpleDuck(type=4, length=0, shotsWhizzing=-1)` (ctor 0x691FC0; size 0x28) y la coloca en el slot secundario 1 (`0x681B60`); en caso de que ya haya un duck lo conmuta con `0x692340/0x6924F0(type 4)`. Al abrir, llama `CTaskSimpleUseGun::ClearAnim (0x61E190)` si hay arma. Siempre devuelve true (la tarea toggle termina al instante).
- `CTaskSimpleDuck::CanPedDuck` (0x692610): **false si el ped es jugador y `moveState >= 6`** (RUN/SPRINT: `ped+0x534`); si no, true si el arma activa (`ped+0x5A0 + slot*0x1C`) tiene weaponInfo con grupo 0 o 5 (`*ecx==0 || ==5`), es el arma 0x29 (spray 41), o weaponInfo.flags bit 13 (0x2000, "crouch ok"). Si no hay weaponInfo => true.
- Jugador: `PlayerControlDucked` (0x687F30) se llama en vez de Zelda mientras haya duck (slot secundario 1 distinto de NULL). Boton de agacharse = `CPad::DuckJustDown` (0x540720 area; el toggle lo emite 0x540720/0x540670, [NV exacto]).
- Salto desde agachado: PlayerControlZelda comprueba `pad.Duck (0x540670)==false` => **no se puede saltar mientras se mantiene agachar** (0x68870F).

### CTaskSimpleDuck (vtable ~0x86xxxx, size 0x28) campos [V]
+0x08 inicio ms, +0x0C length ms (0 = indefinido), +0x0E contador restante, +0x10 anim assoc, +0x18 `abortRequested`, +0x19 `finished/aborting`, +0x1A, +0x1B `active`,
+0x1C `moveMode` (1.0 / -1.0 / otro), +0x20 acumulador, +0x24 `type` (4 = jugador con arma / duck normal; 2 = coger cobertura/shots-whizzing; 0 = cower), +0x25 contador de gracia (init 0xFF).
- `ProcessPed` 0x694390: mientras el ped esta vivo (`ped+0x540 > 1.0` => health/flag), `type!=0`: anim **WEAPON_crouch (ID 55, 0x37)**; `type==0`: **DUCK_cower (ID 141, 0x8D)**; blend delta 4.0; la anim opuesta se quita (blend -4.0 o -2.0 para el id 0xA).
  Si `length>0` y `now > start+length` -> aborta. Si `length`/contador `+0xE>0` decrementa `dt*20` ms. Cuando la anim ya existe y `anim.blend > 0.99` (0x862CD0) y type 2/4 se llama `0x6939F0` (transicion; tipos de duck con arma).
  Si `finished(+0x19)`: devuelve true y limpia el flag ped `0x46c &= ~0x04000000` (bit 26, "agachado").
- Callback de fin de anim 0x692550: pone `finished` segun el ID (tabla de IDs 0x25.. de anims de agachado).
- `ControlDuckMove(x,y)` 0x6923F0: mueve el estado de rodar/avanzar: `+0x1C` pasa a +1.0/-1.0 (adelante/atras en crouch o roll), mientras que `+0x20` interpola hacia `y` a **0.07*dt por frame** (0x858CA8); cuando `|y| < 0.1` y `|x| > 0.9` resetea y fija `moveMode = sign(x)` (activa **Crouch_Roll_R/L** IDs 59/57 mediante UseGun/PlayerControlDucked).
- `PlayerControlDucked` (0x687F30): lee el stick (`/128` = 0x858B88), magnitud m. Si hay apuntado/arma: gira el ped hacia el stick relativo a la camara (`desiredRot = atan2(...) - camHeading`) solo si la camara puede ("0x441E10" test de colision/espacio) y llama `ControlDuckMove`.
  Salir de agachado con stick: si `m > 0.5` (0x858B8C): sin boton sprint -> `BlendAnimation(grupo ped +0x4D4, ID 0 walk)`, `moveState=4 (WALK)`, `ped.moveBlend(+0x480->0x14)=1.5`; con sprint pad (0x5407A0) -> anim ID 1 (run), `moveState=6 (RUN)`, blend 1.5; blend delta 4.0 (`0x8D2FF0`). Si hay arma sin apuntar usa anim ID 0x32 (GunMove_FWD) y `UseGun::ControlGunMove`.
- Velocidad en cuclillas: no hay velocidad programada; el desplazamiento viene del root motion de las anims `GunCrouchFwd/Bwd` y `Crouch_Roll_*` (IDs 56..59) [NV].

## 5. HEADING / GIRO / STICK [V]

### 6. Duraciones y root motion de las animaciones (ped.ifp, ANP3) [V]
Parseado con script propio: cabecera nodo = 24 (nombre) + 3 dwords (tipo, nFrames, boneId); frame tipo 3 = 10 bytes (quat int16*4 + tiempo u16), tipo 4 = 16 bytes (+ trans int16*3 /1024). **Tiempo = u16 / 60 s.**
Nota: el motor reproduce a `speed` (JUMP_launch: speed = modificador de stats ~1.0).

| Anim | Duracion (s) | Root motion (m) primer->ultimo frame del nodo raiz |
|---|---|---|
| JUMP_launch / _R | 0.200 | y: 0 -> +1.036 (pero la fisica manda; el impulso real se aplica al terminar) |
| JUMP_glide | 0.500 (loop) | - |
| JUMP_land | 0.233 | y +0.92 |
| FALL_fall | 0.733 | - |
| FALL_glide | 0.800 | - |
| FALL_land | 0.467 | y +0.355 |
| FALL_collapse | 1.000 | y +2.475 |
| FALL_back / FALL_front | 0.733 / 0.733 | - |
| KO_skid_back | 0.967 | - |
| Getup / Getup_front | 1.367 | z -0.906 -> 0.023 |
| CLIMB_jump | 0.567 | y +0.197 |
| CLIMB_idle | 0.800 (loop, cuelgue) | - |
| CLIMB_Pull | 0.867 | **z +1.075** (y 0.199 -> 0.224) |
| CLIMB_Stand | 0.800 | **z +0.943**, y 0.199 -> 0.522 |
| CLIMB_Stand_finish | 0.200 | y +0.168 |
| CLIMB_jump_B | 0.967 | y 0.224 -> 1.114 (vault/paso sobre borde bajo) |
| CLIMB_jump2fall | 0.033 | - |
| EV_step | 1.067 | - |
| WEAPON_crouch | 0.800 | - |
| GunCrouchFwd | 0.733 | y +2.74 (=3.74 m/s) |
| GunCrouchBwd | 1.000 | y -1.865 (=1.87 m/s) |
| Crouch_Roll_L / _R | 0.933 | x -2.805 / +2.774 |
| DUCK_cower | 0.800 | - |
| WALK_start | 0.267 | y +0.472 |
| Run_stop | 0.867 | y +1.979 |
| Run_stopR | 0.800 | y +1.979 |
| walk_civi | 1.133 (ciclo) | y +1.725 -> 1.52 m/s |
| run_civi | 0.733 (ciclo) | y +3.156 -> 4.30 m/s |
| sprint_panic | 0.600 (ciclo) | y +3.542 -> 5.90 m/s |
| idle_stance | 1.500 (ciclo) | - |
(El grupo `man` de animgrp.dat usa `walk_civi, run_civi, sprint_panic, idle_stance, roadcross, walk_start`; CJ puede usar otros grupos con distintas velocidades.)

## 7. Secuencia de TREPAR (CTaskSimpleClimb, ProcessPed 0x680DC0) [V parcial]
- Campos: +0xD fase (0..6), +0xE sub-fase de posicionado (1..6), +0x2C anim assoc, +0x28 contador ms, +0xB auto-avance (npc), +0xC "forzar suelta".
- Tabla de offsets del objetivo segun sub-fase (relativos al punto de agarre; `x = -sin(h)*d`, `y = cos(h)*d`, h = heading de pared): fases 1,2 -> d=-0.4, dz=-1.1 (colgado 1.1 m bajo el borde, 0.4 m separado de la pared); fase 3 y 5 -> d=-0.4, dz=[0xC18F78] (variable runtime, no inicializada en el exe; [NV] 0); fase 4 -> d=0.15, dz=1.0 (subida sobre el borde); fase 6 -> d=0.5, dz=[0xC18F7C] [NV]. (Consts 0x8D2F1C..0x8D2F30)
- Movimiento hacia el objetivo: el ped no usa fisica; cada frame `vel = (target - pos)*0.25`-ish (const 0x858C84=0.25) mientras la distancia^2 > 0.1; si menor se pega al objetivo; `dt`-escalado con `ApplyMoveForce`.
- Fases por animacion activa (tabla de salto 0x681684 para IDs 0x80..0x85):
  1. CLIMB_jump (0x80, 0.567 s): al terminar avanza; para jugador sigue si se mantiene el boton (0x540750) o auto.
  2. CLIMB_idle (0x81, cuelgue): espera `JumpJustDown` (0x540770) o auto ([0xB]) -> pull; sin pulsar durante >0.5/1.0 s (`[0x28] > 1000 ms` o `>500` con idle) -> se suelta (`0x67DBE0` -> cae, y aplica `ApplyMoveForce(front * mass * -0.1)` = pequeno empujon atras, 0x681360).
  3. CLIMB_Pull (0x82, 0.867 s, z +1.075) -> CLIMB_Stand (0x83, 0.8 s, z +0.943) -> CLIMB_Stand_finish (0x84, 0.2 s) -> fin; al terminar limpia flags ped 0x200|0x100 y devuelve true.
  4. CLIMB_jump_B (0x85, 0.967 s) variante baja (vault; tipo 2 <= 1.4 m).
- Altura total subida cuando el agarre esta a H sobre el origen del ped: el ped queda a 1.1 m bajo el borde en el cuelgue; Pull+Stand = +2.02 m de root motion.
- Cancelacion: boton de soltar (`pad 0x5400D0`) -> `MakeAbortable`.

## 8. Resumen numerico rapido (para replicar) [V salvo indicado]
- dt = 1/50 s; gravedad ped: `vz -= 0.008*dt` por frame (en SI: 0.008*50*50 = 20 m/s^2); masa ped 70 (CPed ctor 0x5E82A4).
- Salto del jugador: espera 0.2 s (JUMP_launch), luego `vz = 8.5/70 = 0.1214 m/frame (6.07 m/s)`, `H = 0.10 / 0.10..0.17 / 0.17..0.22 m/frame` (5 / 5..8.5 / 8.5..11 m/s) segun walk/run/sprint x modificador de stats (~1). Apex = 0.92 m; duracion de aire a nivel = 2*0.1214/0.008 = 30 frames = 0.6 s; alcance sprint ~ 0.22*30 = 6.6 m (+ velocidad previa), run ~ 5.1 m (0.17), parado 3.0 m (0.10)... [NV alcance].
- Caida: FALL_fall con vz <= -0.1 (5 m/s); derribo con vz <= -0.4 (20 m/s) (equivale a caer desde 10 m: 0.4^2/(2*0.008) = 10 m); grito a vz*50 < -20 y >=500 ms en el aire.
- Altura de caida para vz=-0.1: 0.625 m bajo el apex.
- Giro: 9 deg/frame (450 deg/s).
- Agacharse: no si sprint/run (moveState>=6) siendo jugador.

## Estado
Resuelto: ids de anim; flujo de salto/aterrizaje/caida; impulso, gravedad, masa; umbrales de caida; sondas y rechazos de climb; duck; heading; duraciones y root motion de anims.
No resuelto / dudas:
- Dano real de caida: los valores 60/75 y 55/70 de `ComplexInAirAndLand::CreateNextSubTask` (0x67CE7C..0x67D199) pasan a `CPedDamageResponseCalculator`; falta leer 0x5E05B0 y 0x5A7950 para saber si son dano directo o intensidad. Comprobar si el dano aplica en saltos normales (FALL_fall arranca a vz<=-0.1, que un salto normal alcanza al caer).
- Climb: offsets runtime 0xC18F78/0xC18F7C y la maquina de estados completa de `0x680DC0` (0x6811CC.. 0x681617); funciones 0x67B5C0, 0x67E7B0, 0x67EF20, 0x67DBE0, 0x680570.
- SetMoveAnim (0x5E4A00): disparo de WALK_start/Run_stop; ControlButtonSprint (0x60A610) umbrales de sprint/energia.
- Formula exacta del modificador de stats `GetFatAndMuscleModifier(2)` (0x559BB3..): `~1 - 0.2*f(fat) - 0.1*(muscle-50)/950`, minimo 0.8 [NV].
Siguientes pasos (comandos): `powershell -NoProfile -File tools
e\dis.ps1 -Va 0x5e05b0 -Len 0x200`; `-Va 0x5a7950 -Len 0x40`; `-Va 0x5e4a00 -Len 0x600`; `-Va 0x680dc0 -Len 0x900`; `-Va 0x60a610 -Len 0x3b0`.
