I have added README.md files in the root of some folders to explain the structure of the model where commenting on the code was deemed unfeasible.

An extensive description of this model is given in the paper:
Storck, T., Picioreanu, C., Virdis, B., & Batstone, D. J. (2014). Variable Cell Morphology Approach for Individual-Based Modeling of Microbial Communities. Biophysical Journal, 106(9), 2037–2048. http://doi.org/10.1016/j.bpj.2014.03.015

Eclipse and Git are used for managing this project. Arch Linux and JRE 7 (OpenJDK) have been used primarily for running the code, other Unix-based operating systems and JRE versions should work just as well. Basic support for Windows has been included, but is largely untested and likely will require some debugging.

Importing the latest version into Eclipse
-----------------------------------------
1. clone the git repository (https://github.com/tomasstorck/diatomas)
```
git clone https://github.com/tomasstorck/diatomas.git
```
2. create a new project in eclipse and select the folder created in step 1, the defaults will do for now.
3. download the Apache Commons Math library .jar file, available in the Arch Linux User repository (as java-commons-math) or directly from Apache (download the binaries from http://commons.apache.org/proper/commons-math/download_math.cgi). Add this as an external library (Project Properties > Java Build Path > Libraries > Add External JARs).
4. download the MatIO library (https://github.com/gradusnikov/jmatio) and compile using Maven. On Arch Linux, install from the Arch Linux User Repository (as java-jmatio). Add as an external library as described in step 3. 
5. add JUnit to the Java build path (Project Properties > Jva Build Path > Libraries > Add Library).
6. (optional) add the COMSOL library files if you cloned this branch (similar to 3; files are in COMSOL subfolder plugins, add all of them). The model will work fine without this step if you do not use the COMSOL functionalities.

Compiling and running
---------------------
The model is compiled by running the Ant build file (`antjar.xml`). This can easily be done from Eclipse (right-click `antjar.xml` > Run As > Ant Build)

Model parameters and the order in which steps are executed are defined in the `src/ibm/Run*.java` files, therefore files need to be recompiled upon changes. Alternatively, minor changes can be made without recompiling by passing arguments via the command line. For example, to run a simulation for anaerobic oxidation of methane with 36 initial cells, start:
```
java -jar diatomas.jar simulation 4 NCellInit 36
```

A description of the parameters is given in the source code of `src/ibm/Model.java` or via 
```
java -jar diatomas.jar --help
```

Pushing changes
---------------
Feel free to fork the repository on GitHub and submit pull requests, especially for bugfixes and unit tests. Please keep in mind, however, that I am no longer working on this project full-time.
