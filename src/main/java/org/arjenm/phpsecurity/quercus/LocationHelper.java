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

package org.arjenm.phpsecurity.quercus;

import com.caucho.quercus.Location;

/**
 * Utility class that helps around with the fact that the {@link com.caucho.quercus.Location} class
 * has no equals and no hashCode.
 *
 * @author Arjen
 */
public class LocationHelper
{
	/**
	 * Test whether locations a and b are equal to eachother.
	 *
	 * I.e. treat the code as it a had an actual useful equals-implementation and return that outcome.
	 *
	 * @param a The first location.
	 * @param b The second location.
	 * @return True if they are equal.
	 */
	public static boolean equals(Location a, Location b)
	{
		// This code is adapted auto-generated equals, so might be a tad unreadable
		if (a == b)
			return true;
		if (a == null || b == null || a.getClass() != b.getClass())
			return false;

		if (a.getLineNumber() != b.getLineNumber())
			return false;

		if (a.getClassName() != null ? !a.getClassName().equals(b.getClassName()) : b.getClassName() != null)
			return false;

		if (a.getFileName() != null ? !a.getFileName().equals(b.getFileName()) : b.getFileName() != null)
			return false;

		if (a.getFunctionName() != null ? !a.getFunctionName().equals(b.getFunctionName()) : b.getFunctionName() != null)
			return false;

		return true;
	}

	/**
	 * Generate a usable hashcode for the location.
	 *
	 * @param l The location.
	 * @return The hashcode.
	 */
	public static int hashCode(Location l)
	{
		// This code is adapted auto-generated hashCode, so might be a tad unreadable
		int result = l.getFileName() != null ? l.getFileName().hashCode() : 0;
		result = 31 * result + l.getLineNumber();
		result = 31 * result + (l.getClassName() != null ? l.getClassName().hashCode() : 0);
		result = 31 * result + (l.getFunctionName() != null ? l.getFunctionName().hashCode() : 0);
		return result;
	}
}
