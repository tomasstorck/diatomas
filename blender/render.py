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
from numpy import array, pi, reshape, round

import scipy.io

# Define font
times = bpy.data.fonts.load("/usr/share/fonts/TTF/times.ttf")


#%% Function definitions %
rodLib = {}                                           # Contains rods of various aspect ratios
def CreateRod(pos, r, material):
    pos = array(pos)
    LV = pos[1]-pos[0]
    L = norm(LV)
    assert r[0] == r[1]
    rMean = sum(r)/2
    aspect = round(L/(2*rMean),1)
    keyName = str(aspect)+material                      # Dictionary stored based on aspect ratio and material
    if not keyName in rodLib:
        Say("\t\tAspect = {} with material = {} NOT in rodLib".format(aspect, material), verbosity=2)
        bpy.ops.mesh.primitive_cylinder_add(depth=L, location=(0, 0, L/2), radius=rMean)
        spring = bpy.context.object                                 # Ugly but appears to be the way to do it
        bpy.ops.mesh.primitive_uv_sphere_add(location=(0, 0, L), size=rMean)
        ball1 = bpy.context.object
        bpy.ops.mesh.primitive_uv_sphere_add(location=(0, 0, 0), size=rMean)
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
        # Set rotation mode
        rod.rotation_mode = 'AXIS_ANGLE'                # Other rotations are sequential: rotate around X, THEN around y, etc.
        # Set material        
        rod.active_material = bpy.data.materials[material]
        # Add this object to the library
        rodLib[keyName] = [rod, rMean]         # rMean required for scaling only, material is copied by reference, not value, so needs to be recreated for different material
    else:
        Say("\t\tAspect = {} in rodLib".format(aspect), verbosity=2)
        originalRod,originalR = rodLib.get(keyName)
        rod = originalRod.copy()
        rod.scale = [rMean/originalR]*3                 # Make it a len 3 vector
        rod.name = rod.name + "_copy_r{}".format(rMean) # Need to set something as to not have duplicate names in scene
        # Link to scene (not needed for original CreateRod)
        bpy.context.scene.objects.link(rod)
        
    # Identical for copied or new. Define vector in XY plane we will revolve along
    rod.rotation_axis_angle[1] = -1*LV[1]          # X axis (use Y position). Relative, absolute doesn't matter. -1* because we want perpendicular vector
    rod.rotation_axis_angle[2] = LV[0]              # Y axis (use X position)
    # Calculate how much we need to rotate (angle from [0 0 L] to [x y 0] to get vector [0 0 L] to overlay [x y z])
    rotationRad = np.arccos(LV[2]/L)
    rod.rotation_axis_angle[0] = rotationRad     # W (amount of rotation around defined axis)
    # Displace. Since selected object is still ball0 (and this is at the origin), displace to correct position for this one.
    rod.location = pos[0,:]
    return rod                                                # Returns entire, merged cell

sphereLib = {}
def CreateSphere(pos, r, material):
    pos = array(pos)
    r = r
    keyName = material
    if not keyName in sphereLib:
        Say("\t\tDefining initial spherical cell", verbosity=2)
        bpy.ops.mesh.primitive_uv_sphere_add(location=pos, size=r)
        sphere = bpy.context.object
        bpy.ops.object.shade_smooth()
        sphere.active_material = bpy.data.materials[material]
        sphereLib[keyName] = [sphere, r]
    else:
        Say("\t\tCopying existing sphere", verbosity=2)
        originalSphere,originalR = sphereLib[keyName]
        sphere = originalSphere.copy()
        sphere.scale = [r/originalR]*3
        sphere.location = pos
        # Add to scene
        bpy.context.scene.objects.link(sphere)        
    return sphere

