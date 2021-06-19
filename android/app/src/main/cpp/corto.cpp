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
JNIEXPORT jstring Java_com_example_corto_MainActivity_decode (JNIEnv * env, jobject thiz, jbyteArray bytes){
    std::vector<int> coords;
    std::vector<int> index;
    std::vector<int> uvs;
    crt::MeshLoader out = crt::MeshLoader();

    int len = env->GetArrayLength (bytes);
    unsigned char* buf = new unsigned char[len];
    env->GetByteArrayRegion (bytes, 0, len, reinterpret_cast<jbyte*>(buf));

    __android_log_write(ANDROID_LOG_DEBUG, "DECODER", "**** FIRST BYTE IS (native)");
    __android_log_write(ANDROID_LOG_DEBUG, "DECODER", to_string(buf[0]).c_str());

    crt::Decoder decoder = crt::Decoder(len, buf);

   out.coords.resize(decoder.nvert*3);
   out.index.resize(decoder.nface*3);
   decoder.setPositions(out.coords.data());
   uvs.resize(decoder.nvert*2);
   decoder.setUvs(out.uvs.data());

   decoder.setIndex(out.index.data());


   decoder.decode();
   std::string dec = "Decoded";
   jstring str = env->NewStringUTF(dec.c_str());

    return str;
}
