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

import static reebgraph.iisc.vgl.utils.Utilities.er;
import static reebgraph.iisc.vgl.utils.Utilities.pr;

import meshloader.iisc.vgl.external.loader.MeshLoader;
import meshloader.iisc.vgl.external.types.Simplex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TriangleData {
	
	public class DetVertex {
		public float x;
		public float y;
		public float z;
		int no;
		
		public boolean equals(Object obj) {
			DetVertex v = (DetVertex) obj;
			return v.no == no;
		}
		
		public String toString() {
			return "" + no;
		}
	}

	public class Vertex {
		public float fn;
		
		public HashSet<Triangle> star = new HashSet<Triangle>();
		
		public boolean equals(Object obj) {
			System.out.println("Vertex equals called");
			return super.equals(obj);
		}
	}

	public class Triangle {
		// The vertices that forms the triangle
		public int v1;
		public int v2;
		public int v3;
		
		int h;
		// Adjacent triangles wrt edges e(i) = (v(i), v((i + 1) % 3), i = {1, 2, 3}
		public Triangle [][] adjTriangle = new Triangle[3][2];
		
		public boolean equals(Object obj) {
			Triangle t = (Triangle) obj;
			return (t.v1 == v1 && t.v2 == v2 && t.v3 == v3);
		}
		
		public String toString() {
			return "(" + v1 + " " + v2 + " " + v3 + ")";
		}
		
		public int hashCode() {
			return h;
		}
	}
	
	class Edge {
		int v1;
		int v2;
		
		Triangle first;
		Triangle cur;
		
		public boolean equals(Object obj) {
			Edge e = (Edge) obj;
			return (e.v1 == v1 && e.v2 == v2);
		}
		
		public String toString() {
			return "(" + v1 + "," + v2 + ")";
		}
	}
	
	int noVertices;
	public DetVertex [] verts;
	public Vertex [] vertices;
	public HashMap<String,Triangle> triangles;
	HashMap<String, Edge> edges;
	
	public void setNoOfVertices(int n) {
		this.noVertices = n;
		verts = new DetVertex[n];
		vertices = new Vertex[n];
		vertexCt = 0;
		triangles = new HashMap<String,Triangle>(3*n);
		edges = new HashMap<String, Edge>(2*n);
	}
	
	int vertexCt = 0;
	
	public void addVertex(float x, float y, float z, float fn) {
		// use x,y,z if required
		vertices[vertexCt] = new Vertex();
		vertices[vertexCt].fn = fn;
		
		verts[vertexCt] = new DetVertex();
		verts[vertexCt].x = x;
		verts[vertexCt].y = y;
		verts[vertexCt].z = z;
		verts[vertexCt].no = vertexCt;
		
		vertexCt ++;
	}
	
	private void addNewTriangle(int v1, int v2, int v3) {
		String h = "(" + v1 + " " + v2 + " " + v3 + ")";
		Triangle t1 = triangles.get(h);
		if(t1 == null) {
			t1 = new Triangle();
			t1.v1 = v1;
			t1.v2 = v2;
			t1.v3 = v3;
			t1.h = h.hashCode();
			triangles.put(h, t1);
			addTriangle(t1,0);
			addTriangle(t1,1);
			addTriangle(t1,2);
		} else {
			Triangle tt = new Triangle();
			tt.v1 = v1;
			tt.v2 = v2;
			tt.v3 = v3;
			
			if(!t1.equals(tt)) {
				tt.h = h.hashCode();
				triangles.put(h, tt);
				addTriangle(tt,0);
				addTriangle(tt,1);
				addTriangle(tt,2);
			}
		}
	}
	
	/**
	 * To be used in case of d-Manifolds, d >= 3
	 * @param v1 - Vertex 1, number indicating the order in which addVertex() was called with this vertex.
	 * @param v2 - Vertex 2 of the tetrahedra 
	 * @param v3 - Vertex 3 of the tetrahedra
	 * @param v4 - Vertex 4 of the tetrahedra
	 */
	public void addTetraHedra(int v1,int v2,int v3,int v4) {
		ArrayList<Integer> vv = new ArrayList<Integer>(4);
		vv.add(v1);
		vv.add(v2);
		vv.add(v3);
		vv.add(v4);
		
		Collections.sort(vv,comp);
		v1 = vv.get(0);
		v2 = vv.get(1);
		v3 = vv.get(2);
		v4 = vv.get(3);
		
		addNewTriangle(v1, v2, v3);
		addNewTriangle(v1, v2, v4);
		addNewTriangle(v1, v3, v4);
		addNewTriangle(v2, v3, v4);
	}
	
	private static final int PREV = 0;
	private static final int NEXT = 1;
	
	private void addTriangle(Triangle t,int e) {
		String eno;
		int  v1,v2,v3;
		if(e == 0) {
			eno = "(" + t.v1 + "," + t.v2 + ")";
			v1 = t.v1;
			v2 = t.v2;
			v3 = t.v3;
		} else if(e == 1) {
			eno = "(" + t.v2 + "," + t.v3 + ")";
			v1 = t.v2;
			v2 = t.v3;
			v3 = t.v1;
		} else {
			eno = "(" + t.v1 + "," + t.v3 + ")";
			v1 = t.v1;
			v2 = t.v3;
			v3 = t.v2;
		}
		
		vertices[v1].star.add(t);
		vertices[v2].star.add(t);
		vertices[v3].star.add(t);

		Edge ed = edges.get(eno);
		if(ed == null) {
			ed = new Edge();
			ed.v1 = v1;
			ed.v2 = v2;
			ed.first = t;
			ed.cur = t;
			edges.put(eno, ed);
		} else {
			Triangle tt = ed.cur;
			ed.cur = t;
			t.adjTriangle[e][PREV] = tt;
			int cure = getEdgeNo(tt, v1, v2);
			tt.adjTriangle[cure][NEXT] = t;
		}
	}
	
	public void setupTriangles() {
		Set<String> keys = edges.keySet();
		for(Iterator<String> it = keys.iterator();it.hasNext();) {
			String d = it.next();
			Edge e = edges.get(d);
			
			Triangle t1 = e.cur;
			Triangle t2 = e.first;
			int e1 = getEdgeNo(t1, e.v1, e.v2);
			int e2 = getEdgeNo(t2, e.v1, e.v2);
			t1.adjTriangle[e1][NEXT] = t2;
			t2.adjTriangle[e2][PREV] = t1;
		}
		edges = null;
	}
	
	public void cleanUp() {
		verts = null;
		triangles = null;
	}

	public int getEdgeNo(Triangle t1,int v1,int v2) {
		int e = 0;
		if(t1.v1 != v1 && t1.v1 != v2) {
			e = 1;
		} else if(t1.v2 != v1 && t1.v2 != v2) {
			e = 2;
		} else if(t1.v3 != v1 && t1.v3 != v2) {
			e = 0;
		} else {
			er("There's been an error");
		}
		
		return e;
	}
	/**
	 * To be used in case of 2-Manifolds
	 * @param v1 - Vertex 1, number indicating the order in which addVertex() was called with this vertex.
	 * @param v2 - Vertex 2 of the triangle 
	 * @param v3 - Vertex 3 of the triangle
	 */
	public void addTriangle(int vv1,int vv2,int vv3) {
		ArrayList<Integer> vv = new ArrayList<Integer>();
		vv.add(vv1);
		vv.add(vv2);
		vv.add(vv3);
		
		Collections.sort(vv,comp);
		int v1 = vv.get(0);
		int v2 = vv.get(1);
		int v3 = vv.get(2);
		
		addNewTriangle(v1, v2, v3);
	}
	
	public class MyComp implements Comparator<Integer> {
		
		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Integer o1, Integer o2) {
			if(vertices[o1].fn < vertices[o2].fn) {
				return -1;
			} else if(vertices[o1].fn > vertices[o2].fn) {
				return 1;
			}
			return o1 - o2;
		}
		
	}
	
	
	public MyComp comp = new MyComp();
	
	public Edge getEdge(int v1, int v2) {
		String eno = "(" + v1 + "," + v2 + ")";
		return edges.get(eno);
	}
	
	public void loadData(MeshLoader loader, String ftype) {
		try {
			int nv = loader.getVertexCount();
			setNoOfVertices(nv);
			Simplex sim = loader.getNextSimplex();
			while(sim != null) {
				if(sim instanceof meshloader.iisc.vgl.external.types.Vertex) {
					meshloader.iisc.vgl.external.types.Vertex v = (meshloader.iisc.vgl.external.types.Vertex) sim;
					float fn;
					int f = Integer.parseInt(ftype);
					if (f == 0) {
						fn = v.f;
					} else  {
						fn = v.c[f-1];
					}
					addVertex(v.c[0], v.c[1], v.c[2], fn);
				} else if(sim instanceof meshloader.iisc.vgl.external.types.Edge) {
					pr("Simplicial meshes with edges are not yet supported. This edge will be ignored");
				} else if(sim instanceof meshloader.iisc.vgl.external.types.Triangle) {
					// TODO Chk if required vertices are added
					meshloader.iisc.vgl.external.types.Triangle t = (meshloader.iisc.vgl.external.types.Triangle) sim;
					addTriangle(t.v1, t.v2, t.v3);
				}else if(sim instanceof meshloader.iisc.vgl.external.types.Tetrahedron) {
					// TODO Chk if required vertices are added
					meshloader.iisc.vgl.external.types.Tetrahedron t = (meshloader.iisc.vgl.external.types.Tetrahedron) sim;
					addTetraHedra(t.v1, t.v2, t.v3, t.v4);
				} else {
					er("Invalid Simplex");
				}
				sim = loader.getNextSimplex();
			}
			pr("Finished reading data from file. Loading it......");
			setupTriangles();
			pr("Successfully loaded Data");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}