cylLib = {}
def CreateSpring(pos, r, material):
    pos = array(pos)
    LV = pos[1,:]-pos[0,:]
    L = norm(LV)
    keyName = material
    if not keyName in cylLib:
        Say("\t\tDefining initial spring", verbosity=2)
        bpy.ops.mesh.primitive_cylinder_add(depth=L, location=(0, 0, L/2), radius=0.15)
        cyl = bpy.context.object
        bpy.ops.object.shade_smooth()
        # Set rotation mode
        cyl.rotation_mode = 'AXIS_ANGLE'
        # Set material
        cyl.active_material = bpy.data.materials[material]
        # Set as original spring
        cylLib[keyName] = [cyl, L]
    else:
        Say("\t\tCopying existing spring", verbosity=2)
        originalCyl,originalL = cylLib[keyName]
        cyl = originalCyl.copy()
        cyl.scale[2] = L/originalL
        # Add to scene
        bpy.context.scene.objects.link(cyl)        
    # Define vector in XY plane we will revolve along (note: see CreateRod for method)
    cyl.rotation_axis_angle[1] = -1*LV[1]
    cyl.rotation_axis_angle[2] = LV[0]
    # Calculate how much we need to rotate
    rotationRad = np.arccos(LV[2]/L)
    cyl.rotation_axis_angle[0] = rotationRad
    # Displace. 
    cyl.location = (pos[0,:]+pos[1,:])/2
    return cyl
    
###############################################################################

def Offset(offset):
    offsetObjects = ['Sphere', 'Rod', 'Stick', 'Fil', 'Anchor', 'Sun']     # Objects that will be offset
    for k in bpy.data.objects.keys():
        for o in offsetObjects:
            if o in k:                                              # This is an object that will be offset
                obj = bpy.data.objects[k]
                obj.location = offset + np.array(obj.location)


###############################################################################
    
def DefineMaterials():
    # Prepare materials
    inkM = bpy.data.materials.new('ink')                     # ink (text, lines)
    inkM.diffuse_intensity = 0
    inkM.specular_intensity = 0
    inkM.use_cast_shadows = False
    
    whiteM = bpy.data.materials.new('white')                     # Bottom plane (to cash shadows on)
    whiteM.diffuse_color = (1, 1, 1)
    whiteM.diffuse_intensity = 1
    whiteM.specular_intensity = 0
    whiteM.diffuse_shader = 'TOON'                               # Give it a cartoon-ish finish, clear shadows and lines

    greyM = bpy.data.materials.new('grey')                     # Bottom plane (to cash shadows on), for E. coli
    greyM.diffuse_color = (0.5, 0.5, 0.5)                  # Grey (substratum)
    greyM.diffuse_intensity = 1
    greyM.specular_intensity = 0
    greyM.diffuse_shader = 'LAMBERT'
    
    wireM = bpy.data.materials.new('wire')                       # wire (grid)
    wireM.type = 'WIRE'
    wireM.specular_intensity = 0
    wireM.diffuse_color = (0, 0, 0)
    
    cellDssM = bpy.data.materials.new('cellDss')
    cellDssM.diffuse_color = (0.3, 1.0, 0.0)                     # Medium-dark green/DSS
    cellDssM.diffuse_intensity = 0.7
    cellDssM.specular_color = (0.6, 1.0, 0.5)
    cellDssM.specular_intensity = 0.1
    cellDssM.specular_hardness = 5
    cellDssM.specular_shader = 'PHONG'
    
    cellAnmeM = bpy.data.materials.new('cellAnme')
    cellAnmeM.diffuse_color = (0.4, 0.0, 0.0)                      # Dark red/ANME
    cellAnmeM.diffuse_intensity = 0.7
    cellAnmeM.specular_color = (1.0, 0.25, 0.25)
    cellAnmeM.specular_intensity = 0.1
    cellAnmeM.specular_hardness = 5
    cellAnmeM.specular_shader = 'PHONG'

    cell0M = bpy.data.materials.new('cell0')
    cell0M.diffuse_color = (0.4, 0.0, 0.0)                       # Dark red/E. coli gen. 1
    cell0M.diffuse_intensity = 0.7
    cell0M.specular_color = (1.0, 0.25, 0.25)
    cell0M.specular_intensity = 0.1
    cell0M.specular_hardness = 5
    cell0M.specular_shader = 'PHONG'
    
    cell1M = bpy.data.materials.new('cell1')
    cell1M.diffuse_color = (1.0, 1.0, 0.5)                      # Bright yellow/E. coli gen. 2
    cell1M.diffuse_intensity = 0.6
    cell1M.specular_color = (1.0, 1.0, 0.8)
    cell1M.specular_intensity = 0.1
    cell1M.specular_hardness = 5
    cell1M.specular_shader = 'PHONG'
    
    
    cell2M = bpy.data.materials.new('cell2')
    cell2M.diffuse_color = (0.1, 1.0, 1.0)                      # Medium-bright blue/E. coli gen 3
    cell2M.diffuse_intensity = 0.6
    cell2M.specular_color = (1.0, 1.0, 1.0)
    cell2M.specular_intensity = 0.1
    cell2M.specular_hardness = 5
    cell2M.specular_shader = 'PHONG'

    cell3M = bpy.data.materials.new('cell3')
    cell3M.diffuse_color = (0.0, 1.0, 0.0)                     # Medium-dark green/E. coli gen 4
    cell3M.diffuse_intensity = 0.6
    cell3M.specular_color = (0.6, 1.0, 0.5)
    cell3M.specular_intensity = 0.1
    cell3M.specular_hardness = 5
    cell3M.specular_shader = 'PHONG'
    
    
    stickM = bpy.data.materials.new('stick')                    # EPS (sticking, adhesive)
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
###############################################################################

