param(
    [Parameter(Mandatory=$true,ParameterSetName='Source')][string]$SourceContainer,
    [Parameter(Mandatory=$true,ParameterSetName='Source')][string]$SourceDatabase,
    [Parameter(Mandatory=$true,ParameterSetName='Sauvegarde')][string]$BackupFile,
    [Parameter(Mandatory=$true)][string]$DatabaseUser,
    [string]$ValidationContainer = 'transport-soutenance-validation'
)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
function Invoke-DockerChecked([string[]]$DockerArgs) {
    & docker @DockerArgs
    if ($LASTEXITCODE -ne 0) { throw "Commande Docker échouée : $($DockerArgs[0])" }
}
$stamp = Get-Date -Format 'yyyyMMddHHmmssfff'
$copyDatabase = "transport_schema_copy_$stamp"
$dump = "transport_schema_$stamp.dump"
$output = Join-Path (Get-Location) '.local-validation/schema-transport'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$dumpPath = Join-Path $output $dump
# pg_dump fournit un instantané transactionnel ; aucune écriture dans la base source.
if ($PSCmdlet.ParameterSetName -eq 'Sauvegarde') {
    Copy-Item -LiteralPath (Resolve-Path -LiteralPath $BackupFile).Path -Destination $dumpPath
} else {
    Invoke-DockerChecked -DockerArgs @('exec',$SourceContainer,'pg_dump','-U',$DatabaseUser,'-d',$SourceDatabase,'-Fc','-f',"/tmp/$dump")
    Invoke-DockerChecked -DockerArgs @('cp',"${SourceContainer}:/tmp/$dump",$dumpPath)
}
Invoke-DockerChecked -DockerArgs @('cp',$dumpPath,"${ValidationContainer}:/tmp/$dump")
Invoke-DockerChecked -DockerArgs @('exec',$ValidationContainer,'createdb','-U',$DatabaseUser,$copyDatabase)
Invoke-DockerChecked -DockerArgs @('exec',$ValidationContainer,'pg_restore','-U',$DatabaseUser,'--no-owner','--exit-on-error','-d',$copyDatabase,"/tmp/$dump")
foreach ($script in @('issue-3-empreintes.sql','issue-3-schema-transport.sql')) {
    Invoke-DockerChecked -DockerArgs @('cp',"scripts/migrations/$script","${ValidationContainer}:/tmp/$script")
}
$before = @(Invoke-DockerChecked -DockerArgs @('exec',$ValidationContainer,'psql','-X','-U',$DatabaseUser,'-d',$copyDatabase,'-At','-f','/tmp/issue-3-empreintes.sql'))
Invoke-DockerChecked -DockerArgs @('exec',$ValidationContainer,'psql','-X','-U',$DatabaseUser,'-d',$copyDatabase,'-f','/tmp/issue-3-schema-transport.sql')
$after = @(Invoke-DockerChecked -DockerArgs @('exec',$ValidationContainer,'psql','-X','-U',$DatabaseUser,'-d',$copyDatabase,'-At','-f','/tmp/issue-3-empreintes.sql'))
if (Compare-Object $before $after) { throw 'Différence de données, contraintes ou index : copie à examiner, ne pas migrer la base métier.' }
# Une deuxième exécution doit être sans effet.
Invoke-DockerChecked -DockerArgs @('exec',$ValidationContainer,'psql','-X','-U',$DatabaseUser,'-d',$copyDatabase,'-f','/tmp/issue-3-schema-transport.sql')
$second = @(Invoke-DockerChecked -DockerArgs @('exec',$ValidationContainer,'psql','-X','-U',$DatabaseUser,'-d',$copyDatabase,'-At','-f','/tmp/issue-3-empreintes.sql'))
if (Compare-Object $after $second) { throw 'Migration non idempotente.' }
$before | Set-Content (Join-Path $output "$stamp-avant.txt")
$after | Set-Content (Join-Path $output "$stamp-apres.txt")
$copyDatabase | Set-Content (Join-Path $output 'derniere-base.txt')
Write-Output "Sauvegarde restaurée, empreintes et idempotence vérifiées. Base isolée : $copyDatabase"
Write-Output 'Aucune migration appliquée à la source. Garder le dump confidentiel hors Git.'
