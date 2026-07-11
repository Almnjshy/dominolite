
33s
Run gradle build
Starting a Gradle Daemon (subsequent builds will be faster)
> Task :app:preBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:generateDebugResValues
> Task :app:checkDebugAarMetadata
> Task :app:mapDebugSourceSetPaths
> Task :app:generateDebugResources
> Task :app:packageDebugResources
> Task :app:createDebugCompatibleScreenManifests
> Task :app:extractDeepLinksDebug
> Task :app:parseDebugLocalResources
> Task :app:mergeDebugResources
> Task :app:processDebugMainManifest
> Task :app:processDebugManifest
> Task :app:processDebugManifestForPackage
> Task :app:javaPreCompileDebug
> Task :app:mergeDebugShaders
> Task :app:compileDebugShaders NO-SOURCE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:mergeDebugAssets
> Task :app:compressDebugAssets
> Task :app:desugarDebugFileDependencies
> Task :app:mergeDebugStartupProfile
> Task :app:mergeDebugJniLibFolders
> Task :app:checkDebugDuplicateClasses
> Task :app:mergeDebugNativeLibs
> Task :app:processDebugResources
> Task :app:mergeLibDexDebug

> Task :app:stripDebugDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so.

> Task :app:validateSigningDebug
> Task :app:writeDebugAppMetadata
> Task :app:writeDebugSigningConfigVersions
> Task :app:buildKotlinToolingMetadata
> Task :app:preReleaseBuild UP-TO-DATE
> Task :app:checkReleaseAarMetadata
> Task :app:generateReleaseResValues
> Task :app:mapReleaseSourceSetPaths
> Task :app:generateReleaseResources
> Task :app:packageReleaseResources
> Task :app:parseReleaseLocalResources
> Task :app:createReleaseCompatibleScreenManifests
> Task :app:extractDeepLinksRelease
> Task :app:mergeReleaseResources
> Task :app:processReleaseMainManifest
> Task :app:processReleaseManifest
> Task :app:processReleaseManifestForPackage
> Task :app:javaPreCompileRelease
> Task :app:extractProguardFiles
> Task :app:mergeReleaseJniLibFolders
> Task :app:mergeReleaseNativeLibs
> Task :app:processReleaseResources

> Task :app:stripReleaseDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so.

> Task :app:extractReleaseNativeSymbolTables
> Task :app:mergeReleaseNativeDebugMetadata NO-SOURCE
> Task :app:mergeExtDexDebug
> Task :app:desugarReleaseFileDependencies
> Task :app:mergeReleaseStartupProfile
> Task :app:checkReleaseDuplicateClasses
> Task :app:mergeReleaseArtProfile
> Task :app:mergeReleaseShaders
> Task :app:compileReleaseShaders NO-SOURCE
> Task :app:generateReleaseAssets UP-TO-DATE
> Task :app:mergeReleaseAssets
> Task :app:compressReleaseAssets
> Task :app:extractReleaseVersionControlInfo
> Task :app:mergeExtDexRelease
> Task :app:collectReleaseDependencies
> Task :app:sdkReleaseDependencyData
> Task :app:optimizeReleaseResources
> Task :app:writeReleaseAppMetadata
> Task :app:writeReleaseSigningConfigVersions
> Task :app:preDebugAndroidTestBuild SKIPPED
> Task :app:generateDebugAndroidTestResValues
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:preReleaseUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest
> Task :app:javaPreCompileReleaseUnitTest
e: file:///home/runner/work/dominolite/dominolite/app/src/main/java/com/almnjshy/agon/MainActivity.kt:18:45 Primary constructor of data class must only have property ('val' / 'var') parameters.

e: file:///home/runner/work/dominolite/dominolite/app/src/main/java/com/almnjshy/agon/MainActivity.kt:39:40 Unresolved reference 'difficulty'.
> Task :app:compileDebugKotlin FAILED

e: file:///home/runner/work/dominolite/dominolite/app/src/main/java/com/almnjshy/agon/ui/GameScreen.kt:170:57 Smart cast to 'com.almnjshy.agon.engine.DominoTile' is impossible, because 'selectedTile' is a delegated property.
> Task :app:compileReleaseKotlin FAILED
e: file:///home/runner/work/dominolite/dominolite/app/src/main/java/com/almnjshy/agon/ui/GameScreen.kt:194:63 Smart cast to 'com.almnjshy.agon.engine.DominoTile' is impossible, because 'selectedTile' is a delegated property.
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run-1783784051467.json
e: file:///home/runner/work/dominolite/dominolite/app/src/main/java/com/almnjshy/agon/ui/components/PlayerHandView.kt:81:14 Unresolved reference 'clickable'.
e: file:///home/runner/work/dominolite/dominolite/app/src/main/java/com/almnjshy/agon/MainActivity.kt:18:45 Primary constructor of data class must only have property ('val' / 'var') parameters.
e: file:///home/runner/work/dominolite/dominolite/app/src/main/java/com/almnjshy/agon/MainActivity.kt:39:40 Unresolved reference 'difficulty'.
e: file:///home/runner/work/dominolite/dominolite/app/src/main/java/com/almnjshy/agon/ui/GameScreen.kt:170:57 Smart cast to 'com.almnjshy.agon.engine.DominoTile' is impossible, because 'selectedTile' is a delegated property.
e: file:///home/runner/work/dominolite/dominolite/app/src/main/java/com/almnjshy/agon/ui/GameScreen.kt:194:63 Smart cast to 'com.almnjshy.agon.engine.DominoTile' is impossible, because 'selectedTile' is a delegated property.
e: file:///home/runner/work/dominolite/dominolite/app/src/main/java/com/almnjshy/agon/ui/components/PlayerHandView.kt:81:14 Unresolved reference 'clickable'.

FAILURE: Build completed with 2 failures.
67 actionable tasks: 67 executed

1: Task failed with an exception.
-----------
* What went wrong:
Execution failed for task ':app:compileDebugKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.
==============================================================================

2: Task failed with an exception.
-----------
* What went wrong:
Execution failed for task ':app:compileReleaseKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.
==============================================================================

BUILD FAILED in 32s
Error: Process completed with exit code 1.