[CmdletBinding()]
param(
    [switch]$Integracion
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$expectedDbUrl = 'jdbc:postgresql://127.0.0.1:5432/ivera_test'
$integrationVariables = @(
    'DB_URL',
    'DB_USERNAME',
    'DB_PASSWORD',
    'PGPASSWORD',
    'RUN_POSTGRES_INTEGRATION'
)
$originalEnvironment = @{}
$locationPushed = $false
$exitCode = 0

foreach ($name in $integrationVariables) {
    $originalEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

function Clear-IntegrationEnvironment {
    foreach ($name in $integrationVariables) {
        Remove-Item -Path "Env:$name" -ErrorAction SilentlyContinue
    }
}

function Restore-IntegrationEnvironment {
    foreach ($name in $integrationVariables) {
        if ($null -eq $originalEnvironment[$name]) {
            Remove-Item -Path "Env:$name" -ErrorAction SilentlyContinue
        }
        else {
            Set-Item -Path "Env:$name" -Value $originalEnvironment[$name]
        }
    }
}

function Read-IntegrationPassword {
    $securePassword = Read-Host 'DB_PASSWORD' -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)

    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Confirm-PostgresIdentity {
    if ($env:DB_URL -cne $expectedDbUrl) {
        throw 'El destino de integración no coincide con la base local autorizada.'
    }

    $psql = Get-Command 'psql' -ErrorAction SilentlyContinue
    if ($null -eq $psql) {
        throw 'No se encontró psql; no se puede comprobar la identidad de PostgreSQL.'
    }

    $env:PGPASSWORD = $env:DB_PASSWORD
    try {
        $identity = @(& $psql.Source -X -q -w -h '127.0.0.1' -p 5432 -U $env:DB_USERNAME -d 'ivera_test' -A -t -v ON_ERROR_STOP=1 -c 'BEGIN READ ONLY; SELECT current_database(), inet_server_addr(), inet_server_port(); ROLLBACK;')
        if ($LASTEXITCODE -ne 0) {
            throw 'No se pudo comprobar la identidad efectiva de PostgreSQL.'
        }
    }
    finally {
        Remove-Item -Path 'Env:PGPASSWORD' -ErrorAction SilentlyContinue
    }

    $expectedIdentity = 'ivera_test|127.0.0.1|5432'
    if ($identity.Count -ne 1 -or $identity[0].Trim() -cne $expectedIdentity) {
        throw 'PostgreSQL no confirmó el destino local autorizado; se canceló el modo de integración.'
    }
}

try {
    Clear-IntegrationEnvironment

    $opencode = Get-Command 'opencode' -ErrorAction SilentlyContinue
    if ($null -eq $opencode) {
        throw 'No se encontró el comando opencode en PATH.'
    }

    if ($Integracion) {
        Set-Item -Path 'Env:DB_URL' -Value $expectedDbUrl

        $username = $originalEnvironment['DB_USERNAME']
        if ([string]::IsNullOrWhiteSpace($username)) {
            $username = Read-Host 'DB_USERNAME'
        }
        if ([string]::IsNullOrWhiteSpace($username)) {
            throw 'DB_USERNAME es obligatorio para el modo de integración.'
        }
        Set-Item -Path 'Env:DB_USERNAME' -Value $username

        $password = $originalEnvironment['DB_PASSWORD']
        if ([string]::IsNullOrWhiteSpace($password)) {
            $password = Read-IntegrationPassword
        }
        if ([string]::IsNullOrEmpty($password)) {
            throw 'DB_PASSWORD es obligatorio para el modo de integración.'
        }
        Set-Item -Path 'Env:DB_PASSWORD' -Value $password
        Remove-Variable -Name password -ErrorAction SilentlyContinue

        Confirm-PostgresIdentity
        Write-Host 'Identidad PostgreSQL local confirmada. No se iniciaron servicios ni pruebas de integración.'
    }
    else {
        Write-Host 'Modo normal: OpenCode se iniciará sin variables PostgreSQL ni pruebas de integración.'
    }

    Push-Location -LiteralPath $projectRoot
    $locationPushed = $true
    & $opencode.Source
    $exitCode = $LASTEXITCODE
}
finally {
    if ($locationPushed) {
        Pop-Location
    }
    Restore-IntegrationEnvironment
}

return $exitCode