def CameraPerspSetup(location, rotation):                       # Does not allow automatic configuration
    bpy.ops.object.camera_add(location=location, rotation=rotation)
    cam = bpy.context.object
    cam.name = 'CameraPersp'
    cam.data.clip_end = 1000                                         # Render whole range. This number will suffice
    cam.data.lens = 25
    cam.data.sensor_width = 30
    bpy.context.scene.camera = cam           # Set as active camera    
    
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

    # Light point behind camera
    bpy.ops.object.lamp_add(type='POINT', location=(0,0,5))        # Location is relative to camera
    light = bpy.context.object
    light.name = 'PointCamPersp'
    light.data.energy = 0.8                                         # 1.0 is too bright for E. coli
    light.data.falloff_type = 'CONSTANT'
    bpy.ops.object.select_all(action='DESELECT')                    # Make camera the light point's parent  (more detail in animation section, bottom of file)
    light.parent = cam
    lightTracker = light.constraints.new('TRACK_TO')                # Tell camera to track empty (for rotation)
    lightTracker.target = cam
    lightTracker.track_axis = 'TRACK_Z'
    

def CameraSideSetup(distance=None, Lx=20, Ly=20, Lz=20, camWidth=1920, camHeight=1080):   # Add camera to the scene, for side view
    if distance is None:
        distance = max([camWidth/camHeight*Ly+20, camWidth/camHeight*Lz+20, Lx+20])
    camPos,camRot,camName = [(Lx/2.0, Ly-distance, Lz/2.0), (0.5*pi, 0, 0), 'CameraSide']               # This is a useful formula
    bpy.ops.object.camera_add(location=camPos, rotation=camRot)
    cam = bpy.context.object
    cam.name = camName
    cam.data.clip_end = 1000                                                                            # Render whole range. This number will suffice
    cam.data.type = 'ORTHO'
    cam.data.ortho_scale = distance
    bpy.context.scene.camera = cam                                                                      # Set as active camera    
    # Add light point
    bpy.ops.object.lamp_add(type='POINT', location=camPos)        # Location is relative to camera
    light = bpy.context.object
    light.name = 'PointCamSide'
    light.data.falloff_type = 'CONSTANT'

def CameraTopSetup(distance=None, Lx=20, Ly=20, Lz=20, camWidth=1920, camHeight=1080):                  # Add camera to the scene, for top view
    if distance is None:
        distance = max([camWidth/camHeight*Ly+20, camWidth/camHeight*Lz+20, Lx+20])
    camPos,camRot,camName = [(Lx/2.0, Ly/2.0, distance), (0, 0, 0), 'CameraTop']
    bpy.ops.object.camera_add(location=camPos, rotation=camRot)
    cam = bpy.context.object
    cam.name = camName
    cam.data.clip_end = 1000                                    # Render whole range. This number will suffice
    cam.data.type = 'ORTHO'
    cam.data.ortho_scale = distance
    bpy.context.scene.camera = cam           # Set as active camera    

def CamerasDelete():                                                # Delete only the side and top camera
    for cam in ['CameraSide', 'CameraTop']:
        obj = bpy.data.objects[cam]
        bpy.data.scenes[0].objects.unlink(obj)
        bpy.data.objects.remove(obj)        

def CameraPerspDisable():
    bpy.data.objects['PointCamPersp'].hide_render=True

def CameraSideDisable():
    bpy.data.objects['PointCamSide'].hide_render=True

###############################################################################

