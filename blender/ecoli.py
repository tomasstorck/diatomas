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
growthTimeStep = 240            # [s]
relaxationTimeStep = 0.2        # [s]
Lx=140; Ly=140; Lz=20       # Anchoring/gliding simulations
#Lx=120; Ly=120; Lz=20      # DLVO simulations
fontSize = 1
camWidth = 1920; camHeight = 1080
distance = camWidth/camHeight*Ly+20
leftMargin = Lx/2-distance/2 + 10.0
rightMargin = Lx/2+distance/2 - 10.0
iterModDiv = 5 

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
bpy.ops.object.lamp_add(type='SUN', location=(0, 0, 40))
sun = bpy.context.object
sun.data.shadow_method = 'RAY_SHADOW'                           # Sun casts shadow
sun.data.shadow_soft_size = 1.5                                 # Soft shadow, based on distance to light source/plane
sun.data.shadow_ray_samples = 10

# Materials
render.DefineMaterials()
whiteM = bpy.data.materials['white']
whiteM.diffuse_color = (0.5, 0.5, 0.5)                  # Grey (substratum)
whiteM.diffuse_shader = 'LAMBERT'
cellMaterial = ['cell3']
surfaceMaterial = 'greyM'

#%% Draw cells
for iCell,cell in enumerate(model.cellArray[:,0]):
    ancestor = cell
    while np.where(model.cellArray[:,0]==ancestor)[0][0] > 3:        # 0 through 3, because there will be 4 colours available
        ancestor = model.cellArray[ancestor.mother[0][0],0]
    cellType = int(np.where(model.cellArray==ancestor)[0][0])

    cellShape = model.shapeX[cell.type[0,0].astype(int),0].astype(int)
    if cellShape == 0:
        iBall = cell.ballArray[0,0].astype(int)
        ball = model.ballArray[iBall,0]
        pos = ball.pos[:,0] * 1e6
        r = ball.radius[0,0] * 1e6
        cellG = render.CreateSphere(pos, r, cellMaterial[cellType])
        cellG.name = 'Sphere{:d}-{:04d}'.format(cellType, iCell)
    else:
        pos = np.empty([2,3])
        r   = np.empty(2)
        for ii,iBall in enumerate(cell.ballArray[:,0].astype(int)):
            ball        = model.ballArray[iBall,0]
            pos[ii,:]   = ball.pos[:,0] * 1e6
            r[ii]       = ball.radius[0,0] * 1e6
        cellG = render.CreateRod(pos, r, cellMaterial[cellType])
        cellG.name = 'Rod{:d}-{:04d}'.format(cellType, iCell)

filM = bpy.data.materials['fil']
for iFil,fil in enumerate(model.filSpringArray[:,0]):
    pos = np.empty([2,3])
    for ii,iBall in enumerate(fil.ballArray):
        ball = model.ballArray[int(iBall),0]
        pos[ii,:]   = ball.pos[:,0] * 1e6
    filG = render.CreateSpring(pos, 0.1, filM.name)
    filG.name = 'Fil-{:04d}'.format(int(iFil))

anchorM = bpy.data.materials['anchor']
for iAnchor,anchor in enumerate(model.anchorSpringArray[:,0]):
    iBall = anchor.ballArray[0,0]
    ball = model.ballArray[int(iBall),0]
    pos   = ball.pos[:,0] * 1e6
    anchorG = render.CreateSpring(np.concatenate([[pos, [pos[0],pos[1],0.0]]], 0), 0.1, anchorM.name)
    anchorG.name = 'Anchor-{:04d}'.format(int(iAnchor))

# Unselect everything, get ready for playing around
bpy.ops.object.select_all(action='DESELECT')

# Plane
render.SetupPlanes(Lx=Lx,Ly=Ly,Lz=Lz, drawPlaneGrid=(False, False, False), radiusZPlane=400)

# Legend, scalebar 
render.SetupXYZLegend(fontSize=fontSize, location=(leftMargin, 0.0, 0.0))
render.SetupScalebarLegend(location=(rightMargin, Ly-10.0, 0.0), fontSize=fontSize, length=10)

# Put cells in desired location
render.Offset([Lx/2,Ly/2,0])

# Find out paths
matName = os.path.splitext( matPath.split("/")[-1] )[0]
if "/output/"+matName in matPath:
    simulationPath = matPath[:matPath.index("/output/"+matName)]

# Render top
render.CameraTopSetup(distance=distance, Lx=Lx, Ly=Ly, Lz=Lz, camWidth=camWidth, camHeight=camHeight)
bpy.data.scenes['Scene'].render.filepath = simulationPath + "/render_top/" + matName + ".png"
if not os.path.isdir(simulationPath + "/render_top"):
    os.mkdir(simulationPath + "/render_top")
render.Render()

# Render side
render.CameraSideSetup(distance=distance, Lx=Lx, Ly=Ly, Lz=Lz, camWidth=camWidth, camHeight=camHeight)
render.RotateX()                                                # Rotate some of the labels for proper side readability
bpy.data.scenes['Scene'].render.filepath = simulationPath + "/render_side/" + matName + ".png"
if not os.path.isdir(simulationPath + "/render_side"):
    os.mkdir(simulationPath + "/render_side")
render.Render()

# Render persp
render.DeleteLegends()
render.CameraSideDisable()
render.SetupScalebarLegend(location=(80,0,0))
render.CameraPerspSetup(location=[-60.0+Lx/2.0, -120+Ly/2.0, 85.0], rotation=[0.96, 0.0, -0.47])
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
