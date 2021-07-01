#!/bin/bash 
#chmod u+x uVolBuildPrep.command

#dependencies

#homebrew
if ! command -v brew &> /dev/null
then
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"   
fi

#python
if ! command -v python3 &> /dev/null
then
brew install python3
fi

#pip
if ! command -v pip3 &> /dev/null
then
curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
python3 get-pip.py
fi

#mod-pbxproj
if ! pip list | grep -F pbxproj
then
python3 -m pip install pbxproj
fi

#run xcodesetup

BASEDIR=$(dirname "$0")
PYPATH="${BASEDIR}/attachUnityUVol.py"
echo "$PYPATH"
#python3 attachUnityUVol.py
python3 "$PYPATH"

