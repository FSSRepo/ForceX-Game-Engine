# GTA SA 1.0 (HOODLUM): subir y bajar del vehiculo - ingenieria inversa

Fuente: `D:\Games\GTA San Andreas\gta_sa.exe` (base 0x400000), leido con `tools\re\dis.ps1` y scripts python ad-hoc.
Marcas: **[V]** leido/deducido directo del codigo o datos; **[NV]** estimacion.
dt SA: 1.0 = 20 ms.

## Objetivos (estado)

1. Campos de `CVehicleAnimGroup` (tabla 0xc1cdc0): HECHO (ver sec. 1)
2. `CVehicle::ProcessOpenDoor` / angulo de puerta: HECHO (sec. 2)
3. LineUp / CarAlign / GoToCarDoorAndStandStill: PENDIENTE
4. CTaskComplexEnterCar / LeaveCar (estados y condiciones): PENDIENTE
5. GetIn / GetOut / SetPedInAsDriver (root motion vs interpolacion, enganche): PENDIENTE
6. Animaciones de conduccion (CTaskSimpleCarDrive): PENDIENTE

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
Angulos maximos (en ctor 0x6b0a90): +0x5b8 = +-0x3f71463b = +-0.9448 rad (54.1 grados) para las puertas de coche (se pone el signo por lado); capot +0x5d0=0x3fc90fdb (pi/2) o -0.9448 [V valores / NV cual puerta]. 
Pendiente: tabla exacta puerta->angulo (leer 0x6b0dbe-0x6b0e45 con dis.ps1 -Va 0x6b0d90 -Len 0x120).

## 3. GetPositionToOpenCarDoor (0x64e740) [V]
`CVector* GetPositionToOpenCarDoor(CVector* out, CVehicle* veh, int doorNode)`; doorNode 8=RF 9=RR 10=LF 11=LR 0x12=maletero/otro.
`off = ComputeAnimDoorOffsets(group, doorNode in {10,8}?0 : {11,9}?1 : 0x12?2)`;
`seat = ModelInfo->seatPositions[...]` (modelinfo+0x5c; la lateral (puerta) usa el slot del asiento delantero (+0x30) o trasero (+0x3c)),
`side offset = handling.SeatOffsetDistance (+0xd4)` (0 para algunas combinaciones de grupo 0x65 con puertas traseras);
`localPos = seat +- (SeatOffsetDistance) - rotate(off, X invertido para puertas derechas 8/9)`; lado izq (10/11): x=-(seat.x+off_dist).
`pos = vehicleMatrix * localPos`; en coches con handling flag bit 8 (0x8: convertible?) o estado 1/4 se ajusta Z con `0.95 - vtbl+0xd4()`.
Para bicis/motos (veh+0x590==9 o handling flag bit 9) usa otra rama (0x6bf230).
(Detalle completo: ver pseudocodigo Ghidra `FUN_0064e740`; aqui resumido.)

## Estado
(Actualizado a media sesion; ver secciones 3..6 por completar.)
