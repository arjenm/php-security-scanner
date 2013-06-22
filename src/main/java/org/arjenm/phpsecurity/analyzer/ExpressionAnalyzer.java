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
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.expr.*;
import org.arjenm.phpsecurity.analyzer.declaration.DeclarationUsageCollector;
import org.arjenm.phpsecurity.quercus.expr.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Analyzer that understands {@link com.caucho.quercus.expr.Expr Expressions} and dives deeper into those.
 *
 * @author Arjen
 */
public class ExpressionAnalyzer
{
	private static final Logger log = LoggerFactory.getLogger(ExpressionAnalyzer.class);

	public static final String FUNCTION_EXIT = "exit";
	public static final String FUNCTION_DIE = "die";
	public static final String FUNCTION_INCLUDE = "include";
	public static final String FUNCTION_INCLUDE_ONCE = "include_once";

	private Map<String, AnalysisResult> variableResults = new HashMap<>();

	private ResultCollector resultCollector;
	private DeclarationUsageCollector declarationUsageCollector;
	private ProgramAnalyzer programAnalyzer;

	private Location lastKnownValidLocation;

	public ExpressionAnalyzer(ResultCollector resultCollector, DeclarationUsageCollector declarationUsageCollector, ProgramAnalyzer programAnalyzer)
	{
		this.resultCollector = resultCollector;
		this.declarationUsageCollector = declarationUsageCollector;
		this.programAnalyzer = programAnalyzer;
	}

