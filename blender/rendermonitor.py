#!/usr/bin/python
# -*- coding: utf-8 -*-
import os, time, subprocess, re 

def Say(text, verbosity=0, end='\n', suppressTime=False):
    if verbosity<=VERBOSITY:
        if suppressTime:
            timeStr = ''
        else:
            timeStr = time.strftime('%H:%M:%S   ')
        if verbosity == 0:
            print(timeStr + text, end=end)
        else: 
            print('DEBUG: ' + text, end=end)


###############################################################################
# Global settings #
###################

#dirFilter = '.*'        # regular expression
dirFilter = 'ecoli_'
#dirFilter = 'odetol'        # regular expression
VERBOSITY = 0           # Note that only rendermonitor.py is printed to console, render.py shows up in logfile.txt
iterModDiv = 5 

###############################################################################

resultsPath = os.getcwd()[:os.getcwd().index("/blender")] + "/results"
while True:
    t0 = time.time()
    dirList = [dirs for dirs in os.listdir(resultsPath) if os.path.isdir(os.path.join(resultsPath, dirs)) and os.path.isdir(os.path.join(resultsPath, dirs, 'output')) and re.search(dirFilter,dirs)]
    dirList.sort()
    for d in dirList:
        Say(d)
        #######################################################################
        # Pre-render settings #
        #######################
        renderpySettingsDict = {'VERBOSITY':VERBOSITY,
                                'resolution_percentage':50,
                                'offset':'array([120,120,20])',
                                'model.L':'array([60e-6,60e-6,60e-6])',
                                'saveBlend':True,
                                'drawStick':False,
                                'renderDir':'render'
                                }

        #renderpySettingsDict['suppressRender'] = True

        if re.match('^aom', d):
            renderpySettingsDict['model.L'] = 'array([20e-6,20e-6,20e-6])'
            renderpySettingsDict['offset'] = 'array([10,10,10])'
            renderpySettingsDict['configMaterial'] = 'ConfigAOM'
            renderpySettingsDict['gridStepSize'] = 5
        elif re.match('^as', d):
            renderpySettingsDict['model.L'] = 'array([80e-6,80e-6,80e-6])'
            renderpySettingsDict['offset'] = 'array([40,40,40])'
            renderpySettingsDict['configMaterial'] = 'ConfigAS'
        elif re.match('^ecoli', d):
            renderpySettingsDict['model.L'] = 'array([80e-6,80e-6,80e-6])'
            renderpySettingsDict['offset'] = 'array([40,40,0])'
            renderpySettingsDict['configMaterial'] = 'ConfigEcoli'       # Change colours of cells for consistency with paper/experiments
            renderpySettingsDict['colourByGeneration'] = True
            
        #######################################################################        
            
        dAbs = resultsPath + "/" + d + "/output"
        Say(dAbs, 2)
        fileList = [files for files in os.listdir(dAbs) if os.path.splitext(files)[-1]=='.mat']
        fileList.sort(reverse=True)
        for f in fileList:
            if not int(re.match('g(\d{4})r(\d{4}).mat',f).group(2))%iterModDiv == 0:
                # relaxation iteration (YYYY in filename gXXXXrYYYY.mat) % iterModulusDivider == 0
                continue
            fAbs = dAbs + "/" + f
            # Check if file is already plotted
            fName = os.path.splitext(fAbs.split("/")[-1])[0]
            renderPath = (fAbs[:fAbs.index("/output/"+fName)] + "/" + renderpySettingsDict['renderDir']) if ("/output/"+f in fAbs) else ("/".join(fAbs.split("/")[:-1]))
            if os.path.isfile(renderPath + "/" + fName + ".png"):
                Say("    " + f + ' --> already rendered', 2)
            else:
                Say("    " + f, end='\r')
                callStr = ["blender", "--background", "--python", "render.py", "--", fAbs]              # Call string is with filename
                [callStr.extend([key,str(val)]) for key,val in renderpySettingsDict.items()]            # Append settingsDict
                Say("\nCall string = " + " ".join(callStr), verbosity=2)
                [stdout, _] = subprocess.Popen(callStr, stdout=subprocess.PIPE, stderr=subprocess.STDOUT).communicate()
                stdout = stdout.decode()
                if 'Error' in stdout or 'WARNING' in stdout:
                    with open('logfile.txt', 'w') as file:
                        file.write(time.strftime('%Y/%m/%d, %H:%M:%S') + " (" + fAbs + ")\n\n" + stdout)
                    if 'error' in stdout.lower() and 'warning' in stdout.lower():
                        suffix = "     --> WARNING and ERROR"
                    elif 'error' in stdout.lower():
                        suffix = "     --> ERROR"
                    else:
                        suffix = "     --> "
                        for line in stdout.split('\n'):
                            if 'warning' in line.lower():
                                suffix += line + ' '
                    Say("    " + f + suffix)
                else:
                    Say('', suppressTime=True)            # Make newline
    time.sleep(max(0, 10-(time.time()-t0)))               # There must be at least some time between each loop
        
