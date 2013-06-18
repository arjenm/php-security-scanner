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

package org.arjenm.phpsecurity.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.FunArrayExpr;

import java.util.ArrayList;

public class PTAFunArrayExpr extends FunArrayExpr
{
	public PTAFunArrayExpr(Location location, ArrayList<Expr> keyList, ArrayList<Expr> valueList)
	{
		super(location, keyList, valueList);
	}

	public PTAFunArrayExpr(Location location, Expr[] keys, Expr[] values)
	{
		super(location, keys, values);
	}

	public PTAFunArrayExpr(ArrayList<Expr> keyList, ArrayList<Expr> valueList)
	{
		super(keyList, valueList);
	}

	public PTAFunArrayExpr(Expr[] keys, Expr[] values)
	{
		super(keys, values);
	}

	public Expr[] getKeys()
	{
		return _keys;
	}

	public Expr[] getValues()
	{
		return _values;
	}
}
