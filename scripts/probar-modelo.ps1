param(
    [Parameter(Mandatory=$true)][string]$JdbcUrl,
    [Parameter(Mandatory=$true)][string]$Usuario,
    [string]$Dependencias = 'target/dependency'
)
$ErrorActionPreference = 'Stop'
# Ejecutar desde la raiz contra una base vacia donde ya se cargo el esquema.
# Preparar JARs con: mvn dependency:copy-dependencies
$jarsPrueba = @(Get-ChildItem -LiteralPath $Dependencias -Filter '*.jar' | ForEach-Object FullName)
if ($jarsPrueba.Count -eq 0) { throw 'Faltan las dependencias JAR del proyecto.' }
$classpathPrueba = $jarsPrueba -join ';'
$fuentesPrueba = @(Get-ChildItem src/main/java,src/test/java -Recurse -Filter '*.java' | ForEach-Object FullName)
New-Item -ItemType Directory -Force target/it-classes | Out-Null
& javac -encoding UTF-8 -cp $classpathPrueba -d target/it-classes @fuentesPrueba
if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion Java.' }
& java -cp ('target/it-classes;' + $classpathPrueba) com.mycompany.tecnoweb.email.FlujoReducidoIT $JdbcUrl $Usuario
if ($LASTEXITCODE -ne 0) { throw 'Fallo la prueba de integracion.' }
