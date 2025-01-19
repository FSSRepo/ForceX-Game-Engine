package com.fss.rpg;

import com.forcex.FX;
import com.forcex.anim.Animation;
import com.forcex.anim.AnimationControl;
import com.forcex.anim.Animator;
import com.forcex.app.EventType;
import com.forcex.app.Game;
import com.forcex.app.InputListener;
import com.forcex.app.Key;
import com.forcex.core.CoreJni;
import com.forcex.core.GL;
import com.forcex.core.gpu.Texture;
import com.forcex.gfx3d.Camera;
import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;
import com.forcex.gfx3d.ModelObject;
import com.forcex.gfx3d.ModelRenderer;
import com.forcex.gfx3d.Node;
import com.forcex.gfx3d.effect.Light;
import com.forcex.gfx3d.effect.shadow.ShadowMap;
import com.forcex.gfx3d.shapes.Box;
import com.forcex.gtasdk.DFFFrame;
import com.forcex.gtasdk.DFFSDK;
import com.forcex.gtasdk.DFFStream;
import com.forcex.gtasdk.IFPReader;
import com.forcex.gui.Font;
import com.forcex.gui.Layout;
import com.forcex.gui.UIContext;
import com.forcex.gui.View;
import com.forcex.gui.widgets.ImageView;
import com.forcex.gui.widgets.TextView;
import com.forcex.io.BinaryStreamReader;
import com.forcex.io.FileSystem;
import com.forcex.math.Maths;
import com.forcex.math.Matrix3f;
import com.forcex.math.Matrix4f;
import com.forcex.math.Quaternion;
import com.forcex.math.Vector2f;
import com.forcex.math.Vector3f;
import com.forcex.utils.GameUtils;
import com.forcex.utils.Image;

import static com.openpl.PL10.*;

import java.io.File;
import java.util.ArrayList;

public class RPGCarGame extends Game implements InputListener {
    UIContext ctx;
    float ox = -1, oy = -1;
    TextView tvTitle;
    ModelRenderer render_batch;
    ModelRenderer static_renderer;
    Camera camera;
    ArrayList<PhysicObject> physics_objects;
    ArrayList<ModelObject> static_objects;

    Vehicle vehicle;
    Player player;

    boolean[] key_controls = new boolean[255];
    ShadowMap shadowMap;
    TextView tvAction;

    Node vehicle_nodes;

