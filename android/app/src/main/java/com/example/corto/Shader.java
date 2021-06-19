package com.example.corto;

import android.content.Context;
import android.opengl.GLES20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public abstract class Shader {

    private final int program;

    public String read(Context context, String filename){

        StringBuilder buf = new StringBuilder();
        InputStream text;
        BufferedReader in;
        String str;

        try {
            text = context.getAssets().open(filename);
            in = new BufferedReader(new InputStreamReader(text, StandardCharsets.UTF_8));
            while ( (str = in.readLine()) != null ) {
                str += '\n';
                buf.append(str);
            }
            in.close();

            return buf.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new RuntimeException("Error loading file "+filename+".");

    }

    public Shader(Context context, String vs, String fs){
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, compile(GLES20.GL_VERTEX_SHADER, read(context, vs)));
        GLES20.glAttachShader(program, compile(GLES20.GL_FRAGMENT_SHADER, read(context, fs)));
        GLES20.glLinkProgram(program);
    }

    public abstract void bindData();
    public abstract void unbindData();

    private int compile(int type, String source){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public int getProgram(){
        return program;
    }

    protected int getAttrib(String name){
        return GLES20.glGetAttribLocation(program,name);
    }

    protected int getUniform(String name){
        return GLES20.glGetUniformLocation(program,name);
    }
}