	public AnalysisResult analyzeExpression(Expr expression)
	{
		// Keep track of the location in the file, some expressions may not know their own (like arguments of function-calls).
		if(!expression.getLocation().isUnknown())
			lastKnownValidLocation = expression.getLocation();

		// Unfortunately there are a few different types of expressions and this is about as good as any other method to dispatch the correct method :/
		if(expression instanceof PTABinaryAssignExpr)
			return analyzeBinaryAssignExpr((PTABinaryAssignExpr) expression);
		else if(expression instanceof PTABinaryAssignRefExpr)
			return analyzeBinaryAssignRefExpr((PTABinaryAssignRefExpr) expression);
		else if(expression instanceof PTAObjectMethodExpr)
			return analyzeObjectMethodExpr((PTAObjectMethodExpr) expression);
		else if(expression instanceof PTAObjectMethodVarExpr)
			return analyzeObjectMethodVarExpr((PTAObjectMethodVarExpr) expression);
		else if(expression instanceof BinaryAppendExpr)
			return analyzeBinaryAppendExpr((BinaryAppendExpr) expression);
		else if(expression instanceof LiteralStringExpr)
			return analyzeLiteralStringExpr((LiteralStringExpr) expression);
		else if(expression instanceof VarExpr)
			return analyzeVarExpr((VarExpr) expression);
		else if(expression instanceof ArrayGetExpr)
			return analyzeArrayGetExpr((ArrayGetExpr) expression);
		else if(expression instanceof PTAObjectNewExpr)
			return analyzeObjectNewExpr((PTAObjectNewExpr) expression);
		else if(expression instanceof ToLongExpr)
			return analyzeToLongExpr((ToLongExpr) expression);
		else if(expression instanceof ToBooleanExpr)
			return analyzeToBooleanExpr((ToBooleanExpr) expression);
		else if(expression instanceof ToDoubleExpr)
			return analyzeToDoubleExpr((ToDoubleExpr) expression);
		else if(expression instanceof ToArrayExpr)
			return analyzeToArrayExpr((ToArrayExpr) expression);
		else if(expression instanceof ToObjectExpr)
			return analyzeToObjectExpr((ToObjectExpr) expression);
		else if(expression instanceof ToStringExpr)
			return analyzeToStringExpr((ToStringExpr) expression);
		else if(expression instanceof LiteralExpr)
			return analyzeLiteralExpr((LiteralExpr) expression);
		else if(expression instanceof UnarySuppressErrorExpr)
			return analyzeUnarySuppressErrorExpr((UnarySuppressErrorExpr) expression);
		else if(expression instanceof FunIncludeOnceExpr)
			return analyzeFunIncludeOnceExpr((FunIncludeOnceExpr) expression);
		else if(expression instanceof FunIncludeExpr)
			return analyzeFunIncludeExpr((FunIncludeExpr) expression);
		else if(expression instanceof PTAClassMethodExpr)
			return analyzeClassMethodExpr((PTAClassMethodExpr) expression);
		else if(expression instanceof PTAClassConstructExpr)
			return analyzeClassConstructExpr((PTAClassConstructExpr) expression);
		else if(expression instanceof ConstExpr)
			return analyzeConstExpr((ConstExpr) expression);
		else if(expression instanceof PTAFunArrayExpr)
			return analyzeFunArrayExpr((PTAFunArrayExpr) expression);
		else if(expression instanceof PTACallExpr)
			return analyzeCallExpr((PTACallExpr) expression);
		else if(expression instanceof BinaryAddExpr)
			return analyzeBinaryAddExpr((BinaryAddExpr) expression);
		else if(expression instanceof AbstractBinaryExpr)
			return analyzeAbstractBinaryExpr((AbstractBinaryExpr)expression); // After BinaryAdd
		else if(expression instanceof FunIssetExpr)
			return analyzeFunIssetExpr((FunIssetExpr) expression);
		else if(expression instanceof FunGetCalledClassExpr)
			return analyzeFunGetCalledClassExpr((FunGetCalledClassExpr) expression);
		else if(expression instanceof FunGetClassExpr)
			return analyzeFunGetClassExpr((FunGetClassExpr) expression);
		else if(expression instanceof UnaryNotExpr)
			return analyzeUnaryNotExpr((UnaryNotExpr)expression);
		else if(expression instanceof UnaryPostIncrementExpr)
			return analyzeUnaryPostIncrementExpr((UnaryPostIncrementExpr) expression);
		else if(expression instanceof UnaryPreIncrementExpr)
			return analyzeUnaryPreIncrementExpr((UnaryPreIncrementExpr) expression);
		else if(expression instanceof FunCloneExpr)
			return analyzeFunCloneExpr((FunCloneExpr)expression);
		else if(expression instanceof UnaryMinusExpr)
			return analyzeUnaryMinusExpr((UnaryMinusExpr) expression);
		else if(expression instanceof UnaryPlusExpr)
			return analyzeUnaryPlusExpr((UnaryPlusExpr) expression);
		else if(expression instanceof UnaryRefExpr)
			return analyzeUnaryRefExpr((UnaryRefExpr) expression);
		else if(expression instanceof PTAClassConstExpr)
			return analyzeClassConstExpr((PTAClassConstExpr) expression);
		else if(expression instanceof PTAConditionalExpr)
			return analyzeConditionalExpr((PTAConditionalExpr) expression);
		else if(expression instanceof PTAConditionalShortExpr)
			return analyzeConditionalShortExpr((PTAConditionalShortExpr) expression);
		else if(expression instanceof PTAFunExitExpr)
			return analyzeFunExitExpr((PTAFunExitExpr) expression);
		else if(expression instanceof PTAFunDieExpr)
			return analyzeFunDieExpr((PTAFunDieExpr) expression);
		else if(expression instanceof PTABinaryAssignListExpr)
			return analyzeBinaryAssignListExpr((PTABinaryAssignListExpr) expression);
		else if(expression instanceof PTABinaryAssignListEachExpr)
			return analyzeBinaryAssignListEachExpr((PTABinaryAssignListEachExpr) expression);
		else if(expression instanceof PTAListHeadExpr)
			return analyzeListHeadExpr((PTAListHeadExpr) expression);
		else if(expression instanceof VarUnsetExpr)
			return analyzeVarUnsetExpr((VarUnsetExpr) expression);
		else if(expression instanceof PTABinaryCharAtExpr)
			return analyzeBinaryCharAtExpr((PTABinaryCharAtExpr) expression);
		else if(expression instanceof LiteralNullExpr)
			return analyzeLiteralNullExpr((LiteralNullExpr) expression);
		else if(expression instanceof PTAThisMethodExpr)
			return analyzeThisMethodExpr((PTAThisMethodExpr) expression);
		else if(expression instanceof PTAThisMethodVarExpr)
			return analyzeThisMethodVarExpr((PTAThisMethodVarExpr) expression);
		else if(expression instanceof PTAThisFieldVarExpr)
			return analyzeThisFieldVarExpr((PTAThisFieldVarExpr) expression);
		else if(expression instanceof PTAThisFieldExpr)
			return analyzeThisFieldExpr((PTAThisFieldExpr) expression);
		else if(expression instanceof ThisExpr)
			return analyzeThisExpr((ThisExpr) expression);
		else if(expression instanceof ClassFieldExpr)
			return analyzeClassFieldExpr((ClassFieldExpr) expression);
		else if(expression instanceof ClassVirtualConstExpr)
			return analyzeClassVirtualConstExpr((ClassVirtualConstExpr) expression);
		else if(expression instanceof PTAObjectFieldExpr)
			return analyzeObjectFieldExpr((PTAObjectFieldExpr) expression);
		else if(expression instanceof PTAObjectFieldVarExpr)
			return analyzeObjectFieldVarExpr((PTAObjectFieldVarExpr) expression);
		else if(expression instanceof ClassVirtualFieldExpr)
			return analyzeClassVirtualFieldExpr((ClassVirtualFieldExpr) expression);
		else if(expression instanceof PTAClassFieldVarExpr)
			return analyzeClassFieldVarExpr((PTAClassFieldVarExpr) expression);
		else if(expression instanceof PTAClassVarFieldExpr)
			return analyzeClassVarFieldExpr((PTAClassVarFieldExpr) expression);
		else if(expression instanceof PTAObjectNewVarExpr)
			return analyzeObjectNewVarExpr((PTAObjectNewVarExpr) expression);
		else if(expression instanceof PTACallVarExpr)
			return analyzeCallVarExpr((PTACallVarExpr) expression);
		else if(expression instanceof PTAClassVirtualMethodExpr)
			return analyzeClassVirtualMethodExpr((PTAClassVirtualMethodExpr) expression);
		else if(expression instanceof PTAObjectNewStaticExpr)
			return analyzeObjectNewStaticExpr((PTAObjectNewStaticExpr) expression);
		else if(expression instanceof PTAClassVarMethodExpr)
			return analyzeClassVarMethodExpr((PTAClassVarMethodExpr) expression);
		else if(expression instanceof PTAClassVarConstExpr)
			return analyzeClassVarConstExpr((PTAClassVarConstExpr) expression);
		else if(expression instanceof BinaryInstanceOfExpr)
			return analyzeBinaryInstanceOfExpr((BinaryInstanceOfExpr) expression);
		else if(expression instanceof UnaryBitNotExpr)
			return analyzeUnaryBitNotExpr((UnaryBitNotExpr) expression);
		else if(expression instanceof ClosureExpr)
			return analyzeClosureExpr((ClosureExpr) expression);
		else if(expression instanceof ConstFileExpr)
			return analyzeConstFileExpr((ConstFileExpr) expression);
		else if(expression instanceof ConstDirExpr)
			return analyzeConstDirExpr((ConstDirExpr) expression);
		else if(expression instanceof VarVarExpr)
			return analyzeVarVarExpr((VarVarExpr) expression);
		else if(expression instanceof ClassMethodVarExpr)
			return analyzeClassMethodVarExpr((ClassMethodVarExpr) expression);
		else if(expression instanceof ClassVarMethodVarExpr)
			return analyzeClassVarMethodVarExpr((ClassVarMethodVarExpr) expression);
		else if(expression instanceof LiteralLongExpr)
			return analyzeLiteralLongExpr((LiteralLongExpr) expression);
		else if(expression instanceof ArrayTailExpr)
			return analyzeArrayTailExpr((ArrayTailExpr) expression);

		throw new IllegalArgumentException("Unknown expression type: " + expression.getClass() + ", at " + expression.getLocation());
	}

