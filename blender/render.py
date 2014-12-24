# -*- coding: utf-8 -*-

"""
run using: blender --python render.py
Note that this is coded in Python 3
"""

#%% Imports %
import bpy

import sys, os, time, re

import numpy as np
from numpy.linalg import norm
from numpy import array, pi, reshape

import scipy.io

#%% Function definitions %
def CreateRod(pos, r, offset=array([0,0,0])):
    pos = array(pos)
    LV = pos[1]-pos[0]
    L = norm(LV)
    bpy.ops.mesh.primitive_cylinder_add(depth=L, location=(0, 0, L/2), radius=sum(r)/2)
    spring = bpy.context.object                                 # Ugly but appears to be the way to do it
    bpy.ops.mesh.primitive_uv_sphere_add(location=(0, 0, L), size=r[1])
    ball1 = bpy.context.object
    bpy.ops.mesh.primitive_uv_sphere_add(location=(0, 0, 0), size=r[0])
    ball0 = bpy.context.object
    # Deselect everything, then select objects in the right order (making ball0 the active object)
    bpy.ops.object.select_all(action='DESELECT')
    spring.select = True
    ball1.select = True
    ball0.select = True
    rod = ball0
    # Join meshes, easier to work on entire cell
    bpy.ops.object.join()
    # Apply modifiers and smoothing
    bpy.ops.object.modifier_add(type='EDGE_SPLIT')              # Required to get proper "shinyness"
    bpy.ops.object.shade_smooth()
    # Define vector in XY plane we will revolve along
    rod.rotation_mode = 'AXIS_ANGLE'                # Other rotations are sequential: rotate around X, THEN around y, etc.
    rod.rotation_axis_angle[1] = -1*LV[1]          # X axis (use Y position). Relative, absolute doesn't matter. -1* because we want perpendicular vector
    rod.rotation_axis_angle[2] = LV[0]              # Y axis (use X position)
    # Calculate how much we need to rotate (angle from [0 0 L] to [x y 0] to get vector [0 0 L] to overlay [x y z])
    rotationRad = np.arccos(LV[2]/L)
    rod.rotation_axis_angle[0] = rotationRad     # W (amount of rotation around defined axis)
    # Displace. Since selected object is still ball0 (and this is at the origin), displace to correct position for this one.
    rod.location = pos[0,:]+offset
    return rod                                                # Returns entire, merged cell

def CreateSphere(pos, r, offset=array([0,0,0])):
    pos = array(pos)
    r = r
    bpy.ops.mesh.primitive_uv_sphere_add(location=pos+offset, size=r)
    ball0 = bpy.context.object
    bpy.ops.object.shade_smooth()
    return ball0

def CreateSpring(pos, r, offset=array([0,0,0])):
    pos = array(pos)
    LV = pos[1]-pos[0]
    L = norm(LV)
    bpy.ops.mesh.primitive_cylinder_add(depth=L, location=(0, 0, L/2), radius=0.15)
    cyl = bpy.context.object
    bpy.ops.object.shade_smooth()
    # Define vector in XY plane we will revolve along (note: see CreateRod for method)
    cyl.rotation_mode = 'AXIS_ANGLE'
    cyl.rotation_axis_angle[1] = -1*LV[1]
    cyl.rotation_axis_angle[2] = LV[0]
    # Calculate how much we need to rotate
    rotationRad = np.arccos(LV[2]/L)
    cyl.rotation_axis_angle[0] = rotationRad
    # Displace. 
    cyl.location[0] = (pos[0,:]+pos[1,:])/2 + offset
    return cyl

def Say(text, verbosity=0):
    if verbosity<=VERBOSITY:
        if verbosity == 0:
            printText = time.strftime('%H:%M:%S   ') + text
        else: 
            printText = time.strftime('%H:%M:%S-DEBUG:  ') + text
        print(printText)

def ParseVal(val):
    if val.isnumeric():     
        return float(val)
    elif re.search('\(.*\)',val):   # If val contains a function, evaluate it (TODO security hazard)
        return eval(val)
    else:
        return val

