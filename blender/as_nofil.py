# -*- coding: utf-8 -*-

# This is a stripped down, specialised version of the script in render.py, for rendering movies and plotting paper material.

import bpy 
import render
import os,sys
import scipy.io
import numpy as np

import re
import subprocess

# Parameters
growthTimeStep = 540            # [s]
relaxationTimeStep = 0.2        # [s]
fontSize = 1
camWidth = 1920; camHeight = 1080
iterModDiv = 1

argv = sys.argv[sys.argv.index("--")+1:]                    # Get all arguments after -- (Blender won't touch these)
matPath = argv[0];                                          # Get matPath
model = scipy.io.loadmat(matPath, chars_as_strings=True, mat_dtype=False, squeeze_me=False, struct_as_record=False)['model'][0,0]

# Clean up geometry (default cube, camera, light source)
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)

# Set up world
bpy.context.scene.world.horizon_color = (1, 1, 1)           # White background
bpy.context.scene.render.resolution_x = 1920
bpy.context.scene.render.resolution_y = 1080
bpy.context.scene.render.resolution_percentage = 100         # Allows for quick scaling

# Sun
bpy.ops.object.lamp_add(type='SUN', location=(0, 0, 65))    # offset 40 + 50 floc distance / 2
sun = bpy.context.object
sun.data.shadow_method = 'RAY_SHADOW'                           # Sun casts shadow
sun.data.shadow_soft_size = 1.5                                 # Soft shadow, based on distance to light source/plane
sun.data.shadow_ray_samples = 10

# Materials
render.DefineMaterials()
cellMaterial = render.ConfigAS()

#%% Draw cells
for iCell,cell in enumerate(model.cellArray[:,0]):
    cellType = int(cell.type[0,0])

    pos = np.empty([2,3])
    r   = np.empty(2)
    for ii,iBall in enumerate(cell.ballArray[:,0].astype(int)):
        ball        = model.ballArray[iBall,0]
        pos[ii,:]   = ball.pos[:,0] * 1e6
        r[ii]       = ball.radius[0,0] * 1e6
    cellG = render.CreateRod(pos, r, cellMaterial[cellType])
    cellG.name = 'Rod{:d}-{:04d}'.format(cellType, iCell)

#filM = bpy.data.materials['fil']
#for iFil,fil in enumerate(model.filSpringArray[:,0]):
#    pos = np.empty([2,3])
#    for ii,iBall in enumerate(fil.ballArray):
#        ball = model.ballArray[int(iBall),0]
#        pos[ii,:]   = ball.pos[:,0] * 1e6
#    filG = render.CreateSpring(pos, 0.1, filM.name)
#    filG.name = 'Fil-{:04d}'.format(int(iFil))

stickM = bpy.data.materials['stick']
for iStick,stick in enumerate(model.stickSpringArray[:,0]):
    pos = np.empty([2,3])
    for ii,iBall in enumerate(stick.ballArray[:,0]):
        ball = model.ballArray[int(iBall),0]
        pos[ii,:]   = ball.pos[:,0] * 1e6
    stickG = render.CreateSpring(pos, 0.1, stickM.name)
    stickG.name = 'Stick-{:04d}'.format(int(iStick))



# Unselect everything, get ready for playing around
bpy.ops.object.select_all(action='DESELECT')

# Legend, scalebar 
render.SetupXYZLegend(fontSize=fontSize, location=(-33.0, -23.0, -23.0))
render.SetupScalebarLegend(location=(65.0, -33.0, 0.0), fontSize=fontSize, length=10)

# Find out paths
matName = os.path.splitext( matPath.split("/")[-1] )[0]
if "/output/"+matName in matPath:
    simulationPath = matPath[:matPath.index("/output/"+matName)]

# Render persp
render.CameraPerspSetup(location=[25.0, -80.0, 70.0], rotation=[0.8, 0.0, 0])
bpy.data.scenes['Scene'].render.filepath = simulationPath + "/render_persp/" + matName + ".png"
if not os.path.isdir(simulationPath + "/render_persp"):
    os.mkdir(simulationPath + "/render_persp")
render.Render() 

# Annotate persp render
pngPath = bpy.data.scenes['Scene'].render.filepath
growthIter =        int(re.match('g(\d{4})r\d{4}', matName).group(1))
relaxationIter =    int(re.match('g\d{4}r(\d{4})', matName).group(1))
subprocess.call(['convert', '-antialias', '-pointsize', '20', '-font', 'courier-bold', '-annotate', '0x0+30+50',
                 "{:<18} {:>6.2f} h\n{:<18} {:>6.1f} s".format('Growth time:', growthIter*growthTimeStep/3600.0, 'Relaxation time:', relaxationIter*relaxationTimeStep),  
                 pngPath, pngPath]);

# Save
bpy.ops.wm.save_as_mainfile(filepath = simulationPath + "/render_persp/" + matName + ".blend", check_existing=False)    
