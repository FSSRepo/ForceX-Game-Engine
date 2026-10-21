# GTA SA 1.0 (HOODLUM): subir y bajar del vehiculo - ingenieria inversa

Fuente: `D:\Games\GTA San Andreas\gta_sa.exe` (base 0x400000), leido con `tools\re\dis.ps1` y scripts python ad-hoc.
Marcas: **[V]** leido/deducido directo del codigo o datos; **[NV]** estimacion.
dt SA: 1.0 = 20 ms.

## Objetivos (estado)

1. Campos de `CVehicleAnimGroup` (tabla 0xc1cdc0): HECHO (ver sec. 1)
2. `CVehicle::ProcessOpenDoor` / angulo de puerta: HECHO (sec. 2)
3. LineUp / CarAlign / GoToCarDoor: PARCIAL (sec. 3)
4. EnterCar / LeaveCar: PARCIAL (sec. 4: IDs completos, arbol incompleto)
5. GetIn / GetOut / SetPedInAsDriver: HECHO (sec. 5)
6. Animaciones de conduccion: PARCIAL (sec. 6)

## 0. IDs de animacion de vehiculo (grupo `stdcaranims`, indice 88 de ms_aAnimAssocDefinitions @0x8aa5a8)

[V] Origen: nombres en 0x8a8f28 (40 punteros), descriptores {animId, flags} en 0x8a9f68. IDs consecutivos 0x15f..0x186.
Todos los grupos de coche (stdcar, lowcar, trkcar, van, coach, bus, dozer, kart, conv, hover, tank...) usan los mismos IDs; solo cambia el
bloque IFP (`ped`, `truck`, `van`, `coach`, `bus`, `dozer`, `kart`, `vortex`, `tank`, `BF_injection`...) y por tanto la anim real. Los grupos de moto 91..100 y avion 102/114/115/117 tienen su propia tabla de 40.

| ID | hex | nombre en ped.ifp (stdcar) |
|---|---|---|
| 351-354 | 0x15f-0x162 | CAR_align_LHS, CAR_align_RHS, CAR_alignHI_LHS, CAR_alignHI_RHS |
| 355-358 | 0x163-0x166 | CAR_open_LHS, CAR_open_RHS (puerta delantera), CAR_open_LHS, CAR_open_RHS (2a copia = puerta trasera) |
| 359-362 | 0x167-0x16a | CAR_getin_LHS, CAR_getin_RHS (delantera), idem trasera |
| 363 | 0x16b | (vacio en stdcar) getin extra |
| 364,365 | 0x16c,0x16d | CAR_pullout_LHS/RHS (sacar al conductor, jack) ; 366 0x16e vacio |
| 367-370 | 0x16f-0x172 | CAR_closedoor_LHS/RHS (delantera 0x16f/0x170, trasera 0x171/0x172) |
| 371,372 | 0x173,0x174 | CAR_shuffle_RHS (pasar al asiento del conductor) |
| 373-376 | 0x175-0x178 | CAR_getout_LHS/RHS (delantera 0x175/6, trasera 0x177/8); 377 0x179 vacio |
| 378,379 | 0x17a,0x17b | CAR_jackedLHS/RHS (ser sacado) |
| 380-383 | 0x17c-0x17f | CAR_close_LHS/RHS (cerrar desde fuera; delantera 0x17c/d, trasera 0x17e/f) |
| 384,385 | 0x180,0x181 | CAR_rollout_LHS/RHS (saltar fuera) |
| 386 | 0x182 | CAR_rolldoor (flags 0x18) |
| 387,388 | 0x183,0x184 | CAR_fallout_LHS/RHS |
| 389,390 | 0x185,0x186 | CAR_doorlocked_LHS/RHS |

Flags de descriptor: 0x18 (casi todos), 0x14 (jacked, fallout), 0x8d4 (rollout). [NV] significado de los bits (0x10 = ..., 0x8 = ...).

## 1. Tabla `CVehicleAnimGroupData::m_vehicleAnimGroups` (0xc1cdc0, stride 0x94)

### 1.1 Quien la llena [V]
No se calcula a partir de datos de animacion salvo los offsets de puerta. Se llena **leyendo `data\handling.cfg`**: lineas que empiezan por `^`
(parser en `cHandlingDataMgr::LoadHandlingData` 0x5bd830, bloque 0x5bdcc2..0x5be198; construye un temporal con `CAnimGroup::initAnimGroup` 0x5b9eb0
y lo copia con 0x5b9e40 a `0xc1cdc0 + id*0x94`). El indice de grupo de un vehiculo es el byte `tHandlingData+0xde` (columna `aj` de handling.cfg, "vehicle anim group").

Formato de linea `^`:
```
^ id  g1 g2  b0..b17  tGetIn tJumpOut tGetOut tJacked tFall  s0 e0 s1 e1 s2 e2 s3 e3  flags
```
(tokens 1..35; token1=id; token2=grupo 1 (se suma 0x58 => id de assoc group 0x58=stdcar...); token3=grupo 2 (+0x58); tokens 4..21 = 18 bits; 22..26 = 5 floats; 27..34 = 8 floats; 35 = flags).

Layout de `CVehicleAnimGroup` (0x94 bytes) [V]:

| off | tipo | contenido |
|---|---|---|
| +0x00 | u8 | assoc group 1 (= g1 + 0x58) |
| +0x01 | u8 | assoc group 2 (= g2 + 0x58) |
| +0x04 | u32 | mascara de 18 bits: bit k (k = categoria de anim) = 1 => usa el grupo 2 para esa anim (`GetGroup`) |
| +0x08 | u32 | flags especiales (token 35, ver abajo) |
| +0x0c | f32 | tiempo critico GetIn  (se usa para ids 0x15f..0x16b: align/open/getin) |
| +0x10 | f32 | tiempo critico JumpOut (rollout 0x180/0x181) |
| +0x14 | f32 | tiempo critico GetOut  (0x175..0x178) |
| +0x18 | f32 | tiempo critico Jacked  (0x17a/0x17b) |
| +0x1c | f32 | tiempo critico Fall    (0x183/0x184) |
| +0x20..0x2c | f32[4] | START de apertura/cierre de puerta por tipo: [0]=OpenOut (abrir desde fuera) [1]=CloseIn (cerrar desde dentro) [2]=OpenIn (abrir desde dentro al bajar) [3]=CloseOut (cerrar desde fuera) |
| +0x30..0x3c | f32[4] | STOP de los mismos 4 tipos |
| +0x40 + 12*i | CVector[7] | offsets de puerta cacheados (ver 1.4) |