def SetupXYZLegend(location=(0.0, 0.0, 0.0), fontSize=1, textSpacing=2):                                   # The three arrows at the origin, showing which direction is X, Y, Z
    inkM = bpy.data.materials['ink']
    #%% Draw XYZ axis legend
    axLegCylH = 3.0*fontSize                                        # Arrow body
    axLegConeH = 0.8*fontSize                                       # Arrow head
    axLegCylR = 0.2*fontSize                                        # Arrow radius
    for ax,locCyl,locCone,rot in zip(['X', 'Y', 'Z'], \
          [np.add((axLegCylH/2, 0.0, 0.0), location),               np.add((0.0, axLegCylH/2, 0), location),              np.add((0.0, 0.0, axLegCylH/2), location)],  \
          [np.add((axLegCylH+axLegConeH/2, 0.0, 0.0), location),    np.add((0.0, axLegCylH+axLegConeH/2, 0), location),   np.add((0.0, 0.0, axLegCylH+axLegConeH/2), location)],  \
          [(0, pi/2, 0), (3*pi/2, 0, 0), (0, 0, 0)]):
        bpy.ops.mesh.primitive_cylinder_add(radius=axLegCylR, depth=axLegCylH, location=locCyl, rotation=rot)
        bpy.ops.object.shade_smooth()
        bpy.context.object.name = 'legendCyl'+ax
        bpy.context.object.active_material = inkM
        bpy.ops.mesh.primitive_cone_add(radius1=axLegCylR*2, depth=axLegConeH, location=locCone, rotation=rot)
        bpy.ops.object.shade_smooth()
        bpy.context.object.name = 'legendCone'+ax
        bpy.context.object.active_material = inkM
    # Create text
    bpy.ops.object.text_add(location=np.add((textSpacing, -fontSize*5.0*0.5, 0), location))
    xText = bpy.context.object
    xText.name = 'legendX'
    xText.data.body = 'x'
    bpy.ops.object.text_add(location=np.add((-fontSize*5.0*0.5, textSpacing, 0), location))
    yText = bpy.context.object
    yText.name = 'legendY'
    yText.data.body = 'y'
    bpy.ops.object.text_add(location=np.add((-fontSize*5.0*0.5, -fontSize*5.0*0.5, 0), location))
    zText = bpy.context.object
    zText.name = 'legendZ'
    zText.data.body = 'z'
    # Set, move text in place 
    for text in (xText, yText, zText):
        text.data.size = fontSize*5.0
        text.active_material = inkM
        text.data.font = times
    return [xText, yText, zText]

def DeleteLegends():
    for k in bpy.data.objects.keys():
        if 'legend' in k:
            obj = bpy.data.objects[k]
            bpy.data.scenes[0].objects.unlink(obj)
            bpy.data.objects.remove(obj)
            
def SetupScalebarLegend(location=(-20,-20, 0), length=10, fontSize=1):
    inkM = bpy.data.materials['ink']

    bpy.ops.mesh.primitive_cylinder_add(radius=0.2*fontSize, depth=length, location=location, rotation=(0, pi/2, 0))
    bpy.ops.object.shade_smooth()
    scalebarCyl = bpy.context.object
    scalebarCyl.name = 'legendScalebarCyl'
    scalebarCyl.active_material = inkM    

    bpy.ops.mesh.primitive_cube_add(radius=1, location=np.array(location)-np.array((length/2.0, 0.0, 0.0)))
    scalebarLeftMarker = bpy.context.object
    scalebarLeftMarker.name = 'legendScalebarLeftMarker'
    scalebarLeftMarker.dimensions = (0.1*fontSize, fontSize, fontSize)
    scalebarLeftMarker.active_material = inkM

    bpy.ops.mesh.primitive_cube_add(radius=1, location=np.array(location)+np.array((length/2.0, 0.0, 0.0)))
    scalebarRightMarker = bpy.context.object
    scalebarRightMarker.name = 'legendScalebarRightMarker'
    scalebarRightMarker.dimensions = (0.1*fontSize, fontSize, fontSize)
    scalebarRightMarker.active_material = inkM    
    
    bpy.ops.object.text_add(location=np.array(location)-np.array([0,fontSize*5.0,0]))
    text = bpy.context.object
    text.data.body = str(int(length)) + ' um'    
    text.data.align = 'CENTER'
    text.name = 'legendScalebarText'
    text.data.size = fontSize*5.0
    text.active_material = inkM
    text.data.font = times

