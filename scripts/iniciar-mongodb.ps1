# MongoDB Community portátil para desenvolvimento local no Windows.
# Execute em um terminal separado; use Ctrl+C para encerrar o servidor.
$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
$pastaFerramentas = Join-Path $raizProjeto '.tools'
$executavelMongo = Join-Path $pastaFerramentas 'mongodb-win32-x86_64-windows-8.0.12\bin\mongod.exe'
$pastaDados = Join-Path $raizProjeto '.local\mongo-data'

if (Get-NetTCPConnection -LocalPort 27017 -State Listen -ErrorAction SilentlyContinue) {
    Write-Host 'A porta 27017 já está em uso. Se for o MongoDB, execute a aplicação diretamente.'
    exit 0
}
if (-not (Test-Path -LiteralPath $executavelMongo)) {
    New-Item -ItemType Directory -Force -Path $pastaFerramentas | Out-Null
    $arquivoZip = Join-Path $pastaFerramentas 'mongodb.zip'
    Write-Host 'Baixando MongoDB Community 8.0.12 do site oficial...'
    Invoke-WebRequest -Uri 'https://fastdl.mongodb.org/windows/mongodb-windows-x86_64-8.0.12.zip' -OutFile $arquivoZip
    Expand-Archive -LiteralPath $arquivoZip -DestinationPath $pastaFerramentas -Force
}
New-Item -ItemType Directory -Force -Path $pastaDados | Out-Null
Write-Host 'MongoDB em mongodb://localhost:27017. Deixe este terminal aberto; Ctrl+C encerra.'
& $executavelMongo --dbpath $pastaDados --bind_ip 127.0.0.1 --port 27017
exit $LASTEXITCODE