Orden de columnas del token 22..26 -> +0x0c,+0x10,+0x14,+0x18,+0x1c es **[NV]** por el nombre del header de handling.cfg
(GetInTime, JumpOutTime, GetOutTime, JkdOutTime, FallTime); la asignacion de cada campo a cada ID de anim es [V] por `ComputeCriticalBlendTime`.
Orden tokens 27..34: pares (start,stop) = (27,28)=OpenOut, (29,30)=CloseIn, (31,32)=OpenIn, (33,34)=CloseOut: [V] por el codigo de parseo (27,29,31,33 -> +0x20.., 28,30,32,34 -> +0x30..).

Flags (token 35 / +0x08) [V] segun comentario del handling.cfg y accesores 0x645660/0x645680/0x6456a0:
1 = no cerrar puerta tras bajar (accesor 0x645660 devuelve !(f&1)); 2 = no cerrar tras subir (0x645680 devuelve !(f>>1&1));
4 = anims de kart; 8 = anims de camion; 16 = hover; 32 = anim especial puerta bloqueada; 64 = no abrir la puerta al subir (0x6456a0 devuelve !(f>>6&1)).

### 1.2 `CVehicleAnimGroup::GetGroup(animId)` 0x6e3b00 [V]
`cat = tabla[animId-0x15f]`; devuelve `byte[+1]` si `(flags4 & 2^cat)` != 0 si no `byte[+0]`. Categorias (0..17):
0 align(0x15f-0x162), 1 open front(0x163/4), 2 open rear(0x165/6), 3 getin front(0x167/8 y 0x16b), 4 getin rear(0x169/a), 5 pullout(0x16c-e),
6 closedoor front(0x16f/70), 7 closedoor rear(0x171/2), 8 shuffle(0x173/4), 9 getout front(0x175/6), 10 getout rear(0x177/8),
11 jacked(0x17a/b), 12 close front(0x17c/d), 13 close rear(0x17e/f), 14 rollout(0x180/1), 15 rolldoor(0x182), 16 fallout(0x183/4), 17 doorlocked(0x185/6).
(0x179 no tiene categoria: usa bit 2^animId = indefinido.)
Para el resto de los 18 bits del handling: el orden de columnas del token 4..21 es exactamente categoria 0..17 [V: bit k <- token 4+k].

### 1.3 `ComputeCriticalBlendTime(animId)` 0x6e3c80 [V]
0x15f..0x16b -> +0x0c ; 0x16c..0x174 -> 0.0 ; 0x175..0x178 -> +0x14 ; 0x179 -> 0 ; 0x17a,0x17b -> +0x18 ; 0x17c..0x17f -> 0 ;
0x180,0x181 -> +0x10 ; 0x182 -> 0 ; 0x183,0x184 -> +0x1c ; 0x185,0x186 -> 0 ; fuera de 0x15f..0x186 -> 0.

### 1.4 `ComputeAnimDoorOffsets(CVector* out, int idx)` 0x6e3d10 [V]
Cache en +0x40+12*idx. Si el vector es (0,0,0): anim = idx<7 ? {0:0x167,1:0x169,2:0x16b,3:0x175,4:0x177,5:0x17a,6:0x17b}[idx] : idx (se puede pasar un animId);
`grp = GetGroup(anim)`; `hier = CAnimManager::GetAnimation(anim, grp)` (0x4d3a60); `CAnimBlendHierarchy::...` (0x4d41c0, asegura que este cargada);
vector = **traslacion del ULTIMO keyframe de la primera secuencia (hueso raiz)** (keyframe comprimido stride 0x20 / descomprimido 0x14, offset +0x14..). Es decir, el desplazamiento total de root motion de esa anim (getin/getout), calculado en runtime la primera vez y cacheado. Si sigue siendo (0,0,0) usa 0.
Uso por `GetPositionToOpenCarDoor`: idx 0 (puertas 8 y 10: delantera), 1 (puertas 9/11: trasera), 2 (puerta 0x12) => anims 0x167/0x169/0x16b.
NOTA: los valores numericos dependen de ped.ifp (no volcados aun) => PENDIENTE leer del IFP (`anim\ped.ifp`, nombres CAR_getin_LHS etc.).

### 1.5 Datos reales de handling.cfg (lineas `^`, extracto)
Columnas: id g1 g2 | bits b0..b17 | tGetIn tJumpOut tGetOut tJacked tFall | s0 e0 s1 e1 s2 e2 s3 e3 | flags
Valores tipicos (grupo 0 estandar): tiempos criticos `0.5 0.0 -0.5 -0.3 0.3`; ventanas puerta (OpenOut 0.41->0.8, CloseIn 0.3->0.45, OpenIn 0.06->0.43, CloseOut 0.20->0.43), flags 0.
Grupos 0-2 (std, low, truck) comparten ventanas; grupo 2 (truck): ventanas 0.41/0.8, 0.3/0.45, 0.03/0.23 (OpenIn), 0.20/0.43, flags 40 (=8+32).
Motos (grupos 3..12): tiempos `0.5 0.0 -0.5 -0.3 0.3`, ventanas todas 0.0 (sin puerta), bits b1=3? (ver archivo).
Lista completa: `D:\Games\GTA San Andreas\data\handling.cfg`, lineas 407-470 (`^ n ...`).
Tabla de ids de grupo (comentario del archivo): 0 coche estandar, 1 coche bajo, 2 camion, 3 moto std, 4 sport, 5 vespa, 6 harley, 7 dirt, 8 wayfarer, 9 bmx, 10 mtb, 11 choppa, 12 quad, 13 van, 14 rustler, ... 
Mapa assoc group (+0x58): 88 stdcar,89 lowcar,90 trkcar,91 stdbike,92 sportbike,93 vespa,94 harley,95 dirt,96 wayf,97 bmx,98 mtb,99 choppa,100 quad,101 van,102 rustler,103 coach,104 bus,105 dozer,106 kart,107 conv,108 mtrk,109 train,110 stdtall,111 hover,112 tank,113 bfinj,114 learplane,115 harrplane,116 stdcarupright,117 nvada.

