from pbxproj import XcodeProject
from pbxproj.pbxextensions import ProjectFiles
from pbxproj.pbxextensions.ProjectFiles import FileOptions
from pbxproj.pbxextensions.ProjectFiles import HeaderScope
from pbxproj.pbxsections.PBXBuildFile import PBXBuildFile
import os

cur_dir = os.path.dirname(os.path.realpath(__file__))#os.getcwd()# fails when running py script using full path
path = os.path.abspath(cur_dir)
parentdir = os.path.abspath(os.path.join(path, (os.pardir)))

FrameWorkBuilderFolder = path.replace(parentdir+'/', '')
#print('frameworkbuilderfolder '+FrameWorkBuilderFolder)



nativeprojectdir = ""
for f in os.listdir(parentdir):
	if "xcodeproj" in f:
		nativeprojectdir = os.path.join(parentdir, f)

if nativeprojectdir == "":
	print("Place this folder in the same directory as your xcodeproj file!")
	exit()
print('native project '+nativeprojectdir+'/project.pbxproj')
project = XcodeProject.load(nativeprojectdir+'/project.pbxproj')
ProjectFiles._FILE_TYPES[u'.pbxproj'] = (u'project.pbxproj', u'PBXResourcesBuildPhase')
ProjectFiles._FILE_TYPES[u'.xctest'] = (u'project.xctest', u'PBXResourcesBuildPhase')


frameworkprojectpath = os.path.normpath(os.path.abspath(cur_dir+ '/uVolPlayerLibrary/Unity-iPhone.xcodeproj'))#os.path.abspath(os.path.join(path,('/uVolPlayerLibrary/')))
print('UVol path '+(frameworkprojectpath))

os.popen('chmod u+x "' + frameworkprojectpath.replace('Unity-iPhone.xcodeproj','MapFileParser.sh')+'"')
os.popen('chmod u+x "' + frameworkprojectpath.replace('Unity-iPhone.xcodeproj','process_symbols.sh')+'"')

rootGroup = project._get_parent_group( None )


#add unity project if not added
unityiphonefiles = project.get_files_by_name('Unity-iPhone.xcodeproj')
if(len(unityiphonefiles)==0):
    print('no unityfiles. tying to get from ' + frameworkprojectpath)
    project.add_file(frameworkprojectpath, parent=rootGroup, force=True)
    unityiphonefiles = project.get_files_by_name('Unity-iPhone.xcodeproj')
else:
    print('detected some unityfiles')
print('have ' + str(len(unityiphonefiles)) + ' files')
#print('unity proj path ' + unityiphonefiles[0].get_name()+ str(unityiphonefiles[0].get_parent().get_path()))
if (len(unityiphonefiles)==0):
    print('failed to add unity project. quitting setup')
    exit()
else:
    print ((unityiphonefiles[0]))


frameworks = project.get_groups_by_name('Frameworks')#second group is from unityproj
if(len(project.get_files_by_name('UnityFramework.framework', frameworks[0]))==0):
    project.add_file('UnityFramework.framework', parent=frameworks[0], tree='BUILT_PRODUCTS_DIR', force=False, file_options = FileOptions(weak=True, code_sign_on_copy=True, create_build_files=True))
else:
    print('already added UnityFramework.framework')


if(len(project.get_files_by_name('NatCorder.framework', frameworks[0]))==0):
    project.add_file(FrameWorkBuilderFolder+'/uVolPlayerLibrary/Frameworks/NatSuite/NatCorder/Plugins/iOS/NatCorder.framework', parent=frameworks[0], tree='SOURCE_ROOT', force=False, file_options = FileOptions(weak=True, code_sign_on_copy=True, create_build_files=True))
else:
    print('already added NatCorder.framework')

productgroups = project.get_groups_by_name('Products')#second group isfrom unityproj


#modify unity project
unityproject = XcodeProject.load(frameworkprojectpath+'/project.pbxproj')

