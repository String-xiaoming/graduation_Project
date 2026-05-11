$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$crawlerScript = Join-Path $scriptDir "test1.py"
$logDir = Join-Path $scriptDir "scheduled_logs"
$pythonPath = "C:\Users\Lethe\AppData\Local\Programs\Python\Python311\python.exe"

if (-not (Test-Path $pythonPath)) {
    $pythonPath = "python"
}

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$runStamp = Get-Date -Format "yyyyMMdd_HHmmss"
$masterLog = Join-Path $logDir "scheduled_run_$runStamp.log"
$ascii = [System.Text.Encoding]::ASCII

$crawlTasks = @(
    [pscustomobject]@{ City = "qiandongnan"; Label = "qiandongnan"; Target = 7000 },
    [pscustomobject]@{ City = "qianxinan"; Label = "qianxinan"; Target = 6500 }
)

function Write-MasterLog {
    param([string]$Message)
    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    Add-Content -Path $masterLog -Value $line -Encoding UTF8
}

Write-MasterLog "Scheduled crawler started."
Write-MasterLog "Crawler script: $crawlerScript"

foreach ($task in $crawlTasks) {
    $stamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $inputPath = Join-Path $logDir ("{0}_{1}_input.txt" -f $task.City, $task.Target)
    $stdoutLog = Join-Path $logDir ("{0}_{1}_{2}.out.log" -f $task.City, $task.Target, $stamp)
    $stderrLog = Join-Path $logDir ("{0}_{1}_{2}.err.log" -f $task.City, $task.Target, $stamp)

    # test1.py asks for city, target count, and whether to change advanced settings.
    # The third input is n, so the crawler uses the default safe settings.
    $inputText = "{0}`r`n{1}`r`nn`r`n" -f $task.City, $task.Target
    [System.IO.File]::WriteAllText($inputPath, $inputText, $ascii)

    Write-MasterLog ("Start crawl: {0}, target: {1}" -f $task.Label, $task.Target)

    $process = Start-Process `
        -FilePath $pythonPath `
        -ArgumentList ('"{0}"' -f $crawlerScript) `
        -WorkingDirectory $scriptDir `
        -RedirectStandardInput $inputPath `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -NoNewWindow `
        -Wait `
        -PassThru

    if ($process.ExitCode -ne 0) {
        Write-MasterLog ("Crawl failed: {0}, exit code: {1}, stderr log: {2}" -f $task.Label, $process.ExitCode, $stderrLog)
        throw ("Crawl failed: {0}" -f $task.Label)
    }

    Write-MasterLog ("Crawl finished: {0}, stdout log: {1}" -f $task.Label, $stdoutLog)

    if ($task -ne $crawlTasks[-1]) {
        Write-MasterLog "Sleep 90 seconds before the next city."
        Start-Sleep -Seconds 90
    }
}

Write-MasterLog "Scheduled crawler finished."