## 2. Puerta: `CVehicle::ProcessOpenDoor` 0x6d56c0 (slot +0x70 de CAutomobile; la vtable de CAutomobile @0x871120 apunta aqui) [V]

Firma: `ProcessOpenDoor(CPed* ped, uint doorNode, uint animGroup, uint animId, float t)`; `t` = tiempo actual de la animacion en **segundos**
(el que pasan los tasks, `CAnimBlendAssociation::currentTime`). doorNode 8=RF 9=RR 10=LF 11=LR (nodos de coche) -> eDoors: 8->3(FR) 9->5(RR) 10->2(FL) 11->4(RL).
Primero llama `vtbl+0x98(door)` (IsDoorMissing); si la puerta falta, no hace nada.
Aplica `vtbl+0x6c` = `OpenDoor(ped, doorNode, door, ratio, 1)`; `vtbl+0x78` = `GetDoorAngleOpenRatio(door)`.
`ratio` es la fraccion del angulo de apertura maximo (0 = cerrada, 1 = abierta del todo).

`(s,e) = CVehicleAnimGroup::GetDoorTimes(handlingGroup, kind)` = ({+0x20+4k}, {+0x30+4k}) (funcion 0x645630).
Segun el animId (indice animId-0x163):

| animIds | kind (s,e de la tabla) | comportamiento (t = tiempo de anim) |
|---|---|---|
| 0x163-0x166 open (abrir desde fuera) | 0 OpenOut | t<s: ratio=0; s<t<e: r=(t-s)/(e-s), solo se aplica si r > ratio actual; t>e: ratio=1 |
| 0x167-0x16b getin, 0x173/4 shuffle, 0x179 | - | NO hace nada (la puerta queda como estaba, abierta) |
| 0x16c-0x16e pullout | - | si assoc group in {std,low,trk,coach,bus,conv,mtrk,stdtall,bfinj}: ratio=1 (puerta abierta); resto: nada |
| 0x16f-0x172 closedoor (cerrar desde dentro) | 1 CloseIn | t<s: ratio=1; s<t<e: r=1-(t-s)/(e-s), solo si r < ratio actual; t>e: ratio=0 |
| 0x175-0x178 getout | 2 OpenIn | t<s: ratio=0; s<t<e: r=(t-s)/(e-s) (solo si crece); t>e: ratio=1 |
| 0x17a,0x17b jacked | constantes | t<=0.1: ratio=0; 0.1<t<0.4: r=(t-0.1)*3.3333 (=1/0.3); t>=0.4: ratio=1 |
| 0x17c-0x17f close (cerrar desde fuera) | 3 CloseOut | t<s: ratio=1; s<t<e: r=1-(t-s)/(e-s) (solo si decrece); t>e: ratio=0 |
| 0x180,0x181 rollout | constantes (solo std/low/trk) | s=0.01, e=0.1: t<s ratio=0; s<t<e r=(t-s)/(e-s) (solo si crece); t>e ratio=1 |
| 0x182 rolldoor | constantes (std/low/trk) | t<=0.05: nada; 0.05<t<0.2: r=(t-0.05)/0.15 ; 0.2..0.3: sin cambio; 0.3<t<0.475: r=1-(t-0.3)/0.175 ; t>0.475: ratio=0 |
| 0x183,0x184 fallout | constantes | igual que jacked (0.1->0.4, factor 3.3333) |

Observaciones: [V] "solo si crece/decrece" evita que la puerta retroceda cuando el ratio ya esta mas avanzado (p.ej. por una puerta que esta siendo abierta por otra causa).
Con handling del grupo 0: abrir desde fuera ocurre entre t=0.41 s y 0.8 s de CAR_open_*; cerrar desde dentro entre 0.30 y 0.45 s de CAR_closedoor_*;
al bajar, la puerta (OpenIn) se abre entre 0.06 y 0.43 s de CAR_getout_*; cerrar desde fuera 0.20-0.43 s de CAR_close_*.

### 2.1 Angulo de la puerta (`CDoor`, struct de 0x18 bytes en CAutomobile+0x5a0 + 0x18*door) [V]
```
+0x00 float openAngle   (angulo maximo, con signo)
+0x04 float closedAngle
+0x08 s8 direccion, +0x09 eje, +0x0a/+0x0b flags
+0x0c float angle actual, +0x10 prevAngle, +0x14 angVel
CDoor::Open(ratio) 0x6f4790: prev=angle; if ratio<1 { angle=ratio*openAngle; if angle==0 angVel=0 } else { angle=openAngle; flag(+0xb)=1 si dir>=0 }
CDoor::GetAngleOpenRatio 0x6f47e0: openAngle==0 ? 0 : angle/openAngle
```
`CAutomobile::OpenDoor` 0x6a6ae0 ademas aplica el angulo al frame del nodo de la puerta (rotacion sobre el eje `+0x5c2`) y, si se abre del todo desde animacion, reproduce sonido
(0x42700000=60 / 0x42f00000=120 son parametros del emisor de particulas/sonido [NV]).
Indice de puerta d -> struct en `CAutomobile + 0x5b8 + 0x18*d` (0 capot, 1 maletero, 2 FL, 3 FR, 4 RL, 5 RR; `GetDooorAngleOpenRatio` 0x6a2270 hace ecx+(d*3+0xb7)*8) [V].
Angulo maximo `openAngle` (CDoor+0), fijado en el ctor `CAutomobile::CAutomobile` 0x6b0a90 (0x6b0ca8-0x6b0e45) [V]:
- puertas delanteras: FL(2) = -1.2566 rad (-72 grados, 0xbfa0d97c) y FR(3) = +1.2566 (0x3fa0d97c) por defecto; si el bit1 de `veh+0x429` esta puesto (derivado del flag de handling `+0xcc & 2`) -> -pi/2 / +pi/2 (90 grados). closedAngle=0, eje=2 (`+0x5f2`), dir=0x14 (`+0x5f0`).
- puertas traseras RL(4)/RR(5): -1.2566/+1.2566 por defecto; si bit0 de `veh+0x429` (handling `+0xcc & 1`): -pi/2/+pi/2 (0x6b0d50-0x6b0da4).
- capot (0): +-0.9448 rad (54.1 grados; 0x3f71463b): negativo si handling `+0xcc & 0x10`; maletero (1): -0.9448 por defecto, +pi/2 si handling `& 0x40`, 0 con `& 0x20` (hanging boot).
Resumen: angulo de puerta delantera = ratio * 1.2566 rad (o 1.5708) con signo segun lado [V].

