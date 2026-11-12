$ErrorActionPreference = 'Stop'

function Invoke-Pg([string] $Database, [string] $Sql) {
    $result = $Sql | & docker compose exec -T postgres psql -U transport -d $Database -At -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "PostgreSQL command failed for $Database" }
    return $result
}

$oldOid = Invoke-Pg 'postgres' "SELECT oid FROM pg_database WHERE datname = 'padelService'"
$newOid = Invoke-Pg 'postgres' "SELECT oid FROM pg_database WHERE datname = 'transportService'"
if ($newOid -and -not $oldOid) { Write-Output 'Database transportService already migrated.'; return }
if (-not $oldOid -or $newOid) { throw 'Unexpected database names; no changes applied.' }
$connections = Invoke-Pg 'postgres' "SELECT count(*) FROM pg_stat_activity WHERE datname = 'padelService'"
if ([int]$connections -ne 0) { throw 'Stop applications connected to the old database before migration.' }

$tables = @(Invoke-Pg 'padelService' "SELECT format('%I.%I', schemaname, tablename) FROM pg_tables WHERE schemaname='public' ORDER BY tablename")
$counts = @{}
foreach ($table in $tables) { $counts[$table] = Invoke-Pg 'padelService' "SELECT count(*) FROM $table" }
$backupDirectory = Join-Path $PSScriptRoot '../target/database-backups'
New-Item -ItemType Directory -Force -Path $backupDirectory | Out-Null
$backupPath = Join-Path $backupDirectory ("before-database-rename-" + [Guid]::NewGuid().ToString('N') + '.sql')
$dump = & docker compose exec -T postgres pg_dump -U transport -d padelService
if ($LASTEXITCODE -ne 0 -or -not $dump) { throw 'Backup failed; no rename applied.' }
[System.IO.File]::WriteAllLines([System.IO.Path]::GetFullPath($backupPath), [string[]]$dump, (New-Object System.Text.UTF8Encoding($false)))

Invoke-Pg 'postgres' 'ALTER DATABASE "padelService" RENAME TO "transportService"' | Out-Null
$renamedOid = Invoke-Pg 'postgres' "SELECT oid FROM pg_database WHERE datname = 'transportService'"
if ($renamedOid -ne $oldOid) { throw 'Unexpected database identity after rename.' }
foreach ($table in $tables) {
    $after = Invoke-Pg 'transportService' "SELECT count(*) FROM $table"
    if ($after -ne $counts[$table]) { throw "Row count changed for $table" }
}
Write-Output "Database renamed to transportService; verified $($tables.Count) table counts; backup: $backupPath"
