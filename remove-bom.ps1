$file = 'c:\Users\conde-salazar\Documents\GitHub\Natcl-inn-Ontology\src\natclinn\util\CreateLinkProductToArgument.java'
$content = [System.IO.File]::ReadAllText($file)
$utf8 = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($file, $content, $utf8)
Write-Host "BOM supprimé du fichier $file"