	private Location getUsefullLocation(Expr expression)
	{
		if(expression.getLocation().isUnknown())
			return lastKnownValidLocation;

		return expression.getLocation();
	}

	protected AnalysisResult analyzeVarVarExpr(VarVarExpr varVarExpr)
	{
		Expr expression = varVarExpr.getExpr();
		analyzeExpression(expression);

		log.warn("Found $$var-expression: " + varVarExpr + ", at " + getUsefullLocation(varVarExpr));

		// $$var's are so damn tricky, just don't trust 'm
		return AnalysisResult.unsafeInput(varVarExpr);
	}

	protected AnalysisResult analyzeConstDirExpr(ConstDirExpr constDirExpr)
	{
		// __DIR__ is generally safe
		return AnalysisResult.noRisks(constDirExpr);
	}

	protected AnalysisResult analyzeConstFileExpr(ConstFileExpr constFileExpr)
	{
		// __FILE__ is generally safe
		return AnalysisResult.noRisks(constFileExpr);
	}

	protected AnalysisResult analyzeClassVirtualConstExpr(ClassVirtualConstExpr classVirtualConstExpr)
	{
		declarationUsageCollector.addUsage(classVirtualConstExpr);

		// TODO: Analyze var?
		// class::constname stuff is generally a constant or similar
		return AnalysisResult.noRisks(classVirtualConstExpr);
	}

	protected AnalysisResult analyzeClassFieldExpr(ClassFieldExpr classFieldExpr)
	{
		// TODO: Analyze var?
		// self::$foo stuff is generally a constant or similar
		return AnalysisResult.noRisks(classFieldExpr);
	}

	protected AnalysisResult analyzeClassVirtualFieldExpr(ClassVirtualFieldExpr classVirtualFieldExpr)
	{
		// TODO: Analyze var?
		// static::$foo stuff is generally a constant or similar
		return AnalysisResult.noRisks(classVirtualFieldExpr);
	}

	protected AnalysisResult analyzeClassVarConstExpr(PTAClassVarConstExpr classVarConstExpr)
	{
		declarationUsageCollector.addUsage(classVarConstExpr);

		Expr name = classVarConstExpr.getClassName();
		analyzeExpression(name);

		// ClassName::const is generally not dangerous
		return AnalysisResult.noRisks(classVarConstExpr);
	}

	protected AnalysisResult analyzeClassFieldVarExpr(PTAClassFieldVarExpr classFieldVarExpr)
	{
		Expr name = classFieldVarExpr.getVarName();
		analyzeExpression(name);

		// className::$field is generally not very dangerous?
		return AnalysisResult.noRisks(classFieldVarExpr);
	}

	protected AnalysisResult analyzeClassVarFieldExpr(PTAClassVarFieldExpr classVarFieldExpr)
	{
		declarationUsageCollector.addUsage(classVarFieldExpr);

		Expr className = classVarFieldExpr.getClassName();
		analyzeExpression(className);

		// $className::field is generally not very dangerous?
		return AnalysisResult.noRisks(classVarFieldExpr);
	}