    @Override
    public void create() {
        for(int i = 0; i < key_controls.length;i ++) {
            key_controls[i] = false;
        }
        // Interface
        ctx = new UIContext();
        Layout main = new Layout(ctx);
        ctx.bindKeyBoard(0.7f);
        tvTitle = new TextView(new Font("fonts/cascadia.fft"));
        tvTitle.setTextSize(0.04f);
        tvTitle.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
        main.add(tvTitle);
        tvAction = new TextView(UIContext.default_font);
        tvAction.setTextColor(255, 255, 255);
        tvAction.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
        tvAction.setVisibility(View.GONE);
        main.add(tvAction);
        ctx.setContentView(main);

        // Renderer
        camera = new Camera();
        render_batch = new ModelRenderer();
        static_renderer = new ModelRenderer(true);
        render_batch.useShadowMap(true, false);

        Light light = new Light();
        light.setPosition(20, 20, 20);
        render_batch.getEnvironment().setLight(light);
        static_renderer.getEnvironment().setLight(light);

        FX.device.addInputListener(this);
        if(plCreateContext()) {
            System.out.println("Physics engine initialized");
        }

        // Setup objects
        plDynamicWorldi(PL_DEBUG_MODE, PL_TRUE);
        plDynamicWorld3f(PL_GRAVITY, 0, -9.8f, 0);
        plDynamicWorld3f(PL_USE_AXIS_SWEEP3, 500, 500, 500);
        plDynamicWorldi(PL_INIT_VEHICLE_RAYCASTER, PL_TRUE);
        plDynamicWorldi(PL_INIT_CHARACTER, PL_TRUE);

        physics_objects = new ArrayList<>();
        static_objects = new ArrayList<>();
        PhysicObject floor = new PhysicObject(
                CollisionShape.createBox(new Vector3f(200, 1f, 200)),
                0.0f, new Matrix4f(), new Box(200, 1f, 200));

        floor.setShadowMapCullfaceEnabled(true);
        floor.getMesh().getPart(0).material.diffuseTexture = Texture.load("terrain.png");


        Box root_box = new Box(0.5f, 0.5f, 0.5f);
        root_box.getPart(0).material.diffuseTexture = Texture.load("C:\\Users\\stewa\\Pictures\\leonardo watch\\abcc3152b1bea208419002e5a50d0ef8.jpg");
        root_box.initialize();

        for(int i = 0; i < 5;i ++) {
            for(int j = 0; j < 5;j ++) {
                for(int k = 0; k < 5;k ++) {
                    PhysicObject box = new PhysicObject(
                            CollisionShape.createBox(new Vector3f(0.5f, 0.5f, 0.5f)),
                            2.0f, Matrix4f.fromTransform(new Quaternion(), new Vector3f( -10f + i * 4f,  20f + j * 10f, -10f + k * 4f)), root_box.clone());
                    physics_objects.add(box);
                }
            }
        }

        {
            Mesh test = readFx3d();
            PhysicObject prueba = new PhysicObject(CollisionShape.createMeshShape(test.getVertexData().vertices, test.getPart(0).index), 0f,
                    Matrix4f.fromTransform(new Quaternion(), new Vector3f(30, 4, 15)), test);
            physics_objects.add(prueba);
        }

        {
            FX3DModel test = readFx3d_v2().get(0);
            test.mesh.initialize();
            PhysicObject prueba = new PhysicObject(CollisionShape.createMeshShape(test.mesh.getVertexData().vertices, test.mesh.getPart(0).index), 0f,
                    Matrix4f.fromTransform(new Quaternion(), new Vector3f(0, 6, 0)), test.mesh);
            physics_objects.add(prueba);
        }

        {

            DFFSDK dff = DFFStream.readDFF("alucard.dff", null, null, null);
            ModelObject obj = dff.getObject(new ModelObject(), 0, false);
            static_objects.add(obj);

            {
                Mesh mesh = obj.getMesh();
                for(MeshPart part : mesh.getParts().list) {
                    part.material.diffuseTexture = Texture.load("alucard/" + part.material.textureName + ".png");
                }
            }
            player = new Player(obj, dff);
        }

        physics_objects.add(floor);

        {
            DFFSDK dff = DFFStream.readDFF("supergt.dff", null, null, null);
            Mesh wheel = null;

            for(int i = 0; i < dff.geometryCount; i++) {
                ModelObject o = dff.getObject(new ModelObject(), i, false);
                o.setID(i);
                if(o.getName().contains("_vlo")) {
                    o.setVisible(false);
                }
                {
                    Mesh mesh = o.getMesh();
                    for (MeshPart part : mesh.getParts().list) {
                        if(new File("audi/" + part.material.textureName + ".png").exists()) {
                            part.material.diffuseTexture = Texture.load("audi/" + part.material.textureName + ".png");
                        }
                    }
                    if (wheel == null && o.getName().equals("wheel")) {
                        mesh.initialize();
                        wheel = mesh.clone();
                    } else {
                        static_objects.add(o);
                    }
                }
            }

            vehicle_nodes = new Node("Audi");
            framesToNodes(dff.getFrameRoot(), vehicle_nodes);
            linkToObjects(vehicle_nodes);

            vehicle = new Vehicle(vehicle_nodes, wheel);
        }

        FX.gpu.setFPSLimit(60);

        ShadowMap.size = 1024;
        shadowMap = new ShadowMap(light, false, false);

        FX.device.setCursorState(false);
    }

    private Node framesToNodes(DFFFrame in, Node out) {
        out.setupRotationMatrix();
        out.position = in.position;
        out.rotation_matrix = in.rotation;
        out.setID(in.geoAttach);
        for(DFFFrame f : in.children) {
            out.addChild(framesToNodes(f, new Node(f.name)));
        }
        return out;
    }

    private void linkToObjects(Node nodes) {
        if(nodes.getID() != -1) {
            for(ModelObject o : static_objects) {
                if(o.getID() == nodes.getID()) {
                    nodes.attach(o);
                    break;
                }
            }
        }
        for(Node c : nodes.getChildren()) {
            linkToObjects(c);
        }
    }

