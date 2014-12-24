#!/usr/bin/python
# -*- coding: utf-8 -*-
import os, time, subprocess, re 

def Say(text, verbosity=0, end='\n'):
    if verbosity<=VERBOSITY:
        if verbosity == 0:
            print(time.strftime('%H:%M:%S   ') + text, end=end)
        else: 
            print(time.strftime('DEBUG: ') + text, end=end)

###############################################################################
VERBOSITY = 0
dirFilter = '.*'
renderpySettingsDict = {'VERBOSITY':VERBOSITY,
                        'resolution_percentage':50,
                        'model.L':'array([50e-6,50e-6,50e-6])',
                        }

###############################################################################
# Debugging
resultsPath = os.getcwd()[:os.getcwd().index("/blender")] + "/results"
while True:
    t0 = time.time()
    dirList = [dirs for dirs in os.listdir(resultsPath) if os.path.isdir(os.path.join(resultsPath, dirs)) and re.search(dirFilter,dirs)]
    dirList.sort()
    for d in dirList:
        Say(d)
        dAbs = resultsPath + "/" + d + "/output"
        Say(dAbs, 2)
        fileDir = [files for files in os.listdir(dAbs) if os.path.splitext(files)[-1]=='.mat']
        fileDir.sort(reverse=True)
        for f in fileDir:
            fAbs = dAbs + "/" + f
            # Check if file is already plotted
            fName = os.path.splitext(fAbs.split("/")[-1])[0]
            renderDir = (fAbs[:fAbs.index("/output/"+fName)] + "/render") if ("/output/"+f in fAbs) else ("/".join(fAbs.split("/")[:-1]))
            if os.path.isfile(renderDir + "/" + fName + ".png"):
                Say("    " + f + ' --> already rendered', 2)
            else:
                Say("    " + f, end='\r')
                callStr = ["blender", "--background", "--python", "render.py", "--", fAbs]
                [callStr.extend([key,str(val)]) for key,val in renderpySettingsDict.items()]
                Say("\nCall string = " + " ".join(callStr), verbosity=2)
                [stdout, _] = subprocess.Popen(callStr, stdout=subprocess.PIPE, stderr=subprocess.STDOUT).communicate()
                if 'Error' in stdout.decode() or 'WARNING' in stdout.decode():
                    with open('logfile.txt', 'w') as file:
                        file.write(time.strftime('%Y/%m/%d, %H:%M:%S') + " (" + fAbs + ")\n\n" + stdout.decode())
                        Say("    " + f + "     --> warning/error")
                else:
                    Say('')                                 # Make newline

    time.sleep(max(0, 60-(time.time()-t0)))               # There must be at least 60 seconds between each loop
        