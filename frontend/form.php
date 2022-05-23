<?php

// Functions
function parseDebug(&$getParameters, &$postParameters) {
	if ((isset($getParameters["debug"])) || (isset($postParameters["debug"]))) {
		return true;
	}
	return false;
}

function parseMaxYears(&$getParameters, &$postParameters, $startYear, $maxYearsDefault) {
	if (isset($getParameters["max_years"])) {
		return $getParameters["max_years"];
	}
	if ((isset($postParameters["max_years"])) && ($postParameters["max_years"] <= intval(date("Y")) - START_YEAR + 1)) {
		return $postParameters["max_years"];
	}
	return $maxYearsDefault;
}

function parseFam(&$getParameters, &$postParameters, $famDefault) {
	if (isset($getParameters["fam"])) {
		return (strtolower($getParameters["fam"]) == "true") ? true : false;
	}
	if (isset($postParameters["famBox"])) {
		return true;
	} else {
		if (isset($postParameters["fam"])) {
			return false;
		}
	}
	return $famDefault;
}

function parseParameters(&$getParameters, &$postParameters) {
	$result = array();
	$empty_set = array("value" => null, "type" => "auto");
	if (isset($getParameters["ppn"])) {
		$parameters = $getParameters["ppn"];
		$values = explode(";", $parameters);
		foreach ($values as $value) {
			$result[] = array("value" => $value, "type" => "ppn");
		}
		return $result;
	}
	if (isset($postParameters["parameters"])) {
		$parameters = $postParameters["parameters"];
		$parameters_count = count($parameters);
		$empty_parameters_count = count(array_keys($parameters, $empty_set));
		for ($i = 0; $i < $parameters_count; $i++) {
			$parameter = $parameters[$i];
			// If 'remove' button was clicked and there is more than one entry at all, then apply (remove) current entry to used 'parameters' record:
			if (($parameter["remove"]) && ($parameters_count > 1)) {
				continue;
			}
			// Apply entered values as new entry to 'parameters' record:
			$value = trim($parameter["value"]);
			$type = $parameter["type"];
			$record = array("value" => $value, "type" => $type);
			if (!(((isNullOrEmpty($value)) && ($postParameters["search"])) || (in_array($record, $result)))) {
				$result[] = $record;
			}
			// If 'add' button was clicked and neither current entry nor other entries are empty, then add an empty entry to used 'parameters' record at respectively given position:
			if (($parameter["add"]) && ($parameter["value"] != "") && ($empty_parameters_count < 1)) {
				array_splice($result, $i + 1, 0, array(0 => $empty_set));
			}
		}
	}
	if (count($result) < 1) {
		$result[] = $empty_set;
	}
	return $result;
}

function isSearch(&$getParameters, &$postParameters) {
	return ((isNullOrEmpty($getParameters)) && (isset($postParameters["search"])));
}

function printForm(&$parameters, &$isDebug, &$maxYears, $startYear, &$isFam, &$error) {
	$parameters_count = count($parameters);
	echo "<form action=\".\"
			method=\"post\"
			enctype=\"application/x-www-form-urlencoded\"
			accept-charset=\"utf-8\"
			autocomplete=\"off\">
		<p class=\"parameter\">
			<label for=\"max_years\">Maximale Jahre anzeigen:</label>
			<input type=\"number\" name=\"max_years\" id=\"max_years\" value=\"".$maxYears."\" min=\"4\" max=\"".(intval(date("Y")) - $startYear + 1)."\"></input>
		</p>
		<p class=\"parameter last\">
			<label style=\"color: gray; font-style: italic;\" for=\"fam\">Verwandte Titel anzeigen (BETA):</label>
			<input type=\"checkbox\" name=\"famBox\" id=\"famBox\"".(($isFam) ? " checked=\"checked\"" : "")."></input>
			<input type=\"hidden\" name=\"fam\" id=\"fam\"></input>
		</p>";
	echo (($isDebug) ? "<input type=\"hidden\" name=\"debug\" id=\"debug\" value=\"1\" title=\"Ladezeit einblenden ...\"></input>" : "");
	for ($i = 0; $i < $parameters_count; $i++) {
		$value = $parameters[$i]["value"];
		$type = $parameters[$i]["type"];
		$isErrorValue = (isset($error)) && (strtolower($error["value"]) == strtolower($value)) && (strtolower($error["type"]) == strtolower($type));
		echo "<div>
			<fieldset".(($isErrorValue) ? " class=\"error\"" : "").">
				<legend>Suchfeld ".($i + 1)."</legend>
				<table>
					<tr>
						<td class=\"left\">
							<label for=\"parameter".($i + 1)."\">Suchparameter:</label>
						</td>
						<td class=\"right\">
							<input type=\"search\"
								name=\"parameters[".$i."][value]\"
								id=\"parameter".($i + 1)."\"
								class=\"performSearch\"
								value=\"".$value."\"
								title=\"Suchparameter eingeben ...\"
								minlength=\"4\"
								maxlength=\"20\"
								pattern=\".*\" />
						</td>
						<td>
							<select name=\"parameters[".$i."][type]\" id=\"type".($i + 1)."\" required=\"required\" title=\"Suchtyp auswählen ...\">
								<option value=\"auto\"".((($type == "auto") || (($type != "ppn") && ($type != "barcode") && ($type != "signature"))) ? " selected=\"selected\"" : "").">[auto]</option>
								<option value=\"ppn\"".(($type == "ppn") ? " selected=\"selected\"" : "").">PPN</option>
								<option value=\"barcode\"".(($type == "barcode") ? " selected=\"selected\"" : "").">Barcode</option>
								<option value=\"signature\"".(($type == "signature") ? " selected=\"selected\"" : "").">Signatur</option>
							</select>
						</td>
					</tr>
					<tr>
						<td colspan=\"3\">
							<div class=\"add_remove\">
								<input type=\"submit\"
									name=\"parameters[".$i."][add]\"
									id=\"add".($i + 1)."\"
									class=\"add\"
									value=\"+\"
									title=\"Weitere Suchparameter ...\"
									tabindex=\"-1\" />
								<input type=\"submit\"
									name=\"parameters[".$i."][remove]\"
									id=\"remove".($i + 1)."\"
									class=\"remove\"
									value=\"-\"
									title=\" Suchparameter entfernen ...\" ".(($parameters_count <= 1) ? "disabled=\"disabled\" " : "")."
									tabindex=\"-1\" />
							</div>
						</td>
					</tr>
				</table>".(($isErrorValue) ? "<span><b>Fehlerhafte Suche!</b><br /><i>Fehlercode ".$error["code"].":<br />".$error["message"]."</i></span>" : "")."
			</fieldset>
		</div>";
	}
	echo "<input type=\"submit\"
			name=\"search\"
			id=\"search\"
			value=\"Suchen\"
			title=\"Suche starten ...\" />
		</form>";
	return;
}

