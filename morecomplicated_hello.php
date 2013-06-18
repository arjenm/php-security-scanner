<?php

if($afield == 'aValue')
	$potentialInjection = $_GET['forgotToQuote'];
else
	$potentialInjection = mysql_real_escape_string($_GET['thisOneWeDidNotForget']);

$someTrinaryComplications = ($someParam == 'someValue' ? 'This is save to use' : 'and this as well');
$aNumberIsSave = (int)$_GET['number'];

$sql = "SELECT * FROM table1
	WHERE fieldA = '" . $potentialInjection . "'
		AND fieldB = '" . $someTrinaryComplications . "'
		AND fieldC = " . $aNumberIsSave . "
		AND fieldD";
mysql_query($sql);
