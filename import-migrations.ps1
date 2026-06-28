$mysql = "C:\xampp\mysql\bin\mysql.exe"
$db = "playonmytv"
$user = "root"

Get-ChildItem ".\database\migrations*.sql" |
Sort-Object Name |
ForEach-Object {

```
Write-Host ""
Write-Host "====================================="
Write-Host "Running $($_.Name)"
Write-Host "====================================="

Get-Content $_.FullName -Raw | & $mysql -u $user $db

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "FAILED: $($_.Name)"
    exit 1
}

Write-Host "SUCCESS"
```

}

Write-Host ""
Write-Host "All migrations completed successfully."
