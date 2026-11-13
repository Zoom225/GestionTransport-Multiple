[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [ValidatePattern('^transport_schema_copy_[a-z0-9_]+$')]
    [string]$Database,
    [ValidateRange(1,65535)][int]$Port = 5541,
    [string]$DatabaseUser = 'transport'
)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    throw 'Définir DB_PASSWORD pour la copie de validation avant cette commande. Sa valeur ne sera pas affichée.'
}
# Réutilise le mécanisme JWT existant et préserve une clé déjà configurée.
& (Join-Path $PSScriptRoot 'configure-intellij-jwt.ps1')
$workspace = Join-Path (Get-Location) '.idea/workspace.xml'
git check-ignore --quiet -- .idea/workspace.xml
if ($LASTEXITCODE -ne 0) { throw 'La configuration personnelle doit être ignorée par Git.' }
$backupRelative = '.local-validation/intellij-validation-' + (Get-Date -Format 'yyyyMMddHHmmssfff') + '.xml'
git check-ignore --quiet -- $backupRelative
if ($LASTEXITCODE -ne 0) { throw 'La sauvegarde personnelle doit être ignorée par Git.' }
New-Item -ItemType Directory -Force -Path (Join-Path (Get-Location) '.local-validation') | Out-Null
Copy-Item -LiteralPath $workspace -Destination (Join-Path (Get-Location) $backupRelative)
$xml = New-Object System.Xml.XmlDocument
$xml.PreserveWhitespace = $true
$xml.Load($workspace)
$configuration = $xml.SelectSingleNode("//component[@name='RunManager']/configuration[@type='SpringBootApplicationConfigurationType' and option[@name='SPRING_BOOT_MAIN_CLASS' and @value='com.transport.gestiontransport.GestionTransportApplication']]")
if (!$configuration) { throw 'Configuration Spring Boot GestionTransportApplication introuvable.' }
$envs = $configuration.SelectSingleNode('envs')
if (!$envs) { $envs=$xml.CreateElement('envs'); [void]$configuration.AppendChild($envs) }
$values = @{
    DB_URL = "jdbc:postgresql://127.0.0.1:$Port/$Database"
    DB_USER = $DatabaseUser
    DB_PASSWORD = $env:DB_PASSWORD
    SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
    DEMO_DATA_ENABLED = 'false'
}
foreach ($name in $values.Keys) {
    $entry = $envs.SelectSingleNode("env[@name='$name']")
    if (!$entry) { $entry=$xml.CreateElement('env'); $entry.SetAttribute('name',$name); [void]$envs.AppendChild($entry) }
    $entry.SetAttribute('value',$values[$name])
}
$vm = $configuration.SelectSingleNode("option[@name='VM_PARAMETERS']")
if (!$vm) { $vm=$xml.CreateElement('option'); $vm.SetAttribute('name','VM_PARAMETERS'); [void]$configuration.AppendChild($vm) }
$arguments=$vm.GetAttribute('value')
if ($arguments -match '-Dspring\.devtools\.restart\.enabled=\S+') {
    $arguments=$arguments -replace '-Dspring\.devtools\.restart\.enabled=\S+','-Dspring.devtools.restart.enabled=false'
} else { $arguments=($arguments + ' -Dspring.devtools.restart.enabled=false').Trim() }
$vm.SetAttribute('value',$arguments)
$xml.Save($workspace)
Write-Output "Configuration IntelliJ : copie $Database sur le port $Port, ddl-auto=validate, démo désactivée."
Write-Output 'JWT existant préservé ; secrets et sauvegarde restent hors Git. DevTools restart désactivé.'
Write-Output 'Si IntelliJ ne recharge pas automatiquement le fichier, recharger la configuration avant de lancer.'