#%% Import model
argv = sys.argv[sys.argv.index("--")+1:]                    # Get all arguments after -- (Blender won't touch these)
matPath = argv[0];                                          # Get matPath
VERBOSITY = 0 if not 'VERBOSITY' in argv else int(argv[argv.index('VERBOSITY')+1])   # Get VERBOSITY if defined
model = scipy.io.loadmat(matPath, chars_as_strings=True, mat_dtype=False, squeeze_me=False, struct_as_record=False)['model'][0,0]
    
###############################################################################

# Default settings for render.py (better to override from command line or rendermonitor.py)
settingsDict = {'renderDirName':'render',
                'resolution_percentage':100,    # in percent
                'offset':array([0,0,0])
                }

###############################################################################
# Get overwriting dictionary for render.py and model class
modelFields = dir(model)
Say('Argument parsing, analysing {} possible setting values and {} model fields'.format(len(settingsDict), len(modelFields)), verbosity=2)
for key,val in zip(argv[1::2], argv[2::2]):
    if key.startswith('model.'):
        parsedKey = key[6:]                 # 6 is length of 'model.'
        if parsedKey in modelFields:         
            Say("Found key = {} in model".format(parsedKey), verbosity=2)
            parsedVal = ParseVal(val)
            Say("parsedVal = " + str(parsedVal) + " of type " + str(type(parsedVal)), verbosity=3)
            setattr(model, parsedKey, reshape(parsedVal, [len(parsedVal),1]))
        else:
            raise(key + " not found in model class")
    else:
        if key in settingsDict:
            parsedVal = ParseVal(val)
            settingsDict[key] = parsedVal
        elif not key=='VERBOSITY':          # VERBOSITY is already evaluated
            raise(key + " not found in settings dictionary")

#%% Get common parameters
Lx = model.L[0,0] * 1e6
Ly = model.L[1,0] * 1e6
Lz = model.L[2,0] * 1e6
offset = settingsDict['offset']
#Lx = Ly = Lz = 200
# Throw warning if not all L are equal
if not (Lx == Ly == Lz):
    Say("WARNING: domain size is not uniform")

# Throw warning if cells are outside of domain
NBallOutsideDomain = 0
for ball in model.ballArray[:,0]:                       # Must loop row-wise, that's how MATLAB works
    pos = ball.pos[:,0]*1e6 + offset
    if not np.all([(array([0,0,0]) < pos), (pos < array([Lx, Ly, Lz]))]):
        NBallOutsideDomain += 1
if NBallOutsideDomain > 0:
    Say("WARNING: {} balls are outside the domain".format(NBallOutsideDomain))

#%% Clean up geometry (default cube, camera, light source)
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)

#%% Create camera
camPos = Lx/30* np.array([-15, -46, 42])
camRot = np.array([65, 0, -25])
bpy.ops.object.camera_add(location=camPos, rotation=(np.deg2rad(camRot[0]), np.deg2rad(camRot[1]), np.deg2rad(camRot[2])))
bpy.context.scene.camera = bpy.data.objects["Camera"]           # Set as active camera
cam = bpy.context.object
cam.data.clip_end = 1000                                         # Render whole range. This number will suffice
cam.data.lens = 25
cam.data.sensor_width = 30

"""
Focal blur: see http://wiki.blender.org/index.php/Doc:2.6/Tutorials/Composite_Nodes/Setups/Depth_Of_Field
Good settings:
- Add defocus composite
- Use Z buffer
- Link Zs
- Distance 70-100
- fStop 0.3-0.5
Don't forget to deselect preview!
"""

#%% Create light sources
# Sun 
bpy.ops.object.lamp_add(type='SUN', location=(0, 0, 40))
sun = bpy.context.object
sun.data.shadow_method = 'RAY_SHADOW'                           # Sun casts shadow
sun.data.shadow_soft_size = 1.5                                 # Soft shadow, based on distance to light source/plane
sun.data.shadow_ray_samples = 10

