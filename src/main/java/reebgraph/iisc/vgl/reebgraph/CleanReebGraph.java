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
package reebgraph.iisc.vgl.reebgraph;

import reebgraph.iisc.vgl.reebgraph.TriangleData.Vertex;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class CleanReebGraph implements Serializable {

	private static final long serialVersionUID = 1L;

	public static enum VertexType {
		MINIMA, MAXIMA, SADDLE, NONE
	}
	
	class Arc implements Comparable<Arc>, Serializable {
		private static final long serialVersionUID = 1L;

		int v1;
		int v2;
		float fn;
		short icol;
		boolean removed;
		ArrayList <Integer> comps = new ArrayList<Integer>();
		
		
		ArrayList<Vertex> path = new ArrayList<Vertex>();
		HashSet<Vertex> segment = new HashSet<Vertex>();
		
		public boolean equals(Object obj) {
			Arc a = (Arc) obj;
			return (a.icol == icol);
		}

		public int compareTo(Arc o) {
			float ff = fn - o.fn;
			if(ff < 0)
				return -1;
			else if(ff > 0)
				return 1;
			return 0;
		}
	}
	
	class Node implements Serializable {
		private static final long serialVersionUID = 1L;

		int v;
		boolean removed;
		float fn;
		VertexType type;
		ArrayList<Arc> prev = new ArrayList<Arc>();
		ArrayList<Arc> next = new ArrayList<Arc>();
	}
	
	public Node [] nodes;
	public Arc [] arcs;

	ArrayList<Node> an = new ArrayList<Node>();
	ArrayList<Arc> ar = new ArrayList<Arc>();
	public HashMap<Integer, Integer> vmap = new HashMap<Integer, Integer>();
	int ct = 0;
	short ect = 0;
	float min = Float.MAX_VALUE;
	float max = -Float.MAX_VALUE;
	float persistence;
	
	public void addNode(int v, float fn, byte type) {
		Node n = new Node();
		n.fn = fn;
		n.v = v;
		if(type == ReebGraph.MINIMUM) {
			n.type = VertexType.MINIMA;
		}
		if(type == ReebGraph.MAXIMUM) {
			n.type = VertexType.MAXIMA;
		}
		if(type == ReebGraph.SADDLE) {
			n.type = VertexType.SADDLE;
		}
		an.add(n);
		vmap.put(v, ct);
		ct ++;
		
		max = Math.max(max, fn);
		min = Math.min(min, fn);
	}
	
	public void addNode(int v, float fn, VertexType type) {
		Node n = new Node();
		n.fn = fn;
		n.v = v;
		n.type = type;

		an.add(n);
		vmap.put(v, ct);
		ct ++;
		
		max = Math.max(max, fn);
		min = Math.min(min, fn);
	}
	public void setup() {
		nodes = an.toArray(new Node[0]);
		arcs = ar.toArray(new Arc[0]);
		persistence = max - min;
	}
	
	public void addArc(int v1, int v2, ArrayList<Integer> comps) {
		Arc a = new Arc();
		a.v1 = vmap.get(v1);
		a.v2 = vmap.get(v2);
		a.comps.addAll(comps);
		//a.fn is the difference between the two nodes of the edge
		a.fn = an.get(a.v2).fn - an.get(a.v1).fn;

		a.path = new ArrayList<Vertex>();
		
		ect ++;
		a.icol = ect;
		ar.add(a);
		
		an.get(a.v1).next.add(a);
		an.get(a.v2).prev.add(a);
	}

	public void addArc(int v1, int v2, int fn, ArrayList<Integer> comps) {
		Arc a = new Arc();
		a.v1 = vmap.get(v1);
		a.v2 = vmap.get(v2);
		a.comps.addAll(comps);
		a.fn = fn;

		a.path = new ArrayList<Vertex>();
		
		ect ++;
		a.icol = ect;
		ar.add(a);
		
		an.get(a.v1).next.add(a);
		an.get(a.v2).prev.add(a);
	}
	protected void removeDeg2Nodes() {
		// remove degree 2 vertices
		for(int i = 0;i < nodes.length;i ++) {
			if(!nodes[i].removed && nodes[i].next.size() == 1 && nodes[i].prev.size() == 1) {
				mergeNode(i);
			}
		}
	}
	
	private void mergeNode(int i) {
		Arc e1 = nodes[i].prev.get(0);
		Arc e2 = nodes[i].next.get(0);
		
		nodes[i].removed = true;
		e1.v2 = e2.v2;
		e2.removed = true;
		if(e2.path.size() != 0) {
			e2.path.remove(0);
		}
		e1.path.addAll(e2.path);
		e1.segment.addAll(e2.segment);
		if(e1.icol > e2.icol) {
			e1.icol = e2.icol;
		}
		e1.fn += e2.fn;
		nodes[e1.v2].prev.remove(e2);
		nodes[e1.v2].prev.add(e1);
		
	}

	
	public void outputReebGraph(PrintStream p) {
		int nv = 0;
		int ne = 0;
		for(int i = 0;i < nodes.length;i ++) {
			if(!nodes[i].removed) {
				nv ++;
			}
		}
		
		for(int i = 0;i < arcs.length;i ++) {
			if(!arcs[i].removed) {
				ne ++;
			}
		}

		p.println(nv + " " + ne);
		for(int i = 0;i < nodes.length;i ++) {
			if(!nodes[i].removed) {
				p.println(nodes[i].v + " " + nodes[i].fn + " " + nodes[i].type);
			}
		}
		
		for(int i = 0;i < arcs.length;i ++) {
			if(!arcs[i].removed) {
				p.print(nodes[arcs[i].v1].v + " " + nodes[arcs[i].v2].v + " ");
				for(int j = 0;j < arcs[i].comps.size();j ++) {
					p.print(arcs[i].comps.get(j) + " ");
				}
				p.println();
			}
		}
	}
	
}
