/*
 * Copyright (c) Arjen van der Meijden -- all rights reserved
 *
 * This file is part of a open source work.
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * This work is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this work; if not, feel free to download it from:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * @author Arjen van der Meijden
 */

package org.arjenm.phpsecurity;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.caucho.quercus.Quercus;
import com.caucho.quercus.page.InterpretedPage;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.parser.QuercusParseException;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.vfs.FilePath;
import org.arjenm.phpsecurity.analyzer.MethodInformation;
import org.arjenm.phpsecurity.analyzer.ProgramAnalyzer;
import org.arjenm.phpsecurity.analyzer.ResultCollector;
import org.arjenm.phpsecurity.analyzer.Risk;
import org.arjenm.phpsecurity.cli.ScannerCliParameters;
import org.arjenm.phpsecurity.quercus.ParseTreeAccessQuercus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Base entry point for the PHP-security scanner.
 *
 * @author Arjen
 */
public class Scanner
{
	private static final Logger log = LoggerFactory.getLogger(Scanner.class);

	public static void main(String[] argv) throws IOException, NoSuchFieldException, IllegalAccessException
	{
		// Load the command line parameters
		ScannerCliParameters parameters = new ScannerCliParameters();
		JCommander jCommander = new JCommander(parameters);
		try
		{
			jCommander.parse(argv);
		}
		catch(ParameterException pe)
		{
			// Force help-output upon failure
			log.warn("Invalid parameters", pe);
			parameters.setHelp(true);
		}

		if(parameters.isHelp())
		{
			jCommander.usage();
			return;
		}

		// See which risks the user is (not) interested in
		Set<Risk> interestedRisks = EnumSet.allOf(Risk.class);

		if(parameters.getIgnoredRisks() != null)
			interestedRisks.removeAll(parameters.getIgnoredRisks());

		if(interestedRisks.isEmpty())
		{
			log.warn("No risks to analyze, bailing out");
			return;
		}

		// And actually start the scanning
		Pattern fileNamePattern = Pattern.compile(parameters.getFileNamePattern());
		MethodInformation methodInformation = loadMethodInformation();
		ResultCollector resultCollector = new ResultCollector(interestedRisks, methodInformation);

		Scanner scanner = new Scanner(fileNamePattern, resultCollector);

		log.info("Scanning " + parameters.getPaths() + " for risks: " + interestedRisks);

		for(String pathName : parameters.getPaths())
		{
			FilePath filePath = new FilePath(pathName);
			scanner.analyzeFile(filePath);
		}
	}

	private static MethodInformation loadMethodInformation() throws IOException
	{
		Properties dangerousMethods = new Properties();
		dangerousMethods.load(Scanner.class.getResourceAsStream("/dangerousMethods.properties"));

		Properties mitigatingMethods = new Properties();
		mitigatingMethods.load(Scanner.class.getResourceAsStream("/mitigatingMethods.properties"));

		return MethodInformation.create(dangerousMethods, mitigatingMethods);
	}

	private Pattern fileNamePattern;
	private ResultCollector resultCollector;

	public Scanner(Pattern fileNamePattern, ResultCollector resultCollector)
	{
		this.fileNamePattern = fileNamePattern;
		this.resultCollector = resultCollector;
	}

	/**
	 * Scan the given directory recursively.
	 *
	 * @param directoryPath The directory's path.
	 *
	 * @throws IOException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	public void analyzeDirectory(FilePath directoryPath) throws IOException, NoSuchFieldException, IllegalAccessException
	{
		String[] files = directoryPath.list();

		String dirName = directoryPath.getFullPath();

		for(String fileName : files)
		{
			analyzeFile(new FilePath(dirName + "/" + fileName));
		}
	}

	/**
	 * Scan the given file.
	 *
	 * If it turns out to be a file, forward it to {@link #analyzeDirectory(com.caucho.vfs.FilePath)}.
	 *
	 * @param filePath The file's path.
	 *
	 * @throws IOException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	public void analyzeFile(FilePath filePath) throws IOException, NoSuchFieldException, IllegalAccessException
	{
		// Recurse
		if(filePath.isDirectory())
		{
			analyzeDirectory(filePath);
			return;
		}

		if(!fileNamePattern.matcher(filePath.toString()).matches())
		{
			log.debug("Skipping file: " + filePath);
			return;
		}

		log.debug("Analyzing file: " + filePath);

		// Start a new parser for this file
		Quercus quercus = new ParseTreeAccessQuercus();

		quercus.init();
		quercus.start();

		QuercusPage page;
		try
		{
			page = quercus.parse(filePath);
		}
		catch(QuercusParseException parseException)
		{
			log.warn("Couldn't parse file: " + filePath, parseException);
			// Skip it
			return;
		}

		if(!(page instanceof InterpretedPage))
			throw new RuntimeException("Impossible to continue with parsed page of type: " + page.getClass());

		// Load the internal AST/program-representation
		Field programField = InterpretedPage.class.getDeclaredField("_program");
		programField.setAccessible(true);

		QuercusProgram program = (QuercusProgram) programField.get(page);

		// And actually analyze that program-representation for potential problems
		ProgramAnalyzer programAnalyzer = new ProgramAnalyzer(resultCollector);
		programAnalyzer.analyzeProgram(program);

		// Make sure we close the quercus instance, so it can close its threads and clean up its data-structures
		quercus.close();
	}
}
