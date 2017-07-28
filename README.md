libRG - A library to compute Reeb graphs
----------------------------------------
This folder contains the following files / folders:
1. the source file, which contains all the source code, test and configure files for the computation (Detailed information of the src directory is in the next section)
2. pom.xml file, which contains the groupID, artifactID, version, dependencies and plugins.
5. COPYRIGHT notice, license folder and VERSION information

-----------------------------------------
The src folder contains the following files and folders:
1. src/main/java the main source code for computation
2. src/main/resources/loaders.xml contains the paths to the classes for computation. (Please DON'T make any modification on this file)
3. src/main/resources/input.properties contains information of input file path, input type and etc. For further details, see the notes in the file. (Please alert this file based on your needs)
4. src/test/resources/input the test input file
5. src/test/resources/output the test output file

Computing Reeb graphs
--------------------
Make sure you install **Java**, **Git** and **Apache Maven** on your computer

Usage Instructions:
1. clone this repository into your local machine e.g. `git clone https://github.com/AURIN/libRG.git`
2. open your command line prompt and enter the **libRG** directory
3. compile the project using `mvn compile`, all the class file will be in the target folder
4. set the path of input and output files paths along with other configurations in src/main/resources/input.properties
5. build the jar file using `mvn package`, the jar file should be generated in the target folder
6. in the command line, input `java -cp target/vgl_iisc-1.0.jar reebgraph.iisc.vgl.cmd.ComputeReebGraphCLI`. (Note: the jar file name may vary, please replace with the name you generated in the previous step)
7. the output will be in the specified position

Input
------
The library currently supports the following three formats for the input mesh:

OFF
***
1. Optional first line containing "OFF"
2. Next line specifies the no. of vertices (nv) followed by the number of triangles (nt) (space seperated)
3. The next nv lines contains
   x y z [f]
   where x, y & z specify the co-ordinates of the vertex and f specifies the function value. (If the input type is not f, then the function value is optional)
4. the next nt lines has
   [3] v1 v2 v3
   where v1, v2 and v3 are the vertex indices of the vertices that form the triangles (the 3 is optional)


TET
***
1. First line specifies the no. of vertices (nv) followed by the number of tetrahedrons (nt) (space seperated)
2. The next nv lines contains
   x y z [f]
   where x, y & z specify the co-ordinates of the vertex and f specifies the function value. (If the input type is not 0, then the function value is optional)
3. the next nt lines has
   v1 v2 v3 v4
   where v1, v2, v3 and v4 are the vertex indices of the vertices that form the tetrahedron.


SIM
***
1. First line specifies the dimension (d) of the input
2. The next line specifies the no. of vertices (nv) followed by the number of simplices (ns) (space seperated)
3. The next nv lines contains
   c1 c2 ... cd [f]
   where ci specifies the ith co-ordinate of the vertex and f specifies the function value.
4. The next ns lines has
   (l + 1) v1 v2 ... v{l+1}
   where l is the dimension of the simplex and vi is the index of the ith vertex of the simplex.



In case of any errors
---------------------
If you get a OutOfMemory (java heap) exception, try increasing the memory allocated to the jvm and run again.

