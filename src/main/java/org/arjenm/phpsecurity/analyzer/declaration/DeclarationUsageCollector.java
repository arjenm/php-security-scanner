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
import com.caucho.quercus.expr.BinaryInstanceOfExpr;
import com.caucho.quercus.expr.ClassConstExpr;
import com.caucho.quercus.expr.ClassVirtualConstExpr;
import com.caucho.quercus.expr.ConstExpr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.ClassDef;
import org.arjenm.phpsecurity.quercus.expr.*;
import org.arjenm.phpsecurity.quercus.statement.PTAClassStaticStatement;
import org.arjenm.phpsecurity.quercus.statement.PTAStaticStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Collector of method, class, constant and class-constant usage information.
 *
 * While its not necessarily a security issue if code is unused, it does reduce the maintainability of the codebase
 * and that may hinders the security of that codebase.
 *
 * This collector allows both collecting declarations and collecting usages in a single pass. It will match usages to declarations
 * in its final stage.
 *
 * To do that, it'll map all method-calls to literal "$object->" + methodName + "()", all static calls to "static::" + methodName + "()",
 * all class usages (both as static and as construction) to className + ".class" and normal methods to methodName + "()".
 *
 * This will simplify searches, but will give false positives as well.
 *
 * @author Arjen
 */
public class DeclarationUsageCollector
{
	private static final Logger log = LoggerFactory.getLogger(DeclarationUsageCollector.class);
	private static final String STATIC_PREFIX = "static::";
	private static final String FUNCTION_POSTFIX = "()";
	private static final String CLASS_POSTFIX = ".class";
	private static final String OBJECT_PREFIX = "$object->";

	private final List<String> unusedDeclarationPaths;

	private Map<String, Declaration> declarationMap = new HashMap<>();
	private Map<String, DeclarationUsage> usageMap = new HashMap<>();

	/**
	 * Create a collector that looks for unused declarations in the given path.
	 *
	 * @param unusedDeclarationPaths The paths to look at, if empty or null, no paths will be considered.
	 */
	public DeclarationUsageCollector(List<String> unusedDeclarationPaths)
	{
		this.unusedDeclarationPaths = unusedDeclarationPaths;
	}

	// TODO: Add class constants
	// TODO: Add define's and non-class const's (which are parsed into define-function calls)

	/**
	 * Add a declaration of the given function/method.
	 *
	 * @param function The function/method to add.
	 */
	public void addDeclaration(AbstractFunction function)
	{
		// TODO: Add parameter type hints as class-usage?

		// Ignore the various magic methods (__construct, __destruct, __get, __toString, etc)
		if(function.getName().startsWith("__"))
			return;

		// Normal function
		if(function.getDeclaringClass() == null)
		{
			addDeclaration(new Declaration(DeclarationType.FUNCTION, function.getLocation(), function.getName() + FUNCTION_POSTFIX));
			return;
		}

		// Also ignore old-style constructors
		if(function.getDeclaringClassName().equals(function.getName()))
		{
			log.debug("Found old fashioned constructor: " + function);
			return;
		}

		// Ignore private and protected methods as well
		if(function.isPrivate() || function.isProtected())
		{
			return;
		}

		// Static class method
		if(function.isStatic())
		{
			addDeclaration(new Declaration(DeclarationType.STATIC_CLASS_METHOD, function.getLocation(), STATIC_PREFIX + function.getName() + FUNCTION_POSTFIX));
			return;
		}

		// Other class methods
		addDeclaration(new Declaration(DeclarationType.CLASS_METHOD, function.getLocation(), OBJECT_PREFIX + function.getName() + FUNCTION_POSTFIX));
	}

	/**
	 * Add the interface/class-declaration.
	 *
	 * @param classDef The class to add.
	 */
	public void addDeclaration(ClassDef classDef)
	{
		addDeclaration(new Declaration(DeclarationType.CLASS, classDef.getLocation(), classDef.getName() + CLASS_POSTFIX));

		if(classDef.getParentName() != null)
			addUsage(classDef.getParentName() + CLASS_POSTFIX);

		String[] interfaces = classDef.getInterfaces();
		if(interfaces != null)
		{
			for(String interfaceName : interfaces)
			{
				addUsage(interfaceName + CLASS_POSTFIX);
			}
		}

		// TODO: Add constants as definitions
	}

	public void addDeclaration(PTAStaticStatement staticStatement)
	{
		//To change body of created methods use File | Settings | File Templates.
	}

	public void addDeclaration(PTAClassStaticStatement classStaticStatement)
	{
		//To change body of created methods use File | Settings | File Templates.
	}

	public void addDeclaration(ClassConstExpr constExpr)
	{
		//To change body of created methods use File | Settings | File Templates.
	}