	protected AnalysisResult analyzeObjectFieldExpr(PTAObjectFieldExpr objectFieldExpr)
	{
		declarationUsageCollector.addUsage(objectFieldExpr);

		// TODO: Analyze variable content?
		Expr name = objectFieldExpr.getObjExpr();
		analyzeExpression(name);

		// $object->var may be dangerous
		return AnalysisResult.unsafeInput(objectFieldExpr);
	}

	protected AnalysisResult analyzeObjectFieldVarExpr(PTAObjectFieldVarExpr objectFieldVarExpr)
	{
		analyzeExpression(objectFieldVarExpr.getNameExpr());
		analyzeExpression(objectFieldVarExpr.getObjExpr());

		// $object->$var may be dangerous (its at least tricky enough)
		return AnalysisResult.unsafeInput(objectFieldVarExpr);
	}

	protected AnalysisResult analyzeThisExpr(ThisExpr thisExpr)
	{
		// $this
		return AnalysisResult.noRisks(thisExpr);
	}

	protected AnalysisResult analyzeLiteralNullExpr(LiteralNullExpr literalNullExpr)
	{
		// null
		return AnalysisResult.noRisks(literalNullExpr);
	}

	protected AnalysisResult analyzeVarUnsetExpr(VarUnsetExpr varUnsetExpr)
	{
		// TODO: Adjust the variable in the memory?
		return AnalysisResult.noRisks(varUnsetExpr);
	}

	protected AnalysisResult analyzeBinaryCharAtExpr(PTABinaryCharAtExpr binaryCharAtExpr)
	{
		analyzeExpression(binaryCharAtExpr.getObjExpr());
		analyzeExpression(binaryCharAtExpr.getIndexExpr());

		// Its just a single char... so should not have much danger? But it can be a '
		return AnalysisResult.unsafeInput(binaryCharAtExpr);
	}

	protected AnalysisResult analyzeFunExitExpr(PTAFunExitExpr funExitExpr)
	{
		if(funExitExpr.getValue() != null)
		{
			AnalysisResult result = analyzeExpression(funExitExpr.getValue());
			resultCollector.collectResult(funExitExpr, FUNCTION_EXIT, result);
		}

		// You can't really do anything with its result
		return AnalysisResult.noRisks(funExitExpr);
	}

	protected AnalysisResult analyzeFunDieExpr(PTAFunDieExpr funDieExpr)
	{
		if(funDieExpr.getValue() != null)
		{
			AnalysisResult result = analyzeExpression(funDieExpr.getValue());
			resultCollector.collectResult(funDieExpr, FUNCTION_DIE, result);
		}

		// You can't really do anything with its result
		return AnalysisResult.noRisks(funDieExpr);
	}

	protected AnalysisResult analyzeFunCloneExpr(FunCloneExpr funCloneExpr)
	{
		// A clone of something is as dangerous as its original
		AnalysisResult result = analyzeExpression(funCloneExpr.getExpr());
		result.setExpression(funCloneExpr);

		return result;
	}

	protected AnalysisResult analyzeUnaryMinusExpr(UnaryMinusExpr unaryMinusExpr)
	{
		analyzeExpression(unaryMinusExpr.getExpr());

		// -$x Turns it in a integer
		return AnalysisResult.noRisks(unaryMinusExpr);
	}

	protected AnalysisResult analyzeUnaryPlusExpr(UnaryPlusExpr unaryPlusExpr)
	{
		analyzeExpression(unaryPlusExpr.getExpr());

		// +$x Turns it in a integer
		return AnalysisResult.noRisks(unaryPlusExpr);
	}

	protected AnalysisResult analyzeUnaryRefExpr(UnaryRefExpr unaryRefExpr)
	{
		analyzeExpression(unaryRefExpr.getExpr());

		// &$x Is unpredictable at best, so don't trust it
		return AnalysisResult.unsafeInput(unaryRefExpr);
	}

	protected AnalysisResult analyzeUnaryNotExpr(UnaryNotExpr unaryNotExpr)
	{
		analyzeExpression(unaryNotExpr.getExpr());

		// !$x Turns it in a boolean
		return AnalysisResult.noRisks(unaryNotExpr);
	}

	protected AnalysisResult analyzeBinaryInstanceOfExpr(BinaryInstanceOfExpr binaryInstanceOfExpr)
	{
		declarationUsageCollector.addUsage(binaryInstanceOfExpr);

		analyzeExpression(binaryInstanceOfExpr.getExpr());

		// $a instanceof Class Turns it in a boolean
		return AnalysisResult.noRisks(binaryInstanceOfExpr);
	}

	protected AnalysisResult analyzeUnaryBitNotExpr(UnaryBitNotExpr unaryBitNotExpr)
	{
		analyzeExpression(unaryBitNotExpr.getExpr());

		// ^$a Turns it in a boolean/int
		return AnalysisResult.noRisks(unaryBitNotExpr);
	}

	protected AnalysisResult analyzeUnaryPostIncrementExpr(UnaryPostIncrementExpr unaryPostIncrementExpr)
	{
		analyzeExpression(unaryPostIncrementExpr.getExpr());

		// $a++ Turns it in a int
		return AnalysisResult.noRisks(unaryPostIncrementExpr);
	}

