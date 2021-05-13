current_frame = 88

#current keframe raw binary string
bin_ = '{0:016b}'.format(current_frame)
print(bin_)



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

    for files in os.listdir(Data_Path):
        if files.endswith(".png"): # Exception Handling
                textures_in_group.append(files)
                print("Adding texture " +  str(frame_number+1) + " to group")
                frame_number = frame_number + 1

    for current_texture in range(len(textures_in_group)):
        # Read image
        # Draw test into corner
        # Read binary from frame number
        for value in '{0:016b}'.format(current_texture):
            # if value is '0', draw a black square
            # if value is '1', draw a white square
        # save the image

    print("Finished adding binary frame counter to textures");
    print("Make sure this isn't bleeding into your textures. You might need to scale your UVs to keep square texture.")
    print("We'll add UV autoscaling and appending later.")
#======================================================================================================================