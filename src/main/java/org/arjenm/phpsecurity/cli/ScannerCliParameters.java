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

package org.arjenm.phpsecurity.cli;

import com.beust.jcommander.Parameter;
import org.arjenm.phpsecurity.analyzer.Risk;

import java.io.File;
import java.util.List;

/**
 * JCommander-compatible CLI-configuration for the {@link org.arjenm.phpsecurity.Scanner}.
 *
 * @author Arjen
 */
public class ScannerCliParameters
{
	@Parameter(names = {"-h", "--help"}, description = "This output", help = true)
	private boolean help;

	@Parameter(names = {"-f", "--filenamepattern"}, description = "The regular expression to use for testing which file-types should be included")
	private String fileNamePattern = ".*\\.(php|dsp|inc|php3|php4)$";

	@Parameter(names = {"-i", "--ignorerisk"}, description = "Select the risks that should be ignored", converter = CliRiskConverter.class)
	private List<Risk> ignoredRisks;

	@Parameter(names = {"-d", "--dangerousmethods"}, description = "The properties-filename with the dangerous methods. If not specified, the internal default file is used.",
			converter = FileConverter.class)
	private File dangerousMethodsPropertiesFile;

	@Parameter(names = {"-m", "--mitigatingmethods"}, description = "The properties-filename with the mitigating methods. If not specified, the internal default file is used.",
			converter = FileConverter.class)
	private File mitigatingMethodsPropertiesFile;

	@Parameter(description = "[One or more paths to files or directories to scan]", required = true)
	private List<String> paths;

	public boolean isHelp()
	{
		return help;
	}

	public void setHelp(boolean help)
	{
		this.help = help;
	}

	public List<Risk> getIgnoredRisks()
	{
		return ignoredRisks;
	}

	public void setIgnoredRisks(List<Risk> ignoredRisks)
	{
		this.ignoredRisks = ignoredRisks;
	}

	public File getDangerousMethodsPropertiesFile()
	{
		return dangerousMethodsPropertiesFile;
	}

	public void setDangerousMethodsPropertiesFile(File dangerousMethodsPropertiesFile)
	{
		this.dangerousMethodsPropertiesFile = dangerousMethodsPropertiesFile;
	}

	public File getMitigatingMethodsPropertiesFile()
	{
		return mitigatingMethodsPropertiesFile;
	}

	public void setMitigatingMethodsPropertiesFile(File mitigatingMethodsPropertiesFile)
	{
		this.mitigatingMethodsPropertiesFile = mitigatingMethodsPropertiesFile;
	}

	public String getFileNamePattern()
	{
		return fileNamePattern;
	}

	public void setFileNamePattern(String fileNamePattern)
	{
		this.fileNamePattern = fileNamePattern;
	}

	public List<String> getPaths()
	{
		return paths;
	}

	public void setPaths(List<String> paths)
	{
		this.paths = paths;
	}
}
