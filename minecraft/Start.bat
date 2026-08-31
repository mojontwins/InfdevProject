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
set "JVM_ARGS=%JVM_ARGS% -Dhttp.proxyHost=betacraft.ee -Dhttp.proxyPort=11702"

REM bin MUST come first so freshly-compiled classes shadow the stale
REM .class files inside jars\deobfuscated.jar. The libraries are listed
REM explicitly because the * wildcard does not expand on Windows java.
set "CP=bin;jars\deobfuscated.jar;jars\minecraft.jar;..\libraries\net\java\jinput\jinput\2.0.5\jinput-2.0.5.jar;..\libraries\net\java\jutils\jutils\1.0.0\jutils-1.0.0.jar;..\libraries\org\lwjgl\lwjgl\lwjgl\2.9.4-nightly-20150209\lwjgl-2.9.4-nightly-20150209.jar;..\libraries\org\lwjgl\lwjgl\lwjgl_util\2.9.4-nightly-20150209\lwjgl_util-2.9.4-nightly-20150209.jar;..\libraries\org\lwjgl\lwjgl\lwjgl-platform\2.9.4-nightly-20150209\lwjgl-platform-2.9.4-nightly-20150209.jar;..\libraries\com\paulscode\codecjorbis\20230120\codecjorbis-20230120.jar;..\libraries\com\paulscode\codecwav\20101023\codecwav-20101023.jar;..\libraries\com\paulscode\libraryjavasound\20101123\libraryjavasound-20101123.jar;..\libraries\com\paulscode\librarylwjglopenal\20100824\librarylwjglopenal-20100824.jar;..\libraries\com\paulscode\soundsystem\20120107\soundsystem-20120107.jar;..\libraries\org\mcphackers\launchwrapper\1.3.0\launchwrapper-1.3.0.jar;..\libraries\org\json\json\20230311\json-20230311.jar;..\libraries\org\ow2\asm\asm\9.10.1\asm-9.10.1.jar;..\libraries\org\ow2\asm\asm-tree\9.10.1\asm-tree-9.10.1.jar;..\libraries\org\ow2\asm\asm-commons\9.10.1\asm-commons-9.10.1.jar"
set "ARGS=--username Player --uuid - --session - --version inf-20100420 --gameDir . --assetsDir .\assets --assetIndex 20100212 --accessToken - --userProperties {} --userType legacy --versionType release --skinProxy pre-b1.9-pre4"

"%JAVA_HOME%\bin\javaw.exe" %JVM_ARGS% -cp "%CP%" net.minecraft.client.Start %ARGS%
