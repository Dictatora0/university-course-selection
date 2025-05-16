@echo off
setlocal enabledelayedexpansion

:: 设置颜色
set "GREEN=[32m"
set "RED=[31m"
set "YELLOW=[33m"
set "RESET=[0m"

:: 标题
cls
echo %GREEN%=============================================%RESET%
echo %GREEN%         大学生在线选课系统启动脚本           %RESET%
echo %GREEN%=============================================%RESET%
echo.

:: 检查Java环境
echo %YELLOW%检查Java环境...%RESET%
java -version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f tokens^=2^ delims^=^" %%a in ('java -version 2^>^&1') do (
        set "JAVA_VERSION=%%a"
        goto :checkJavaComplete
    )
    :checkJavaComplete
    echo %GREEN%Java已安装，版本: !JAVA_VERSION!%RESET%
) else (
    echo %RED%Java未安装，请安装JDK 8或更高版本%RESET%
    exit /b 1
)

:: 创建目录结构（如果不存在）
echo %YELLOW%创建必要的目录结构...%RESET%
if not exist classes mkdir classes
if not exist web\WEB-INF\classes mkdir web\WEB-INF\classes
if not exist web\WEB-INF\lib mkdir web\WEB-INF\lib

:: 编译Java代码
echo %YELLOW%编译Java代码...%RESET%
if exist src (
    :: 创建CLASSPATH
    set "CLASSPATH="
    if exist lib (
        for %%f in (lib\*.jar) do (
            set "CLASSPATH=!CLASSPATH!;%%f"
        )
    )
    
    :: 创建文件列表
    dir /s /b src\*.java > sources.txt
    
    :: 编译
    javac -d classes -cp "%CLASSPATH%" @sources.txt 2> compile_errors.log
    
    if %ERRORLEVEL% EQU 0 (
        echo %GREEN%Java代码编译成功%RESET%
        :: 复制编译后的类文件到web目录
        xcopy /E /Y classes\* web\WEB-INF\classes\ >nul
        del sources.txt
    ) else (
        echo %RED%Java代码编译失败，请查看compile_errors.log文件了解详细信息%RESET%
        exit /b 1
    )
) else (
    echo %RED%未找到src目录%RESET%
    exit /b 1
)

:: 复制库文件
if exist lib (
    echo %YELLOW%复制库文件到WEB-INF\lib目录...%RESET%
    xcopy /Y lib\*.jar web\WEB-INF\lib\ >nul 2>&1
)

:: 启动简易HTTP服务器
echo %YELLOW%启动HTTP服务器...%RESET%
echo %YELLOW%请在浏览器中访问: http://localhost:8000%RESET%
echo %YELLOW%使用以下账号登录系统：%RESET%
echo %YELLOW%学号: 2021001    密码: 123456%RESET%
echo %YELLOW%学号: 2021002    密码: 123456%RESET%
echo %YELLOW%按Ctrl+C停止服务器%RESET%
echo.

cd web
python -m http.server 8000 2>nul || python -m SimpleHTTPServer 8000

exit /b 0 