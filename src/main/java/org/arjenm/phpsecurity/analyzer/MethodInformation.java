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

package org.arjenm.phpsecurity.analyzer;

import java.util.*;

/**
 * Container for information about methods with certain risk sensitivenesses or mitigations.
 *
 * @author Arjen
 */
public class MethodInformation
{
	public static MethodInformation create(Properties dangerousMethods, Properties mitigatingMethods)
	{
		Map<String, Set<Risk>> dangerousMethodMap = parseProperties(dangerousMethods);
		Map<String, Set<Risk>> mitigatingMethodMap = parseProperties(mitigatingMethods);

		return new MethodInformation(dangerousMethodMap, mitigatingMethodMap);
	}

	private static Map<String, Set<Risk>> parseProperties(Properties methodProperties)
	{
		Map<String, Set<Risk>> methodInformation = new HashMap<>();

		for(Map.Entry<Object, Object> entry : methodProperties.entrySet())
		{
			String methodName = (String) entry.getKey();
			Set<Risk> risks = parseRisks((String) entry.getValue());

			methodInformation.put(methodName, risks);
		}

		return methodInformation;
	}

	/**
	 * Parse a string of one or more risks into a Set of Risks.
	 *
	 * @param riskLine The line with comma-separated values.
	 * @return The risks.
	 */
	private static Set<Risk> parseRisks(String riskLine)
	{
		String[] elements =  riskLine.split("\\s*,\\s*");

		Set<Risk> risks = EnumSet.noneOf(Risk.class);
		for(String element : elements)
		{
			risks.add(Risk.valueOf(element));
		}

		return risks;
	}

	// Functions that need extra care
	private final Map<String, Set<Risk>> dangerousMethods;
	private final Map<String, Set<Risk>> mitigatingMethods;

	private MethodInformation(Map<String, Set<Risk>> dangerousMethods, Map<String, Set<Risk>> mitigatingMethods)
	{
		this.dangerousMethods = dangerousMethods;
		this.mitigatingMethods = mitigatingMethods;
	}

	public Set<Risk> getDangersForMethod(String methodName)
	{
		return dangerousMethods.get(methodName);
	}

	public Set<Risk> getMitigationsForMethod(String methodName)
	{
		return mitigatingMethods.get(methodName);
	}
}