	protected AnalysisResult analyzeUnaryPreIncrementExpr(UnaryPreIncrementExpr unaryPreIncrementExpr)
	{
		analyzeExpression(unaryPreIncrementExpr.getExpr());

		// ++$a Turns it in a int
		return AnalysisResult.noRisks(unaryPreIncrementExpr);
	}

	protected AnalysisResult analyzeFunGetCalledClassExpr(FunGetCalledClassExpr funGetCalledClassExpr)
	{
		// get_called_class returns class name
		return AnalysisResult.noRisks(funGetCalledClassExpr);
	}

	protected AnalysisResult analyzeFunGetClassExpr(FunGetClassExpr funGetClassExpr)
	{
		// get_class() and __CLASS__ returns class name
		return AnalysisResult.noRisks(funGetClassExpr);
	}

	protected AnalysisResult analyzeFunIssetExpr(FunIssetExpr funIssetExpr)
	{
		analyzeExpression(funIssetExpr.getExpr());

		// isset returns boolean
		return AnalysisResult.noRisks(funIssetExpr);
	}

	protected AnalysisResult analyzeBinaryAddExpr(BinaryAddExpr binaryAddExpr)
	{
		AnalysisResult result = analyzeExpression(binaryAddExpr.getLeft());
		result = AnalysisResult.merge(result, analyzeExpression(binaryAddExpr.getRight()));

		// Add can be of two arrays, so its not always a safe number...
		return result;
	}

	protected AnalysisResult analyzeAbstractBinaryExpr(AbstractBinaryExpr binaryExpr)
	{
		// TODO: BinaryCommaExpr may be different, like Add

		analyzeExpression(binaryExpr.getLeft());
		analyzeExpression(binaryExpr.getRight());

		// All other abstractBinaryExpr's yields numbers and/or booleans
		return AnalysisResult.noRisks(binaryExpr);
	}

	protected AnalysisResult analyzeFunArrayExpr(PTAFunArrayExpr funArrayExpr)
	{
		for(Expr keyArgument : funArrayExpr.getKeys())
		{
			// Key's are optional
			if(keyArgument != null)
				analyzeExpression(keyArgument);
		}

		for(Expr valueArgument : funArrayExpr.getValues())
			analyzeExpression(valueArgument);

		// Array's shouldn't be trusted as input
		// TODO: unless all inputs are safe? But if this is changed, the binaryAssign should be changed as well...
		return AnalysisResult.unsafeInput(funArrayExpr);
	}

	protected AnalysisResult analyzeArrayTailExpr(ArrayTailExpr arrayTailExpr)
	{
		// an $array[] expression can be considered safe
		return AnalysisResult.noRisks(arrayTailExpr);
	}

	protected AnalysisResult analyzeConstExpr(ConstExpr constExpr)
	{
		declarationUsageCollector.addUsage(constExpr);

		// Constants are generally save to use
		return AnalysisResult.noRisks(constExpr);
	}

	protected AnalysisResult analyzeClassConstExpr(PTAClassConstExpr constExpr)
	{
		declarationUsageCollector.addUsage(constExpr);

		// Class-constants are generally save to use
		return AnalysisResult.noRisks(constExpr);
	}

	protected AnalysisResult analyzeLiteralExpr(LiteralExpr literalExpr)
	{
		// TODO: Are there literals that are dangerous?
		// Int, float, long, boolean literals... normally not dangerous
		return AnalysisResult.noRisks(literalExpr);
	}

	protected AnalysisResult analyzeUnarySuppressErrorExpr(UnarySuppressErrorExpr unarySuppressErrorExpr)
	{
		// Suppressing errors (@expression) doesn't make it safer
		return analyzeExpression(unarySuppressErrorExpr.getExpr());
	}

	protected AnalysisResult analyzeToLongExpr(ToLongExpr toLongExpr)
	{
		analyzeExpression(toLongExpr.getExpr());

		// Cast to int/long is normally a mitigating method
		return AnalysisResult.noRisks(toLongExpr);
	}

	protected AnalysisResult analyzeToDoubleExpr(ToDoubleExpr toDoubleExpr)
	{
		analyzeExpression(toDoubleExpr.getExpr());

		// Cast to double is normally a mitigating method
		return AnalysisResult.noRisks(toDoubleExpr);
	}

	protected AnalysisResult analyzeToBooleanExpr(ToBooleanExpr toBooleanExpr)
	{
		analyzeExpression(toBooleanExpr.getExpr());

		// Cast to boolean is normally a mitigating method
		return AnalysisResult.noRisks(toBooleanExpr);
	}

	protected AnalysisResult analyzeToArrayExpr(ToArrayExpr toArrayExpr)
	{
		// Cast to array doesn't make it any safer
		AnalysisResult result = analyzeExpression(toArrayExpr.getExpr());
		result.setExpression(toArrayExpr);

		return result;
	}

	protected AnalysisResult analyzeToObjectExpr(ToObjectExpr toObjectExpr)
	{
		// Cast to object doesn't make it any safer
		AnalysisResult result = analyzeExpression(toObjectExpr.getExpr());
		result.setExpression(toObjectExpr);

		return result;
	}

