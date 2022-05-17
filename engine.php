<?php

// Imports
require_once("helper/helper.php");
require_once("form.php");
require_once("dropbill.php");
require_once("pdf.php");

// Constants
define("WEBSERVICE_PROT", "https");
define("WEBSERVICE_HOST", "<your host for LSF queries>");
define("WEBSERVICE_NAME", "<your endpoint for LSF queries>");
define("WEBSERVICE_STUB", WEBSERVICE_PROT."://".WEBSERVICE_HOST."/".WEBSERVICE_NAME);
define("START_YEAR", 2007);
define("MAX_YEARS_DEFAULT", 10);
define("FAM_DEFAULT", false);

// Main

// Check for dropbill to be downloaded:
if ((isset($_POST["download"])) && (isset($_POST["dropbill"])) && (isset($_POST["filename"]))) {
	$dropbill = urldecode($_POST["dropbill"]);
	$filename = urldecode($_POST["filename"]);
	printPDF($dropbill, $filename);
} else {
	// Parse possibly entered GET parameters from permalinks or POST parameters from form to be resolved and print new form accordingly:
	$timestamp = hrtime(true);
	$isDebug = parseDebug($_GET, $_POST);
	$maxYears = parseMaxYears($_GET, $_POST, START_YEAR, MAX_YEARS_DEFAULT);
	$isFam = parseFam($_GET, $_POST, FAM_DEFAULT);
	$parameters = parseParameters($_GET, $_POST);
	$error = null;
	// Do not request a search if parameters were handed by GET parameters:
	if (isSearch($_GET, $_POST)) {
		$query = getWebServiceQuery($parameters, $isFam);
		$response = getQueryResponse($query, $maxYears);
		$count = $response["count"];
		$ppns = $response["ppns"];
		$dropbillFrame = $response["dropbill_frame"];
		$error = $response["error"];
	}

	echo "<table id=\"form\"><tr><td colspan=\"3\"><img src=\"res/logo_lisa.png\" alt=\"Library Inventory and Statistics Application\" title=\"Library Inventory and Statistics Application\" width=\"320\" height=\"208\"></td></tr><tr><td class=\"margin\" /><td>";
	printForm($parameters, $isDebug, $maxYears, START_YEAR, $isFam, $error);
	echo "</td><td class=\"margin\">";
	printOptions($ppns, $dropbillFrame, $maxYears, MAX_YEARS_DEFAULT, $isFam, $error);
	echo "</td></tr></table>";
	if ((isset($ppns)) && (isset($dropbillFrame))) {
		echo "<table id=\"form\"><tr><td class=\"margin\" /><td>";
		printResultCount($count, $error);
		echo "</td><td class=\"margin info\">";
		printProcessingTime($timestamp, $isDebug);
		echo "</td></tr></table>";
		printDropbill($dropbillFrame);
	}
}

?>