###############################################################################

def SetupPlanes(drawPlaneZ=True, drawPlaneGrid=(False, True, True), Lx=20, Ly=20, Lz=20, radiusZPlane=None, stepSize=10.0):
    surfaceM = surfaceMaterial    
    wireM = bpy.data.materials['wire']    
    
    # Plane to project shadows on
    if drawPlaneZ:
        if radiusZPlane is None:
            planeRadius = Lx/2    
        else:
            planeRadius = radiusZPlane
        bpy.ops.mesh.primitive_plane_add(radius=planeRadius, location=(Lx/2, Ly/2, -0.1))  
        planeZ = bpy.context.object
        planeZ.name = 'planeZ'
        planeZHeightScale = Ly/Lx
        planeZ.scale[1] = planeZHeightScale
        planeZ.active_material = surfaceM
    
    #%% Draw grid
    if drawPlaneGrid[2]:
        # Z plane (horizontal)    
        bpy.ops.mesh.primitive_grid_add(x_subdivisions=int(Lx/stepSize)+1, y_subdivisions=int(Ly/stepSize)+1, radius=Lx/2)
        planeZGrid = bpy.context.object
        planeZGrid.name = 'planeZGrid'
        planeZGrid.location = [Lx/2, Ly/2, 0.0]
        planeZGrid.active_material = wireM
        planeZGrid.rotation_euler[2] = 1*pi
        planeZGrid.scale[1] = planeZHeightScale
     
    if drawPlaneGrid[1]:
        # Y plane (back)
        PlaneYHeightScale = Lz/Lx
        bpy.ops.mesh.primitive_grid_add(x_subdivisions=int(Lx/stepSize)+1, y_subdivisions=int(Lz/stepSize)+1, radius=Lx/2)
        PlaneYGrid = bpy.context.object
        PlaneYGrid.name = 'planeYGrid'
        PlaneYGrid.active_material = wireM
        PlaneYGrid.location = [Lx/2, Ly, Lz/2]
        PlaneYGrid.rotation_euler[0] = 0.5*pi
        PlaneYGrid.scale[1] = PlaneYHeightScale

def DeletePlanes():
    for k in bpy.data.objects.keys():
        if 'plane' in k:
            obj = bpy.data.objects[k]
            bpy.data.scenes[0].objects.unlink(obj)
            bpy.data.objects.remove(obj)        

###############################################################################
        
def SetupTicks(drawTicks = (True, True, True), Lx = 20.0, Ly = 20.0, Lz = 20.0, fontSize=1.0, stepSize=10.0):
    inkM = bpy.data.materials['ink']
    
    pos = 0.0
    tickListLarge = []
    tickDone = False
    while not tickDone:
        tickList = []
        tickDone = True
        if drawTicks[0] and pos <= Lx:               # x ticks (x plane)
            tickDone = False
            bpy.ops.object.text_add(location=(pos, -fontSize*4.0, 0))
            xTick = bpy.context.object
            xTick.name = "tickX{:g}".format(pos)
            xTick.data.body = "{:g}".format(pos)
            tickList.append(xTick)
        if drawTicks[1] and pos <= Ly:               # y ticks (x plane)
            tickDone = False
            bpy.ops.object.text_add(location=(-fontSize*4.0, pos-fontSize/2.0, 0))
            yTick = bpy.context.object
            yTick.name = "tickY{:g}".format(pos)
            yTick.data.body = "{:g}".format(pos)
            tickList.append(yTick)
        if drawTicks[2] and pos <= Lz:               # z ticks (y plane)
            tickDone = False
            bpy.ops.object.text_add(location=(-fontSize*4.0, Ly, pos-fontSize/2.0))
            zTick = bpy.context.object
            zTick.name = "tickZ{:g}".format(pos)
            zTick.data.body = "{:g}".format(pos)
            zTick.rotation_euler[0] = 0.5*pi
            tickList.append(zTick)
        for tick in tickList:   # assign material
            tick.data.size = fontSize*4.0
            tick.active_material = inkM
            tick.data.font = times
            tick.data.align = 'CENTER'                     # only horizontal
            tickListLarge.append(tick)
        pos += stepSize
    return tickListLarge