	protected AnalysisResult analyzeToStringExpr(ToStringExpr toStringExpr)
	{
		// Cast to string doesn't make it any safer
		AnalysisResult result = analyzeExpression(toStringExpr.getExpr());
		result.setExpression(toStringExpr);

		return result;
	}

	protected AnalysisResult analyzeObjectNewExpr(PTAObjectNewExpr objectNewExpr)
	{
		declarationUsageCollector.addUsage(objectNewExpr);

		Expr[] arguments = objectNewExpr.getArgs();

		// TODO: Are there object constructors with dangerous parameters?
		for(Expr argument : arguments)
		{
			analyzeExpression(argument);
		}

		// TODO: Are there objects that can't be stored in dangerous functions?
		// Normally new Object(...) can be trusted
		return AnalysisResult.noRisks(objectNewExpr);
	}

	protected AnalysisResult analyzeVarExpr(VarExpr varExpr)
	{
		String variableName = varExpr.getName().toString();
		AnalysisResult result = variableResults.get(variableName);

		// Use previously assigned variable-outcome if we have it
		if(result != null)
			return result;

		// Otherwise, don't trust it
		return AnalysisResult.unsafeInput(varExpr);
	}

	protected AnalysisResult analyzeThisFieldExpr(PTAThisFieldExpr thisFieldExpr)
	{
		// TODO: Make this actually work:
//		String variableName = thisFieldExpr.getName().toString();
//		AnalysisResult result = variableResults.get(variableName);
//
//		if(result != null)
//			return result;

		// Don't trust values of $this->field
		return AnalysisResult.unsafeInput(thisFieldExpr);
	}

	protected AnalysisResult analyzeThisFieldVarExpr(PTAThisFieldVarExpr thisFieldVarExpr)
	{
		analyzeExpression(thisFieldVarExpr.getNameExpr());

		// This is so unpredictable, just always assume danger
		// Don't trust $this->$field
		return AnalysisResult.unsafeInput(thisFieldVarExpr);
	}

	protected AnalysisResult analyzeArrayGetExpr(ArrayGetExpr arrayGetExpr)
	{
		// TODO: Look up whether its actually dangerous or not (see analyzeBinaryAssignExpr) using the index?
		// TODO: Check for _GET, _POST etc?

		analyzeExpression(arrayGetExpr.getExpr());

		return AnalysisResult.unsafeInput(arrayGetExpr);
	}

	protected AnalysisResult analyzeLiteralStringExpr(LiteralStringExpr literalStringExpr)
	{
		// A literal string is generally not very dangerous
		return AnalysisResult.noRisks(literalStringExpr);
	}

	protected AnalysisResult analyzeLiteralLongExpr(LiteralLongExpr literalLongExpr)
	{
		// A literal long is generally not very dangerous
		return AnalysisResult.noRisks(literalLongExpr);
	}

	protected AnalysisResult analyzeBinaryAppendExpr(BinaryAppendExpr binaryAppendExpr)
	{
		// TODO: this one of the most common problem-areas. So if we see user input used directly, we should perhaps add additional warnings?

		AnalysisResult result = analyzeExpression(binaryAppendExpr.getValue());
		BinaryAppendExpr next = binaryAppendExpr.getNext();
		if(next != null)
			result = AnalysisResult.merge(result, analyzeExpression(next));

		return result;
	}

	protected AnalysisResult analyzeBinaryAssignExpr(PTABinaryAssignExpr binaryAssignExpr)
	{
		AbstractVarExpr varExpr = binaryAssignExpr.getVar();

		// See if we can store the assignment itself alongside the variable
		AnalysisResult result = analyzeExpression(binaryAssignExpr.getValue());

		return analyzeVariable(varExpr, result);
	}

	protected AnalysisResult analyzeBinaryAssignRefExpr(PTABinaryAssignRefExpr binaryAssignRefExpr)
	{
		AbstractVarExpr varExpr = binaryAssignRefExpr.getVar();

		// See if we can store the assignment itself alongside the variable
		AnalysisResult result = analyzeExpression(binaryAssignRefExpr.getValue());

		return analyzeVariable(varExpr, result);
	}

	private AnalysisResult analyzeVariable(AbstractVarExpr varExpr, AnalysisResult result)
	{
		if(varExpr instanceof VarExpr)
		{
			// TODO: Support other types of var's like ThisFieldExpr
			String variableName = ((VarExpr) varExpr).getName().toString();
			// Escalate to most dangerous variant seen so far (i.e. doesn't support $var = danger; $var = escape($var);)
			result = AnalysisResult.merge(result, variableResults.get(variableName));

			variableResults.put(variableName, result);
		}

		return result;
	}

	protected AnalysisResult analyzeConditionalExpr(PTAConditionalExpr conditionalExpr)
	{
		analyzeExpression(conditionalExpr.getTest());

		AnalysisResult result = analyzeExpression(conditionalExpr.getTrueExpr());
		result = AnalysisResult.merge(result, analyzeExpression(conditionalExpr.getFalseExpr()));

		// The conditional ? left : right, just take the most dangerous version
		return result;
	}

