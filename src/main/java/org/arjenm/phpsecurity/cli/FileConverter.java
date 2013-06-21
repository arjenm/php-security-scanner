package org.arjenm.phpsecurity.cli;

import com.beust.jcommander.IStringConverter;

import java.io.File;

/**
 * Convert input-strings to files.
 *
 * @author Arjen
 */
public class FileConverter implements IStringConverter<File>
{
	@Override
	public File convert(String s)
	{
		File returnFile = new File(s);

		if(!returnFile.exists())
			throw new IllegalArgumentException("File does not exist: " + returnFile.getAbsolutePath());

		return returnFile;
	}
}