## 3. GetPositionToOpenCarDoor (0x64e740) [V]
`CVector* GetPositionToOpenCarDoor(CVector* out, CVehicle* veh, int doorNode)`; doorNode 8=RF 9=RR 10=LF 11=LR 0x12=maletero/otro.
`off = ComputeAnimDoorOffsets(group, doorNode in {10,8}?0 : {11,9}?1 : 0x12?2)`;
`seat = ModelInfo->seatPositions[...]` (modelinfo+0x5c; la lateral (puerta) usa el slot del asiento delantero (+0x30) o trasero (+0x3c)),
`side offset = handling.SeatOffsetDistance (+0xd4)` (0 para algunas combinaciones de grupo 0x65 con puertas traseras);
`localPos = seat +- (SeatOffsetDistance) - rotate(off, X invertido para puertas derechas 8/9)`; lado izq (10/11): x=-(seat.x+off_dist).
`pos = vehicleMatrix * localPos`; en coches con handling flag bit 8 (0x8: convertible?) o estado 1/4 se ajusta Z con `0.95 - vtbl+0xd4()`.
Para bicis/motos (veh+0x590==9 o handling flag bit 9) usa otra rama (0x6bf230).
(Detalle completo: ver pseudocodigo Ghidra `FUN_0064e740`; aqui resumido.)

## 3. Alineacion y llegada a la puerta

### 3.1 CTaskSimpleCarAlign (ctor 0x645b40, ProcessPed 0x64d6d0) [V]
Campos: +8 bool terminado, +0xc CAnimBlendAssociation*, +0x10 vehiculo, +0x14 CVector, +0x20 puerta (nodo 8..11/0x12), +0x24 CTaskUtilityLineUpPedWithCar*.
`ProcessPed`: si `terminado` o sin vehiculo -> true (fin). Si no hay anim (+0xc==0): llama 0x645cb0 (orienta al ped) y 0x64ac00 (arranca la anim CAR_align*; el callback 0x645e40 pone +0xc=0 y +8=1 al terminar) y devuelve false. Mientras corre la anim: `SetPedPosition` (0x645c70) llama cada frame `GetPositionToOpenCarDoor(&pos, veh, puerta)` y `LineUp::ProcessPed(ped, veh, assoc)` (0x6513a0). La tarea acaba cuando acaba la anim CAR_align_* (duracion = la de ped.ifp).
0x645cb0 (inicio): si el coche esta quieto (`veh+0x590`==0) calcula la direccion ped->puerta en XY y, si el producto escalar con la direccion lateral (x -1 para puertas 10/11) esta en (0, 0.1), pone `ped.targetRot(+0x55c)` y `ped.curRot(+0x558)` = atan2 hacia el punto (giro instantaneo) [V estructura, NV umbral exacto].

### 3.2 CTaskUtilityLineUpPedWithCar::ProcessPed 0x6513a0 (ped, veh, assoc) [V parcial]
Estado (this): +0..+8 CVector (velocidad residual), +0xc float Z objetivo, +0x10 u32 timestamp fin del giro (ms, CTimer 0xb7cb84), +0x14 modo (0,1,2), +0x18 nodo de puerta.
1. Rumbo objetivo `ped+0x55c` = atan2(veh.fwd.y, -veh.fwd.x) (rumbo del coche); puertas 8/9 (derecha) -PI [`fsub 3.14159`], puerta 0x12 (maletero/otro) +PI, resto: rumbo del coche (puertas izquierdas) [V formas, NV signo exacto]. Con modo +0x14==2 no se recalcula.
2. Progreso `p = assoc.currentTime / hier.totalTime (hier+0x10)`; tiempo critico `Tc = ComputeCriticalBlendTime(grupo, animId)` (0x64e4b0). Si |Tc| >= 10 se interpreta como Tc' = |Tc| - 11 (p.ej. 11.5 -> 0.5) con signo [NV]; si no Tc' = |Tc|. Con eso se calcula un factor de mezcla Z `r` (0..1): getin (0x167-0x16b): r = 1 - p/Tc' (limitado), getout (0x175-0x178): r = 1 - clamp(p/Tc') ...; align/open: r=0 (salvo Tc>=10); jacked/pullout/etc: valores 0/1 segun grupo y flag 32 (`CVehicleAnimGroup flags>>5`) [V estructura (0x651503-0x651885), NV semantica fina].
3. Punto destino: modo 0 -> `GetPositionToOpenCarDoor`; otro -> `0x650a80(veh, ped, factor, anim)`. La Z destino = ped.Z - halfHeight(vfunc +0xd4) + 1.0, buscando suelo (0x616920) y limitada por debajo a (suelo - 0.5); en moto (+0x590==5) +1.0.
4. Rotacion: mientras `CTimer ms < +0x10`: `frac = (+0x10 - ahora) * (1/600)` (0.0016667); `ped.rot(+0x558) = curr - wrap(curr - target) * (1 - frac)` con envoltura +-2PI (6.28319); al expirar `+0x558 = +0x55c` [V].
5. Durante getin/getout (0x167-0x16a, 0x175-0x178) la posicion sigue el **root motion** de la anim: toma el ultimo keyframe (ComputeAnimDoorOffsets, quaternion slerp 0x7eb5c0/0x7ec220 con polinomio) y mueve al ped a `puerta + R_veh * offset * p`; Z mezclada por `r`. Se escribe la posicion (0x54f610) cada frame.
Detalle numerico del polinomio de interpolacion (0x651ee4-0x651f67, constantes 0x86d2a4-0x86d2b0 y 0x864e0c, 0x85f0a0) NO decodificado: es el polinomio de aproximacion de sin() del slerp de quaternions de RenderWare [NV].

