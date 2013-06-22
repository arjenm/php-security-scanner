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

package org.arjenm.phpsecurity.analyzer.declaration;

import com.caucho.quercus.Location;
import org.arjenm.phpsecurity.quercus.LocationHelper;

/**
 * Container for a declaration of "something" (i.e. method, function, class, constant).
 *
 * @author Arjen
 */
public class Declaration
{
	private DeclarationType declarationType;
	private Location declarationLocation;
	private String declarationName;

	Declaration(DeclarationType declarationType, Location declarationLocation, String declarationName)
	{
		this.declarationType = declarationType;
		this.declarationLocation = declarationLocation;
		this.declarationName = declarationName;
	}

	public DeclarationType getDeclarationType()
	{
		return declarationType;
	}

	public Location getDeclarationLocation()
	{
		return declarationLocation;
	}

	public void setDeclarationLocation(Location declarationLocation)
	{
		this.declarationLocation = declarationLocation;
	}

	public String getDeclarationName()
	{
		return declarationName;
	}

	public void setDeclarationName(String declarationName)
	{
		this.declarationName = declarationName;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Declaration that = (Declaration) o;

		if (! declarationType.equals(that.declarationType)) return false;
		if (! LocationHelper.equals(declarationLocation, that.declarationLocation)) return false;
		if (!declarationName.equals(that.declarationName)) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		int result = declarationType.hashCode();
		result = 31 * result + LocationHelper.hashCode(declarationLocation);
		result = 31 * result + declarationName.hashCode();
		return result;
	}

	@Override
	public String toString()
	{
		return declarationName + " at " + declarationLocation;
	}
}