	private void addDeclaration(Declaration declaration)
	{
		if(unusedDeclarationPaths == null || unusedDeclarationPaths.isEmpty())
			return;

		if(declaration.getDeclarationLocation().isUnknown())
			return;

		boolean pathAllowed = false;
		for(String unusedDeclarationPath : unusedDeclarationPaths)
		{
			if(declaration.getDeclarationLocation().getFileName().startsWith(unusedDeclarationPath))
			{
				pathAllowed = true;
				break;
			}
		}

		if(!pathAllowed)
		{
			if(log.isTraceEnabled())
				log.trace("Skipping declaration on unallowed path: " + declaration);

			return;
		}

		// TODO: Merge multiple definitions?
		if(declarationMap.containsKey(declaration.getDeclarationName()))
		{
			if(log.isDebugEnabled())
				log.debug("Collector already contains a declaration for " + declaration.getDeclarationName());

			return;
		}

		if(log.isTraceEnabled())
			log.trace("Adding declaration of " + declaration.getDeclarationName());

		declarationMap.put(declaration.getDeclarationName(), declaration);
	}

	public void addUsage(PTAClassVarMethodExpr classVarMethodExpr)
	{
		addUsage(STATIC_PREFIX + classVarMethodExpr.getMethodName() + FUNCTION_POSTFIX);
	}

	public void addUsage(PTAClassVirtualMethodExpr classVirtualMethodExpr)
	{
		addUsage(STATIC_PREFIX + classVirtualMethodExpr.getMethodName() + FUNCTION_POSTFIX);
	}

	public void addUsage(PTAThisMethodExpr thisMethodExpr)
	{
		addUsage(OBJECT_PREFIX + thisMethodExpr.getMethodName() + FUNCTION_POSTFIX);
	}

	public void addUsage(PTACallExpr callExpr)
	{
		// Add with both the simple name and non-namespaced-name

		addUsage(callExpr.getName().toString() + FUNCTION_POSTFIX);
		if(callExpr.getNsName() != null)
			addUsage(callExpr.getNsName().toString() + FUNCTION_POSTFIX);
	}

	public void addUsage(PTAClassConstructExpr classConstructExpr)
	{
		addUsage(classConstructExpr.getClassName() + CLASS_POSTFIX);
	}

	public void addUsage(PTAClassMethodExpr classMethodExpr)
	{
		addUsage(STATIC_PREFIX + classMethodExpr.getMethodName() + FUNCTION_POSTFIX);
		addUsage(classMethodExpr.getClassName() + CLASS_POSTFIX);
	}

	public void addUsage(PTAObjectMethodExpr objectMethodExpr)
	{
		addUsage(OBJECT_PREFIX + objectMethodExpr.getName() + FUNCTION_POSTFIX);
	}

	public void addUsage(PTAObjectNewExpr objectNewExpr)
	{
		addUsage(objectNewExpr.getName() + CLASS_POSTFIX);
	}

	public void addUsage(ConstExpr constExpr)
	{
		//TODO: Add constant usage
	}

	public void addUsage(BinaryInstanceOfExpr binaryInstanceOfExpr)
	{
		//TODO: Add as class-usage? In that case, create a PTA-version with access to the 'right'
	}

	public void addUsage(PTAObjectFieldExpr objectFieldExpr)
	{
		//To change body of created methods use File | Settings | File Templates.
	}

	public void addUsage(PTAClassVarFieldExpr classVarFieldExpr)
	{
		//To change body of created methods use File | Settings | File Templates.
	}

	public void addUsage(PTAClassVarConstExpr classVarConstExpr)
	{
		//To change body of created methods use File | Settings | File Templates.
	}

	public void addUsage(ClassVirtualConstExpr classVirtualConstExpr)
	{
		//To change body of created methods use File | Settings | File Templates.
	}

	public void addUsage(PTAClassConstExpr constExpr)
	{
		//TODO: Add field usage as well
		addUsage(constExpr.getClassName() + CLASS_POSTFIX);
	}

	public void addUsage(PTAClassMethodVarExpr classMethodVarExpr)
	{
		addUsage(classMethodVarExpr.getClassName() + CLASS_POSTFIX);
	}

	public void addUsage(PTAClassVarNameConstExpr classVarNameConstExpr)
	{
		addUsage(classVarNameConstExpr.getClassName() + CLASS_POSTFIX);
	}

	private void addUsage(String declaredName)
	{
		String searchName = declaredName.toLowerCase();

		DeclarationUsage usage = usageMap.get(searchName);
		if(usage == null)
		{
			usage = new DeclarationUsage(declaredName);
			usageMap.put(searchName, usage);
		}

		usage.incrementUsageCount();
	}

	public void analyzeUsage()
	{
		log.info("Starting analysis of " + declarationMap.size() + " declarations");

		List<Declaration> declarationList = new ArrayList<>(declarationMap.values());

		// Sort the declarations by location, so they won't end up in a user unfriendly ordering
		Collections.sort(declarationList, new Comparator<Declaration>() {
				@Override
				public int compare(Declaration o1, Declaration o2)
				{
					// Compare locations
					Location l1 = o1.getDeclarationLocation();
					Location l2 = o2.getDeclarationLocation();

					int result = l1.getFileName().compareTo(l2.getFileName());

					if(result != 0)
						return result;

					return Integer.compare(l1.getLineNumber(), l2.getLineNumber());
				}
			}
		);

		for(Declaration declaration : declarationList)
		{
			String searchName = declaration.getDeclarationName().toLowerCase();
			DeclarationUsage usage = usageMap.get(searchName);

			if(usage != null)
				continue;

			log.info("Found unused declaration: " + declaration);
		}
	}
}