## INTRODUCTION

The php-bug-scanner is a relatively simple set of classes that leverages Caucho's PHP-engine, Quercus,
 to analyze source code and determine potential dangerous inputs to specific methods.

I.e. it can scan for classic sql injection, cross site scripting and similar vulnerabilities which are of this structure:
```php
<?php
mysql_query("SELECT * FROM table WHERE a = '" . $potentialInjection . "' AND b = '" . $noInjection . "'");
```

In such a statement, its uses the parse-tree of Quercus to be able to trace back how a variable came to be. So,
if we make the program a little more complete, it could be something like this:

```php
<?php
$potentialInjection = $_GET['forgotToQuote'];
mysql_query("SELECT * FROM table1 WHERE a = '" . $potentialInjection . "'");

$noInjection = mysql_real_escape_string($_GET['help']);
mysql_query("SELECT * FROM table2 WHERE a = b = '" . $noInjection . "'");
```

Depending on your selection of dangerous methods, the output will look something like this:

```
WARN  ResultCollector - Unmitigated risks for method: mysql_query = [sqlInjection], at Location[hello.php:3], via expression: $_GET["forgotToQuote"]
```


So in that code, it doesn't just notice the $potentialInjection may be dangerous, it will also point to the most dangerous
 expression it saw assigned to $potentialInjection.

By using Quercus' parser, it can actually determine fairly complicated structures. It will be able to follow trinary-statements, if/else-structures and many more.
For the morecomplicated_hello.php, it will actually output something like this:

```
WARN  ResultCollector - Unmitigated risks for method: mysql_query = [sqlInjection], at Location[morecomplicated_hello.php:16], via expression: $_GET["forgotToQuote"]
```


## USAGE

Normally, it should be simply a matter of calling maven, to download resources and create packages:
mvn clean dependency:copy-dependencies package

Note: since Caucho has no Maven repository for the latest Resin or Quercus (4.0.36 or higher), you'll have to download that yourself:
[http://www.caucho.com/download/](http://www.caucho.com/download/)

Just download the resin open source servlet container, extract the tarball or zipfile and find the resin.jar in it. Adjust the pom.xml-file so it has a absolute path to
 your resin.jar.

After that you can just use the above mvn-command.

Maven will create a 'target'-directory, download the dependencies and copy the various jar-files to target/dependencies.
A jar-file for this program is created in the target-directory as well.

After that its just a matter of running your java:

```
java -cp target/*:target/dependency/*  org.arjenm.phpsecurity.Scanner ./morecomplicated_hello.php
```

Or to see all the commandline options, just do something like this:

```
java -cp target/*:target/dependency/*  org.arjenm.phpsecurity.Scanner
```

The risks to ignore (and for usage in  can be found in Risk.java, but currently are:
sqlInjection, crossSiteScripting, programExecution

You can adjust the selection of dangerous and mitigating methods by simply changing dangerousMethods.properties or mitigatingMethods.properties in the src/main/resources-directory.

## DRAWBACKS AND LIMITATIONS

The parser/scanner is not perfect. Here are a few issues of note:
- It has a very simple way of matching methods found in code to the methods from the two properties files. I.e. if you have a 'query' method that does something different than 'mysqli::query', it may give false positives
- It only supports PHP-code that Quercus can read, although it is actually fairly robust there may always be files that don't work. At the moment Quercus supports PHP 5.4.
- It treats most functions as unsafe, it has no support to store security-outcomes of return-statements from functions and classes for later use (it does scan the function and method bodies).
- It has limited variable support, i.e. it only understands the basic variables. Everything else, including class-parameters and array-fields are treated as unsafe.
- It is just a static analyzer, as all such tools... they will not find all problems and should only be used as a part of your toolset.
- Especially for HTML-construction, you commonly use bits and pieces. It will only warn on the final echo-statements, rather than all the lines that may cause problems.

## TIP

If you run the application from within your IDE (like IntelliJ IDEA or Eclipse), the output will most likely be converted to have links to your files, so its just a matter of a mouseclick to go to each dangerous construction.

## LICENSE
Since Quercus is licensed under GPLv2, this automatically is GPLv2 as well.