### 3.3 CTaskComplexGoToCarDoorAndStandStill (ctor 0x645780, CreateFirstSubTask 0x64d440) [V]
Campos: +0xc vehiculo, +0x10 puerta (nodo), +0x14 bool, +0x18 moveState, +0x28 timestamp, +0x34 indice de puerta, +0x38 CVector punto, +0x44 puerta fijada (0 = elegir la mas cercana), +0x48/+0x49 flags.
CreateFirstSubTask: 1) 0x6458a0 (si no es posible -> 0xcb StandStill). 2) `+0x48` -> 0xca Pause(1). 3) Punto = `GetPositionToOpenCarDoor` (si hay puerta fijada +0x44) o `GetNearestCarDoor` 0x6528f0 / `GetNearestCarPassengerDoor` 0x650bb0; se guarda en +0x38 y se marca timestamp. 4) `0x651210(ped,veh,pos)` (punto dentro de cajas de colision del coche) -> true: 0x389 CTaskComplexMoveFollowPointRoute; false: 0x384 CTaskSimpleGoToPoint(pos, moveState). Puerta ocupada: 0x32a o 0x33c.
IDs (CreateSubTask 0x64a5f0): 0xca SimplePause(1), 0xcb SimpleStandStill(1,0,0,8.0), 0x32a WaitForDoorNotToBeInUse, 0x33c CarGoToPointNearDoorUntilDoorNotInUse, 0x384 SimpleGoToPoint, 0x389 MoveFollowPointRoute.
Tolerancia/radio de llegada y ControlSubTask (0x64a820): PENDIENTE.

## 4. CTaskComplexEnterCar

### 4.1 Tabla ID -> subtarea (CreateSubTask 0x63e040; ctor entre parentesis) [V]
| ID | clase |
|---|---|
| 0xc8 | CTaskComplexFallAndGetUp (0x6786c0) |
| 0xcb | CTaskSimpleStandStill (0x62f310) |
| 0x2c0 | CTaskComplexLeaveCar (0x63b8c0) |
| 0x2cb | 0x63b5e0 (nombre del simbolo: CTaskComplexEnterBoatAsDriver; ctor es de la 2a rama) [NV] |
| 0x2cf | CTaskSimpleCarDriveTimed (0x5ff940) |
| 0x320 | CTaskComplexGoToCarDoorAndStandStill (0x645780) |
| 0x321 | CTaskSimpleCarAlign (0x645b40) |
| 0x322 | CTaskSimpleCarOpenDoorFromOutside (0x645e50) |
| 0x323 | CTaskSimpleCarOpenLockedDoorFromOutside (0x646070) |
| 0x324 | CTaskSimplePickUpBike (0x6461b0) |
| 0x325 | CTaskSimpleCarCloseDoorFromInside (0x646300) |
| 0x326 | CTaskSimpleCarCloseDoorFromOutside (0x6464f0) |
| 0x327 | CTaskSimpleCarGetIn (0x646690) |
| 0x328 | CTaskSimpleCarShuffle (0x646810) |
| 0x329 | ninguna |
| 0x32a | CTaskSimpleCarWaitForDoorNotToBeInUse (0x646b70) |
| 0x32b | CTaskSimpleCarSetPedInAsPassenger (0x646fe0) |
| 0x32c | CTaskSimpleCarSetPedInAsDriver (0x6470e0) |
| 0x330 | CTaskSimpleCarSetPedOut (0x6478b0) |
| 0x334 | CTaskSimpleCarSlowDragPedOut (0x647fe0) |
| 0x33d | CTaskSimpleWaitUntilPedIsOutCar (0x649450) |
| 0x516 | evento (0x63a110) |
(0x32d-0x32f, 0x331-0x333: mismo `jmp` a default). Jump tables: 0x63e960 (0x320..0x324), 0x63e974 (0x326..0x32c).

### 4.2 CreateNextSubTask (0x63e990; jump table 0x63f914 para 0x320..0x323) [V parcial]
- Tras 0x320 (GoToCarDoor): si llego ([sub+0x49]) toma la puerta (this+0x14=sub+0x34) y el punto (this+0x2c=sub+0x38); `CVehicle::AnyOfDoorsReady` (0x64ec90); si hay ocupante vivo en el asiento objetivo -> espera 0x32a o jack; si puede entrar: SetVehicleFlags (0x63ab90, 0x63ac10, 0x63ac80, 0x63acc0) y crea **0x321 Align** (salvo moto `veh+0x590==9` o `veh+0x594==2`, donde delega en vtbl+0x2c).
- Tras 0x321 Align: `CreateNextSubTask_AfterSimpleCarAlign` 0x63f970 decide abrir (0x322; 0x323 si puerta bloqueada; 0x324 moto).
- Tras 0x323 (bloqueada): guarda el float +0x48 en `veh+0x3d0` y crea 0xcb StandStill.
- Tras 0x322: si el coche se mueve (|v_xy| de `veh+0x44/+0x48` > umbrales `[0x86f104]`/`[0x86f108]`, por ped no-jugador/jugador) se cancela (0xd0/0x334 SlowDragPedOut); si hay otro ocupante -> 0x334; si libre: 0x327 GetIn -> 0x32c/0x32b SetPedInAs* -> 0x325 CloseDoorFromInside. Si el flag de grupo "no cerrar tras subir" (bit1 en +0x8) se omite 0x325.
[Arbol completo 0x63f1e3..0x63f8b8 y ControlSubTask 0x63a890: PENDIENTE.]

