<?php

// Functions
function parseType(&$instance, &$date) {
	$query = explode("=", $date["query"]);
	$type = strtolower(trim($query[0]));
	$value = trim($query[1]);
	switch ($type) {
		case "ppn":
			$label = "PPN";
			break;
		case "bar":
			$label = "Barcode";
			break;
		case "sig":
			$label = "Signatur";
			break;
		case "fam":
			$label = "Super PPN";
			break;
		default:
			$label = "Auto";
	}
	$instance["type"] = $type;
	$instance["query"] = $label." = ".$value;
	return;
}

function parseError(&$instance, &$errorcode) {
	$instance["errorcode"] = $errorcode;
	$message = null;
	if (count($errorcode) > 0) {
		switch ($errorcode[0]) {
			case "1":
				$message = "Fehler in der URL-Query: ";
				break;
			case "2":
				$message = "Fehler in der XML-Schnittstelle; ";
				break;
			case "3":
				$message = "Fehler in der DB: ";
				break;
			case "4":
				$message = "Fehler bei der Suche: ";
				break;
		}
		switch ($errorcode) {
			case "100":
				$message .= "Zu viele Titel (max. 100)";
				break;
			case "101":
				$message .= "PPN konnte nicht geparst werden (mit spezieller Query)";
				break;
			case "102":
				$message .= "Barcode konnte nicht geparst werden (mit spezieller Query)";
				break;
			case "103":
				$message .= "Signatur konnte nicht geparst werden (mit spezieller Query)";
				break;
			case "104":
				$message .= "Autoerkennung fehlgeschlagen (mit spezieller Query \"auto=\")";
				break;
			case "200":
				$message .= "Konfigurationsfehler";
				break;
			case "201":
				$message .= "Fehler beim Öffnen des Streams";
				break;
			case "202":
				$message .= "Fehler in der XML-Syntax (Timeout?)";
				break;
			case "300":
				$message .= "Statistiken konnten nicht abgerufen werden";
				break;
			case "400":
				$message .= "Kein Ergebnis bei der Suche, bitte Suchparameter prüfen oder diese Suche entfernen";
				break;
		}
	}
	$instance["message"] = $message;
	return;
}

function parseData(&$instance, &$date, &$max_years) {
	$instance["title"] = $date["title"];
	$volume = $date["volume"];
	$instance["volume"] = (!isNullOrEmpty($volume)) ? $volume : null;
	$author = getFriendlyAuthor($date["author"]);
	$instance["author"] = (!isNullOrEmpty($author)) ? $author.((!isNullOrEmpty($date["co_authors"])) ? " et al." : "") : null;
	$instance["edition"] = $date["edition"];
	$instance["year_of_creation"] = $date["year_of_creation"];
	$instance["classification"] = $date["classification"];
	$years = array();
	foreach ($date["copies"] as $element) {
		$copy = array();
		$signature = $element["signature"];
		if (isNullOrEmpty($signature)) {
			continue;
		}
		$copy["signature"] = $signature;
		$copy["location"] = $element["location"];
		$remark = trim($element["remark"]);
		switch (strtolower($remark)) {
			case "i":
				$remark = "Mit Zustimmung";
				break;
			case "f":
				$remark = "Nur Fotokopie";
				break;
			case "z":
				$remark = "Gelöscht";
				break;
			case "a":
				$remark = "Bestellt";
				break;
			case "g":
			case "o":
				$remark = "Gesperrt";
				break;
		}
		$remark = (strtolower($signature) == "gelöscht") ? implode(", ", array_filter(array("gelöscht", $remark))) : $remark;
		$remark = (strtolower($element["status"]) == "ausleihbar|ausgeliehen") ? implode(", ", array_filter(array($remark, "derzeit ausgeliehen"))) : $remark;
		$copy["remark"] = $remark;
		$stats = $element["stats"];
		usort($stats, "statsYearCmp");
		$year_wrap = "";
		for ($i = 0; $i < count($stats); $i++) {
			$stat = $stats[$i];
			if (($i >= $max_years - 1) && ($max_years > 0)) {
				if ($i == $max_years - 1) {
					$year_wrap = "<=".preg_replace("/[^0-9]/", "", $stats[$max_years - 1]["year"]);
					$copy["loans"][$year_wrap] = 0;
					if (!in_array($year_wrap, $years)) {
						$years[] = $year_wrap;
					}
				}
				$copy["loans"][$year_wrap] += $stat["num_loans"];
			} else {
				$year = $stat["year"];
				$copy["loans"][$year] = $stat["num_loans"];
				if (!in_array($year, $years)) {
					$years[] = $year;
				}
			}
		}
		$instance["copies"][] = $copy;
	}
	$instance["years"] = $years;
	return null;
}

function getModelFromArray(&$data, &$max_years) {
	$instances = array();
	foreach ($data as $date) {
		$instance = array();
		parseType($instance, $date);
		$errorcode = $date["errorcode"];
		if (isset($errorcode)) {
			parseError($instance, $errorcode);
		} else {
			parseData($instance, $date, $max_years);
		}
		$instances[] = $instance;
	}
	return $instances;
}

