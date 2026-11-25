$ErrorActionPreference = 'Stop'

function Assert-BackendPortAvailable {
    $listeners = [Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners()
    if ($listeners.Port -contains 8080) {
        throw 'Le port 8080 est déjà occupé. Libérez-le avant de démarrer TransportMultiple.'
    }
}

$environment = @{}
foreach ($name in @('DB_URL', 'DB_USER', 'DB_PASSWORD', 'JWT_SECRET')) {
    $environment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

Push-Location -LiteralPath $PSScriptRoot
try {
    Assert-BackendPortAvailable

    $java = if ($env:JAVA_HOME) {
        Join-Path $env:JAVA_HOME 'bin/java.exe'
    } else {
        (Get-Command java.exe -ErrorAction Stop).Source
    }
    if (!(Test-Path -LiteralPath $java -PathType Leaf)) {
        throw 'Java est introuvable. Vérifiez la configuration de Java 21 et de JAVA_HOME.'
    }
    $javaVersion = @(& $java --version)
    if ($LASTEXITCODE -ne 0 -or $javaVersion[0] -notmatch '^(openjdk|java) 21(\.|\s|$)') {
        throw 'TransportMultiple nécessite Java 21. Vérifiez JAVA_HOME et le PATH.'
    }

    $docker = (Get-Command docker.exe -ErrorAction Stop).Source
    & $docker info --format '{{.ServerVersion}}' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Démarrez Docker Desktop avant de continuer.' }

    $config = & $docker compose config --format json
    if ($LASTEXITCODE -ne 0) { throw 'Configuration Docker Compose invalide. Vérifiez le volume PostgreSQL déclaré.' }
    $postgres = ($config -join [Environment]::NewLine | ConvertFrom-Json).services.postgres
    if (!$postgres) { throw 'Le service postgres est absent de la configuration Compose.' }

    Write-Host 'Démarrage de PostgreSQL et vérification de son état de santé…'
    & $docker compose up -d --no-recreate --wait --wait-timeout 60 postgres
    if ($LASTEXITCODE -ne 0) { throw "PostgreSQL n’est pas prêt. Consultez docker compose ps et docker compose logs postgres." }

    if (!$env:DB_URL) {
        $port = $postgres.ports | Where-Object { $_.target -eq 5432 -and $_.protocol -eq 'tcp' } |
            Select-Object -First 1 -ExpandProperty published
        if (!$port) { throw 'Déclarez un port hôte pour PostgreSQL dans Compose ou définissez DB_URL.' }
        $env:DB_URL = "jdbc:postgresql://localhost:${port}/$($postgres.environment.POSTGRES_DB)"
    }
    if (!$env:DB_USER) { $env:DB_USER = $postgres.environment.POSTGRES_USER }
    if (!$env:DB_PASSWORD) { $env:DB_PASSWORD = $postgres.environment.POSTGRES_PASSWORD }

    if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
        $bytes = New-Object byte[] 64
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
        Write-Host 'Une clé JWT locale temporaire est disponible pour ce lancement.'
    }

    & .\mvnw.cmd -B -ntp compile
    if ($LASTEXITCODE -ne 0) { throw 'La compilation du backend a échoué.' }

    Assert-BackendPortAvailable
    Write-Host 'Swagger UI après démarrage  http://localhost:8080/swagger-ui.html'
    Write-Host 'OpenAPI après démarrage    http://localhost:8080/v3/api-docs'
    & .\mvnw.cmd -B -ntp '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false' '-Dspring-boot.run.arguments=--server.port=8080' spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw 'Le démarrage du backend a échoué.' }
} finally {
    foreach ($name in $environment.Keys) {
        [Environment]::SetEnvironmentVariable($name, $environment[$name], 'Process')
    }
    Pop-Location
}