Todos los GetTaskType (leidos de las funciones `mov eax,id; ret`) [V]: 0x2bc EnterCarAsPassenger, 0x2bd EnterCarAsDriver, 0x2c0 LeaveCar, 0x2c1 LeaveCarAndDie, 0x2c2 LeaveCarAndFlee, 0x2c3 LeaveCarAndWander, 0x2c5 SimpleCarDrive, 0x2cd EnterAnyCarAsDriver, 0x2ca LeaveAnyCar, 0x2cf SimpleCarDriveTimed, 0x320 GoToCarDoorAndStandStill, 0x321 CarAlign, 0x322 CarOpenDoorFromOutside, 0x323 CarOpenLockedDoorFromOutside, 0x324 PickUpBike, 0x325 CarCloseDoorFromInside, 0x326 CarCloseDoorFromOutside, 0x327 CarGetIn, 0x328 CarShuffle, 0x329 CarWaitToSlowDown, 0x32a CarWaitForDoorNotToBeInUse, 0x32b SetPedInAsPassenger, 0x32c SetPedInAsDriver, 0x32d CarGetOut, 0x32e CarJumpOut, 0x32f CarForcePedOut, 0x330 CarSetPedOut, 0x334 CarSlowDragPedOut, 0x335 CarSlowBeDraggedOut, 0x336 CarSetPedSlowDraggedOut, 0x337 ComplexCarSlowBeDraggedOut, 0x33c CarGoToPointNearDoorUntilDoorNotInUse, 0x33d WaitUntilPedIsOutCar, 0x341 WaitUntilPedIsInCar, 0x342 CarFallOut. Otros: 0xca Pause, 0xcb StandStill.

### 4.3 CTaskComplexLeaveCar (ctor 0x63b8c0; CreateFirstSubTask 0x641fc0; CreateNextSubTask 0x6419f0; ControlSubTask 0x6421b0; CreateSubTask 0x641530) [V parcial]
ctor(veh, door, delayTime(ms), bool leaveAsDriverFlag?, bool dontCloseDoor?): campos +0xc veh, +0x10 puerta (indice 0..3: 0 conductor, 1 pasajero delantero, 2/3 traseros; se resuelve a nodo), +0x14 tiempo de espera/frenado, +0x18 bool (hurry/"force"), +0x19 bool "no cerrar la puerta", +0x1a bool, +0x24 = 0xf (tiempo de pausa), +0x28 = 4.0f, +0x2c = 1.0f (parametros de blend de salto) [V valores, NV significado exacto].
CreateFirstSubTask: ped muerto (`0x530`==0x3f) o fuera del coche -> Pause (0xca, -1). Barco (veh+0x590==5) -> subtarea de salir del barco (0x63bb60). Si no -> **CTaskSimpleCarDriveTimed(veh, +0x14)** (0x2cf): sigue conduciendo/frenando hasta tiempo o hasta que el coche baje de velocidad.
ControlSubTask: si el ped NO esta ya fuera (flag `ped+0x46c bit 0x100` = en vehiculo) y la subtarea actual no es 0x330/0x32e/0xce/0xd4 -> fuerza CreateNext con 0x330 (SetPedOut). Si subtarea es 0x329 y `+0x18==0` hay un abort manual.
CreateNextSubTask (0x6419f0) por subtarea actual:
- tras 0x2cf DriveTimed: si [+0x1a] -> 0xd4 sino 0x330 (o sigue con 0x329 WaitToSlowDown).
- 0x329 WaitToSlowDown: pregunta al coche (vtbl+0xe4) si puede salir (volcado?); sino espera (0x32a). Elige puerta: `CCarEnterExit::IsRoomForPedToLeaveCar` (0x6504c0) por asiento: conductor (veh+0x460) puerta 0, pasajero (0x464) 1, trasero (0x468/0x46c) 2/3 (jump table 0x641f84 con tests de `veh+0x486` bits 1,2,4,8 = asiento bloqueado). Guarda [+0x10]=puerta valida.
- Si hay puerta valida -> (opcional 0x337 SlowBeDraggedOut si [+0x1a]) -> **0x32d CarGetOut**; si no hay hueco -> **0x32e CarJumpOut** (si el coche esta volcado o a mucha velocidad: `veh+0x4f8`/estado) o 0x32f ForcePedOut.
- tras 0x32d GetOut: si `[+0x19]==0` y puerta con hoja (`CCarEnterExit::CarHasDoorToCloseOnExit` 0x64ee10) y `veh+0x4f8 != 1` -> **0x326 CarCloseDoorFromOutside**; luego 0x330 SetPedOut (si ocurre cierre se hace `DamageManager::SetDoorStatus`).
- tras 0x326: 0x330 SetPedOut -> (si [+0x1a]) 0xd4 con parametros 0xf / 1000.0 / 0.5 (anim de caida/espera) -> si coche no es barco/bici y no `[+0x30]`: 0xce o termina (null).
La regla "puerta bloqueada / coche en movimiento / prisa": el salto (0x32e) y 0xd4 dependen de [+0x1a] y de `veh+0x594` (clase de vehiculo: 0xa..), y `CarJumpOut` se escoge cuando `veh` esta en movimiento y no hay puerta libre [NV: condiciones exactas no verificadas, ver 0x641c89-0x641d3a].

## 5. GetIn / GetOut / SetPedInAsDriver

