# Analyse des marqueurs NOVA
$xmlPath = "c:\var\www\natclinn\ontologies\NatclinnNOVAmarkers.xml"
$content = Get-Content $xmlPath -Raw

# Pattern pour extraire les marqueurs
$pattern = '(?s)<ncl:NOVAMarker.*?<ncl:markerValue>(.*?)</ncl:markerValue>.*?<ncl:markerType>(.*?)</ncl:markerType>.*?<ncl:belongsToNOVAGroup>.*?<ncl:groupNumber.*?>(.*?)</ncl:groupNumber>'

$matches = [regex]::Matches($content, $pattern)

Write-Host "Total markers found: $($matches.Count)`n"

# Grouper par groupe NOVA
$byGroup = @{}
foreach ($match in $matches) {
    $value = $match.Groups[1].Value
    $type = $match.Groups[2].Value  
    $group = $match.Groups[3].Value
    
    if (-not $byGroup.ContainsKey($group)) {
        $byGroup[$group] = @()
    }
    $byGroup[$group] += @{Value=$value; Type=$type}
}

# Afficher statistiques
foreach ($group in 1..4) {
    $count = if ($byGroup.ContainsKey("$group")) { $byGroup["$group"].Count } else { 0 }
    Write-Host "Groupe $group : $count marqueurs"
}

Write-Host "`n=== GROUPE 4 (Ultra-processed) ==="
if ($byGroup.ContainsKey("4")) {
    $byGroup["4"] | Where-Object { $_.Type -eq "ingredients" } | 
        Select-Object -ExpandProperty Value | 
        Sort-Object | ForEach-Object { Write-Host " - $_" }
}
