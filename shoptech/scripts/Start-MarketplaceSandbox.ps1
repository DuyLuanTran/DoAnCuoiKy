$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$localRoot = Join-Path $projectRoot '.local'
$postgresEnv = Join-Path $localRoot 'postgres.env'
if (!(Test-Path -LiteralPath $postgresEnv)) { throw 'Run Initialize-MarketplaceSandbox.ps1 first.' }
$dbPasswordLine = Get-Content -LiteralPath $postgresEnv | Where-Object { $_.StartsWith('POSTGRES_PASSWORD=') }
$env:SANDBOX_DB_PASSWORD = $dbPasswordLine.Substring('POSTGRES_PASSWORD='.Length)
$previewEnv = Join-Path $localRoot 'preview.env'
if (!(Test-Path -LiteralPath $previewEnv)) {
    $secret = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
    $demoPassword = 'ShopDemo-' + [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(6)) + '!'
    @("SANDBOX_JWT_SECRET=$secret", "SANDBOX_DEMO_PASSWORD=$demoPassword") | Set-Content -LiteralPath $previewEnv -Encoding utf8
}
foreach ($line in Get-Content -LiteralPath $previewEnv) {
    $parts = $line.Split('=',2)
    if ($parts.Count -eq 2) { [Environment]::SetEnvironmentVariable($parts[0],$parts[1],'Process') }
}
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://127.0.0.1:55432/shoptech_marketplace'
$env:SPRING_DATASOURCE_USERNAME = 'postgres'
$env:SPRING_DATASOURCE_PASSWORD = $env:SANDBOX_DB_PASSWORD
$env:SPRING_CONFIG_LOCATION = 'classpath:/application-isolated.properties'
$env:SPRING_PROFILES_ACTIVE = 'isolated'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
Write-Host 'Starting isolated preview at http://127.0.0.1:18081. Real email, chatbot and Cloudinary writes are disabled.'
& .\mvnw.cmd -o spring-boot:run
if ($LASTEXITCODE -ne 0) { throw 'Sandbox application exited with an error.' }