def DeleteTicks():
    for k in bpy.data.objects.keys():
        if 'tick' in k:
            obj = bpy.data.objects[k]
            bpy.data.scenes[0].objects.unlink(obj)
            bpy.data.objects.remove(obj)        

def DeleteTick(x=None, y=None, z=None):
    for a,prefix in zip([x,y,z],["tickX", "tickY", "tickZ"]):
        if a is not None:
            if type(a) is list or type(a) is tuple:
                for t in a:
                    obj = bpy.data.objects[prefix + str(t)]
                    bpy.data.scenes[0].objects.unlink(obj)
                    bpy.data.objects.remove(obj)
            else:
                print("deleting "+prefix + str(a))
                obj = bpy.data.objects[prefix + str(a)]
                bpy.data.scenes[0].objects.unlink(obj)
                bpy.data.objects.remove(obj)

def RotateX():
    for t in bpy.data.objects.keys():
        if 'tickX' in t:
            obj = bpy.data.objects[t]
            if obj.rotation_euler[0] == 0.0:            # Default
                obj.rotation_euler[0] = 0.5*pi
                obj.location[2] -= 5
            else:
                obj.rotation_euler[0] = 0.0
                obj.location[2] += 5

        if 'legendX' in t:
            obj = bpy.data.objects[t]
            if obj.rotation_euler[0] == 0.0:            # Default
                obj.rotation_euler[0] = 0.5*pi
                obj.location[2] -= 3
            else:
                obj.rotation_euler[0] = 0.0
                obj.location[2] += 3
                
        if 'legendScalebarText' in t:
            obj = bpy.data.objects[t]
            if obj.rotation_euler[0] == 0.0:            # Default
                obj.rotation_euler[0] = 0.5*pi
                obj.location[2] -= 5
            else:
                obj.rotation_euler[0] = 0.0
                obj.location[2] += 5

        if t == 'legendZ':                          
            obj = bpy.data.objects[t]
            if obj.rotation_euler[0] == 0.0:            # Default
                obj.rotation_euler[0] = 0.5*pi
                obj.location[2] += 3
            else:
                obj.rotation_euler[0] = 0.0
                obj.location[2] -= 3

###############################################################################
def Render():
    bpy.ops.render.render(write_still=True)                

###############################################################################

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
    elif val.lower() == 'true':
        return True
    elif val.lower() == 'false':
        return False
    elif re.search('\(.*\)',val):   # If val contains a function, evaluate it (TODO security hazard)
        return eval(val)
    else:
        return val

###############################################################################

###############################################################################

# Default settings for render.py (better to override from command line or rendermonitor.py)
settingsDict = {'camPos':None,
                'camRot':array([65, 0, -25]),
                'colourByGeneration':False,
                'drawAxisLegend':True,
                'drawPlane':True,
                'drawPlaneGrid':(False, True, True),
                'drawPlaneGridY':True,
                'drawAnchor':True,
                'drawFil':True,
                'drawStick':True,
                'gridStepSize':10.0,
                'offset':array([0,0,0]),
                'planeInfinite':False,
                'configMaterial':None,               # Set materials, pre-configured
                'renderDir':'render',
                'resolution_percentage':100,    # in percent
                'saveBlend':True,
                'suppressRender':False,
                'textSizeDivider':50,
                }
VERBOSITY = 0
###############################################################################

