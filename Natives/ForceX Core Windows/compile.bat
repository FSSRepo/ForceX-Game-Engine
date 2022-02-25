@echo off
cd C:\proyectos c++\ForceXCore
@echo compilando....
g++ -c main.cpp -o main.o -I"C:\mingw64\include" -I"C:\mingw64\x86_64-w64-mingw32\include" -I"C:\proyectos c++\ForceXCore\include" -I"C:\Program Files (x86)\Java\jdk1.8.0_141\include" -I"C:\Program Files (x86)\Java\jdk1.8.0_141\include\win32" -m32 -DBUILDING_DLL=1
g++ -c rg_etc1.cpp -o rg_etc1.o -I"C:\mingw64\include" -I"C:\mingw64\x86_64-w64-mingw32\include" -I"C:\proyectos c++\ForceXCore\include" -I"C:\Program Files\Java\jdk1.8.0_111\include" -I"C:\Program Files (x86)\Java\jdk1.8.0_111\include\win32" -m32 -DBUILDING_DLL=1
@echo se completo la compilacion. Escribe s o n si quieres continuar la compilacion del dll 
@set /p Op=
if %Op% == "n" goto exit
@echo compilando dll....
g++ -shared main.o rg_etc1.o -o bin\fxcore.dll -L"C:\mingw64\x86_64-w64-mingw32\lib32" -static-libgcc libs/core32.a -m32 -Wl,--output-def,bin\fxcore.def,--out-implib,bin\fxcore.a,--add-stdcall-alias
@echo se completo la compilacion. Escribe e para terminar
@set /p Op=
if %Op% == "e" goto exit