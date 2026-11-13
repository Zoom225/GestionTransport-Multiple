$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$projectRoot = Split-Path -Parent $PSScriptRoot
$workspacePath = Join-Path $projectRoot '.idea/workspace.xml'
if (-not (Test-Path -LiteralPath $workspacePath)) {
    throw 'Ouvrez le projet dans IntelliJ et creez la configuration Spring Boot GestionTransportApplication.'
}
& git -C $projectRoot check-ignore --quiet .idea/workspace.xml
if ($LASTEXITCODE -ne 0) {
    throw 'Le fichier IntelliJ personnel doit etre ignore par Git avant de configurer une cle locale.'
}

$workspace = New-Object System.Xml.XmlDocument
$workspace.PreserveWhitespace = $true
$workspace.Load($workspacePath)
$configurations = @($workspace.SelectNodes('//component[@name="RunManager"]/configuration[@type="SpringBootApplicationConfigurationType"][option[@name="SPRING_BOOT_MAIN_CLASS" and @value="com.transport.gestiontransport.GestionTransportApplication"]]'))
if ($configurations.Count -eq 0) {
    throw 'Configuration Spring Boot introuvable : selectionnez GestionTransportApplication et sa classe principale Transport.'
}

$changed = $false
foreach ($configuration in $configurations) {
    $jwt = $configuration.SelectSingleNode('envs/env[@name="JWT_SECRET"]')
    if ($null -ne $jwt -and -not [string]::IsNullOrWhiteSpace($jwt.GetAttribute('value'))) {
        if ([Text.Encoding]::UTF8.GetByteCount($jwt.GetAttribute('value')) -lt 32) {
            throw 'La cle JWT locale existante fait moins de 32 octets. Elle est preservee ; remplacez-la explicitement.'
        }
        continue
    }
    $envs = $configuration.SelectSingleNode('envs')
    if ($null -eq $envs) {
        $envs = $workspace.CreateElement('envs')
        [void]$configuration.AppendChild($envs)
    }
    if ($null -eq $jwt) {
        $jwt = $workspace.CreateElement('env')
        $jwt.SetAttribute('name', 'JWT_SECRET')
        [void]$envs.AppendChild($jwt)
    }
    $bytes = New-Object byte[] 64
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes) }
    finally { $generator.Dispose() }
    $jwt.SetAttribute('value', [Convert]::ToBase64String($bytes))
    $changed = $true
}

if ($changed) {
    $backupDirectory = Join-Path $projectRoot '.local-validation/intellij-jwt'
    [void](New-Item -ItemType Directory -Force -Path $backupDirectory)
    & git -C $projectRoot check-ignore --quiet .local-validation/intellij-jwt/workspace.backup.xml
    if ($LASTEXITCODE -ne 0) { throw 'Le repertoire de sauvegarde local doit etre ignore par Git.' }
    $backupPath = Join-Path $backupDirectory ('workspace_' + [Guid]::NewGuid().ToString('N') + '.xml')
    Copy-Item -LiteralPath $workspacePath -Destination $backupPath
    $workspace.Save($workspacePath)
}
Write-Output 'JWT_SECRET configuree pour Spring Boot dans IntelliJ. Aucune cle affichee ; aucune cle existante remplacee.'
