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

import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.program.MethodDeclaration;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.quercus.statement.Statement;
import org.arjenm.phpsecurity.quercus.program.PTAFunction;
import org.arjenm.phpsecurity.quercus.program.PTAObjectMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class ProgramAnalyzer
{
	private static final Logger log = LoggerFactory.getLogger(ProgramAnalyzer.class);

	private ResultCollector resultCollector;

	public ProgramAnalyzer(ResultCollector resultCollector)
	{
		this.resultCollector = resultCollector;
	}

	public void analyzeProgram(QuercusProgram program)
	{
		Statement programStatement = program.getStatement();

		// Will also analyze the classes and functions when it hits the corresponding ClassDefStatement and FunctionDefStatement
		analyzeStatement(programStatement);
	}

	public void analyzeClass(InterpretedClassDef classDef)
	{
		Set<Map.Entry<StringValue, AbstractFunction>> functionSet = classDef.functionSet();

		for(Map.Entry<StringValue, AbstractFunction> functionEntry : functionSet)
		{
			AbstractFunction function = functionEntry.getValue();
			analyzeFunction(function);
		}
	}

	public void analyzeFunction(AbstractFunction function)
	{
		// Start new scopes for function analysis
		if(function instanceof PTAFunction)
		{
			analyzeStatement(((PTAFunction) function).getStatement());
			return;
		}
		else if(function instanceof PTAObjectMethod)
		{
			analyzeStatement(((PTAObjectMethod) function).getStatement());
			return;
		}
		else if(function instanceof MethodDeclaration)
		{
			// abstract declaration, so no statement to analyze
			return;
		}

		log.warn("Unable to analyze function: " + function + " of class " + function.getClass());
	}

	private void analyzeStatement(Statement programStatement)
	{
		StatementAnalyzer statementAnalyzer = new StatementAnalyzer(resultCollector, this);
		statementAnalyzer.analyzeStatement(programStatement);
	}
}