if __name__ == '__main__':                                      # Run if not imported as module
    
    #%% Import model
    argv = sys.argv[sys.argv.index("--")+1:]                    # Get all arguments after -- (Blender won't touch these)
    matPath = argv[0];                                          # Get matPath
    VERBOSITY = 0 if not 'VERBOSITY' in argv else int(argv[argv.index('VERBOSITY')+1])   # Get VERBOSITY if defined
    model = scipy.io.loadmat(matPath, chars_as_strings=True, mat_dtype=False, squeeze_me=False, struct_as_record=False)['model'][0,0]
        
    # Get overwriting dictionary for render.py and model class
    modelFields = dir(model)
    Say('Argument parsing, analysing {} possible setting values and {} model fields'.format(len(settingsDict), len(modelFields)), verbosity=3)
    for key,val in zip(argv[1::2], argv[2::2]):
        if key.startswith('model.'):
            parsedKey = key[6:]                 # 6 is length of 'model.'
            if parsedKey in modelFields:         
                Say("Found key = {} in model".format(parsedKey), verbosity=3)
                parsedVal = ParseVal(val)
                Say("parsedVal = " + str(parsedVal) + " of type " + str(type(parsedVal)), verbosity=4)
                setattr(model, parsedKey, reshape(parsedVal, [len(parsedVal) if not type(parsedVal) is bool else 1,1]))
            else:
                raise(key + " not found in model class")
        else:
            if key in settingsDict:
                parsedVal = ParseVal(val)
                settingsDict[key] = parsedVal
            elif not key=='VERBOSITY':          # VERBOSITY is already evaluated
                raise(key + " not found in settings dictionary")
    
    #%% Get common parameters
    Say("Analysing domain and setting common parameters", verbosity=1)
    Lx = model.L[0,0] * 1e6
    Ly = model.L[1,0] * 1e6
    Lz = model.L[2,0] * 1e6
    offset = settingsDict['offset']
    #Lx = Ly = Lz = 200
    
    # Throw warning if cells are outside of domain
    NBallOutsideDomain = 0
    for ball in model.ballArray[:,0]:                       # Must loop row-wise, that's how MATLAB works
        pos = ball.pos[:,0]*1e6
        if not np.all([(array([0,0,0]) < pos), (pos < array([Lx, Ly, Lz]))]):
            NBallOutsideDomain += 1
    if NBallOutsideDomain > 0:
        Say("WARNING: {} balls are outside the domain".format(NBallOutsideDomain))
    
    #%% Clean up geometry (default cube, camera, light source)
    Say("Cleaning up geometry", verbosity=1)
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    
    #%%
    # Set up world
    Say("Setting up world", verbosity=1)
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
    
    
    #%% Create camera
    Say("Calculating and creating camera", verbosity=1)
    if settingsDict['camPos'] == None:
        camPos = Lx/30* (array([-15, -46, 42]))                    # limited by camera width    
    else:
        camPos= settingsDict['camPos']
    camRot = (np.deg2rad(settingsDict['camRot'][0]), np.deg2rad(settingsDict['camRot'][1]), np.deg2rad(settingsDict['camRot'][2]))
    CameraPerspSetup(location=camPos, rotation=camRot)
   
    #%% Create light sources
    Say("Creating light sources", verbosity=1)
    # Sun 
    bpy.ops.object.lamp_add(type='SUN', location=(0, 0, 40))
    sun = bpy.context.object
    sun.data.shadow_method = 'RAY_SHADOW'                           # Sun casts shadow
    sun.data.shadow_soft_size = 1.5                                 # Soft shadow, based on distance to light source/plane
    sun.data.shadow_ray_samples = 10
    
    #%% Materials # FIXME remove
    DefineMaterials()
    
    #%% Legend
    if settingsDict['drawAxisLegend']:
        Say("Drawing XYZ arrows/legend", verbosity=1)
        SetupXYZLegend(fontSize=round((norm(camPos)/settingsDict['textSizeDivider'])**0.5))
        
    #%% Draw planes with all bells and whistles
    Say("Drawing plane, grid, etc", verbosity=1)
    
    if settingsDict['drawPlane']:
        if settingsDict['planeInfinite']:
            radiusZPlane = Lx*50
        else:
            radiusZPlane = None
        SetupPlanes(Lx=Lx, Ly=Ly, Lz=Lz, drawPlaneGrid=settingsDict['drawPlaneGrid'], radiusZPlane=radiusZPlane, stepSize=settingsDict['gridStepSize'])
        SetupTicks(Lx=Lx, Ly=Ly, Lz=Lz, fontSize=round((norm(camPos)/settingsDict['textSizeDivider'])**0.5))
        DeleteTick(x=0, y=[0, int(Ly)])
        
    ###############################################################################
       
    #%% Draw cells
    Say("Drawing cells", verbosity=1)
    for iCell,cell in enumerate(model.cellArray[:,0]):
        Say("\tCell = {}".format(iCell, ), verbosity=1)
        if settingsDict['colourByGeneration']:
            ancestor = cell
            while np.where(model.cellArray[:,0]==ancestor)[0][0] > 3:        # 0 through 3, because there will be 4 colours available
                ancestor = model.cellArray[ancestor.mother[0][0],0]
            cellType = int(np.where(model.cellArray==ancestor)[0][0])
            Say("\t\tCell generation = " + str(cellType), verbosity=3)
        else:
            cellType = cell.type[0][0].astype(int)

        if cell.type[0,0].astype(int) <= 1:
            iBall = cell.ballArray[0,0].astype(int)
            ball = model.ballArray[iBall,0]
            pos = ball.pos[:,0] * 1e6
            r = ball.radius[0,0] * 1e6
            cellG = CreateSphere(pos, r, cellMaterial[cellType])
            cellG.name = 'Sphere{:d}-{:04d}'.format(cellType, iCell)
        else:
            pos = np.empty([2,3])
            r   = np.empty(2)
            for ii,iBall in enumerate(cell.ballArray[:,0].astype(int)):
                ball        = model.ballArray[iBall,0]
                pos[ii,:]   = ball.pos[:,0] * 1e6
                r[ii]       = ball.radius[0,0] * 1e6
            cellG = CreateRod(pos, r, cellMaterial[cellType])
            cellG.name = 'Rod{:d}-{:04d}'.format(cellType, iCell)
    Say("fraction {} in rodLib".format(round(1-len(rodLib)/len(model.cellArray[:,0]),2)), verbosity=1)
    
    if settingsDict['drawStick']:
        stickM = bpy.data.materials['stick']
        for iStick,stick in enumerate(model.stickSpringArray[:,0]):
            Say("\tSticking spring = {}".format(iStick), verbosity=1)
            pos = np.empty([2,3])
            for ii,iBall in enumerate(stick.ballArray[:,0]):
                ball = model.ballArray[int(iBall),0]
                pos[ii,:]   = ball.pos[:,0] * 1e6
            stickG = CreateSpring(pos, 0.1, stickM.name)
            stickG.name = 'Stick-{:04d}'.format(int(iStick))
    
    if settingsDict['drawFil']:
        filM = bpy.data.materials['fil']
        for iFil,fil in enumerate(model.filSpringArray[:,0]):
            Say("\tFilament spring = {}".format(iFil), verbosity=1)
            pos = np.empty([2,3])
            for ii,iBall in enumerate(fil.ballArray):
                ball = model.ballArray[int(iBall),0]
                pos[ii,:]   = ball.pos[:,0] * 1e6
            filG = CreateSpring(pos, 0.1, filM.name)
            filG.name = 'Fil-{:04d}'.format(int(iFil))
    
    if settingsDict['drawAnchor']:
        anchorM = bpy.data.materials['anchor']
        for iAnchor,anchor in enumerate(model.anchorSpringArray[:,0]):
            Say("\tAnchoring spring = {}".format(iAnchor), verbosity=1)
            iBall = anchor.ballArray[0,0]
            ball = model.ballArray[int(iBall),0]
            pos   = ball.pos[:,0] * 1e6
            anchorG = CreateSpring(np.concatenate([[pos, [pos[0],pos[1],0.0]]], 0), 0.1, anchorM.name)
            anchorG.name = 'Anchor-{:04d}'.format(int(iAnchor))
    
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

    # Offset
    Offset(offset)
    
    ###########################################################################

    ## Set viewport clipping to something reasonable
    #bpy.context.space_data.clip_end = 2000    
    
    ###############################################################################
    
    #%% Save
    Say("Saving", verbosity=1)
    matName = os.path.splitext( matPath.split("/")[-1] )[0]
    matDir = "/".join(matPath.split('/')[:-1])
    if "/output/"+matName in matPath:
        renderPath = matPath[:matPath.index("/output/"+matName)] + "/" + settingsDict['renderDir']
        if not os.path.isdir(renderPath):
            os.mkdir(renderPath)
    else:
        Say("WARNING: output directory not found, writing .png and .blend to same folder as .mat")
        renderPath = matDir
        
    if not os.path.isdir(renderPath):
            os.mkdir(renderPath)
            
    if settingsDict['saveBlend']:
        bpy.ops.wm.save_as_mainfile(filepath = renderPath + "/" + matName + ".blend", check_existing=False)
    
    #%% Render
    bpy.data.scenes['Scene'].render.filepath = renderPath + "/" + matName + ".png"
    if not settingsDict['suppressRender']:
        Say("Rendering", verbosity=1)
        Render()
    
    ###############################################################
