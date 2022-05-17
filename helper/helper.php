<?php

// Functions
function isNullOrEmpty(&$parameter){
	return (!isset($parameter) || empty($parameter));
}

function setNullIfEmpty(&$value) {
	return (!empty($value)) ? $value : null;
}

function getFriendlyAuthor(&$author) {
	$result = trim($author);
	$pos = strpos($result, ",");
	if ($pos != false) {
		$result = trim(substr($result, $pos + 1))." ".trim(substr($result, 0, $pos));
	}
	return $result;
}

function statsYearCmp(&$a, &$b) {
	if (is_numeric($a["year"])) {
		if (is_numeric($b["year"])) return (int)$a["year"] < (int)$b["year"];
		else return -1;
	} else {
		if (is_numeric($b["year"])) return 1;
		else return strcmp($b["year"], $a["year"]);
	}
	return 0;
}

function typeCmp(&$a, &$b) {
	switch ($a) {
		case "signature": return (in_array($b, array("auto", "ppn", "barcode"))) ? 1 : -1;
		case "barcode": return (in_array($b, array("auto", "ppn"))) ? 1 : -1;
		case "ppn": return ($b == "auto") ? 1 : -1;
		default: return -1;
	}
	return 0;
}

function getWebServiceQuery(&$parameters, &$isFamSearch) {
	$result = array();
	foreach ($parameters as $parameter) {
		// If search value is neither empty nor already used then collect it:
		$type = $parameter["type"];
		$value = $parameter["value"];
		if (!((isNullOrEmpty($type)) || (isNullOrEmpty($value)))) {
			$value = urlencode($value);
			if (!((isset($result[$type])) && (in_array($value, $result[$type])))) {
				$result[$type][] = $value;
			}
		}
	}
	$query = array();
	foreach ($result as $key => $value) {
		$query[$key] = implode(";", $value);
	}
	if (count($query) < 1) {
		return null;
	}
	uksort($query, "typeCmp");
	$query["fam"] = ($isFamSearch) ? "true" : null;
	return "?".urldecode(http_build_query($query));
}

function getQueryResponse(&$query, &$maxYears) {
	$data = null;
	$ppns = null;
	$dropbillFrame = null;
	$error = null;
	if (!isNullOrEmpty($query)) {
		$url = WEBSERVICE_STUB.$query;
		$json = callQuery($url);
		$data = getArrayFromJSON($json);
		$instances = getModelFromArray($data, $maxYears);
		$instances_count = count($instances);
		if ($instances_count > 0) {
			$ppns = getPPNs($data);
			$error = getError($instances);
			$dropbillFrame = getDropbillFrameFromModel($instances, $error);
		}
	}
	return array("count" => $instances_count, "ppns" => $ppns, "dropbill_frame" => $dropbillFrame, "error" => $error);
}

function callQuery(&$url) {
	$curl = curl_init($url);
	$options = array(
		CURLOPT_HTTPHEADER	=> array("Content-Type: application/json", "charset=utf-8"),
		CURLOPT_ENCODING	=> "",
		CURLOPT_USERAGENT	=> "ub-dropbill",
		CURLOPT_HEADER		=> false,
		CURLOPT_RETURNTRANSFER	=> true,
		CURLOPT_SSL_VERIFYPEER	=> true,
		CURLOPT_SSL_VERIFYHOST	=> 2
	);
	curl_setopt_array($curl, $options);
	$result = curl_exec($curl);
	$error_no = curl_errno($curl);
	$error_msg = curl_error($curl);
	if (!$result) {
		die("<b>Probleme beim Verbinden zum Hermes-WebService!</b><p>Fehlercode: ".$error_no." (\"".$error_msg."\")<br />Request-URI: <a href=\"".$url."\">".$url."</a></p>");
	}
	curl_close($curl);
	return $result;
}

function getArrayFromJSON(&$json) {
	return json_decode($json, null, 512, JSON_INVALID_UTF8_SUBSTITUTE | JSON_OBJECT_AS_ARRAY);
}

function getPPNs(&$data) {
	$ppns = array();
	foreach ($data as $date) {
		$ppns[] = $date["ppn"];
	}
	return implode(";", $ppns);
}

function getError(&$instances) {
	foreach ($instances as $instance) {
		if ((isset($instance["errorcode"])) && (isset($instance["message"])) && (isset($instance["query"]))) {
			$query = explode("=", $instance["query"]);
			return array("type" => trim($query[0]), "value" => trim($query[1]), "code" => $instance["errorcode"], "message" => $instance["message"]);
		}
	}
	return null;
}

?>
