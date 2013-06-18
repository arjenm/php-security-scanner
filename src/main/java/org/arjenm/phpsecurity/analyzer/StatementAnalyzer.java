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
import com.caucho.quercus.statement.*;
import org.arjenm.phpsecurity.quercus.statement.*;

import java.util.List;

/**
 * Analyzer that understands {@link com.caucho.quercus.statement.Statement Statements} and dives deeper into the details of those.
 *
 * @author Arjen
 */
public class StatementAnalyzer
{
	public static final String FUNCTION_ECHO = "echo";
	private ResultCollector resultCollector;
	private ProgramAnalyzer programAnalyzer;
	private ExpressionAnalyzer expressionAnalyzer;

	public StatementAnalyzer(ResultCollector resultCollector, ProgramAnalyzer programAnalyzer)
	{
		this.resultCollector = resultCollector;
		this.programAnalyzer = programAnalyzer;

		this.expressionAnalyzer = new ExpressionAnalyzer(resultCollector, programAnalyzer);
	}

	public AnalysisResult analyzeStatement(Statement statement)
	{
		if(statement instanceof BlockStatement)
			return analyzeBlockStatement((BlockStatement) statement);
		else if(statement instanceof ExprStatement)
			return analyzeExprStatement((ExprStatement) statement);
		else if(statement instanceof PTAEchoStatement)
			return analyzeEchoStatement((PTAEchoStatement) statement);
		else if(statement instanceof PTAIfStatement)
			return analyzeIfStatement((PTAIfStatement) statement);
		else if(statement instanceof PTAWhileStatement)
			return analyzeWhileStatement((PTAWhileStatement) statement);
		else if(statement instanceof PTADoStatement)
			return analyzeDoStatement((PTADoStatement) statement);
		else if(statement instanceof PTAForeachStatement)
			return analyzeForeachStatement((PTAForeachStatement) statement);
		else if(statement instanceof PTAForStatement)
			return analyzeForStatement((PTAForStatement) statement);
		else if(statement instanceof PTASwitchStatement)
			return analyzeSwitchStatement((PTASwitchStatement) statement);
		else if(statement instanceof PTABreakStatement)
			return analyzeBreakStatement((PTABreakStatement) statement);
		else if(statement instanceof PTAContinueStatement)
			return analyzeContinueStatement((PTAContinueStatement) statement);
		else if(statement instanceof TextStatement)
			return analyzeTextStatement((TextStatement) statement);
		else if(statement instanceof NullStatement)
			return analyzeNullStatement((NullStatement) statement);
		else if(statement instanceof GlobalStatement)
			return analyzeGlobalStatement((GlobalStatement)statement);
		else if(statement instanceof PTAReturnStatement)
			return analyzeReturnStatement((PTAReturnStatement) statement);
		else if(statement instanceof PTAReturnRefStatement)
			return analyzeReturnRefStatement((PTAReturnRefStatement) statement);
		else if(statement instanceof PTAClassDefStatement)
			return analyzeClassDefStatement((PTAClassDefStatement) statement);
		else if(statement instanceof PTAFunctionDefStatement)
			return analyzeFunctionDefStatement((PTAFunctionDefStatement) statement);
		else if(statement instanceof PTAThrowStatement)
			return analyzeThrowStatement((PTAThrowStatement) statement);
		else if(statement instanceof PTATryStatement)
			return analyzeTryStatement((PTATryStatement) statement);
		else if(statement instanceof PTAClassStaticStatement)
			return analyzeClassStaticStatement((PTAClassStaticStatement) statement);
		else if(statement instanceof PTAStaticStatement)
			return analyzeStaticStatement((PTAStaticStatement) statement);

		throw new IllegalArgumentException("Unknown statement type: " + statement.getClass() + ", on " + statement.getLocation());
	}

