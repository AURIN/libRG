/*
 *	Copyright (C) 2010 Visualization & Graphics Lab (VGL), Indian Institute of Science
 *
 *	This file is part of libRG, a library to compute Reeb graphs.
 *
 *	libRG is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU Lesser General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	libRG is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public License
 *	along with libRG.  If not, see <http://www.gnu.org/licenses/>.
 *
 *	Author(s):	Harish Doraiswamy
 *	Version	 :	1.0
 *
 *	Modified by : -- 
 *	Date : --
 *	Changes  : --
 */
package reebgraph.iisc.vgl.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.StringTokenizer;

public class Utilities {
	
	public static int TRUE = 0;
	public static int FALSE = 1;
	public static int NOIDEA = 2;
	
	public static void pr(String s) {
		System.out.println(s);
	}
	
	public static String[] splitString(String s) {
		String[] ret = null;
		StringTokenizer tok = new StringTokenizer(s);
		ret = new String[tok.countTokens()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = tok.nextToken();
		}
		return ret;
	}
	
	public static void er(String s) {
		System.out.println("Error!!");
		System.out.println(s);
		System.exit(1);
	}
	
	public static String roundDouble(double num, int decimal) {
		String s = "0.";
		for (int i = 0; i < decimal; i++) {
			s += "0";
		}
		NumberFormat f = new DecimalFormat(s);
		s = f.format(num);
		return s;
	}
}
