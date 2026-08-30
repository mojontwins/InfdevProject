@echo off
setlocal
cd /d "%~dp0"

REM Find a Java 8 runtime. Honor JAVA_HOME if set; otherwise probe well-known locations.
if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk1.8.0_361\bin\javaw.exe" set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_361"
    if exist "C:\Program Files\Java\jdk1.8.0_202\bin\javaw.exe" set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202"
    if exist "C:\Program Files (x86)\Java\jre1.8.0_xxx\bin\javaw.exe" set "JAVA_HOME=C:\Program Files (x86)\Java\jre1.8.0_xxx"
)
if not defined JAVA_HOME (
    echo No Java 8 runtime found. Set JAVA_HOME or install JDK 8.
    exit /b 1
)

set "JVM_ARGS=-Djava.library.path=..\libraries\natives -Xmx1024M"
REM Uncomment to route sound downloads through the betacraft proxy:
REM set "JVM_ARGS=%JVM_ARGS% -Dhttp.proxyHost=betacraft.uk -Dhttp.proxyPort=11702"

set "CP=jars\deobfuscated.jar;jars\minecraft.jar;..\libraries\*;bin"
set "ARGS=--username Player --uuid - --session - --version inf-20100420 --gameDir . --assetsDir .\assets --assetIndex 20100212 --accessToken - --userProperties {} --userType legacy --versionType release --skinProxy pre-b1.9-pre4"

"%JAVA_HOME%\bin\javaw.exe" %JVM_ARGS% -cp "%CP%" net.minecraft.client.Start %ARGS%
