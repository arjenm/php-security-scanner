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

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.statement.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

public class ResultCollector
{
	private static final Logger log = LoggerFactory.getLogger(ResultCollector.class);

	private final Set<Risk> interestedRisks;
	private final MethodInformation methodInformation;

	public ResultCollector(Set<Risk> interestedRisks, MethodInformation methodInformation)
	{
		this.interestedRisks = interestedRisks;
		this.methodInformation = methodInformation;
	}

	public MethodInformation getMethodInformation()
	{
		return methodInformation;
	}

	public void collectResult(Expr expression, String functionName, AnalysisResult result)
	{
		logAnalysis(functionName, expression.getLocation(), result);
	}

	public void collectResult(Statement statement, String functionName, AnalysisResult result)
	{
		logAnalysis(functionName, statement.getLocation(), result);
	}

	private void logAnalysis(String functionName, Location location, AnalysisResult result)
	{
		// See if there are any dangerous risks we need to know of
		Set<Risk> sensitiveRisks = methodInformation.getDangersForMethod(functionName);

		if(sensitiveRisks == null)
		{
			log.debug("No risk information known for method: " + functionName + ", at " + location);
			return;
		}

		Set<Risk> overlap = EnumSet.copyOf(result.getPotentialRisks());
		// Only report on the risks we care about
		overlap.retainAll(interestedRisks);
		// And only for the risks actually valid for the given method
		overlap.retainAll(sensitiveRisks);

		if(overlap.isEmpty())
		{
			log.debug("All risks for method mitigated: " + functionName + ", at " + location);
			return;
		}

		log.warn("Unmitigated risks for method: " + functionName + " = " + overlap + ", at " + location + ", via expression: " + result.getExpression());
	}
}