### 5.1 Animaciones y blend [V]
- Todas las anims de entrada/salida se arrancan con `CAnimManager::BlendAnimation(ped.clump(+0x18), assocGroup=CVehicleAnimGroup::GetGroup(animId), animId, blendDelta)` (0x4d4610) y registran un callback de fin con `CAnimBlendAssociation::SetFinishCallback` (0x4cebe0).
- blendDelta: open/closedoor/close/getin/shuffle = **4.0** (0x40800000, entrada de blend en 0.25 s); getout = **1000.0** (0x447a0000, corte instantaneo); alguna (cerrar, 0x64af13/0x64bf2e) 8.0 (0x41000000).
- Seleccion de anim por puerta (nodo): open: 10->0x163 8->0x164 11->0x165 9->0x166; getin: 10->0x167 8->0x168 11->0x169 9->0x16a, 0x12->0x16b; closedoor(dentro): 10->0x16f 8->0x170 11->0x171 9->0x172; getout: 10->0x175 8->0x176 11->0x177 9->0x178; close(fuera): 10->0x17c 8->0x17d 11->0x17e 9->0x17f. (LHS = puertas izquierdas 10/11; RHS = derechas 8/9; "delantera" = 10/8, trasera = 11/9.)
- Si el coche esta volcado/casi (CarGetOut 0x647330: `veh+0x590==0` y 0x463830 true, con la componente up.z del coche > -0.5 (puertas 10/11 usan 0x858f40=-0.5; otras 0.5)) se usa `CAR_crawloutRHS` (anims default-group ids 108/109, grupo 0) en lugar de CAR_getout [V].
- Fin de getin/getout: el callback (0x6467f0 para GetIn; 0x647480 GetOut) pone `done=1`, `assoc=null` y fija blendDelta = -1000 (0xc47a0000) para cortarla.

### 5.2 Posicion del ped durante getin/getout [V]
`SetPedPosition` de GetIn (0x6467a0) y GetOut (0x6472f0) y de Open/Close puertas (0x645f80, 0x6463f0, 0x6465e0) llaman TODOS a `CTaskUtilityLineUpPedWithCar::ProcessPed` (sec. 3.2) cada frame: el ped NO sigue el root motion crudo del IFP; el motor fuerza la posicion/rotacion: posicion = punto de puerta + offset de root motion (ultimo keyframe de la anim escalado por el progreso p) y rumbo del coche con giro suave. En GetOut: si la anim ya termino (blend==0, `[assoc+0x18]==0`) llama `CPed::SetPedPositionInCar` solo si no hay anim.
Progreso GetIn: `p = assoc.currentTime / hierarchy.totalTime` (0x6467c0; 1.0 si done).
Empuje al coche (moto, `veh+0x594==0xa`) durante GetIn: mientras `t < 10*0.0333=0.3333 s` (0x8d2ed4 * 0x858f10) aplica a la moto una fuerza `dt * veh.mass(+0x8c) * 0.01 * ped.right` (0x5429f0, ApplyMoveForce) [V].
GetIn al terminar: `ped.flags(+0x40) |= 0x800000|0x40000`, `veh+0x42a &= ~0x20` (marca vehiculo ocupado) [V valores, NV nombres de flags]. GetOut al terminar: se revierten (`&= ~0x800000; &= ~0x40000; veh+0x42a |= 0x20`).
GetOut cada frame con blend==1.0 (`assoc+0x18`): `ped+0x46c |= 0x2000`; y si la puerta debe abrirse (flag `[+9]` calculado por 0x64edd0(veh,puerta)) y no es salto (`[+0x10]==0`): llama `veh->ProcessOpenDoor(ped, puerta, grupo, animId, assoc.currentTime)` (vtbl+0x70). Idem GetIn/Open/Close llaman el mismo virtual (ver sec. 2). Es decir, **la puerta se anima por el tiempo de la anim (segundos)**, no por interpolacion propia.

### 5.3 CTaskSimpleCarSetPedInAsDriver::ProcessPed 0x64b950 (el 'enganche' al asiento) [V]
Se ejecuta UNA vez (devuelve true) tras terminar GetIn (y tras Shuffle si aplica):
1. `ped.attachedVehicle(+0x58c) = veh` (con conteo de refs).
2. Si ya hay otro conductor y no esta en tareas de entrar/salir (0x2c0,0x338,0x2c1,0x33a) lo expulsa con CTaskSimpleCarSetPedOut.
3. `ped.targetRot(+0x55c) = ped.rot(+0x558)`.
4. Si `this+0x18` (colocar en el centro): `ped.pos = veh.pos`.
5. `CVehicle::SetDriver(ped)` (0x6d16a0 => veh+0x460 = ped), `ped+0x46c |= 0x100` (bInVehicle), jugador: `playerData+0x8d = 1`, veh+0x42a |= 2 (ocupado/robado), vida del vehiculo +0x45c: 0xffff -> 0x3a98 (15000 ms), etc.
6. `CPed::SetPedPositionInCar` (0x5df910) [ver 5.4], `ped.bUsesCollision(+0x1c bit0) = 0`, `ped.flags +0x46c bit 0x2000` segun `veh+0x429 & 4` salvo modelo 0x1af.
7. `CCarEnterExit::RemoveGetInAnims` (0x64f6e0) y `AddInCarAnim` (0x64f720, ver sec. 6): anim de sentado con blend 1000 (instantaneo).
8. Crea `CTaskSimpleCarDrive(veh, this+0x14, 0)` (size 0x60, ctor 0x63c340) y lo pone como tarea primaria por defecto (slot 4 = TASK_PRIMARY_DEFAULT) via 0x681af0.
9. Si es policia y el coche policial: evento de robo (0x4b1740/0x4b17f0) [NV].

### 5.4 CPed::SetPedPositionInCar (0x5df910) [V]
Cada frame que el ped esta dentro, `ped.pos = vehMatrix * seatOffset` + pequeno offset y `ped.rot = rumbo del coche` (atan2(fwd.y, -fwd.x)), escribiendo `ped+0x558` y `+0x55c`. `seatOffset` viene de `modelInfo+0x5c` (tabla de posiciones de asiento de CVehicleModelInfo, 'dummies' ped_frontseat/backseat): conductor = entrada [0] (`+0x30` salvo si `modelInfo+0x3c==5`), x NEGADA salvo moto/barco (`veh+0x590` 5 o 9); pasajero delantero (+0x464) = entrada +0x30 o +0x3c segun clase; traseros (+0x468,+0x46c) = entrada +0x3c (x negada para +0x468). Motos (`veh+0x594==0xa`): z -= |veh+0x814| * 0x858cdc. Modelo 0x214 (cierto camion) ajusta z con el eje. Para el grupo anim 0x65 ('van', bancos laterales) el ped trasero gira: rumbo -pi/2 (+0x468) / +pi/2 (+0x46c).
La Z/XY exacta de cada asiento depende del modelo (vehicles.dff dummies) => no son constantes del exe.

