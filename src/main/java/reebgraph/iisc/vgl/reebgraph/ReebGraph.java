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
import meshloader.iisc.vgl.external.loader.MeshLoader;
import reebgraph.iisc.vgl.reebgraph.TriangleData;
import reebgraph.iisc.vgl.reebgraph.TriangleData.Triangle;
import reebgraph.iisc.vgl.utils.DisjointSets;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ReebGraph implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public static final byte REGULAR = 0;
	public static final byte MINIMUM = 1;
	public static final byte MAXIMUM = 2;
	public static final byte SADDLE = 3;

	class LevelSet {
		HashSet<Triangle> tris = new HashSet<Triangle>();
		HashSet<Triangle> q = new HashSet<Triangle>();
		Triangle usedTri; 
		boolean used = false;
		int cno;
	}
	
	public class CriticalPoint implements Comparable<CriticalPoint> {
		public HashMap<Integer, Components> star = new HashMap<Integer, Components>();
		public HashMap<Integer, Components> lstar = new HashMap<Integer, Components>();

		public int vertex;
		public byte type;
		public int ul = 0;
		public int ll = 0;

		LevelSet[] lset;
		public int [] cnos;
		
		public int compareTo(CriticalPoint p) {
			if (data.vertices[vertex].fn < data.vertices[p.vertex].fn) {
				return -1;
			} else if (data.vertices[vertex].fn > data.vertices[p.vertex].fn) {
				return 1;
			} else {
				if (vertex < p.vertex) {
					return -1;
				} else {
					return 1;
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return vertex + ": " + data.vertices[vertex].fn;
		}
	}

	class LaggingPoints {
//		int tar;
		int cno;
		int src;
		
//		Triangle cont;
	}
	
	public class Components  {
		public ArrayList<Triangle> triangles = new ArrayList<Triangle>();
	}

	private TriangleData data;
	private int noVertices;
	public CriticalPoint[] criticalPoints;
	boolean[] criticalPointFlag;

	class ReebGraphEdge {
		int src;
		int tar;

		// ArrayList<Vertex> path;
		ArrayList<Integer> comps = new ArrayList<Integer>();
	}

	ArrayList<ReebGraphEdge> edges = new ArrayList<ReebGraphEdge>();

	class CPList {
		ArrayList<Integer> cps = new ArrayList<Integer>();
	}
	HashMap<Triangle, CPList> triangleMap = new HashMap<Triangle, CPList>();
	
	// for debug purposes
	int max = 0;
	int min = 0;
	int ct = 0;
	int lsize = 0;

	public void computeReebGraph(MeshLoader loader, String ftype) {
		data = new TriangleData();
		data.loadData(loader, ftype);
		noVertices = data.noVertices;
		System.out.println("Vertices : " + noVertices);
		System.out.println("Triangles : " + data.triangles.size());
		data.cleanUp();
		long st = System.currentTimeMillis();		
		setupCriticalPoints();
		computeReebGraph();
		long en = System.currentTimeMillis();
		System.out.println("Time taken to compute Reeb graph : " + (en - st) + " ms");
	}

	public void setData(TriangleData data) {
		this.data = data;
		noVertices = data.noVertices;
	}
	
	/**
	 * This function is to be called in order to find the critical points of the
	 * input data and store it for further usage
	 */
	public void setupCriticalPoints() {
		int ct = 0;
		ArrayList<CriticalPoint> cp = new ArrayList<CriticalPoint>();
		DisjointSets llink = new DisjointSets();
		DisjointSets ulink = new DisjointSets();
		HashSet<Integer> lsets = new HashSet<Integer>();
		HashSet<Integer> usets = new HashSet<Integer>();
		HashMap<Double, Integer> edges = new HashMap<Double, Integer>();
		for (int i = 0; i < noVertices; i++) {
			if (data.vertices[i].star == null) {
				// isolated vertex
				continue;
			}
			CriticalPoint pt = criticalPoint(i, llink, ulink, lsets, usets, edges);
			if (pt != null) {
				cp.add(pt);
				ct++;
			}
		}
		criticalPoints = cp.toArray(new CriticalPoint[0]);
		System.out.println("No. of potential critical points : " + criticalPoints.length);
	}

	public CriticalPoint criticalPoint(int i, DisjointSets llink, DisjointSets ulink, HashSet<Integer> lsets, HashSet<Integer> usets,
			HashMap<Double, Integer> edges) {
		ct = 0;
		llink.clear();
		ulink.clear();
		lsets.clear();
		usets.clear();
		edges.clear();
		HashMap<Integer, Components> comp = new HashMap<Integer, Components>();
		HashMap<Integer, Components> lcomp = new HashMap<Integer, Components>();
		setupLinks(llink, ulink, data.vertices[i].star, i, edges);
		countComponents(llink, ulink, data.vertices[i].star, i, lsets, usets, edges, comp, lcomp);
		if (lsets.size() == 1 && usets.size() == 1) {
			return null;
		}
		if (lsets.size() == 0 && usets.size() == 0) {
			return null;
		}

		CriticalPoint cp = new CriticalPoint();
		cp.star = comp;
		cp.lstar = lcomp;
		cp.vertex = i;
		cp.type = SADDLE;
		if (lsets.size() == 0) {
			min++;
			cp.type = MINIMUM;
		}
		if (usets.size() == 0) {
			max++;
			cp.type = MAXIMUM;
		}
		cp.ll = lsets.size();
		cp.ul = usets.size();

		return cp;
	}

	private void setupLinks(DisjointSets llink, DisjointSets ulink, HashSet<Triangle> star, int v, HashMap<Double, Integer> edges) {
		for (Iterator<Triangle> it = star.iterator(); it.hasNext();) {
			Triangle t = it.next();
			if (t == null) {
				continue;
			}
			double d = t.v1 * noVertices + t.v2;
			Integer i = edges.get(d);
			if (i == null) {
				edges.put(d, ct);
				i = ct;
				ct++;
			}
			int v1 = i;

			d = t.v2 * noVertices + t.v3;
			i = edges.get(d);
			if (i == null) {
				edges.put(d, ct);
				i = ct;
				ct++;
			}
			int v2 = i;

			d = t.v1 * noVertices + t.v3;
			i = edges.get(d);
			if (i == null) {
				edges.put(d, ct);
				i = ct;
				ct++;
			}
			int v3 = i;

			if (v == t.v1) {
				ulink.union(ulink.find(v1), ulink.find(v3));
			} else if (v == t.v2) {
				llink.union(llink.find(v1), llink.find(v3));
				ulink.union(ulink.find(v2), ulink.find(v3));
			} else if (v == t.v3) {
				llink.union(llink.find(v2), llink.find(v3));
			} else {
				er("Vertex not part of triangle its supposed to be in!!");
			}
		}
	}

	private void countComponents(DisjointSets llink, DisjointSets ulink, HashSet<Triangle> star, int v, HashSet<Integer> lsets,
			HashSet<Integer> usets, HashMap<Double, Integer> edges, HashMap<Integer, Components> comp, HashMap<Integer, Components> lcomp) {
		for (Iterator<Triangle> it = star.iterator(); it.hasNext();) {
			Triangle t = it.next();
			if (t == null) {
				continue;
			}

			double d = t.v1 * noVertices + t.v2;
			Integer i = edges.get(d);
			if (i == null) {
				edges.put(d, ct);
				ct++;
				i = ct;
			}
			int v1 = i;

			d = t.v2 * noVertices + t.v3;
			i = edges.get(d);
			if (i == null) {
				edges.put(d, ct);
				ct++;
				i = ct;
			}
			int v2 = i;

			d = t.v1 * noVertices + t.v3;
			i = edges.get(d);
			if (i == null) {
				edges.put(d, ct);
				ct++;
				i = ct;
			}
			int v3 = i;

			if (v == t.v1) {
				int cno = ulink.find(v1);
				if (cno != ulink.find(v3)) {
					er("Endpoints of an edge are not in the same component!!");
				}
				usets.add(cno);
				Components cmp = comp.get(cno);
				if (cmp == null) {
					cmp = new Components();
					comp.put(cno, cmp);
				}
				cmp.triangles.add(t);
			} else if (v == t.v2) {
				int cno = llink.find(v1);
				lsets.add(cno);
				if (cno != llink.find(v3)) {
					er("Endpoints of an edge are not in the same component!!");
				}
				Components lcmp = lcomp.get(cno);
				if (lcmp == null) {
					lcmp = new Components();
					lcomp.put(cno, lcmp);
				}
				lcmp.triangles.add(t);

				cno = ulink.find(v2);
				if (cno != ulink.find(v3)) {
					er("Endpoints of an edge are not in the same component!!");
				}

				usets.add(cno);
				Components cmp = comp.get(cno);
				if (cmp == null) {
					cmp = new Components();
					comp.put(cno, cmp);
				}
				cmp.triangles.add(t);
			} else if (v == t.v3) {
				int cno = llink.find(v2);
				if (cno != llink.find(v3)) {
					er("Endpoints of an edge are not in the same component!!");
				}
				lsets.add(cno);

				Components lcmp = lcomp.get(cno);
				if (lcmp == null) {
					lcmp = new Components();
					lcomp.put(cno, lcmp);
				}
				lcmp.triangles.add(t);
			} else {
				er("Vertex not part of triangle its supposed to be in!!");
			}
		}
	}
	
	ArrayList<LaggingPoints> laggingPoints;
	int tot;
	private void computeReebGraph() {
		int start = 0;
		boolean flag = true;
		laggingPoints = new ArrayList<LaggingPoints>();
		Arrays.sort(criticalPoints);
		criticalPointFlag = new boolean[noVertices];

		tot = criticalPoints.length / 100;
		if (tot == 0) {
			tot = 1;
		}
		ct = 0;
		if(granularity == -1) {
			granularity = criticalPoints.length;
		}
		while(flag) {
			int end = start + granularity;
			if(end >= criticalPoints.length) {
				end = criticalPoints.length;
				flag = false;
			}
			findCriticalLevelSets(start, end);
			connectCriticalPoints(start, end);
			releaseCriticalLevelSets(start, end);
			start = end;
		}
		if(laggingPoints.size() > 0) {
			er("points are still lagging!!");
		}
		System.out.println("No. of cps removed : " + ct);
	}
	
	private void releaseCriticalLevelSets(int start, int end) {
		for(int i = start;i < end;i ++) {
			if(criticalPoints[i] != null) {
				criticalPoints[i].lset = null;
			}
		}
		triangleMap = new HashMap<Triangle, CPList>();
	}

	boolean computingMaximum = false;
	
	private void findCriticalLevelSets(int start, int end) {
		for (int i = start; i < end; i++) {
			if (i % tot == 0) {
				System.out.println(i / tot);
			}
			if (criticalPoints[i].type == SADDLE) {
				if (criticalPoints[i].ll > 1) {
					Set<Integer> cnos = criticalPoints[i].lstar.keySet();
					LevelSet[] tmp = new LevelSet[cnos.size()];
					int x = 0;
					for (Iterator<Integer> it = cnos.iterator(); it.hasNext();) {
						int cno = it.next();
						Components comp = criticalPoints[i].lstar.get(cno);
						tmp[x] = new LevelSet();
						tmp[x].q.addAll(comp.triangles);
						tmp[x].cno = cno;
						x++;
					}
					HashSet<Triangle> used = new HashSet<Triangle>();
					HashSet<Triangle> qq = new HashSet<Triangle>();
					HashSet<Triangle> flag = new HashSet<Triangle>();
					DisjointSets dj = new DisjointSets();
					for (int j = 0; j < tmp.length; j++) {
						used.clear();
						qq.clear();
						flag.clear();
						HashSet<Integer> nocomp = new HashSet<Integer>();
						for (int xx = 0; xx < tmp.length; xx++) {
							if (dj.find(tmp[xx].cno) != dj.find(tmp[j].cno)) {
								used.addAll(tmp[xx].q);
								used.addAll(tmp[xx].tris);
							} else {
								qq.addAll(tmp[xx].q);
								flag.addAll(tmp[xx].tris);
							}
							nocomp.add(dj.find(tmp[xx].cno));
						}
						if (nocomp.size() == 1) {
							criticalPoints[i].ll = 1;
							criticalPoints[i].lset = tmp;
							break;
						}
						
						LevelSet lset = getLLevelSet(qq, i, used, flag);
						lset.cno = tmp[j].cno;
						tmp[j] = lset;
						if (lset.used) {
							for (int xx = 0; xx < tmp.length; xx++) {
								if (xx != j) {
									if (tmp[xx].tris.contains(lset.usedTri) || tmp[xx].q.contains(lset.usedTri)) {
										dj.union(dj.find(tmp[j].cno), dj.find(tmp[xx].cno));
									}
								}
							}
						}
					}

					HashSet<Integer> nocomp = new HashSet<Integer>();
					for (int xx = 0; xx < tmp.length; xx++) {
						nocomp.add(dj.find(tmp[xx].cno));
					}
					LevelSet[] lset = new LevelSet[nocomp.size()];
					x = 0;
					criticalPoints[i].ll = lset.length;
					for (Iterator<Integer> it = nocomp.iterator(); it.hasNext();) {
						int cmp = it.next();
						lset[x] = new LevelSet();
						for (int xx = 0; xx < tmp.length; xx++) {
							if (dj.find(tmp[xx].cno) == cmp) {
								lset[x].tris.addAll(tmp[xx].tris);
								lset[x].q.addAll(tmp[xx].q);
							}
						}
						x ++;
					}
					criticalPoints[i].lset = lset;
				}

				if (criticalPoints[i].ul > 1) {
					Set<Integer> cnos = criticalPoints[i].star.keySet();
					LevelSet[] tmp = new LevelSet[cnos.size()];
					int x = 0;
					for (Iterator<Integer> it = cnos.iterator(); it.hasNext();) {
						int cno = it.next();
						Components comp = criticalPoints[i].star.get(cno);
						tmp[x] = new LevelSet();
						tmp[x].q.addAll(comp.triangles);
						tmp[x].cno = cno;
						x++;
					}
					HashSet<Triangle> used = new HashSet<Triangle>();
					HashSet<Triangle> qq = new HashSet<Triangle>();
					HashSet<Triangle> flag = new HashSet<Triangle>();
					DisjointSets dj = new DisjointSets();
					for (int j = 0; j < tmp.length; j++) {
						used.clear();
						qq.clear();
						flag.clear();
						HashSet<Integer> nocomp = new HashSet<Integer>();
						for (int xx = 0; xx < tmp.length; xx++) {
							if (dj.find(tmp[xx].cno) != dj.find(tmp[j].cno)) {
								used.addAll(tmp[xx].q);
								used.addAll(tmp[xx].tris);
							} else {
								qq.addAll(tmp[xx].q);
								flag.addAll(tmp[xx].tris);
							}
							nocomp.add(dj.find(tmp[xx].cno));
						}
						if (nocomp.size() == 1) {
							criticalPoints[i].ul = 1;
							break;
						}
						
						LevelSet lset = getULevelSet(qq, i, used, flag);
						if (!lset.used) {
							lset.cno = tmp[j].cno;
							tmp[j] = lset;
						} else {
							for (int xx = 0; xx < tmp.length; xx++) {
								if (xx != j) {
									if (tmp[xx].tris.contains(lset.usedTri) || tmp[xx].q.contains(lset.usedTri)) {
										dj.union(dj.find(tmp[j].cno), dj.find(tmp[xx].cno));
									}
								}
							}
						}
					}

					HashSet<Integer> nocomp = new HashSet<Integer>();
					for (int xx = 0; xx < tmp.length; xx++) {
						nocomp.add(dj.find(tmp[xx].cno));
					}
					criticalPoints[i].ul = nocomp.size();
				}

				if (criticalPoints[i].ul == 1 && criticalPoints[i].ll == 1) {
					criticalPoints[i] = null;
					ct++;
				} else {
					if (criticalPoints[i].ll > 1) {
						for (int x = 0; x < criticalPoints[i].ll; x++) {
							criticalPoints[i].lset[x] = getCriticalLevelSet(criticalPoints[i].lset[x].q, i, criticalPoints[i].lset[x].tris);
						}
					} else if (criticalPoints[i].ll == 1) {
						if (criticalPoints[i].lset == null) {
							criticalPoints[i].lset = new LevelSet[1];
							HashSet<Triangle> comp = new HashSet<Triangle>();
							comp.addAll(criticalPoints[i].lstar.values().iterator().next().triangles);
							criticalPoints[i].lset[0] = getCriticalLevelSet(comp, i, new HashSet<Triangle>());
						} else {
							criticalPoints[i].lset[0] = getCriticalLevelSet(criticalPoints[i].lset[0].q, i, criticalPoints[i].lset[0].tris);
						}
					}
				}
			} else if(criticalPoints[i].type == MAXIMUM) {
				computingMaximum = true;
				if (criticalPoints[i].ll > 1) {
					Set<Integer> cnos = criticalPoints[i].lstar.keySet();
					LevelSet[] tmp = new LevelSet[cnos.size()];
					int x = 0;
					for (Iterator<Integer> it = cnos.iterator(); it.hasNext();) {
						int cno = it.next();
						Components comp = criticalPoints[i].lstar.get(cno);
						tmp[x] = new LevelSet();
						tmp[x].q.addAll(comp.triangles);
						tmp[x].cno = cno;
						x++;
					}
					HashSet<Triangle> used = new HashSet<Triangle>();
					HashSet<Triangle> qq = new HashSet<Triangle>();
					HashSet<Triangle> flag = new HashSet<Triangle>();
					DisjointSets dj = new DisjointSets();
					for (int j = 0; j < tmp.length; j++) {
						used.clear();
						qq.clear();
						flag.clear();
						HashSet<Integer> nocomp = new HashSet<Integer>();
						for (int xx = 0; xx < tmp.length; xx++) {
							if (dj.find(tmp[xx].cno) != dj.find(tmp[j].cno)) {
								used.addAll(tmp[xx].q);
								used.addAll(tmp[xx].tris);
							} else {
								qq.addAll(tmp[xx].q);
								flag.addAll(tmp[xx].tris);
							}
							nocomp.add(dj.find(tmp[xx].cno));
						}
						if (nocomp.size() == 1) {
							criticalPoints[i].ll = 1;
							criticalPoints[i].lset = tmp;
							break;
						}
						
						LevelSet lset = getLLevelSet(qq, i, used, flag);
						if (!lset.used) {
							lset.cno = tmp[j].cno;
							tmp[j] = lset;
						} else {
							for (int xx = 0; xx < tmp.length; xx++) {
								if (xx != j) {
									if (tmp[xx].tris.contains(lset.usedTri) || tmp[xx].q.contains(lset.usedTri)) {
										dj.union(dj.find(tmp[j].cno), dj.find(tmp[xx].cno));
									}
								}
							}
						}
					}

					HashSet<Integer> nocomp = new HashSet<Integer>();
					for (int xx = 0; xx < tmp.length; xx++) {
						nocomp.add(dj.find(tmp[xx].cno));
					}
					LevelSet[] lset = new LevelSet[nocomp.size()];
					x = 0;
					criticalPoints[i].ll = lset.length;
					for (Iterator<Integer> it = nocomp.iterator(); it.hasNext();) {
						int cmp = it.next();
						lset[x] = new LevelSet();
						for (int xx = 0; xx < tmp.length; xx++) {
							if (dj.find(tmp[xx].cno) == cmp) {
								lset[x].tris.addAll(tmp[xx].tris);
								lset[x].q.addAll(tmp[xx].q);
							}
						}
						x ++;
					}
					criticalPoints[i].lset = lset;
				} else {
					criticalPoints[i].lset = new LevelSet[1];
					HashSet<Triangle> comp = new HashSet<Triangle>();
					comp.addAll(criticalPoints[i].lstar.values().iterator().next().triangles);
					criticalPoints[i].lset[0] = getCriticalLevelSet(comp, i, new HashSet<Triangle>());
				}
				computingMaximum = false;
			}
			
			if(criticalPoints[i] != null) {
				criticalPoints[i].cnos = new int[criticalPoints[i].ll];
				for(int x = 0;x < criticalPoints[i].ll;x ++) {
					criticalPoints[i].cnos[x] = -1;
					
					for(Iterator<Triangle> iit = criticalPoints[i].lset[x].tris.iterator();iit.hasNext();) {
						Triangle t = iit.next();
						CPList cp = triangleMap.get(t);
						if(cp == null) {
							cp = new CPList();
							triangleMap.put(t,cp);
						}
						cp.cps.add(i);
					}
				}
				criticalPointFlag[criticalPoints[i].vertex] = true;
			}
		}
	}

	boolean isGreater(int v1, int v2) {
		if (data.vertices[v1].fn > data.vertices[v2].fn || data.vertices[v1].fn == data.vertices[v2].fn && v1 > v2) {
			return true;
		}
		return false;
	}

	private LevelSet getLLevelSet(HashSet<Triangle> comp, int i, HashSet<Triangle> used, HashSet<Triangle> flag) {
		int v = criticalPoints[i].vertex;
		LevelSet set = new LevelSet();
		ArrayList<Triangle> q = new ArrayList<Triangle>();
		HashSet<Triangle> qq = new HashSet<Triangle>();

		for(Iterator<Triangle> it = comp.iterator();it.hasNext();) {
			Triangle t = it.next();
			q.add(t);
			qq.add(t);
		}
		set.tris.addAll(flag);
		while (q.size() != 0) {
			Triangle t = q.get(0);
			
//			if(computingMaximum) {
//				CPList cp = triangleMap.get(t);
//				if(cp == null) {
//					cp = new CPList();
//					triangleMap.put(t,cp);
//				}
//				cp.cps.add(i);
//			}

			set.tris.add(t);
			if (used.contains(t)) {
				set.used = true;
				set.usedTri = t;
				set.q = qq;
				return set;
			}
			q.remove(0);
			qq.remove(t);

			flag.add(t);

			if (!isGreater(v, t.v2)) {
				// v1 v2 and v1 v3
				Triangle t1 = t.adjTriangle[0][0];
				if (t1 != null && !flag.contains(t1) && !qq.contains(t1)) {
					q.add(t1);
					qq.add(t1);
				}

				Triangle t2 = t.adjTriangle[2][0];
				if (t2 != null && !flag.contains(t2) && !qq.contains(t2)) {
					q.add(t2);
					qq.add(t2);
				}

				t1 = t.adjTriangle[0][1];
				if (t1 != null && !flag.contains(t1) && !qq.contains(t1)) {
					q.add(t1);
					qq.add(t1);
				}

				t2 = t.adjTriangle[2][1];
				if (t2 != null && !flag.contains(t2) && !qq.contains(t2)) {
					q.add(t2);
					qq.add(t2);
				}
			} else {
				// v2 v3 and v1 v3
				Triangle t1 = t.adjTriangle[1][0];
				if (t1 != null && !flag.contains(t1) && !qq.contains(t1)) {
					q.add(t1);
					qq.add(t1);
				}

				Triangle t2 = t.adjTriangle[2][0];
				if (t2 != null && !flag.contains(t2) && !qq.contains(t2)) {
					q.add(t2);
					qq.add(t2);
				}

				t1 = t.adjTriangle[1][1];;
				if (t1 != null && !flag.contains(t1) && !qq.contains(t1)) {
					q.add(t1);
					qq.add(t1);
				}
				t2 = t.adjTriangle[2][1];
				if (t2 != null && !flag.contains(t2) && !qq.contains(t2)) {
					q.add(t2);
					qq.add(t2);
				}
			}
		}
		return set;
	}

	private LevelSet getULevelSet(HashSet<Triangle> comp, int i, HashSet<Triangle> used, HashSet<Triangle> flag) {
		int v = criticalPoints[i].vertex;
		LevelSet set = new LevelSet();
		ArrayList<Triangle> q = new ArrayList<Triangle>();
		HashSet<Triangle> qq = new HashSet<Triangle>();

		for(Iterator<Triangle> it = comp.iterator();it.hasNext();) {
			Triangle t = it.next();
			q.add(t);
			qq.add(t);
		}
		set.tris.addAll(flag);
		while (q.size() != 0) {
			Triangle t = q.get(0);
			set.tris.add(t);
			if (used.contains(t)) {
				set.used = true;
				set.usedTri = t;
				set.q = qq;
				return set;
			}
			q.remove(0);
			qq.remove(t);

			flag.add(t);

			if (isGreater(t.v2, v)) {
				// v1 v2 and v1 v3
				Triangle t1 = t.adjTriangle[0][0];
				if (t1 != null && !flag.contains(t1) && !qq.contains(t1)) {
					q.add(t1);
					qq.add(t1);
				}

				Triangle t2 = t.adjTriangle[2][0];
				if (t2 != null && !flag.contains(t2) && !qq.contains(t2)) {
					q.add(t2);
					qq.add(t2);
				}

				t1 = t.adjTriangle[0][1];
				if (t1 != null && !flag.contains(t1) && !qq.contains(t1)) {
					q.add(t1);
					qq.add(t1);
				}

				t2 = t.adjTriangle[2][1];
				if (t2 != null && !flag.contains(t2) && !qq.contains(t2)) {
					q.add(t2);
					qq.add(t2);
				}
			} else {
				// v2 v3 and v1 v3
				Triangle t1 = t.adjTriangle[1][0];
				if (t1 != null && !flag.contains(t1) && !qq.contains(t1)) {
					q.add(t1);
					qq.add(t1);
				}

				Triangle t2 = t.adjTriangle[2][0];
				if (t2 != null && !flag.contains(t2) && !qq.contains(t2)) {
					q.add(t2);
					qq.add(t2);
				}

				t1 = t.adjTriangle[1][1];;
				if (t1 != null && !flag.contains(t1) && !qq.contains(t1)) {
					q.add(t1);
					qq.add(t1);
				}
				t2 = t.adjTriangle[2][1];
				if (t2 != null && !flag.contains(t2) && !qq.contains(t2)) {
					q.add(t2);
					qq.add(t2);
				}
			}
		}
		return set;
	}
	
	private LevelSet getCriticalLevelSet(HashSet<Triangle> comp, int i, HashSet<Triangle> flag) {
		int v = criticalPoints[i].vertex;
		LevelSet set = new LevelSet();
		ArrayList<Triangle> q = new ArrayList<Triangle>();
		HashSet<Triangle> qq = new HashSet<Triangle>();

		for(Iterator<Triangle> it = comp.iterator();it.hasNext();) {
			Triangle t = it.next();
			q.add(t);
			qq.add(t);
		}
		
//		set.tris.addAll(flag);
		for(Iterator<Triangle> it = flag.iterator();it.hasNext();) {
			Triangle t = it.next();
			set.tris.add(t);
			
//			CPList cp = triangleMap.get(t);
//			if(cp == null) {
//				cp = new CPList();
//				triangleMap.put(t,cp);
//			}
//			cp.cps.add(i);
		}

		while (q.size() != 0) {
			Triangle t = q.get(0);
			
//			CPList cp = triangleMap.get(t);
//			if(cp == null) {
//				cp = new CPList();
//				triangleMap.put(t,cp);
//			}
//			cp.cps.add(i);
			
			set.tris.add(t);
			q.remove(0);
			qq.remove(t);

			flag.add(t);

			if (!isGreater(v, t.v2)) {
				// v1 v2 and v1 v3
				Triangle t1 = t.adjTriangle[0][0];
				if (t1 != null && !flag.contains(t1) && !qq.contains(t1)) {
					q.add(t1);
					qq.add(t1);
				}

				Triangle t2 = t.adjTriangle[2][0];
				if (t2 != null && !flag.contains(t2) && !qq.contains(t2)) {
					q.add(t2);
					qq.add(t2);
				}

				t1 = t.adjTriangle[0][1];
				if (t1 != null && !flag.contains(t1) && !qq.contains(t1)) {
					q.add(t1);
					qq.add(t1);
				}

				t2 = t.adjTriangle[2][1];
				if (t2 != null && !flag.contains(t2) && !qq.contains(t2)) {
					q.add(t2);
					qq.add(t2);
				}
			} else {
				// v2 v3 and v1 v3
				Triangle t1 = t.adjTriangle[1][0];
				if (t1 != null && !flag.contains(t1) && !qq.contains(t1)) {
					q.add(t1);
					qq.add(t1);
				}

				Triangle t2 = t.adjTriangle[2][0];
				if (t2 != null && !flag.contains(t2) && !qq.contains(t2)) {
					q.add(t2);
					qq.add(t2);
				}

				t1 = t.adjTriangle[1][1];;
				if (t1 != null && !flag.contains(t1) && !qq.contains(t1)) {
					q.add(t1);
					qq.add(t1);
				}
				t2 = t.adjTriangle[2][1];
				if (t2 != null && !flag.contains(t2) && !qq.contains(t2)) {
					q.add(t2);
					qq.add(t2);
				}
			}
		}
		return set;
	}

	int lastCp = -1;
	private void connectCriticalPoints(int start, int end) {
		for(int i = end-1;i >= start;i --) {
			if(criticalPoints[i] != null) {
				lastCp= i;
				break;
			}
		}
		// first compute for lagging points
		ArrayList<LaggingPoints> updatedLagQueue = new ArrayList<LaggingPoints>();
		for(Iterator<LaggingPoints> lag = laggingPoints.iterator();lag.hasNext();) {
			LaggingPoints lp = lag.next();
			boolean connected = connectCriticalPoint(lp, start -1, end, criticalPoints[lp.src].star.get(lp.cno).triangles.get(0));
			if(!connected) {
				updatedLagQueue.add(lp);
			}
		}
		laggingPoints = updatedLagQueue;
		
		for(int i = start;i < end;i ++) {
			if(criticalPoints[i] == null) {
				continue;
			}
			if(criticalPoints[i].type != MAXIMUM) {
				HashMap<Integer,Components> star = criticalPoints[i].star;
				Set<Integer> cmps = star.keySet();
				for(Iterator<Integer> it = cmps.iterator();it.hasNext();) {
					LaggingPoints lp = new LaggingPoints();
					lp.src = i;
					lp.cno = it.next();
					Components comps = star.get(lp.cno);
//					lp.cont = comps.triangles.get(0);
					boolean connected = connectCriticalPoint(lp, i, end, comps.triangles.get(0));
					if(!connected) {
						updatedLagQueue.add(lp);
					}
				}
			}
		}
	}

	private boolean connectCriticalPoint(LaggingPoints lp, int st, int end, Triangle start) {
//		Triangle start = lp.cont;
		int src = lp.src;
		int tar = st;
		int cno = lp.cno;
		while(start != null) {
			boolean flag = false;
			while(!flag) {
				tar ++;
				if(tar >= end) {
					break;
				}
				if(criticalPoints[tar] == null) {
					continue;
				}
				if(criticalPoints[tar].type != MINIMUM) {
					flag = true;
				}
			}
			if(tar >= criticalPoints.length) {
				er("no edge from non-maximum");
			}
			if(tar >= end) {
//				lp.tar = tar - 1;
//				lp.cont = start;
				break;
			}
			start = connect(src,cno,tar,start);
			if(start != null && isGreater(start.v3, criticalPoints[lastCp].vertex)) {
				tar = end - 1;
			}
		}
		if(start != null) {
			return false;
		}
		return true;
	}

	int nextTar = -1;
	private Triangle connect(int src, int cno, int tar, Triangle start) {
		int sv = criticalPoints[src].vertex;
		int tv = criticalPoints[tar].vertex;
		
		Triangle t = start;
		while(t != null) {
			if(!isGreater(sv,t.v3)) {
				if(!isGreater(tv,t.v3) && !isGreater(t.v1,tv)) {
					break;
				}
				if(!criticalPointFlag[t.v3]) {
					for(Iterator<Triangle> it = data.vertices[t.v3].star.iterator();it.hasNext();) {
						Triangle tt = it.next();
						if(isGreater(tt.v3, t.v3)) {
							t = tt;
							break;
						}
					}
				} else {
					er("How did it end up with a critical point??");
				}
			}
		}
		if(t == null) {
			er("No path??");
		}
		CPList cp = triangleMap.get(t);
		nextTar = -1;
		if(cp != null) {
			for(Iterator<Integer> it = cp.cps.iterator();it.hasNext();) {
				int tarr = it.next();
				if(tarr > src && !isGreater(criticalPoints[tarr].vertex, t.v3)) {
					nextTar = tarr;
					break;
				}
			}
		}
		if(nextTar == -1) {
			return t;
		} else {
			tar = nextTar;
		}
		for(int i = 0;i < criticalPoints[tar].ll;i ++) {
			if(criticalPoints[tar].lset[i].tris.contains(t)) {
				// reached
				if(criticalPoints[tar].cnos[i] == -1) {
					ReebGraphEdge ed = new ReebGraphEdge();
					ed.src = src;
					ed.tar = tar;
					ed.comps.add(cno);
					criticalPoints[tar].cnos[i] = cno;
					
					edges.add(ed);
				} else {
					boolean added = false;
					for(int j = 0;j < edges.size();j ++) {
						ReebGraphEdge ed = edges.get(j);
						if(ed.src == src && ed.tar == tar) {
							if(ed.comps.contains(criticalPoints[tar].cnos[i])) {
								ed.comps.add(cno);
								added = true;
							}
						}
					}
					if(!added) {
						er("Some problem, chk!!!!");
					}
				}
				return null;
			}
		}
		return t;
	}

	/* Main functionality ends here */

	public void outputReebGraph(String file) {
		if(file == null) {
			return;
		}
		
		CleanReebGraph s = new CleanReebGraph();
		HashMap<Integer, Integer> cpMap = new HashMap<Integer, Integer>();
		for(int i = 0;i < criticalPoints.length;i ++) {
			if(criticalPoints[i] == null) {
				continue;
			}
			cpMap.put(criticalPoints[i].vertex, i);
		}
		try {
			PrintStream p = new PrintStream(new File(file));
			for(int i = 0;i < criticalPoints.length;i ++) {
				if(criticalPoints[i] == null) {
					continue;
				}
				s.addNode(criticalPoints[i].vertex, data.vertices[criticalPoints[i].vertex].fn, criticalPoints[i].type);
			}

			for(Iterator<ReebGraphEdge> it = edges.iterator();it.hasNext();) {
				ReebGraphEdge e = it.next();
				s.addArc(criticalPoints[e.src].vertex, criticalPoints[e.tar].vertex, e.comps);
			}
			
			s.setup();
			s.removeDeg2Nodes();
			
			s.outputReebGraph(p);
			p.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	int granularity = 1000;
	
	public void setGranularity(int granularity) {
		this.granularity = granularity;
	}
	
}
