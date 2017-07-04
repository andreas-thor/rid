<?php 


/**
 * $argv[1] = file that contains list of RIDs
 * $argv[2] = directory for storing downloaded html files
 * $argv[3] = URL
 */


$handle = fopen($argv[1], "r");
if ($handle) {
    while (($line = fgets($handle)) !== false) {

	$rid = trim($line);
	$filename = $argv[2] . "rid_" . $rid . ".html";
	if (file_exists($filename)) {
		print ("Skipping " . $filename . "\n");
	} else {
		print ("Downloading " . $rid . "\n");
		file_put_contents($filename, file_get_contents("http://" . $argv[3] . "/hello?id=".$rid));
		$s = rand(10,50);
		print ("Sleeping for " . $s . " seconds.\n");
		sleep ($s);
	}
	
    }

    fclose($handle);
}

?>