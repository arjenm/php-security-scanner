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
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.expr.*;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.Arg;
import com.caucho.quercus.program.Function;
import com.caucho.quercus.program.FunctionInfo;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.statement.*;
import org.arjenm.phpsecurity.quercus.expr.*;
import org.arjenm.phpsecurity.quercus.program.PTAFunction;
import org.arjenm.phpsecurity.quercus.program.PTAObjectMethod;
import org.arjenm.phpsecurity.quercus.statement.*;

import java.util.ArrayList;

/**
 * ExprFactory that creates Expr(ession)s and Statements that allow access to their internals.
 *
 * Other than that, it is exactly like the {@link ExprFactory}.
 *
 * @author Arjen
 */
public class ParseTreeAccessExpressionFactory extends ExprFactory
{
	@Override
	public Statement createEcho(Location loc, Expr expr)
	{
		return new PTAEchoStatement(loc, expr);
	}

	@Override
	public Expr createAssign(AbstractVarExpr left, Expr right)
	{
		return new PTABinaryAssignExpr(left, right);
	}

	public Expr createAssignRef(AbstractVarExpr left, Expr right)
	{
		return new PTABinaryAssignRefExpr(left, right);
	}

	@Override
	public Expr createMethodCall(Location loc, Expr objExpr, StringValue methodName, ArrayList<Expr> args)
	{
		return new PTAObjectMethodExpr(loc, objExpr, methodName, args);
	}

	@Override
	public Expr createMethodCall(Location loc, Expr objExpr, Expr methodName, ArrayList<Expr> args)
	{
		return new PTAObjectMethodVarExpr(loc, objExpr, methodName, args);
	}

	@Override
	public ObjectNewExpr createNew(Location loc, String name, ArrayList<Expr> args)
	{
		return new PTAObjectNewExpr(loc, name, args);
	}

	@Override
	public Expr createClassMethodCall(Location loc, String className, StringValue methodName, ArrayList<Expr> args)
	{
		if ("__construct".equals(methodName.toString()))
		{
			return new PTAClassConstructExpr(loc, className, args);
		}
		else
		{
			return new PTAClassMethodExpr(loc, className, methodName, args);
		}
	}

	@Override
	public BinaryCharAtExpr createCharAt(Expr base, Expr index)
	{
		return new PTABinaryCharAtExpr(base, index);
	}

	@Override
	public Expr createArrayFun(ArrayList<Expr> keys, ArrayList<Expr> values)
	{
		return new PTAFunArrayExpr(keys, values);
	}

	@Override
	public Statement createIf(Location loc, Expr test, Statement trueBlock, Statement falseBlock)
	{
		return new PTAIfStatement(loc, test, trueBlock, falseBlock);
	}

	@Override
	public Expr createCall(QuercusParser parser, StringValue name, ArrayList<Expr> args)
	{
		Location loc = parser.getLocation();

		if (name.equalsString("isset") && args.size() == 1)
			return new FunIssetExpr(args.get(0));
		else if (name.equalsString("get_called_class") && args.size() == 0)
			return new FunGetCalledClassExpr(loc);
		else if (name.equalsString("get_class") && args.size() == 0)
			return new FunGetClassExpr(parser);
		else if (name.equalsString("each") && args.size() == 1)
			return new FunEachExpr(args.get(0));
		else
			return new PTACallExpr(loc, name, args);
	}

	@Override
	public Statement createWhile(Location loc, Expr test, Statement block, String label)
	{
		return new PTAWhileStatement(loc, test, block, label);
	}

	@Override
	public Statement createDo(Location loc, Expr test, Statement block, String label)
	{
		return new PTADoStatement(loc, test, block, label);
	}

	@Override
	public Statement createFor(Location loc, Expr init, Expr test, Expr incr, Statement block, String label)
	{
		return new PTAForStatement(loc, init, test, incr, block, label);
	}

	@Override
	public Statement createForeach(Location loc, Expr objExpr, AbstractVarExpr key, AbstractVarExpr value,
								   boolean isRef, Statement block, String label)
	{
		return new PTAForeachStatement(loc, objExpr, key, value, isRef, block, label);
	}

	@Override
	public Expr createConditional(Expr test, Expr left, Expr right)
	{
		return new PTAConditionalExpr(test, left, right);
	}

	@Override
	public Expr createShortConditional(Expr test, Expr right)
	{
		return new PTAConditionalShortExpr(test, right);
	}

	@Override
	public Statement createSwitch(Location loc, Expr value, ArrayList<Expr[]> caseList, ArrayList<BlockStatement> blockList,
								  Statement defaultBlock, String label)
	{
		return new PTASwitchStatement(loc, value, caseList, blockList,
				defaultBlock, label);
	}

	@Override
	public BreakStatement createBreak(Location location, Expr target, ArrayList<String> loopLabelList)
	{
		return new PTABreakStatement(location, target, loopLabelList);
	}

	@Override
	public ContinueStatement createContinue(Location location,
											Expr target,
											ArrayList<String> loopLabelList)
	{
		return new PTAContinueStatement(location, target, loopLabelList);
	}