# Light point behind camera
bpy.ops.object.lamp_add(type='POINT', location=(0,0,5))        # Location is relative to camera
light = bpy.context.object
light.data.falloff_type = 'CONSTANT'
bpy.ops.object.select_all(action='DESELECT')                    # Make camera the light point's parent  (more detail in animation section, bottom of file)
light.parent = cam
lightTracker = light.constraints.new('TRACK_TO')                # Tell camera to track empty (for rotation)
lightTracker.target = cam
lightTracker.track_axis = 'TRACK_Z'

#%%
# Set up world
bpy.context.scene.world.horizon_color = (1, 1, 1)           # White background
bpy.context.scene.render.resolution_x = 1920
bpy.context.scene.render.resolution_y = 1080
bpy.context.scene.render.resolution_percentage = settingsDict['resolution_percentage']         # Allows for quick scaling

"""
# Mist/fog, fading distant cells and nanowires
bpy.context.scene.world.mist_settings.falloff = 'LINEAR'    
bpy.context.scene.world.mist_settings.intensity = 0
bpy.context.scene.world.mist_settings.height = 0
bpy.context.scene.world.mist_settings.start = 100
bpy.context.scene.world.mist_settings.depth = 40
bpy.context.scene.world.mist_settings.use_mist = True
"""

#%% Prepare materials
yellowM = bpy.data.materials.new('yellow')
yellowM.diffuse_color = (1.0, 1.0, 0.0)
yellowM.diffuse_intensity = 0.8
yellowM.specular_intensity = 0.1
yellowM.specular_hardness = 50

redM = bpy.data.materials.new('red')
redM.diffuse_color = (0.4, 0.0, 0.0)
redM.diffuse_intensity = 0.5
redM.specular_intensity = 0.1
redM.specular_hardness = 50

blueM = bpy.data.materials.new('blue')
blueM.diffuse_color = (0.0, 0.2, 0.8)
blueM.diffuse_intensity = 0.5
blueM.specular_intensity = 0.1
blueM.specular_hardness = 50

inkM = bpy.data.materials.new('ink')                     # ink (text, lines)
inkM.diffuse_intensity = 0
inkM.specular_intensity = 0
inkM.use_cast_shadows = False

wireM = bpy.data.materials.new('wire')                       # wire (grid)
wireM.type = 'WIRE'
wireM.specular_intensity = 0
wireM.diffuse_color = (0, 0, 0)

stickM = bpy.data.materials.new('stick')                    # EPS (sticking)
stickM.diffuse_color = (1.0, 1.0, 1.0)
stickM.diffuse_intensity = 1.0
stickM.specular_intensity = 0.1
#stickM.use_shadows = False                                # Shadows are not cast on the nanowire
#stickM.use_object_color = True                            # So we can assign colour scale to the nanowire, for rates

filM = bpy.data.materials.new('fil')                        # EPS (filament)
filM.diffuse_color = (0.0, 0.0, 0.0)
filM.diffuse_intensity = 0.5
filM.specular_intensity = 0.1

anchorM = bpy.data.materials.new('anchor')                        # EPS (anchoring)
anchorM.diffuse_color = (0.0, 0.0, 0.0)
anchorM.diffuse_intensity = 0.5
anchorM.specular_intensity = 0.1

whiteM = bpy.data.materials.new('white')                     # Bottom plane (to cash shadows on)
whiteM.diffuse_color = (1, 1, 1)
whiteM.diffuse_intensity = 1
whiteM.specular_intensity = 0
whiteM.diffuse_shader = 'TOON'                               # Give it a cartoon-ish finish, clear shadows and lines

#%% Draw XYZ axis legend
axLegCylH = 2.0*Lx/30                                          # Arrow body
axLegConeH = 0.4*Lx/30                                         # Arrow head
axLegCylR = 0.1*Lx/30
for ax,locCyl,locCone,rot in zip(['X', 'Y', 'Z'], \
      [(axLegCylH/2, 0.0, 0.0),             (0.0, axLegCylH/2, 0),              (0.0, 0.0, axLegCylH/2)],  \
      [(axLegCylH-axLegConeH/2, 0.0, 0.0),  (0.0, axLegCylH-axLegConeH/2, 0),   (0.0, 0.0, axLegCylH+axLegConeH/2)],  \
      [(0, pi/2, 0), (pi/2, 0, 0), (0, 0, 0)]):
    bpy.ops.mesh.primitive_cylinder_add(radius=axLegCylR, depth=axLegCylH, location=locCyl, rotation=rot)
    bpy.ops.object.shade_smooth()
    bpy.context.object.name = 'legendCyl'+ax
    bpy.context.object.active_material = inkM
    bpy.ops.mesh.primitive_cone_add(radius1=axLegCylR*2, depth=axLegConeH, location=locCone, rotation=rot)
    bpy.ops.object.shade_smooth()
    bpy.context.object.name = 'legendCone'+ax
    bpy.context.object.active_material = inkM
