
$dir = "E:\Dev\ResearcherId\data"
get-content "${dir}\rid_gender.txt" | Sort-Object | ForEach-Object { 

	if (!(Test-Path ${dir}\rid_gender\rid_$_.html -PathType Leaf)) {
		Write-Host "Download $_"
		Invoke-WebRequest -URI http://hftlcdmvl008.appspot.com/hello?id=$_ -Outfile ${dir}\rid_gender\rid_$_.html -TimeoutSec 30
		$wait = Get-Random -Minimum 20 -Maximum 40
		Write-Host "Sleep for ${wait} seconds"
		Start-Sleep -s ${wait}
	}
	
}

