package com.fss.ai;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.app.Game;
import com.forcex.app.InputListener;
import com.forcex.app.Key;
import com.forcex.core.GL;
import com.forcex.core.gpu.Texture;
import com.forcex.gfx3d.Camera;
import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;
import com.forcex.gfx3d.ModelObject;
import com.forcex.gfx3d.ModelRenderer;
import com.forcex.gfx3d.effect.Light;
import com.forcex.gui.Font;
import com.forcex.gui.Layout;
import com.forcex.gui.UIContext;
import com.forcex.gui.widgets.TextView;
import com.forcex.io.BinaryStreamReader;
import com.forcex.io.FileSystem;
import com.forcex.math.Matrix4f;
import com.forcex.math.Quaternion;
import com.forcex.math.Vector3f;
import com.forcex.postprocessor.BloomPass;
import com.forcex.postprocessor.BlurPass;
import com.forcex.postprocessor.BrightnessPass;
import com.forcex.postprocessor.ContrastPass;
import com.forcex.postprocessor.FrameBuffer;
import com.forcex.postprocessor.PostProcessing;

import java.util.ArrayList;

public class AIScreen extends Game implements InputListener {
    UIContext ctx;
    TextView tvTitle;
    ModelRenderer abstract_render;
    Camera camera;
    ArrayList<ModelObject> static_objects;

    PostProcessing post;
    FrameBuffer fbo;

    @Override
    public void create() {
        // Interface
        ctx = new UIContext();
        Layout main = new Layout(ctx);
        ctx.bindKeyBoard(0.7f);
        tvTitle = new TextView(new Font("fonts/cascadia.fft"));
        tvTitle.setText("Super AI");
        tvTitle.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
        main.add(tvTitle);
        ctx.setContentView(main);

        static_objects = new ArrayList<>();

        // Renderer
        camera = new Camera();
        abstract_render = new ModelRenderer();

        {
            ArrayList<FX3DModel> fxm = readFx3d_v2();

            for(FX3DModel md : fxm) {
                md.mesh.initialize();
                ModelObject obj = new ModelObject(md.mesh);
                obj.getTransform().setLocation(md.position);
                obj.getTransform().setScale(md.scale);
                if(md.name.contains("ring-norm")) {
                    MeshPart part = obj.getMesh().getPart(0);
                    part.material.color.set(20, 250, 160);
                    part.material.diffuseTexture = Texture.load("abstract.png");
                }
                if(md.name.contains("ring-light")) {
                    MeshPart part = obj.getMesh().getPart(0);
                    part.material.color.set(120, 80, 90, 120);
                }
                if(md.name.contains("ring-solid")) {
                    MeshPart part = obj.getMesh().getPart(0);
                    part.material.color.set(190, 100, 90, 200);
                }
                static_objects.add(obj);
            }
        }

        FX.gpu.setFPSLimit(60);

        camera.setPosition(0,400, 0);

        camera.lookAt(0,0,0);

        System.out.println(camera.direction.toString());
        System.out.println(camera.up.toString());

        camera.update();

        System.out.println(camera.getProjViewMatrix().toString());

        FX.device.addInputListener(this);

        fbo = new FrameBuffer(FX.gpu.getWidth(), FX.gpu.getHeight());
        post = new PostProcessing();

        post.addPass(new BloomPass(new BrightnessPass(FX.gpu.getWidth(), FX.gpu.getHeight()),
                new BlurPass(BlurPass.HORIZONTAL, true, FX.gpu.getWidth() / 2, FX.gpu.getHeight() / 2),
                new BlurPass(BlurPass.VERTICAL, true, FX.gpu.getWidth() / 2, FX.gpu.getHeight() / 2)));
//        post.addPass(new ContrastPass());
    }

    public class FX3DModel {
        public Mesh mesh;
        public String name;
        public Vector3f position;
        public Vector3f scale;
        public Quaternion rotation;
    }

    private ArrayList<FX3DModel> readFx3d_v2() {
        BinaryStreamReader is = FX.fs.open("sci.fx3d", FileSystem.ReaderType.STREAM);
        System.out.println("Header: "+ is.readString(4));
        int num_objects = is.readInt();
        ArrayList<FX3DModel> models = new ArrayList<>();
       for(int i = 0; i < num_objects;i ++) {
           FX3DModel model = new FX3DModel();
           model.name = is.readString(20);
           System.out.println("Name: "+ model.name);
           boolean hasNormals = is.readInt() == 1;
           boolean hasUVs = is.readInt() == 1;
           boolean keep_transformation = is.readInt() == 1;
           if(keep_transformation) {
               model.position = is.readVector();
               model.rotation = Quaternion.fromEulerAngles(is.readFloat(), is.readFloat(), is.readFloat());
               model.scale = is.readVector();
           }

           int vertex_count = is.readInt();

           System.out.println("Vertex Count: "+ vertex_count);
           Mesh mesh = new Mesh(true);
           mesh.setVertices(is.readFloatArray(vertex_count * 3));
           if(hasUVs) {
               mesh.setTextureCoords(is.readFloatArray(vertex_count * 2));
           }
           if(hasNormals) {
               mesh.setNormals(is.readFloatArray(vertex_count * 3));
           }
           int num_splits = is.readInt();
           System.out.println("splits: " + num_splits);
           for(int j = 0; j < num_splits; j ++) {
               int num_indices = is.readInt();
               float[] color = is.readFloatArray(4);
               System.out.println("Triangles: "+ num_indices/3);
               MeshPart part = new MeshPart(is.readShortArray(num_indices));
               part.material.color.set((int)(color[0] * 255), (int)(color[1] * 255),(int)(color[2] * 255), (int)(color[3] * 255));
               System.out.println(part.material.color.toString());
               mesh.addPart(part);
           }
           model.mesh = mesh;
           models.add(model);
       }
       return models;
    }

    @Override
    public int pause(int type) {
        if (type == EventType.BACK_BUTTON) {
            return EventType.REQUEST_EXIT;
        }
        return 0;
    }

    @Override
    public void render(float deltaTime) {

        camera.update();

        fbo.begin();
        FX.gl.glClearColor(0, 0, 0, 1);
        FX.gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        abstract_render.begin(camera);
        for(ModelObject obj : static_objects) {
            abstract_render.render(obj);
        }
        abstract_render.end();
        fbo.end();

        post.doProcessing(fbo.getTexture());

        ctx.draw();
    }

    private static float ZOOM_CAMERA_AMOUNT = 20;
    private static float MOVING_CAMERA = 5f;

    @Override
    public void onTouch(float x, float y, byte type, int pointer) {

    }

    @Override
    public void onKeyEvent(byte key, boolean down) {
        if(key == Key.KEY_ESC) {
            FX.device.destroy();
        }
    }

    @Override
    public void destroy() {
        ctx.destroy();
    }
}