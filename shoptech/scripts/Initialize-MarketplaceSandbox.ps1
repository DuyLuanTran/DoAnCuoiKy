$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$localRoot = Join-Path $projectRoot '.local'
$backupRoot = Join-Path $localRoot 'backups'
New-Item -ItemType Directory -Force $backupRoot | Out-Null
function Docker-Checked {
    param([string[]]$DockerArgs)
    & docker @DockerArgs
    if ($LASTEXITCODE -ne 0) { throw "Docker failed: $($DockerArgs[0])" }
}
$image = 'postgres:18'
Docker-Checked @('pull', $image)
$dumpPath = Join-Path $backupRoot 'source-before-marketplace.dump'
if (!(Test-Path -LiteralPath $dumpPath)) {
    $source = @{}
    foreach ($line in Get-Content -LiteralPath (Join-Path $projectRoot '.env')) {
        if ($line -match '^\s*(SPRING_DATASOURCE_(URL|USERNAME|PASSWORD))\s*=(.*)$') {
            $source[$Matches[1]] = $Matches[3].Trim().Trim('"').Trim("'")
        }
    }
    foreach ($key in @('SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD')) {
        $override = [Environment]::GetEnvironmentVariable($key)
        if ($override) { $source[$key] = $override }
        if (!$source[$key]) { throw "Missing source setting: $key" }
    }
    $sourceUri = [Uri]($source.SPRING_DATASOURCE_URL -replace '^jdbc:', '')
    $readOnlyEnv = Join-Path $localRoot 'source-readonly.env'
    @(
        "PGHOST=$($sourceUri.Host)", "PGPORT=$($sourceUri.Port)",
        "PGDATABASE=$($sourceUri.AbsolutePath.TrimStart('/'))",
        "PGUSER=$($source.SPRING_DATASOURCE_USERNAME)", "PGPASSWORD=$($source.SPRING_DATASOURCE_PASSWORD)",
        'PGSSLMODE=require', 'PGCONNECT_TIMEOUT=20', 'PGOPTIONS=-c default_transaction_read_only=on'
    ) | Set-Content -LiteralPath $readOnlyEnv -Encoding utf8
    try {
        Docker-Checked @('run','--rm','--env-file',$readOnlyEnv,'--mount',"type=bind,source=$backupRoot,target=/backup",$image,
            'pg_dump','--format=custom','--schema=public','--no-owner','--no-privileges','--file=/backup/source-before-marketplace.dump')
        Docker-Checked @('run','--rm','--mount',"type=bind,source=$backupRoot,target=/backup",$image,
            'pg_restore','--list','/backup/source-before-marketplace.dump') | Set-Content (Join-Path $backupRoot 'source-archive-list.txt')
    } finally { Remove-Item -LiteralPath $readOnlyEnv -ErrorAction SilentlyContinue }
}
$sandboxEnv = Join-Path $localRoot 'postgres.env'
if (!(Test-Path -LiteralPath $sandboxEnv)) {
    $password = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
    @('POSTGRES_DB=shoptech_marketplace','POSTGRES_USER=postgres',"POSTGRES_PASSWORD=$password") |
        Set-Content -LiteralPath $sandboxEnv -Encoding utf8
}
$existing = & docker ps -a --filter 'name=^/shoptech-marketplace-db$' --format '{{.Names}}'
if (!$existing) {
    Docker-Checked @('run','-d','--name','shoptech-marketplace-db','--env-file',$sandboxEnv,
        '-p','127.0.0.1:55432:5432','--mount','type=volume,source=shoptech-marketplace-pgdata,target=/var/lib/postgresql',$image)
} else { Docker-Checked @('start','shoptech-marketplace-db') }
$ready = $false
for ($i=0; $i -lt 30; $i++) {
    & docker exec shoptech-marketplace-db pg_isready -U postgres -d shoptech_marketplace *> $null
    if ($LASTEXITCODE -eq 0) { $ready=$true; break }
    Start-Sleep -Seconds 1
}
if (!$ready) { throw 'Isolated PostgreSQL did not become ready.' }
$tableCount = & docker exec shoptech-marketplace-db psql -U postgres -d shoptech_marketplace -Atc "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'"
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect isolated database.' }
if ([int]$tableCount -eq 0) {
    Docker-Checked @('cp',$dumpPath,'shoptech-marketplace-db:/tmp/source.dump')
    Docker-Checked @('exec','shoptech-marketplace-db','pg_restore','-U','postgres','-d','shoptech_marketplace',
        '--clean','--if-exists','--no-owner','--no-privileges','--exit-on-error','--single-transaction','/tmp/source.dump')
}
Docker-Checked @('exec','shoptech-marketplace-db','psql','-U','postgres','-d','shoptech_marketplace','-c',
    "SELECT current_database(), (SELECT count(*) FROM users) users, (SELECT count(*) FROM products) products, (SELECT count(*) FROM orders) orders")
Write-Host 'Sandbox ready: PostgreSQL 127.0.0.1:55432/shoptech_marketplace. Source database was accessed read-only.'
