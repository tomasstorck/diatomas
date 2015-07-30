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
3. copy the following library files directly into the folder `lib`:
  * download the Apache Commons Math library `commons-math3-*.jar` file, available directly from Apache (download and extract the binaries from http://commons.apache.org/proper/commons-math/download_math.cgi) or via the Arch Linux User repository (as `java-commons-math`).
  * download the MatIO library (https://github.com/gradusnikov/jmatio), compile using Maven and copy the resulting .jar file. On Arch Linux, download and compile using the Arch Linux User Repository (package `java-jmatio`).
  * (optional) copy the COMSOL library files if you cloned this branch. Copy the `*.jar` files in the root of the `plugins` subdirectory.
4. add the library files from step 3 to the Java Build Path (Project > Properties > Java Build Path > Libraries > Add JARs).
5. add JUnit to the Java build path (Project > Properties > Java Build Path > Libraries > Add Library).


Compiling and running
---------------------
The model is compiled well by Eclipse without making changes to the default configuration.

In order to run with COMSOL support, the following VM argument must be included under Run > Run Configurations > Arguments > VM arguments:
```
-Dcs.root=$PATH
```
with the path to the COMSOL folder root substituted for `$PATH` (e.g., `/opt/comsol43a`)

Model parameters and the order in which steps are executed are defined in the `src/ibm/Run*.java` files, therefore files need to be recompiled upon changes. Alternatively, minor changes can be made without recompiling by passing arguments via the command line. For example, to run a simulation for anaerobic oxidation of methane with 36 initial cells, start with program arguments:
```
simulation 4 NCellInit 36
```

A description of the parameters is given in the source code of `src/ibm/Model.java` or via program argument:
```
--help
```

Note that Eclipse will prompt for program arguments on every run if the following string is set as program argument:
```
${string_prompt}
```

Pushing changes
---------------
Feel free to fork the repository on GitHub and submit pull requests, especially for bugfixes and unit tests. Please keep in mind, however, that I am no longer working on this project full-time.
