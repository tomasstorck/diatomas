# -*- coding: utf-8 -*-

# This is a stripped down, specialised version of the script in render.py, for rendering movies and plotting paper material.

import sys
sys.path.append(r'/home/tomas/diatomas/blender') 

import bpy 
import render
import os
import scipy.io
import numpy as np

import re
import subprocess

# Parameters
growthTimeStep = 7*24*3600      # [s]
relaxationTimeStep = 0.2        # [s]
Lx=10; Ly=10; Lz=10
fontSize = 0.25
camWidth = 1920; camHeight = 1080
iterModDiv = 5 
#drawPlanes = True
drawPlanes = False

argv = sys.argv[sys.argv.index("--")+1:]                    # Get all arguments after -- (Blender won't touch these)
matPath = argv[0];                                          # Get matPath
model = scipy.io.loadmat(matPath, chars_as_strings=True, mat_dtype=False, squeeze_me=False, struct_as_record=False)['model'][0,0]

# Throw warning if cells are outside of domain
NBallOutsideDomain = 0
for ball in model.ballArray[:,0]:                       # Must loop row-wise, that's how MATLAB works
    pos = ball.pos[:,0]*1e6
    if not np.all([(np.array([0,0,0]) < pos), (pos < np.array([Lx, Ly, Lz]))]):
        NBallOutsideDomain += 1
if NBallOutsideDomain > 0:
    render.Say("WARNING: {} balls are outside the domain".format(NBallOutsideDomain))
    
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
sun.data.shadow_soft_size = 0.5                                 # Soft shadow, based on distance to light source/plane
sun.data.shadow_ray_samples = 10

# Materials
render.DefineMaterials()
cellMaterial = ['cellAnme', 'cellDss', 'cell1']                 # Java code sets 0 as ANME, 1 as DSS, possibly 2 as S8 (s)
surfaceMaterial = 'white'

#%% Draw cells
for iCell,cell in enumerate(model.cellArray[:,0]):
    cellType = cell.type[0][0].astype(int)
    cellShape = model.shapeX[cellType][0]
    if model.ballArray[cell.ballArray[0,0].astype(int),0].pos[1,0] > 0.0:                                     # Only plot if on far side of aggregate
        if cellShape==0:
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
    
## Draw sticking links
#stickM = bpy.data.materials['stick']
#for iStick,stick in enumerate(model.stickSpringArray[:,0]):
#    pos = np.empty([2,3])
#    for ii,iBall in enumerate(stick.ballArray[:,0]):
#        ball = model.ballArray[int(iBall),0]
#        pos[ii,:]   = ball.pos[:,0] * 1e6
#    stickG = render.CreateSpring(pos, 0.1, stickM.name)
#    stickG.name = 'Stick-{:04d}'.format(int(iStick))

# Unselect everything, get ready for playing around
bpy.ops.object.select_all(action='DESELECT')

# Legend, plane and other support
if drawPlanes:
    render.SetupPlanes(Lx=Lx, Ly=Ly, Lz=Lz, drawPlaneGrid=(False, True, True), radiusZPlane=None, stepSize=2.5)
    tickList = render.SetupTicks(Lx=Lx, Ly=Ly, Lz=Lz, fontSize=fontSize, stepSize=2.5)
    for xTick in tickList[::3]:
        xTick.rotation_euler[0] = 0.5*np.pi
        xTick.location[2] = -1.0
    render.DeleteTick(x=0, z=0)
    textList = render.SetupXYZLegend(fontSize=fontSize, location=(0.0, 0.0, 0.0), textSpacing=0.5)
    # Move x and z legend text into place
    textList[0].rotation_euler[0] = textList[2].rotation_euler[0] = 0.5*np.pi
    textList[0].location[2] = -0.75
    textList[2].location[0] = -0.75
    textList[2].location[2] = 0.5

# Put cells in desired location
render.Offset([Lx/2,Ly/2,Lz/2])

# Find out paths
matName = os.path.splitext( matPath.split("/")[-1] )[0]
if "/output/"+matName in matPath:
    simulationPath = matPath[:matPath.index("/output/"+matName)]

# Camera and scene
if drawPlanes:
    renderFolder = "/render_side/"
else:
    renderFolder = "/render_side_noplanes/"
render.CameraSideSetup(Lx=5.0, Ly=2*-15.0, Lz=2*5.0, distance=22.0)
bpy.data.scenes['Scene'].render.filepath = simulationPath + renderFolder + matName + ".png"
if not os.path.isdir(simulationPath + renderFolder):
    os.mkdir(simulationPath + renderFolder)
render.Render() 

# Annotate render
pngPath = bpy.data.scenes['Scene'].render.filepath
growthIter =        int(re.match('g(\d{4})r\d{4}', matName).group(1))
relaxationIter =    int(re.match('g\d{4}r(\d{4})', matName).group(1))
subprocess.call(['convert', '-antialias', '-pointsize', '20', '-font', 'courier-bold', '-annotate', '0x0+30+50',
                 "{:<18} {:>6.1f} weeks\n{:<18} {:>6.1f} s".format('Growth time:', growthIter*growthTimeStep/3600.0/24/7, 'Relaxation time:', relaxationIter*relaxationTimeStep),  
                 pngPath, pngPath]);

bpy.ops.wm.save_as_mainfile(filepath = simulationPath + renderFolder + matName + ".blend", check_existing=False)    