# Create text
fontSize = 3*np.round(Lx/30)
bpy.ops.object.text_add(location=(2, -fontSize*0.5, 0))
xText = bpy.context.object
xText.data.body = 'x'
bpy.ops.object.text_add(location=(-fontSize*0.5, 2, 0))
yText = bpy.context.object
yText.data.body = 'y'
bpy.ops.object.text_add(location=(-fontSize*0.5, -fontSize*0.5, 0))
zText = bpy.context.object
zText.data.body = 'z'
# Define font
times = bpy.data.fonts.load("/usr/share/fonts/TTF/times.ttf")
# Set, move text in place 
for text in (xText, yText, zText):
    text.data.size = fontSize
    text.active_material = inkM
    text.data.font = times

# Draw planes with all bells and whistles
if model.normalForce[0,0]:
    #%% Draw grid
    bpy.ops.mesh.primitive_grid_add(x_subdivisions=int(Lx/10)+1, y_subdivisions=int(Ly/10)+1, radius=Lx/2)
    zPlaneGrid = bpy.context.object
    zPlaneGrid.location = [Lx/2, Ly/2, 0.0]
    zPlaneGrid.active_material = wireM
    zPlaneGrid.rotation_euler[2] = 1*pi
    
    bpy.ops.mesh.primitive_plane_add(radius=Lx/2, location=(Lx/2, Ly/2, -0.1))  # Plane to project shadows on
    zPlane = bpy.context.object
    zPlane.active_material = whiteM
        
    bpy.ops.mesh.primitive_grid_add(x_subdivisions=int(Lx/10)+1, y_subdivisions=int(Ly/10)+1, radius=Lx/2)
    yPlaneGrid = bpy.context.object
    yPlaneGrid.active_material = wireM
    yPlaneGrid.location = [Lx/2, Ly, Lz/2]
    yPlaneGrid.rotation_euler[0] = 0.5*pi
    yPlaneGrid.rotation_euler[2] = 1*pi
    
    #%% Draw ticks
    fontSize = 2*np.round(Lx/30)
    for p in np.arange(0,Lx+1,10):
        tickList = []
        if p > 0:               # x ticks
            bpy.ops.object.text_add(location=(p, -fontSize, 0))
            xTick = bpy.context.object
            xTick.data.body = str(int(p))    
            tickList.append(xTick)
        if p > 0 and p < Ly:    # y ticks
            bpy.ops.object.text_add(location=(-fontSize, p, 0))
            yTick = bpy.context.object
            yTick.data.body = str(int(p))
            tickList.append(yTick)
        if True:                # z ticks
            bpy.ops.object.text_add(location=(-fontSize, Ly, p))
            zTick = bpy.context.object
            zTick.data.body = str(int(p))
            zTick.rotation_euler[0] = 0.5*pi
            tickList.append(zTick)
        for tick in tickList:   # assign material
            tick.data.size = 2*np.round(Lx/30)
            tick.active_material = inkM
            tick.data.font = times
            tick.data.align = 'CENTER'                     # only horizontal