function getDropbillFrameFromModel(&$instances, &$error) {
	// Remarks section:
	$result = "<div id=\"dropbill_frame\">";
	$noError = !isset($error);
	$hasMultiInstances = count($instances) > 1;
	$hasValidInstances = ($noError) || ($hasMultiInstances);
	if ($hasValidInstances) {
		if ($noError) {
			$result .= "<a class=\"anchor\" id=\"start\"></a>";
		}
		$result .= "<h1>Bestandspflegezettel</h1>
			<p>
				<table class=\"header\">
					<tr>
						<td><div>Fachreferent*in:</div><div class=\"field_mark last_column\"></div></td>
						<td><div>Unterschrift:</div><div class=\"field_mark last_column\"></div></td>
					</tr>
				</table>
				<table class=\"header\">
					<tr>
						<td><div class=\"check_mark\"></div><div class=\"last_column\">Neues Schild</div></td>
						<td><div class=\"check_mark\"></div><div class=\"last_column\">Aussonderung (siehe unten)</div></td>
						<td><div class=\"check_mark\"></div><div class=\"last_column\">Ersatz bestellt</div></td>
					</tr>
					<tr>
						<td><div class=\"check_mark\"></div><div class=\"last_column\">Neue Auflage bestellt</div></td>
						<td colspan=\"2\"><div class=\"check_mark\"></div><div>Weitergabe an FR:</div><div class=\"field_mark last_column\"></div></td>
					</tr>
					<tr>
						<td><div class=\"check_mark\"></div><div class=\"last_column\">Buchbinder</div></td>
						<td colspan=\"2\"><div class=\"check_mark\"></div><div>Notation:</div><div class=\"field_mark last_column\"></div></td>
					</tr>
				</table>
				<table class=\"header\">
					<tr>
						<td><div>Bearbeiter*in:</div><div class=\"field_mark last_column\"></div></td>
					</tr>
				</table>
				<table class=\"header\">
					<tr>
						<td><div>Neueste lieferbare Auflage mit Preisangabe:</div><div class=\"field_mark last_column\"></div></td>
					</tr>
				</table>
				<table class=\"header\">
					<tr>
						<td><div>Bemerkungen:</div><div class=\"field_mark last_column\"></div></td>
					</tr>
				</table>
			</p>
			<h2>Vorhandene Auflagen und Ausleihstatistik</h2>";
	}
	// Result details section:
	foreach ($instances as $instance) {
		if ($hasValidInstances) {
			$result .= "<div class=\"".(($instance["type"]) == "fam" ? "famitem" : "item")."\"><hr />";
		}
		$errorcode = $instance["errorcode"];
		if (isset($errorcode)) {
			$result .= "<span class=\"information\"><b>Die Anfrage enthielt einen Fehler bei der folgenden Suche: </b>".$instance["query"].".</span>";
			$result .= "<p><span class=\"information\"><small><b>Fehlercode:</b> ".$errorcode."<br /><b>Fehlermeldung:</b> ".$instance["message"]."</small></span></p>";
		} else {
			$years = $instance["years"];
			$noStats = isNullOrEmpty($years);
			$totalSum = 0;
			$result .= "<p class=\"parameter\">Suche: \"<span>".$instance["query"]."\"</span></p>";
			// Build drop bill header:
			$header = implode(", ", array_filter(array($instance["title"], $instance["volume"], $instance["author"], $instance["edition"], $instance["year_of_creation"], $instance["classification"])));
			$result .= htmlspecialchars($header);
			// Build copy table header:
			$copies = $instance["copies"];
			if (count($copies) > 0) {
				$result .= "<table class=\"copy\"><tr><th>Signatur</th><th>Standort</th><th>Bemerkungen</th>";
				if (!$noStats) {
					foreach ($years as $year) {
						$result .= "<th>".htmlspecialchars($year)."</th>";
					}
					$result .= "<th>Summe</th>";
				}
				$result .= "</tr>";
				// Build copy table content:
				foreach ($copies as $copy) {
					$result .= "<tr><td>".htmlspecialchars($copy["signature"])."</td><td>".htmlspecialchars($copy["location"])."</td><td>".htmlspecialchars($copy["remark"])."</td>";
					if (!$noStats) {
						for ($i = 0; $i < count($years); $i++) {
							$year = $years[$i];
							$loans = $copy["loans"];
							$content = $loans[$year];
							$value = (!isNullOrEmpty($content)) ? $content : 0;
							// If there is no loan entry after the current entry then mark it as 'last', otherwise mark non-empty entries with 'value' and empty entries with 'null' generally:
							$class = ($value != 0) ? ((array_unique(array_merge(array(0), array_slice($loans, 0, $i))) !== array(0)) ? "value" : "last") : "null";
							$result .= "<td class=\"".$class."\">".htmlspecialchars($value)."</td>";
						}
						$sum = array_sum($copy["loans"]);
						$totalSum += $sum;
						$result .= "<td class=\"sum\">".htmlspecialchars($sum)."</td>";
					}
					$result .= "</tr>";
				}
				$result .= "</table>";
			}
			// Build copy table footer:
			$result .= ((($noStats) || ($totalSum < 1)) ? "<span class=\"information\">Exemplare wurden noch nie ausgeliehen.</span>" : "");
			$result .= "<table class=\"header footer\"><tr><td><div>Bemerkungen:</div><div class=\"field_mark last_column\"></div></td></tr></table>";
			$result .= "<table class=\"header footer\"><tr><td><div>Entscheidung:</div><div class=\"field_mark last_column\"></div></td></tr></table></div>";
		}
	}
	$result .= "</div>";
	return $result;
}

function wrapDropbillHTML(&$dropbill, &$title) {
	$css = file_get_contents("dropbill.css");
	$result = "<html>
			<head>
				<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">
				<title>".$title."</title>
				<style type=\"text/css\" media=\"all\">".$css."</style>
			</head>
			<body>".$dropbill."</body>
		</html>";
	return $result;
}

?>