	protected AnalysisResult analyzeClassDefStatement(PTAClassDefStatement classDefStatement)
	{
		programAnalyzer.analyzeClass(classDefStatement.getClassDefinition());

		// Class-declarations are fairly risk-free
		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeFunctionDefStatement(PTAFunctionDefStatement functionDefStatement)
	{
		programAnalyzer.analyzeFunction(functionDefStatement.getFunctionDefition());

		// Function-declarations are fairly risk-free
		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeClassStaticStatement(PTAClassStaticStatement classStaticStatement)
	{
		// Static class variable declaration/initialization
		// TODO: Mark the variable's outcome?
		Expr initValue = classStaticStatement.getInitValue();

		if(initValue != null)
			expressionAnalyzer.analyzeExpression(initValue);

		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeStaticStatement(PTAStaticStatement staticStatement)
	{
		// Static variable declaration/initialization (in function bodies)
		// TODO: Mark the variable's outcome?
		Expr initValue = staticStatement.getInitValue();
		if(initValue != null)
			expressionAnalyzer.analyzeExpression(initValue);

		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeBlockStatement(BlockStatement blockStatement)
	{
		Statement[] statements = blockStatement.getStatements();

		for(Statement subStatement : statements)
		{
			// Accumulate the most dangerous blocks' info
			analyzeStatement(subStatement);
		}

		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeExprStatement(ExprStatement exprStatement)
	{
		return expressionAnalyzer.analyzeExpression(exprStatement.getExpr());
	}

	protected AnalysisResult analyzeEchoStatement(PTAEchoStatement echoStatement)
	{
		AnalysisResult result = expressionAnalyzer.analyzeExpression(echoStatement.getExpr());

		resultCollector.collectResult(echoStatement, FUNCTION_ECHO, result);

		return result;
	}

	protected AnalysisResult analyzeIfStatement(PTAIfStatement ifStatement)
	{
		expressionAnalyzer.analyzeExpression(ifStatement.getTest());
		analyzeStatement(ifStatement.getTrueBlock());

		if(ifStatement.getFalseBlock() != null)
			analyzeStatement(ifStatement.getFalseBlock());

		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeWhileStatement(PTAWhileStatement whileStatement)
	{
		expressionAnalyzer.analyzeExpression(whileStatement.getTest());
		analyzeStatement(whileStatement.getBlock());

		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeDoStatement(PTADoStatement doStatement)
	{
		expressionAnalyzer.analyzeExpression(doStatement.getTest());
		analyzeStatement(doStatement.getBlock());

		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeForeachStatement(PTAForeachStatement foreachStatement)
	{
		expressionAnalyzer.analyzeExpression(foreachStatement.getObjExpr());
		analyzeStatement(foreachStatement.getBlock());

		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeForStatement(PTAForStatement forStatement)
	{
		if(forStatement.getInit() != null)
			expressionAnalyzer.analyzeExpression(forStatement.getInit());

		if(forStatement.getTest() != null)
			expressionAnalyzer.analyzeExpression(forStatement.getTest());

		if(forStatement.getIncr() != null)
			expressionAnalyzer.analyzeExpression(forStatement.getIncr());

		analyzeStatement(forStatement.getBlock());

		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeSwitchStatement(PTASwitchStatement switchStatement)
	{
		for(Statement caseStatement : switchStatement.getBlocks())
		{
			analyzeStatement(caseStatement);
		}

		if(switchStatement.getDefaultblock() != null)
			analyzeStatement(switchStatement.getDefaultblock());

		for(Expr[] caseExpressionLists : switchStatement.getCases())
		{
			for(Expr caseExpression : caseExpressionLists)
			{
				expressionAnalyzer.analyzeExpression(caseExpression);
			}
		}

		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeBreakStatement(PTABreakStatement breakStatement)
	{
		if(breakStatement.getTarget() != null)
			expressionAnalyzer.analyzeExpression(breakStatement.getTarget());

		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeContinueStatement(PTAContinueStatement continueStatement)
	{
		if(continueStatement.getTarget() != null)
			expressionAnalyzer.analyzeExpression(continueStatement.getTarget());

		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeReturnStatement(PTAReturnStatement returnStatement)
	{
		if(returnStatement.getExpr() != null)
			return expressionAnalyzer.analyzeExpression(returnStatement.getExpr());

		// Empty return just yields null, so that's ok
		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeReturnRefStatement(PTAReturnRefStatement returnRefStatement)
	{
		if(returnRefStatement.getExpr() != null)
			return expressionAnalyzer.analyzeExpression(returnRefStatement.getExpr());

		// Empty return just yields null, so that's ok
		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeThrowStatement(PTAThrowStatement throwStatement)
	{
		expressionAnalyzer.analyzeExpression(throwStatement.getExpr());

		// Throw stops the flow, so is safe for that particular point
		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeTryStatement(PTATryStatement tryStatement)
	{
		analyzeStatement(tryStatement.getBlock());

		List<TryStatement.Catch> catchList = tryStatement.getCatchList();
		for(TryStatement.Catch catchBlock : catchList)
		{
			analyzeStatement(catchBlock.getBlock());
		}

		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeTextStatement(TextStatement textStatement)
	{
		// Probably trustworthy literal text(echo) statement
		// TODO: is it?
		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeNullStatement(NullStatement nullStatement)
	{
		// Probably trustworthy null ;)
		return AnalysisResult.noRisks(null);
	}

	protected AnalysisResult analyzeGlobalStatement(GlobalStatement globalStatement)
	{
		// While global is generally a bad idea, its not unsafe on itself. The unknown variables will be marked independently anyway
		// TODO: Add warning about global?
		return AnalysisResult.noRisks(null);
	}
}
