<?php
$potentialInjection = $_GET['forgotToQuote'];
mysql_query("SELECT * FROM table1 WHERE a = '" . $potentialInjection . "'");

$noInjection = mysql_real_escape_string($_GET['help']);
mysql_query("SELECT * FROM table2 WHERE a = b = '" . $noInjection . "'");