	protected AnalysisResult analyzeConditionalShortExpr(PTAConditionalShortExpr conditionalShortExpr)
	{
		AnalysisResult result = analyzeExpression(conditionalShortExpr.getTest());

		// Short condiontal/elvis operator. condition ?: right, combine the condition and the right
		return AnalysisResult.merge(result, analyzeExpression(conditionalShortExpr.getFalseExpr()));
	}

	protected AnalysisResult analyzeObjectMethodExpr(PTAObjectMethodExpr objectMethodExpr)
	{
		declarationUsageCollector.addUsage(objectMethodExpr);

		Expr objectExpression = objectMethodExpr.getObjExpr();
		analyzeExpression(objectExpression);

		Expr[] arguments = objectMethodExpr.getArgs();

		// See if the object's method-call is potentially dangerous
		return analyzeMethodArguments(objectMethodExpr, objectMethodExpr.getName(), arguments);
	}

	protected AnalysisResult analyzeObjectMethodVarExpr(PTAObjectMethodVarExpr objectMethodExpr)
	{
		Expr objectName = objectMethodExpr.getObjExpr();
		Expr name = objectMethodExpr.getName();
		Expr[] arguments = objectMethodExpr.getArgs();

		// Somewhat unpredictable like '$object->{$var}()'
		analyzeExpression(objectName);
		analyzeExpression(name);
		analyzeMethodArguments(objectMethodExpr, "$var->$method", arguments);

		// Its too unpredictable, so don't trust its input
		return AnalysisResult.unsafeInput(objectMethodExpr);
	}

	protected AnalysisResult analyzeClassMethodVarExpr(ClassMethodVarExpr classMethodVarExpr)
	{
		log.warn("Found class::$method()-expression: " + classMethodVarExpr + ", at " + getUsefullLocation(classMethodVarExpr));

		// class::$method() expressions are tricky, don't trust it
		return AnalysisResult.unsafeInput(classMethodVarExpr);
	}

	protected AnalysisResult analyzeClassVarMethodVarExpr(ClassVarMethodVarExpr classVarMethodVarExpr)
	{
		log.warn("Found $class::$method()-expression: " + classVarMethodVarExpr + ", at " + getUsefullLocation(classVarMethodVarExpr));

		// $class::$method() expressions are tricky, don't trust it
		return AnalysisResult.unsafeInput(classVarMethodVarExpr);
	}

	protected AnalysisResult analyzeClassMethodExpr(PTAClassMethodExpr classMethodExpr)
	{
		declarationUsageCollector.addUsage(classMethodExpr);

		StringValue methodName = classMethodExpr.getMethodName();
		Expr[] arguments = classMethodExpr.getArgs();

		// Class::method() see if we know anything about it
		return analyzeMethodArguments(classMethodExpr, methodName.toString(), arguments);
	}

	protected AnalysisResult analyzeClassConstructExpr(PTAClassConstructExpr classConstructExpr)
	{
		declarationUsageCollector.addUsage(classConstructExpr);

		Expr[] arguments = classConstructExpr.getArgs();

		// Class::__construct explictly called
		analyzeMethodArguments(classConstructExpr, "__construct", arguments);

		// Still, should be as safe as new Object
		return AnalysisResult.noRisks(classConstructExpr);
	}

	protected AnalysisResult analyzeCallExpr(PTACallExpr callExpr)
	{
		declarationUsageCollector.addUsage(callExpr);

		Expr[] arguments = callExpr.getArgs();

		// Normal function-call, see if we know anything about it
		return analyzeMethodArguments(callExpr, callExpr.getSimpleMethodName(), arguments);
	}

	protected AnalysisResult analyzeThisMethodExpr(PTAThisMethodExpr thisMethodExpr)
	{
		declarationUsageCollector.addUsage(thisMethodExpr);

		Expr[] arguments = thisMethodExpr.getArgs();

		// $this->method, see if we know anything about it
		return analyzeMethodArguments(thisMethodExpr, thisMethodExpr.getName(), arguments);
	}

	protected AnalysisResult analyzeThisMethodVarExpr(PTAThisMethodVarExpr thisMethodVarExpr)
	{
		Expr name = thisMethodVarExpr.getName();
		Expr[] arguments = thisMethodVarExpr.getArgs();

		analyzeExpression(name);
		analyzeMethodArguments(thisMethodVarExpr, "$this->{$variable}", arguments);

		// $this->{$methodName}(...), too unpredictable, so don't trust it
		return AnalysisResult.unsafeInput(thisMethodVarExpr);
	}

	protected AnalysisResult analyzeObjectNewVarExpr(PTAObjectNewVarExpr objectNewVarExpr)
	{
		Expr name = objectNewVarExpr.getName();
		Expr[] arguments = objectNewVarExpr.getArgs();

		analyzeExpression(name);
		analyzeMethodArguments(objectNewVarExpr, "new $variable", arguments);

		// new $var, somewhat tricky, but should be mostly thurstworthy
		return AnalysisResult.noRisks(objectNewVarExpr);
	}

