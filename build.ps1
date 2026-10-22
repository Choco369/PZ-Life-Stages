param(
    [Parameter(Mandatory=$true)][string]$GameJar,
    [Parameter(Mandatory=$true)][string]$ZombieBuddyJar,
    [string]$JdkHome='',
    [string]$SourceDirectory=(Join-Path $PSScriptRoot 'src/main/java'),
    [string]$ResourcesDirectory=(Join-Path $PSScriptRoot 'src/main/resources'),
    [string]$OutputJar=(Join-Path $PSScriptRoot 'out/playable-children.jar')
)
$ErrorActionPreference='Stop'
foreach($taskDependency in @($GameJar,$ZombieBuddyJar)) {
    if(!(Test-Path -LiteralPath $taskDependency -PathType Leaf)){throw "Missing dependency: $taskDependency"}
}
$taskJavac=if($JdkHome){Join-Path $JdkHome 'bin/javac.exe'}else{(Get-Command javac -ErrorAction Stop).Source}
$taskJar=if($JdkHome){Join-Path $JdkHome 'bin/jar.exe'}else{(Get-Command jar -ErrorAction Stop).Source}
$taskSources=@(Get-ChildItem -LiteralPath $SourceDirectory -Recurse -File -Filter '*.java' | Sort-Object FullName | ForEach-Object FullName)
if($taskSources.Count -eq 0){throw 'No Java sources found'}
$taskResource=Join-Path $ResourcesDirectory 'pz/playablechildren/clothing-restrictions.tsv'
if(!(Test-Path -LiteralPath $taskResource)){throw 'Missing clothing policy resource'}
$taskOutput=[IO.Path]::GetFullPath($OutputJar)
$taskOutputParent=Split-Path -Parent $taskOutput
New-Item -ItemType Directory -Path $taskOutputParent -Force | Out-Null
$taskBuild=Join-Path $taskOutputParent ('build-'+[Guid]::NewGuid().ToString('N'))
$taskClasses=Join-Path $taskBuild 'classes'
New-Item -ItemType Directory -Path $taskClasses -Force | Out-Null
& $taskJavac --release 21 -encoding UTF-8 -cp "$GameJar;$ZombieBuddyJar" -d $taskClasses @taskSources
if($LASTEXITCODE -ne 0){throw 'Java compilation failed'}
Copy-Item -LiteralPath $taskResource -Destination (Join-Path $taskClasses 'pz/playablechildren/clothing-restrictions.tsv')
& $taskJar --create --file $taskOutput --date=2026-10-18T00:00:00Z -C $taskClasses .
if($LASTEXITCODE -ne 0){throw 'JAR packaging failed'}
$taskHash=(Get-FileHash -LiteralPath $taskOutput -Algorithm SHA256).Hash
Write-Output "Built $($taskSources.Count) source files: $taskOutput"
Write-Output "SHA256 $taskHash"
