#!/usr/bin/python
# -*- coding: utf-8 -*-

# Driver for ecoli.py

import os, re, time, subprocess

iterModDiv = 1

dirPathList = ["/home/tomas/documenten/modelling/diatomas_symlink/results/as_low_bridging_seed2"]
for d in dirPathList:
    print(time.strftime('%H:%M:%S   ') + d)
    dAbs = d + "/output"
    fileList = [files for files in os.listdir(dAbs) if os.path.splitext(files)[-1]=='.mat']
    fileList.sort(reverse=True)
    for f in fileList:
        ###################
        # Optional: skip some files manually
        if int(re.match('g(\d{4})r(\d{4}).mat',f).group(2)) != 560:
            continue
        ###################
        print(time.strftime('%H:%M:%S   ') + "\t" + f)
        if not int(re.match('g(\d{4})r(\d{4}).mat',f).group(2))%iterModDiv == 0:
            # relaxation iteration (YYYY in filename gXXXXrYYYY.mat) % iterModulusDivider == 0
            continue
        fAbs = dAbs + "/" + f
        callStr = ["blender", "--background", "--python", "as_nofil.py", "--", fAbs]              # Call string is with filename
        [stdout, _] = subprocess.Popen(callStr, stdout=subprocess.PIPE, stderr=subprocess.STDOUT).communicate()
        stdout = stdout.decode()
        if 'Error' in stdout:
            print("#####################################")
            print(stdout)
            print("#####################################")