#args = {
#    '--target': 'UnityFramework',
#    '--header-scope': 'public'
#}
#nobj = nativeCallProxy[0].parse(args)
nativeProjectUVolGroup = project.get_or_create_group('UVolPlayer')
#copy UVolPlayer to native Project
uVolPlayerH = unityproject.get_files_by_name('UVolPlayer.h')
uVolPlayerM = unityproject.get_files_by_name('UVolPlayer.mm')

projectTarget = ''
for obj in project.objects.get_targets():
    if('application' in obj.productType):
        projectTarget = (str(obj.productName))
    #if('mm' in obj..get_keys().name):
    #    print('target to add uvolplayer to'+obj.get_targets[0].get_name())
if(len(uVolPlayerH)>0):
    project.add_file(FrameWorkBuilderFolder+'/uVolPlayerLibrary/Libraries/_uVolPlayer/Plugins/iOS/UVolPlayer.h', parent=nativeProjectUVolGroup, target_name=projectTarget,file_options=FileOptions(header_scope=HeaderScope.PUBLIC))
    unityproject.remove_file_by_id(uVolPlayerH[0].get_id())
    project.add_file(FrameWorkBuilderFolder+'/uVolPlayerLibrary/Libraries/_uVolPlayer/Plugins/iOS/UVolPlayer.mm', parent=nativeProjectUVolGroup, target_name=projectTarget,file_options=FileOptions(header_scope=HeaderScope.PUBLIC))
    unityproject.remove_file_by_id(uVolPlayerM[0].get_id())


#update Target Membership for data and nativecallproxy file
nativeCallProxy = unityproject.get_files_by_name('NativeCallProxy.h')
#print('nativecallproxy file  ' + str((nativeCallProxy[0])))
if(len(nativeCallProxy)<2):
     unityproject.add_file('Libraries/_uVolPlayer/Plugins/iOS/NativeCallProxy.h',  target_name='UnityFramework', file_options=FileOptions(header_scope=HeaderScope.PUBLIC))
     unityproject.remove_file_by_id(nativeCallProxy[0].get_id())
else:
    print('already created nativcallproxy.h buildfile')
    
#Data folder is a file reference
Datafolderref = unityproject.get_files_by_name('Data')
if(len(Datafolderref)<2):
    unityproject.add_file('Data', target_name='UnityFramework')
    unityproject.remove_file_by_id(Datafolderref[0].get_id())
else:
    print('already created Data buildfile')
print('datagroup ' + str(Datafolderref))
#for file in nativeCallProxy:
#    print (str(file.get_configurations_on_targets('UnityFramework')))
print (str(unityproject.get_target_by_name('UnityFramework')))

print('nativecallproxyfile ' + str((nativeCallProxy[0])))
#nativeCallProxyID =nativeCallProxy[0].get_id()
unityframeworktarget = unityproject.get_target_by_name('UnityFramework')

print(str(unityframeworktarget.buildPhases[0]))


build_files_to_remove = []
build_files = project.objects.get_objects_in_section('PBXBuildFile')
for build_file in build_files:
    if('UnityFramework.framework' in str(build_file) or 'NatCorder.framework' in str(build_file)):
        build_files_to_remove.append(build_file)#
    #if(build_file.get_attributes() == 'UnityFramework.framework'):
    #    print(str(build_file))



for targ in project.objects.get_targets():
    for build_phase_id in targ.buildPhases:
        if(project.objects[build_phase_id].isa=='PBXFrameworksBuildPhase'):#PBXResourcesBuildPhase, PBXFrameworksBuildPhase, PBXSourcesBuildPhase
            for buildfile in build_files_to_remove:
                if(buildfile.get_id() in str(project.objects[build_phase_id].files)):
                    project.objects[build_phase_id].remove_build_file(buildfile)
#print(str(project.objects[build_phase_id].files))


unityproject.save()

project.save()
exit()
