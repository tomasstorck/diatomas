This folder contains the files used for rendering the outcomes using Blender. Any Blender version should work.
The functions require Python 3, numpy, scipy and Blender to be installed

Since late 2014, Blender has been replacing POVRay, because it allows up close, intuitive viewing of the cell configuration. 

Note that you need to add at least the file render.py to the Blender path. For example:
$ sudo ln -s /home/tomas/diatomas/blender/render.py /usr/share/blender/2.73/scripts/modules/render.py 

The file render.py is the main Python script used for Blender and is the library containing the methods used in other files based on it (e.g., ecoli.py).

render.py, ecoli.py etc. need to be run from within Blender. For example:
$  blender --python render.py -- /home/tomas/documenten/modelling/diatomas_symlink /results/ecoli_noanchor_dlvo_newModel2/output/g0100r0500.mat
Other files (e.g., rendermonitor.py, ecolidriver.py) can be run in pure python and will call Blender when needed.  It is generally not needed to manuallly call blender --python ecoli.py etc.

