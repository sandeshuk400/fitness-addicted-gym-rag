$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force backups | Out-Null
$name = "gym-$(Get-Date -Format 'yyyy-MM-dd-HHmm').dump"
$c = @('compose','-f','docker-compose.prod.yml','--env-file','.env.prod')

docker @c exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/gym.dump'
if ($LASTEXITCODE -ne 0) { throw 'pg_dump failed' }
docker @c cp db:/tmp/gym.dump "backups\$name"
if ($LASTEXITCODE -ne 0) { throw 'copy failed' }
if ((Get-Item "backups\$name").Length -lt 1024) { throw 'Backup file is too small' }

# Keep the newest 14 backups
Get-ChildItem backups -Filter 'gym-*.dump' | Sort-Object LastWriteTime -Descending |
  Select-Object -Skip 14 | Remove-Item