function printOptions(&$ppns, &$dropbillFrame, &$maxYears, $maxYearsDefault, &$isFam, &$error) {
	echo "<fieldset id=\"options\"><legend>Optionen</legend>";
	printHelpButton();
	printResetButton();
	// Check for search button to be clicked without an empty search record:
	if ((isset($ppns)) && (isset($dropbillFrame))) {
		if (!isset($error)) {
			printPermalinkButton($ppns, $maxYears, $maxYearsDefault, $isFam);
			printDownloadButton($dropbillFrame, $ppns);
		}
		printHideButton();
	}
	echo "</fieldset>";
	return;
}

function printHelpButton() {
	echo "<button type=\"button\"
		id=\"help\"
		class=\"button\"
		title=\"Hilfeseite anzeigen ...\">Hilfe</button>";
	return;
}

function printResetButton() {
	echo "<button type=\"button\"
		id=\"reset\"
		class=\"button\"
		title=\"Alle Suchfelder zurücksetzen ...\">Zurücksetzen</button>";
	return;
}

function printPermalinkButton(&$ppns, &$maxYears, &$maxYearsDefault, &$isFam) {
	$query = (!isNullOrEmpty($ppns)) ? "?".urldecode(http_build_query(array_merge(array("ppn" => $ppns), (($maxYears != $maxYearsDefault) ? array("max_years" => $maxYears) : array()), (($isFam) ? array("fam" => "false") : array())))) : null;
	$uri = (((isset($_SERVER["HTTPS"])) && ($_SERVER["HTTPS"] === "on")) ? "https" : "http")."://".$_SERVER["HTTP_HOST"].strtok($_SERVER["REQUEST_URI"], '?');
	$link = $uri.$query;
	echo "<button type=\"button\"
		id=\"permalink\"
		class=\"button\"
		link=\"".$link."\"
		message=\"Der Permalink für Ihre Suche wurde erfolgreich in die Zwischenablage kopiert:\n\n".$link."\"
		title=\"Permalink zum Wiederaufruf in die Zwischenablage kopieren ...\">Permalink</button>";
	return;
}

function printDownloadButton(&$dropbillFrame, &$ppns) {
	$title = "Dropbill_".$ppns;
	$filename = date("Ymd-His_").$title;
	echo "<form action=\".\"
		method=\"post\"
		enctype=\"application/x-www-form-urlencoded\"
		accept-charset=\"utf-8\"
		autocomplete=\"off\">
			<input type=\"hidden\"
				name=\"dropbill\"
				id=\"dropbill\"
				value='".urlencode(wrapDropbillHTML($dropbillFrame, $title))."' />
			<input type=\"hidden\"
				name=\"filename\"
				id=\"filename\"
				value='".$filename."' />
			<input type=\"submit\"
				name=\"download\"
				id=\"download\"
				class=\"button\"
				value=\"Herunterladen\"
				title=\"Erstellten Löschzettel als PDF-Datei herunterladen ...\" />
		</form>";
	return;
}

function printHideButton() {
	echo "<button type=\"button\"
		id=\"hide\"
		class=\"button\"
		status=\"show\"
		showcontent=\"Ausblenden\"
		showtitle=\"Löschzettel ausblenden ...\"
		hidecontent=\"Einblenden\"
		hidetitle=\"Löschzettel einblenden ...\"></button>";
	return;
}

function printResultCount($count, &$error) {
	if (isset($error)) {
		$count--;
		echo "<span class=\"error\">Die Suche enthielt Fehler!</span>";
	}
	if ($count >= 0) {
		echo "<b>Gefundene Datensätze: ".$count."</b>";
	}
	return;
}

function printProcessingTime(&$timestamp, &$isDebug) {
	if (($isDebug) && (isset($timestamp))) {
		echo "<i>Ladezeit: ".number_format(round((hrtime(true) - $timestamp) / 1000000, 3), 3, ".", "")." ms</i>";
	}
}

function printDropbill(&$dropbillFrame) {
	echo "<hr id=\"stripline\" />".$dropbillFrame;
	return;
}

?>