    private Mesh readFx3d() {
        BinaryStreamReader is = FX.fs.open("terrain.fx3d", FileSystem.ReaderType.STREAM);
        System.out.println("Header: "+ is.readString(4));
        System.out.println("Name: "+ is.readString(10));
        boolean hasNormals = is.readInt() == 1;
        int num_vertices = is.readInt();
        System.out.println("Vertices: "+ num_vertices/3);
        Mesh mesh = new Mesh(true);
        mesh.setVertices(is.readFloatArray(num_vertices));
        if(hasNormals) {
            mesh.setNormals(is.readFloatArray(num_vertices));
        }
        int num_indices = is.readInt();
        System.out.println("Triangles: "+ num_indices/3);
        mesh.addPart(new MeshPart(is.readShortArray(num_indices)));
        return mesh;
    }

    public class FX3DModel {
        public Mesh mesh;
        public String name;
        public Vector3f position;
        public Vector3f scale;
        public Quaternion rotation;
    }

    private ArrayList<FX3DModel> readFx3d_v2() {
        BinaryStreamReader is = FX.fs.open("road.fx3d", FileSystem.ReaderType.STREAM);
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
           if(num_splits == 0) {
               return models;
           }
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
        plStepSimulation(deltaTime, 1, 1.0f / 60.0f);
        FX.gl.glClearColor(0.8f, 0.8f, 0.8f, 1);
        FX.gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        vehicle.update(camera, deltaTime, vehicle_nodes);
        player.update(camera, key_controls, deltaTime);
        camera.update();

        vehicle_nodes.update();

        {
            // shadow mapping render
            shadowMap.begin();
            for(PhysicObject obj : physics_objects) {
                obj.updatePhysics();
                obj.update();
                shadowMap.render(obj);
            }
            shadowMap.end();
        }

        render_batch.shadowMap = shadowMap.getShadowTexture();

        render_batch.begin(camera);
        vehicle.render(render_batch);

        for(PhysicObject obj : physics_objects) {
            render_batch.render(obj);
        }

        render_batch.end();

        static_renderer.begin(camera);
        for(ModelObject obj : static_objects) {
            static_renderer.render(obj);
        }

        player.render(static_renderer);
        static_renderer.end();

        ctx.draw();

        if(player.onFoot && (GameUtils.distance(player.player.getPosition(), vehicle.chassis.getPosition()) < 10)) {
            tvAction.setText("Press F to drive");
            tvAction.setVisibility(View.VISIBLE);
        } else {
            tvAction.setVisibility(View.GONE);
        }

        tvTitle.setText(player.player_transform.getLocation(null).toString());
    }

    private static float ZOOM_CAMERA_AMOUNT = 20;
    private static float MOVING_CAMERA = 5f;

    @Override
    public void onTouch(float x, float y, byte type, int pointer) {
        if(type == EventType.MOUSE_SCROLL) {
            if(vehicle.onVehicle) {
                vehicle.distance += FX.gpu.getDeltaTime() * y * ZOOM_CAMERA_AMOUNT;
            } else if(player.onFoot) {
                player.distance += FX.gpu.getDeltaTime() * y * ZOOM_CAMERA_AMOUNT;
            }
            return;
        }
        Vector2f touch = GameUtils.getTouchNormalized(x, y);
        if (!ctx.testTouch(touch.x, touch.y)) {
            if(type == EventType.MOUSE_HOVER) {
                if(ox == -1) {
                    ox = x;
                    oy = y;
                }

                float dx = (x - ox) * MOVING_CAMERA * FX.gpu.getDeltaTime();
                float dy = (y - oy) * MOVING_CAMERA * FX.gpu.getDeltaTime();

                // no lock angles
                if(player.onFoot) {
                    player.rot_x += dx;
                    player.rot_y += dy;
                } else if(vehicle.onVehicle) {
                    vehicle.rot_x += dx;
                    vehicle.rot_y += dy;
                }
                ox = x;
                oy = y;
            }
        } else {
            ctx.onTouch(touch.x, touch.y, type);
        }
    }

    @Override
    public void onKeyEvent(byte key, boolean down) {
        if(down && key == Key.KEY_ESC) {
            FX.device.destroy();
        }
        if(down && key == Key.F_KEY) {
            player.onFoot = !player.onFoot;
            vehicle.onVehicle = !vehicle.onVehicle;
        }
        key_controls[key] = down;
        vehicle.control(key, down);
        player.control(key, down);
        ctx.onKeyEvent(key, down);
    }

    @Override
    public void destroy() {
        plDestroyContext();
        ctx.destroy();
    }
}