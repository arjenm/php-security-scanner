package org.arjenm.phpsecurity.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.ClassVarNameConstExpr;
import com.caucho.quercus.expr.Expr;

public class PTAClassVarNameConstExpr extends ClassVarNameConstExpr
{
	public PTAClassVarNameConstExpr(Location location, String className, Expr name)
	{
		super(location, className, name);
	}

	public PTAClassVarNameConstExpr(String className, Expr name)
	{
		super(className, name);
	}

	public String getClassName()
	{
		return _className;
	}

	public Expr getName()
	{
		return _name;
	}
}