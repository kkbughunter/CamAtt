$ErrorActionPreference = 'Stop'

$pythonCommand = $null
$pythonArguments = @()

if (Get-Command py -ErrorAction SilentlyContinue) {
    $pythonCommand = 'py'
    $pythonArguments = @('-3.14')
} elseif (Get-Command python -ErrorAction SilentlyContinue) {
    $pythonCommand = 'python'
}

if (-not $pythonCommand) {
    throw 'Python 3.14 is required. Install it from python.org, then run this script again.'
}

try {
    $version = & $pythonCommand @pythonArguments -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')"
} catch {
    throw 'Python 3.14 was not found. Install Python 3.14, including the Python Launcher, then run this script again.'
}

if ($version -ne '3.14') {
    throw "CamAtt face recognition requires Python 3.14; found Python $version."
}

& $pythonCommand @pythonArguments -m venv .venv
& .\.venv\Scripts\python.exe -m pip install --upgrade pip setuptools wheel
& .\.venv\Scripts\python.exe -m pip install -r requirements.txt

Write-Host 'AI environment is ready. Start it with:' -ForegroundColor Green
Write-Host '.\.venv\Scripts\python.exe -m uvicorn app.main:app --reload --port 8000'
