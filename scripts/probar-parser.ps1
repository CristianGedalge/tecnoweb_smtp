param([string]$Dependencias = 'target/dependency')
$ErrorActionPreference = 'Stop'
$jarsParser = @(Get-ChildItem -LiteralPath $Dependencias -Filter '*.jar' | ForEach-Object FullName)
if ($jarsParser.Count -eq 0) { throw 'Ejecutar primero mvn dependency:copy-dependencies.' }
$cpParser = $jarsParser -join ';'
$fuentesParser = @(Get-ChildItem src/main/java,src/test/java -Recurse -Filter '*.java' | ForEach-Object FullName)
New-Item -ItemType Directory -Force target/it-classes | Out-Null
& javac -encoding UTF-8 -cp $cpParser -d target/it-classes @fuentesParser
if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion Java.' }
& java -cp ('target/it-classes;' + $cpParser) com.mycompany.tecnoweb.email.AnalizadorComandosTest
if ($LASTEXITCODE -ne 0) { throw 'Fallo la prueba del parser.' }
& java -cp ('target/it-classes;' + $cpParser) com.mycompany.tecnoweb.email.ValidacionPersonaTest
if ($LASTEXITCODE -ne 0) { throw 'Fallo la validacion de datos personales.' }
