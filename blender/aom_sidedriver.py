#!/usr/bin/python
# -*- coding: utf-8 -*-

# Driver for aom.py

import sys
print(sys.path)

import os, re, time, subprocess

iterModDiv = 1

dirPathList = ["../results/aom_seed174_anmeStick_randomInit","../results/aom_seed174_fullStick_KsDSS1e-12","../results/aom_seed174_fullStick_KsDSS1e-13","../results/aom_seed174_fullStick_KsDSS1e-14","../results/aom_seed174_fullStick_KsDSS1e-15"]
#dirFilter = 'aom_seed5_anmeStick_randomInit_rAggregate07$'        # regular expression
#resultsPath = os.getcwd()[:os.getcwd().index("/blender")] + "/results"
#dirPathList = [os.path.join(resultsPath, d) for d in os.listdir(resultsPath) if os.path.isdir(os.path.join(resultsPath, d)) and os.path.isdir(os.path.join(resultsPath, d, 'output')) and re.search(dirFilter,d)]
#dirPathList.sort()
    
for d in dirPathList:
    print(time.strftime('%H:%M:%S   ') + d)
    dAbs = d + "/output"
    fileList = [files for files in os.listdir(dAbs) if os.path.splitext(files)[-1]=='.mat']
    fileList.sort()#reverse=True)
    for f in fileList:
#        ###################
#        # Optional: skip some files manually
#        if int(re.match('g(\d{4})r(\d{4}).mat',f).group(2)) != 250*5:
#            continue
#        ###################
        print(time.strftime('%H:%M:%S   ') + "\t" + f)
        if not int(re.match('g(\d{4})r(\d{4}).mat',f).group(2))%iterModDiv == 0:
            # relaxation iteration (YYYY in filename gXXXXrYYYY.mat) % iterModulusDivider == 0
            continue
        fAbs = dAbs + "/" + f
        callStr = ["blender", "--background", "--python", "aom_side.py", "--", fAbs]              # Call string is with filename
        [stdout, _] = subprocess.Popen(callStr, stdout=subprocess.PIPE, stderr=subprocess.STDOUT).communicate()
        stdout = stdout.decode()
        if 'Error' in stdout:
            print("#####################################")
            print(stdout)
            print("#####################################")