	protected AnalysisResult analyzeCallVarExpr(PTACallVarExpr callVarExpr)
	{
		Expr name = callVarExpr.getName();
		Expr[] arguments = callVarExpr.getArgs();

		analyzeExpression(name);
		analyzeMethodArguments(callVarExpr, "$variable", arguments);

		// $variable(...), too tricky to trust
		return AnalysisResult.unsafeInput(callVarExpr);
	}

	protected AnalysisResult analyzeClassVirtualMethodExpr(PTAClassVirtualMethodExpr classVirtualMethodExpr)
	{
		declarationUsageCollector.addUsage(classVirtualMethodExpr);

		Expr[] arguments = classVirtualMethodExpr.getArgs();

		// ClassName::method, see if we know anything about it
		return analyzeMethodArguments(classVirtualMethodExpr, classVirtualMethodExpr.getMethodName().toString(), arguments);
	}

	protected AnalysisResult analyzeObjectNewStaticExpr(PTAObjectNewStaticExpr objectNewStaticExpr)
	{
		Expr[] arguments = objectNewStaticExpr.getArgs();

		analyzeMethodArguments(objectNewStaticExpr, "new static", arguments);

		// literal 'new static', should be okay.
		return AnalysisResult.noRisks(objectNewStaticExpr);
	}

	protected AnalysisResult analyzeClassVarMethodExpr(PTAClassVarMethodExpr classVarMethodExpr)
	{
		declarationUsageCollector.addUsage(classVarMethodExpr);

		Expr name = classVarMethodExpr.getClassName();
		Expr[] arguments = classVarMethodExpr.getArgs();

		AnalysisResult result = analyzeExpression(name);

		// $classVar::methodName(...), see if we know anything about it
		return AnalysisResult.merge(result, analyzeMethodArguments(classVarMethodExpr, classVarMethodExpr.getMethodName().toString(), arguments));
	}

	private AnalysisResult analyzeMethodArguments(Expr methodExpr, String methodName, Expr[] arguments)
	{
		// TODO: Test specific arguments for interest?

		// Basic method-arguments, if any of them is dangerous... treat the entire input as dangerous
		AnalysisResult result = AnalysisResult.noRisks(null);

		for(Expr argument : arguments)
		{
			result = AnalysisResult.merge(result, analyzeExpression(argument));
		}

		// Store the worst argument result
		resultCollector.collectResult(methodExpr, methodName, result);

		// We don't know what comes out of a function... so don't trust it.
		AnalysisResult functionResult = AnalysisResult.unsafeInput(methodExpr);

		// But if the function is known to mitigate some risks, remove those
		Set<Risk> mitigations = resultCollector.getMethodInformation().getMitigationsForMethod(methodName);
		if(mitigations != null)
			functionResult.removeRisks(mitigations);

		return functionResult;
	}

	protected AnalysisResult analyzeFunIncludeOnceExpr(FunIncludeOnceExpr funIncludeOnceExpr)
	{
		Expr argument = funIncludeOnceExpr.getExpr();

		// See if the include/require is dangerously called
		AnalysisResult result = analyzeExpression(argument);
		resultCollector.collectResult(funIncludeOnceExpr, FUNCTION_INCLUDE_ONCE, result);

		// Don't trust a include/require's output
		return AnalysisResult.unsafeInput(funIncludeOnceExpr);
	}

	protected AnalysisResult analyzeFunIncludeExpr(FunIncludeExpr funIncludeExpr)
	{
		Expr argument = funIncludeExpr.getExpr();

		// See if the include/require is dangerously called
		AnalysisResult result = analyzeExpression(argument);
		resultCollector.collectResult(funIncludeExpr, FUNCTION_INCLUDE, result);

		// Don't trust a include/require's output
		return AnalysisResult.unsafeInput(funIncludeExpr);
	}

	protected AnalysisResult analyzeBinaryAssignListEachExpr(PTABinaryAssignListEachExpr binaryAssignListEachExpr)
	{
		analyzeExpression(binaryAssignListEachExpr.getListHead());

		// TODO: Store outcomes?
		// list(...) = each(...)
		return analyzeExpression(binaryAssignListEachExpr.getValue());
	}

	protected AnalysisResult analyzeBinaryAssignListExpr(PTABinaryAssignListExpr binaryAssignListExpr)
	{
		analyzeExpression(binaryAssignListExpr.getListHead());

		// TODO: Store outcomes?
		// list(...) = ...
		return analyzeExpression(binaryAssignListExpr.getValue());
	}

	protected AnalysisResult analyzeListHeadExpr(PTAListHeadExpr listHeadExpr)
	{
		for(Expr var : listHeadExpr.getVarList())
		{
			// Variables may actually be empty, like: list(,$var) = ...
			if(var != null)
				analyzeExpression(var);
		}

		// List itself is fairly safe
		return AnalysisResult.noRisks(listHeadExpr);
	}

	protected AnalysisResult analyzeClosureExpr(ClosureExpr closureExpr)
	{
		programAnalyzer.analyzeFunction(closureExpr.getFunction());

		// Function-calls are always a bit tricky, so closures as well
		return AnalysisResult.unsafeInput(closureExpr);
	}
}
