I have added readme.txt files in the root of some folders to explain the structure of the model where commenting on the code was deemed unfeasible.

An extensive description of this model is given in the paper:
Storck, T., Picioreanu, C., Virdis, B., & Batstone, D. J. (2014). Variable Cell Morphology Approach for Individual-Based Modeling of Microbial Communities. Biophysical Journal, 106(9), 2037–2048. http://doi.org/10.1016/j.bpj.2014.03.015

Eclipse and Git are used for managing this project. Arch Linux and JRE 7 (OpenJDK) have been used primarily for running the code, although other versions of Linux and JRE should work well. Basic support for Windows has been included, but is largely untested and likely will require some debugging.

IMPORTING THE CODE INTO ECLIPSE
1. clone the git repository (https://github.com/tomasstorck/diatomas)
2. import the project into Eclipse as an existing project
3. add the Apache Commons Math library .jar file to the path (Project Properties > Java Build Path > Libraries > Add External JARs)
4. add the JMatIO source folders with .java files to the path (Project Properties > Java Build Path > Source > Link Source)
5. (optional) set up Git repository via Eclipse
6. (optional) add the COMSOL library files if you cloned this branch (similar to 3; files are in COMSOL subfolder plugins):