###############################################################################
#%% Draw cells
for iCell,cell in enumerate(model.cellArray[:,0]):
    if cell.type[0,0] <= 1:
        iBall = cell.ballArray[0,0].astype(int)
        ball = model.ballArray[iBall,0]
        pos = ball.pos[:,0] * 1e6
        r = ball.radius[0,0] * 1e6
        cellG = CreateSphere(pos, r, offset)
        cellG.name = 'Sphere{:d}-{:04d}'.format(int(cell.type[0,0]), int(iCell))
        cellG.active_material = yellowM
    else:
        pos = np.empty([2,3])
        r   = np.empty(2)
        for ii,iBall in enumerate(cell.ballArray[:,0].astype(int)):
            ball        = model.ballArray[iBall,0]
            pos[ii,:]   = ball.pos[:,0] * 1e6
            r[ii]       = ball.radius[0,0] * 1e6
#            CreateSphere(ball.pos*1e6,ball.radius*1e6+0.05)        # Debugging purposes
        cellG = CreateRod(pos, r, offset)
        cellG.name = 'Rod{:d}-{:04d}'.format(int(cell.type[0,0]), int(iCell))
        cellG.active_material = redM

for iStick,stick in enumerate(model.stickSpringArray[:,0]):
    pos = np.empty([2,3])
    for ii,iBall in enumerate(stick.ballArray[:,0]):
        ball = model.ballArray[iBall,0]
        pos[ii,:]   = ball.pos[:,0] * 1e6
    stickG = CreateSpring(pos, 0.1, offset)
    stickG.name = 'Stick-{:04d}'.format(int(iStick))
    stickG.active_material = stickM

for iFil,fil in enumerate(model.filSpringArray[:,0]):
    pos = np.empty([2,3])
    for ii,iBall in enumerate(fil.ballArray):
        ball = model.ballArray[iBall,0]
        pos[ii,:]   = ball.pos[:,0] * 1e6
    filG = CreateSpring(pos, 0.1, offset)
    filG.name = 'Fil-{:04d}'.format(int(iFil))
    filG.active_material = filM

for iAnchor,anchor in enumerate(model.anchorSpringArray[:,0]):
    iBall = anchor.ballArray[0,0]
    ball = model.ballArray[iBall,0]
    pos   = ball.pos[:,0] * 1e6
    anchorG = CreateSpring(np.concatenate([[pos, [pos[0],pos[1],0.0]]], 0), 0.1, offset)
    anchorG.name = 'Anchor-{:04d}'.format(int(iAnchor))
    anchorG.active_material = anchorM

#CreateSphere([Lx/2+5,Ly/2+5,5],10)
#CreateRod(array([[Lx/2,Ly/2,0],[Lx/2,Ly/2,4]]),array([1,1]))
#CreateSpring(array([[Lx/2,Ly/2,0],[Lx/2,Ly/2,4]]))
#CreateSphere(array([ -8.64662208,  14.65630608,   9.16357743]),0.3)

"""
# Create coloured nanowires
rx = cellRx * (4./3.*pi*(0.5e-6)**3)*6 * N_A
rx_max = rx.max()
rx_min = rx[rx>0].min()
cFact = 255/(rx_max-rx_min)
cMat = (cFact*(rx-rx_min)).astype(int)
for i0, i1 in cellPair:
    cyl = CreateNanowire(cellPos[i0], cellPos[i1])
    cyl.active_material = nanowire
    cyl.color = cMap(cMat[i0,i1])                           # Needs 4, last one being alpha (1 using cMap directly)
    cyl.name = 'Nanowire '+str(i0)+'-'+str(i1)
"""

# Unselect everything, get ready for playing around
bpy.ops.object.select_all(action='DESELECT')

###############################################################################
#%% Save
matName = os.path.splitext( matPath.split("/")[-1] )[0]
matDir = "/".join(matPath.split('/')[:-1])
if "/output/"+matName in matPath:
    renderDir = matPath[:matPath.index("/output/"+matName)] + "/" + settingsDict['renderDirName']
else:
    Say("WARNING: output directory not found, writing .png and .blend to same folder as .mat")
    renderDir = matDir

bpy.data.scenes['Scene'].render.filepath = renderDir + "/" + matName + ".png"
#bpy.ops.wm.save_as_mainfile(filepath=renderDir + "/" + matName + ".blend", check_existing=False)

#%% Render
if not os.path.isdir(renderDir):
    os.mkdir(renderDir)
bpy.ops.render.render(write_still=True)