#requires -Version 5.1

[CmdletBinding()]
param(
    [string]$Action,
    [ValidateRange(1, 65535)]
    [int]$PortFrontend = 4200
)

$ErrorActionPreference = 'Stop'

function Invoke-Commande([string]$Executable, [string[]]$Arguments) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Échec de $Executable (code $LASTEXITCODE)."
    }
}

function Invoke-Frontend([string[]]$Arguments) {
    Push-Location -LiteralPath 'frontend'
    try {
        Invoke-Commande 'npm.cmd' $Arguments
    } finally {
        Pop-Location
    }
}

$actions = [ordered]@{
    DemarrerBackend = @{
        Libelle = 'Démarrer le backend et PostgreSQL'
        Executer = { & .\start-local.ps1 }
    }
    DemarrerFrontend = @{
        Libelle = 'Démarrer Angular'
        Executer = { Invoke-Frontend @('start', '--', '--port', [string]$PortFrontend) }
    }
    CompilerBackend = @{
        Libelle = 'Compiler le backend'
        Executer = { Invoke-Commande '.\mvnw.cmd' @('-B', '-ntp', 'compile') }
    }
    TesterBackend = @{
        Libelle = 'Tester le backend avec PostgreSQL isolé'
        Executer = { & .\scripts\test-all.ps1 -NoIdeConfig }
    }
    InstallerFrontend = @{
        Libelle = 'Installer les dépendances Angular'
        Executer = { Invoke-Frontend @('ci') }
    }
    CompilerFrontend = @{
        Libelle = 'Compiler Angular pour la production'
        Executer = { Invoke-Frontend @('run', 'build') }
    }
    TesterFrontend = @{
        Libelle = 'Exécuter les tests Angular'
        Executer = { Invoke-Frontend @('test') }
    }
    EtatDocker = @{
        Libelle = 'Consulter les services Docker Compose'
        Executer = { Invoke-Commande 'docker' @('compose', 'ps') }
    }
    ConstruireImage = @{
        Libelle = 'Générer une image Docker du backend'
        Executer = { Invoke-Commande 'docker' @('build', '--tag', 'transportmultiple:local', '.') }
    }
}

Push-Location -LiteralPath $PSScriptRoot
try {
    $noms = @($actions.Keys)
    if ($Action) {
        if (!$actions.Contains($Action)) {
            throw "Action inconnue. Actions disponibles : $($noms -join ', ')."
        }
        & ($actions[$Action].Executer)
        return
    }

    while ($true) {
        Write-Host ''
        Write-Host 'TransportMultiple'
        for ($i = 0; $i -lt $noms.Count; $i++) {
            Write-Host ('{0}. {1}' -f ($i + 1), $actions[$noms[$i]].Libelle)
        }
        Write-Host '0. Quitter'

        $choix = Read-Host 'Choisir une action'
        if ($null -eq $choix -or $choix -eq '0') { break }

        $numero = 0
        if (![int]::TryParse($choix, [ref]$numero) -or $numero -lt 1 -or $numero -gt $noms.Count) {
            Write-Warning 'Choix invalide.'
            continue
        }

        try {
            & ($actions[$noms[$numero - 1]].Executer)
        } catch {
            Write-Warning $_.Exception.Message
        }
    }
} finally {
    Pop-Location
}
