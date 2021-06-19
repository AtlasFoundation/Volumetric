package com.example.corto;

import android.content.Context;

public class MeshObj extends Mesh {

    public MeshObj(Context context, String mesh) {
        super(context, mesh);
    }

    @Override
    public void doTransformation(float[] mMatrix) {
        rotation.y += 0.5f;
        rotateY(mMatrix);
    }
}
