# Binary sequence texture encoder
# Takes a sequence of PNG images
# Adds a macro 

# importing cv2 
import cv2
  
# importing os module  
import os

dirname = os.path.dirname(__file__)

encoderWindowSize = 8;

#======================================================================================================================
#--------------------------------------------- MAIN Function ----------------------------------------------------------
#======================================================================================================================

def main():
    frame_number = 0
    textures_in_group = []

    # TODO: Add path to where you can to encode, default to this
    Data_Path = dirname + "/encode"

    # Exception Handling : If 'encode' folder is not there, create one
    assert os.path.exists(Data_Path), 'The Dataset Folder not found. Please consider giving full(absolute) file path.'

    print("Main")
    for files in os.listdir(Data_Path):
        if files.endswith(".png"): # Exception Handling
                textures_in_group.append(Data_Path+"/"+files)
                print("Adding texture " +  str(frame_number+1) + " to group")
                frame_number = frame_number + 1

    for current_texture in range(len(textures_in_group)):
        # Read image
        print(img)
        height, width, channels = img.shape
        # Read binary from frame number
        for index, value in enumerate('{0:016b}'.format(current_texture)):
            color = (0,0,0)
            if(value == '1'):
                color = (255,255,255)
            print("Painting color at")
            print(color)
            print(height-1-encoderWindowSize)
            cv2.rectangle(img,(index*encoderWindowSize, height-1-encoderWindowSize),((index+1)*encoderWindowSize,height-1),color,-1)
        
        # save the image
        cv2.imwrite(textures_in_group[current_texture], img)

    print("Finished adding binary frame counter to textures");
    print("Make sure this isn't bleeding into your textures. You might need to scale your UVs to keep square texture.")
    print("We'll add UV autoscaling and appending later.")
#======================================================================================================================

if __name__ == '__main__':
    main()
