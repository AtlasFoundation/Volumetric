#include <jni.h>
#include <string>
#include "decoder.cpp"
#include "meshloader.cpp"
#include <fstream>
#include <iostream>
#include <string>
#include <istream>
#include <sstream>
#include <dirent.h>
#include <android/log.h>

extern "C"
JNIEXPORT jobject Java_com_example_corto_MainActivity_decode (JNIEnv * env, jobject thiz, jbyteArray bytes){
    std::vector<uint16_t> index;
    std::vector<float> coords;
    std::vector<float> normals;
    std::vector<float> uvs;

    int len = env->GetArrayLength (bytes);
    unsigned char* buf = new unsigned char[len];
    env->GetByteArrayRegion (bytes, 0, len, reinterpret_cast<jbyte*>(buf));

    __android_log_write(ANDROID_LOG_DEBUG, "DECODER", "**** FIRST BYTE IS (native)");
    __android_log_write(ANDROID_LOG_DEBUG, "DECODER", to_string(buf[0]).c_str());

    crt::Decoder decoder = crt::Decoder(len, buf);

    coords.resize(decoder.nvert*3);
    index.resize(decoder.nface*3);
    uvs.resize(decoder.nvert*2);
    normals.resize(decoder.nvert*3);

    decoder.setPositions(coords.data());
    decoder.setUvs(uvs.data());
    decoder.setNormals(normals.data());
    decoder.setIndex(index.data());

    decoder.decode();

    // Create a jclass from actual Java object class path
    jclass geometryClass = env->FindClass("com/example/corto/Geometry");
    // Get constructor method
    jmethodID methodId = env->GetMethodID(geometryClass, "<init>", "()V");
    // Instantiate new object from class
    jobject geometryObj = env->NewObject(geometryClass, methodId);

    // TODO:
    // 1: Assign the index vector<int> array to the index [] from the geometryObj we just created
    // 2: Same for the normals, UVs and coords
    // 3: Return the object to java

    // jfieldID indexIntArrayField = env->GetFieldID(geometryClass, "index", "[I");
    // env->SetIntArrayRegion(indexIntArrayField, 0, index.size(), index.data());

    return geometryObj;
   
   
   
   
   
   
   
   std::string dec = "Decoded";
   jstring str = env->NewStringUTF(dec.c_str());

    return str;
}
