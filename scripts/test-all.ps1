[CmdletBinding()]
param([switch]$PrepareOnly, [switch]$NoIdeConfig)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
function Docker-Checked([string[]]$Arguments) {
    $result = & docker @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Echec Docker : $($Arguments[0])" }
    return $result
}
$container = 'gestiontransport-tests'
$port = 5542
$label = 'gestiontransport.isolated-tests'
$exists = & docker ps -a --filter "name=^/$container`$" --format '{{.Names}}'
if ($LASTEXITCODE -ne 0) { throw 'Docker doit etre demarre.' }
if ($exists -eq $container) {
    $inspection = (Docker-Checked @('inspect',$container)) -join [Environment]::NewLine
    $owned = ($inspection | ConvertFrom-Json)[0].Config.Labels.$label
    if ($owned -ne 'true') { throw 'Le conteneur existant ne porte pas le label de test attendu.' }
    Docker-Checked @('start',$container) | Out-Null
} else {
    Docker-Checked @('run','-d','--name',$container,'--label',"$label=true",'-p',"127.0.0.1:${port}:5432",'-e','POSTGRES_USER=transport_test','-e','POSTGRES_PASSWORD=transport_test_local_only','-e','POSTGRES_DB=postgres','postgres:15') | Out-Null
}
$ready = $false
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    & docker exec $container pg_isready -U transport_test -d postgres *> $null
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep -Seconds 1
}
if (!$ready) { throw 'PostgreSQL de test indisponible.' }
$stamp = Get-Date -Format 'yyyyMMddHHmmssfff'
$baseline = "issue1_validation_$stamp"
$schema = "transport_schema_copy_$stamp"
Docker-Checked @('exec',$container,'createdb','-U','transport_test',$baseline) | Out-Null
$names = @('POSTGRES_BASELINE_URL','TRANSPORT_SCHEMA_URL','DB_USER','DB_PASSWORD')
$saved = @{}
foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name,'Process') }
try {
    $env:POSTGRES_BASELINE_URL = "jdbc:postgresql://127.0.0.1:$port/$baseline"
    $env:DB_USER = 'transport_test'
    $env:DB_PASSWORD = 'transport_test_local_only'
    & .\mvnw.cmd -B '-Dtest=PostgresBaselineTest' test
    if ($LASTEXITCODE -ne 0) { throw 'Tests PostgreSQL echoues ; preparation interrompue.' }
    Docker-Checked @('exec',$container,'pg_dump','-U','transport_test','-Fc','-f','/tmp/test-baseline.dump',$baseline) | Out-Null
    Docker-Checked @('exec',$container,'createdb','-U','transport_test',$schema) | Out-Null
    Docker-Checked @('exec',$container,'pg_restore','-U','transport_test','--no-owner','--exit-on-error','-d',$schema,'/tmp/test-baseline.dump') | Out-Null
    foreach ($file in @('src/test/resources/schema-historique-fixture.sql','scripts/migrations/issue-3-schema-transport.sql')) {
        Docker-Checked @('cp',$file,"${container}:/tmp/test-migration.sql") | Out-Null
        Docker-Checked @('exec',$container,'psql','-X','-U','transport_test','-d',$schema,'-v','ON_ERROR_STOP=1','-f','/tmp/test-migration.sql') | Out-Null
    }
    foreach ($file in @('scripts/migrations/issue-8-organisateur-penalite.sql',
                        'scripts/migrations/issue-9-suspension-168h.sql',
                        'scripts/migrations/issue-10-auteur-annulation.sql',
                        'scripts/migrations/issue-45-montants-bigdecimal.sql')) {
        Docker-Checked @('cp',$file,"${container}:/tmp/test-migration.sql") | Out-Null
        foreach ($database in @($baseline, $schema)) {
            Docker-Checked @('exec',$container,'psql','-X','-U','transport_test','-d',$database,'-v','ON_ERROR_STOP=1','-f','/tmp/test-migration.sql') | Out-Null
        }
    }
    $env:TRANSPORT_SCHEMA_URL = "jdbc:postgresql://127.0.0.1:$port/$schema"
    # Configuration personnelle ignoree par Git, avec les deux bases isolees preparees.
    if (!$NoIdeConfig) {
    $directory = Join-Path (Get-Location) '.idea/runConfigurations'
    New-Item -ItemType Directory -Force $directory | Out-Null
    $configuration = @"
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="GestionTransport - Tous les tests PostgreSQL" type="JUnit" factoryName="JUnit">
    <module name="gestiontransport-multiple" />
    <option name="PACKAGE_NAME" value="com.transport.gestiontransport" />
    <option name="TEST_OBJECT" value="package" />
    <option name="WORKING_DIRECTORY" value="`$PROJECT_DIR`$" />
    <envs>
      <env name="POSTGRES_BASELINE_URL" value="$env:POSTGRES_BASELINE_URL" />
      <env name="TRANSPORT_SCHEMA_URL" value="$env:TRANSPORT_SCHEMA_URL" />
      <env name="DB_USER" value="$env:DB_USER" />
      <env name="DB_PASSWORD" value="$env:DB_PASSWORD" />
    </envs>
    <method v="2"><option name="Make" enabled="true" /></method>
  </configuration>
</component>
"@
    # Utiliser le nom du module Maven importe par IntelliJ lorsqu'il est disponible.
    $workspace = Join-Path (Get-Location) '.idea/workspace.xml'
    if (Test-Path $workspace) {
        [xml]$idea = Get-Content -Raw $workspace
        $module = $idea.SelectSingleNode('//configuration[@type="JUnit"]/module')
        if ($module) { $configuration = $configuration.Replace('name="gestiontransport-multiple"', ('name="' + $module.GetAttribute('name') + '"')) }
    }
    [IO.File]::WriteAllText((Join-Path $directory 'GestionTransport_Tests_PostgreSQL.xml'), $configuration, (New-Object System.Text.UTF8Encoding($false)))
    }
    if (!$PrepareOnly) {
        & .\mvnw.cmd -B clean verify
        if ($LASTEXITCODE -ne 0) { throw 'Validation complete echouee.' }
        foreach ($suiteName in @('com.transport.gestiontransport.PostgresBaselineTest','com.transport.gestiontransport.repository.TransportSchemaValidationTest')) {
            [xml]$report = Get-Content -Raw "target/surefire-reports/TEST-$suiteName.xml"
            if ([int]$report.testsuite.tests -eq 0 -or [int]$report.testsuite.skipped -ne 0) {
                throw "Tests PostgreSQL non executes : $suiteName"
            }
        }
    }
    Write-Host "Bases de test isolees : $baseline et $schema."
    if (!$NoIdeConfig) { Write-Host 'Dans IntelliJ : GestionTransport - Tous les tests PostgreSQL.' }
    Write-Host 'Le conteneur gestiontransport-tests reste disponible sur 127.0.0.1:5542. Aucune ecriture dans la base metier.'
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name,$saved[$name],'Process') }
}
