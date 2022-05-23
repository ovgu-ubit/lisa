<?php

// Imports
require_once("lib/dompdf/autoload.inc.php");
use Dompdf\Dompdf;
use Dompdf\Options;

// Functions
function printPDF(&$html, &$filename) {
	ob_clean();
	$options = new Options();
	$options->setIsHtml5ParserEnabled(true);
	$dompdf = new Dompdf($options);
	$dompdf->loadHtml($html, "utf-8");
	$dompdf->setPaper("a4", "portrait");
	$dompdf->render();
	$dompdf->stream($filename, array("compress" => 1, "Attachment" => 1));
}

?>