	@Override
	public Expr createExit(Expr expr)
	{
		return new PTAFunExitExpr(expr);
	}

	@Override
	public Expr createDie(Expr expr)
	{
		return new PTAFunDieExpr(expr);
	}

	@Override
	public Expr createList(ListHeadExpr head, Expr value)
	{
		return new PTABinaryAssignListExpr(head, value);
	}

	@Override
	public Expr createListEach(ListHeadExpr head, Expr value)
	{
		return new PTABinaryAssignListEachExpr(head, value);
	}

	@Override
	public ListHeadExpr createListHead(ArrayList<Expr> keys)
	{
		return new PTAListHeadExpr(keys);
	}

	@Override
	public Function createFunction(Location loc, String name, FunctionInfo info, Arg[] argList, Statement[] statementList)
	{
		return new PTAFunction(this, loc, name, info, argList, statementList);
	}

	@Override
	public Statement createReturn(Location loc, Expr value)
	{
		return new PTAReturnStatement(loc, value);
	}

	@Override
	public Statement createReturnRef(Location loc, Expr value)
	{
		return new PTAReturnRefStatement(loc, value);
	}

	@Override
	public Function createObjectMethod(Location loc, InterpretedClassDef cl, String name, FunctionInfo info,
									   Arg[] argList, Statement[] statementList)
	{
		return new PTAObjectMethod(this, loc, cl, name, info, argList, statementList);
	}

	@Override
	public Expr createThisMethod(Location loc, ThisExpr qThis, StringValue methodName, ArrayList<Expr> args, boolean isInStaticClassScope)
	{
		return new PTAThisMethodExpr(loc, qThis, methodName, args, isInStaticClassScope);
	}

	@Override
	public Expr createThisMethod(Location loc, ThisExpr qThis, Expr methodName, ArrayList<Expr> args, boolean isInStaticClassScope)
	{
		return new PTAThisMethodVarExpr(loc, qThis, methodName, args, isInStaticClassScope);
	}

	@Override
	public ThisFieldVarExpr createThisField(Location location, ThisExpr qThis, Expr name, boolean isInStaticClassScope)
	{
		return new PTAThisFieldVarExpr(location, qThis, name, isInStaticClassScope);
	}

	@Override
	public ThisFieldExpr createThisField(Location location, ThisExpr qThis, StringValue name, boolean isInStaticClassScope)
	{
		return new PTAThisFieldExpr(location, qThis, name, isInStaticClassScope);
	}

	@Override
	public Statement createThrow(Location loc, Expr value)
	{
		return new PTAThrowStatement(loc, value);
	}

	@Override
	public TryStatement createTry(Location loc, Statement block)
	{
		return new PTATryStatement(loc, block);
	}

	@Override
	public ObjectNewVarExpr createVarNew(Location loc, Expr name, ArrayList<Expr> args)
	{
		return new PTAObjectNewVarExpr(loc, name, args);
	}

	@Override
	public ObjectNewStaticExpr createNewStatic(Location loc, ArrayList<Expr> args)
	{
		return new PTAObjectNewStaticExpr(loc, args);
	}

	@Override
	public Expr createClassConst(Expr className, StringValue name)
	{
		return new PTAClassVarConstExpr(className, name);
	}

	@Override
	public CallVarExpr createVarFunction(Location loc, Expr name, ArrayList<Expr> args)
	{
		return new PTACallVarExpr(loc, name, args);
	}

	@Override
	public Expr createFieldGet(Expr base, StringValue name)
	{
		return new PTAObjectFieldExpr(base, name);
	}

	@Override
	public Expr createFieldVarGet(Expr base, Expr name)
	{
		return new PTAObjectFieldVarExpr(base, name);
	}

	@Override
	public Expr createClassMethodCall(Location loc, Expr className, StringValue methodName, ArrayList<Expr> args)
	{
		return new PTAClassVarMethodExpr(loc, className, methodName, args);
	}

	@Override
	public Statement createClassStatic(Location loc, String className, VarExpr var, Expr value)
	{
		return new PTAClassStaticStatement(loc, className, var, value);
	}

	@Override
	public Statement createStatic(Location loc, VarExpr var, Expr value)
	{
		return new PTAStaticStatement(loc, var, value);
	}

	@Override
	public Expr createClassVirtualMethodCall(Location loc, StringValue methodName, ArrayList<Expr> args)
	{
		return new PTAClassVirtualMethodExpr(loc, methodName, args);
	}

	@Override
	public Expr createClassField(String className, Expr name)
	{
		return new PTAClassFieldVarExpr(className, name);
	}

	@Override
	public ClassConstExpr createClassConst(String className, StringValue name)
	{
		return new PTAClassConstExpr(className, name);
	}

	@Override
	public Expr createClassField(Expr className, StringValue name)
	{
		return new PTAClassVarFieldExpr(className, name);
	}

	@Override
	public Statement createFunctionDef(Location loc, Function fun)
	{
		return new PTAFunctionDefStatement(loc, fun);
	}

	@Override
	public Statement createClassDef(Location loc, InterpretedClassDef cl)
	{
		return new PTAClassDefStatement(loc, cl);
	}
}
