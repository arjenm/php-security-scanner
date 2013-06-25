<?php

if($afield == 'aValue')
	$potentialInjection = $_GET['forgotToQuote'];
else
	$potentialInjection = mysql_real_escape_string($_GET['thisOneWeDidNotForget']);

$someTrinaryComplications = ($someParam == 'someValue' ? 'This is save to use' : 'and this as well');
$aNumberIsSafe = (int)$_GET['number'];

$sql = "SELECT * FROM table1
	WHERE fieldA = '" . $potentialInjection . "'
		AND fieldB = '" . $someTrinaryComplications . "'
		AND fieldC = " . $aNumberIsSafe . "
		AND fieldD";
mysql_query($sql);