## 6. Animaciones al conducir [V parcial]
`CCarEnterExit::AddInCarAnim(veh, ped, isDriver)` 0x64f720 usa `BlendAnimation(clump, group, id, 1000.0)`:
| caso | grupo/anim |
|---|---|
| vehiculo con grupo propio (vtbl+0xbc != null) | grupo del vehiculo, id 0xc2 (194) |
| conductor barco (`veh+0x590==5`) | default, 0x51 = DRIVE_BOAT |
| conductor coche normal | default, 0x3c = CAR_sit |
| conductor, `veh+0x429 & 8` (low rider) | default, 0x3d = CAR_Lsit |
| pasajero | default, 0x40 (64) normal / 0x41 (65) si bit 8 |
Nombres de default group (`ped.ifp`) para ids 60-98 [V pareo nombre-id por indice de las tablas 0x8d0f90 / 0x8a7788]: 60 CAR_sit, 61 CAR_Lsit, 62 CAR_sitp?, 63 CAR_sitpLO?, 64 CAR_sit_weak?, 65 CAR_sit_pro? (el pareo nombre<->id de 62..65 esta cruzado entre tablas: nombres en orden sit_weak,sit_pro,sitp,sitpLO con ids 64,65,62,63; semantica de pasajero [NV]), 66 DRIVE_L, 67 Drive_R, 68 Drive_LO_l, 69 Drive_LO_R, 70 Drive_L_weak, 71 Drive_R_weak, 72 Drive_L_pro, 73 Drive_R_pro, 74 Driveby_L, 75 Driveby_R, 76 DrivebyL_L, 77 DrivebyL_R, 78 CAR_LB (mirar atras), 79 CAR_LB_weak, 80 CAR_LB_pro, 81 DRIVE_BOAT, 82-84 DRIVE_BOAT_L/R/back, 85 Drive_L_slow, 86 Drive_R_slow, 87-90 weak/pro slow, 91 Drive_truck, 92 Drive_truck_L, 93 Drive_truck_R, 94 Drive_truck_back, 95 Kart_drive, 96 Kart_L, 97 Kart_R, 98 Kart_LB. 108/109 CAR_crawloutRHS, 110/111 CAR_rollout_LHS/RHS (default group), 169/170 Smoke_in_car/pass_Smoke_in_car, 99-106 BIKE_*.
Variantes weak/pro/slow/LO se eligen por estilo de conduccion (walkstyle del ped, low-rider) y velocidad [NV]. Dos IDs de coche relevantes en CarDrive: cierre de ventana/puerta 'rolldoor' 0x182 con blend 1.0: `CTaskSimpleCarDrive::ProcessPed` llama `veh->ProcessOpenDoor(driver, 10, group, 0x182, 1.0)` (puerta 10, anim rolldoor, t=1.0 => puerta cerrada) al subir (0x644806-0x64482a) y cada ~, y `StartBopping` (cabeceo con musica). `CTaskSimpleCarDrive::SetPedPosition` (0x63c770): si hay LineUp util (this+0x10) lo ejecuta con la anim asociada (ids de la tabla 0x8d2e9c..0x8d2ecc) durante los primeros frames y luego lo borra.
NO resuelto: el blend de volante (DRIVE_L/R segun giro, LB al mirar atras): se hace en `CTaskSimpleCarDrive::ProcessPed` 0x644470 (usa `CAnimBlendAssociation` por tablas) o en `CAutomobile` [no localizado].

## Estado
Resuelto [V]: 0 (IDs), 1 (todos los campos y el origen; algoritmo de offsets), 2 (ProcessOpenDoor y angulos), 3.1 (Align), 5.1-5.4, 4.1 (tabla de IDs), parte de 3.3, 4.2, 4.3, 6.
Parcial / no resuelto:
- Valores numericos de ComputeAnimDoorOffsets (dependen de ped.ifp; hay que extraer el ultimo keyframe del hueso raiz de CAR_getin_LHS/RHS y CAR_getout_*, p.ej. parseando `D:\\Games\\GTA San Andreas\\anim\\ped.ifp` (formato ANP3) o con gtasa tools) y de los seatPos por modelo (vehicles.dff).
- LineUp::ProcessPed: constantes del polinomio de slerp y semantica exacta del tiempo critico negativo/>=10 (0x651503-0x651885) y signo del rumbo por puerta (0x651421-0x6514b0).
- Tolerancias de llegada de GoToCarDoorAndStandStill (`ControlSubTask` 0x64a820; radio de `CTaskSimpleGoToPoint` 0x667cd0 y moveState +0x18) y distancia a la que se inicia la entrada (`CTaskComplexEnterCar::CreateFirstSubTask`, 0x640xxx; `IsPlayerToQuitCarEnter` 0x64f240).
- Arbol completo de CreateNextSubTask de EnterCar (0x63f1e3-0x63f8b8) y ControlSubTask 0x63a890; condiciones exactas de LeaveCar (0x641c89-0x641d3a).
- Steering anims (6).
Pasos para retomar: `powershell -NoProfile -File tools\\re\\dis.ps1 -Va 0x63f1e0 -Len 0x6e0` (EnterCar next), `-Va 0x64a820 -Len 0x3c0` (GoToCarDoor Control), `-Va 0x640xxx` buscar `CTaskComplexEnterCar::CreateFirstSubTask` con `Grep CreateFirstSubTask gta_sa_symbols.tsv`, `-Va 0x651ee0 -Len 0x1a0` (polinomio slerp), `-Va 0x644470 -Len 0x9b0` (CarDrive). Scripts python auxiliares (lectura de tablas por VA) estaban en el scratchpad de la sesion y no se conservan: se pueden rehacer leyendo el PE (seccion -> offset).
