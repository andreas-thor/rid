<?php 


$handle = fopen("rid_gender.txt", "r");
if ($handle) {
    while (($line = fgets($handle)) !== false) {

	$rid = trim($line);
	$filename = "download/rid_" . $rid . ".html";
	if (file_exists($filename)) {
		print ("Skipping " . $filename . "\n");
	} else {
		print ("Downloading " . $rid . "\n");
		file_put_contents($filename, file_get_contents("http://hftlcdmvl008.appspot.com/hello?id=".$rid));
		$s = rand(10,50);
		print ("Sleeping for " . $s . " seconds.\n");
		sleep ($s);
	}
	
    }

    fclose($handle);
}

?>