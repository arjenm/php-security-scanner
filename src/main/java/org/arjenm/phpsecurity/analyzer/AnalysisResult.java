
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

import com.caucho.quercus.expr.Expr;

import java.util.EnumSet;
import java.util.Set;

/**
 * Risks found and/or removed from the given path.
 *
 * @author Arjen
 */
public class AnalysisResult
{
	public static AnalysisResult noRisks(Expr expression)
	{
		return new AnalysisResult(EnumSet.noneOf(Risk.class), expression);
	}

	public static AnalysisResult unsafeInput(Expr expression)
	{
		return new AnalysisResult(EnumSet.allOf(Risk.class), expression);
	}

	/**
	 *
	 * @param first The base result.
	 * @param second An additional result, if its null, its ignored.
	 * @return The merged analysis result.
	 */
	public static AnalysisResult merge(AnalysisResult first, AnalysisResult second)
	{
		AnalysisResult merged = new AnalysisResult(EnumSet.copyOf(first.getPotentialRisks()), first.getExpression());

		if(second != null)
		{
			merged.addRisks(second.getPotentialRisks());

			// See if the other expression is 'better' as a replacement for the one already stored.
			if(second.getExpression() != null &&
					(second.getPotentialRisks().size() > first.getPotentialRisks().size() || merged.getExpression() == null)
				)
			{
				merged.setExpression(second.getExpression());
			}
		}

		return merged;
	}

	private Set<Risk> potentialRisks;
	private Expr expression;

	/**
	 * Create a new AnalysisResult.
	 *
	 * @param potentialRisks The risks that may be involved.
	 * @param expression The expression that caused (some of) those risks.
	 */
	private AnalysisResult(Set<Risk> potentialRisks, Expr expression)
	{
		this.potentialRisks = potentialRisks;
		this.expression = expression;
	}

	public void addRisks(Set<Risk> additionalRisks)
	{
		potentialRisks.addAll(additionalRisks);
	}

	public void removeRisks(Set<Risk> mitigatedRisks)
	{
		potentialRisks.removeAll(mitigatedRisks);
	}

	public Set<Risk> getPotentialRisks()
	{
		return potentialRisks;
	}

	public Expr getExpression()
	{
		return expression;
	}

	public void setExpression(Expr expression)
	{
		this.expression = expression;
	}
}
