[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^[a-z_][a-z0-9_]{0,62}$', Options = [System.Text.RegularExpressions.RegexOptions]::None)]
    [string] $AncienRole
)

$ErrorActionPreference = 'Stop'
if ($AncienRole -eq 'transport') { throw 'Indiquer le nom antérieur du rôle.' }

function Invoke-Database([string] $Account, [string] $Sql) {
    $result = $Sql | & docker compose exec -T postgres psql -X -U $Account -d postgres -At -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "PostgreSQL command failed for $Account" }
    return $result
}

$existing = Invoke-Database $AncienRole "SELECT oid FROM pg_roles WHERE rolname = '$AncienRole'"
if (-not $existing) { throw 'Expected development account missing; inspect the cluster before proceeding.' }
$conflict = Invoke-Database $AncienRole "SELECT 1 FROM pg_roles WHERE rolname = 'transport'"
if ($conflict) { throw 'Account transport already exists; no changes applied.' }

$backupDirectory = Join-Path $PSScriptRoot '../target/database-backups'
New-Item -ItemType Directory -Force -Path $backupDirectory | Out-Null
$operation = [Guid]::NewGuid().ToString('N')
$backupPath = Join-Path $backupDirectory "before-transport-$operation.sql"
$dump = & docker compose exec -T postgres pg_dumpall -U $AncienRole
if ($LASTEXITCODE -ne 0 -or -not $dump) { throw 'Backup failed; no changes applied.' }
[System.IO.File]::WriteAllLines([System.IO.Path]::GetFullPath($backupPath), [string[]]$dump, (New-Object System.Text.UTF8Encoding($false)))

$temporary = "transport_migration_$operation"
Invoke-Database $AncienRole "CREATE ROLE $temporary LOGIN SUPERUSER" | Out-Null
try {
    $rename = @"
\getenv nouveau_mot_de_passe POSTGRES_PASSWORD
BEGIN;
ALTER ROLE "$AncienRole" RENAME TO transport;
ALTER ROLE transport PASSWORD :'nouveau_mot_de_passe';
COMMIT;
"@
    Invoke-Database $temporary $rename | Out-Null
    $renamed = Invoke-Database 'transport' "SELECT oid FROM pg_roles WHERE rolname = 'transport'"
    if ($renamed -ne $existing) { throw 'Unexpected role identity after rename.' }
} finally {
    $cleanupAccount = Invoke-Database $temporary "SELECT CASE WHEN EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'transport') THEN 'transport' ELSE '$AncienRole' END"
    Invoke-Database $cleanupAccount "DROP ROLE $temporary" | Out-Null
}
Write-Output "Development account migrated to transport; backup: $backupPath"
