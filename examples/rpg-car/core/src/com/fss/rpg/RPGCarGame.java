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
import com.forcex.gfx3d.effect.SkyBox;
import com.forcex.gfx3d.effect.shadow.ShadowMap;
import com.forcex.gfx3d.particle.ParticleRenderer;
import com.forcex.gfx3d.shapes.Box;
import com.forcex.gtasdk.DFFFrame;
import com.forcex.gtasdk.DFFSDK;
import com.forcex.gtasdk.DFFStream;
import com.forcex.gtasdk.IFPReader;
import com.forcex.gui.Font;
import com.forcex.gui.UIContext;
import com.forcex.gui.UiInflater;
import com.forcex.gui.View;
import com.forcex.gui.Layout;
import com.forcex.gui.widgets.Button;
import com.forcex.gui.widgets.ImageView;
import com.forcex.gui.widgets.Joystick;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

public class RPGCarGame extends Game implements InputListener {
    UIContext ctx;
    float ox = -1, oy = -1;
    TextView tvTitle;
    ModelRenderer render_batch;
    ModelRenderer static_renderer;
    Camera camera;
    ArrayList<PhysicObject> physics_objects;
    ArrayList<ModelObject> static_objects;
    ArrayList<Car> cars;

    Vehicle vehicle;
    Player player;
    Scenery scenery;
    ParticleRenderer particles;
    SkyBox sky;

    boolean[] key_controls = new boolean[255];
    ShadowMap shadowMap;
    TextView tvAction;
    Layout controls;
    Joystick joystick;
    Button btnEnterExit;
    // id del dedo que controla cada elemento (-1 = ninguno)
    int joystick_pointer = -1, button_pointer = -1, look_pointer = -1;
    boolean[] virtual_keys = new boolean[255];
    private static final float TOUCH_LOOK_SENSITIVITY = 0.1f;

    Node vehicle_nodes;

    @Override
    public void create() {
        for(int i = 0; i < key_controls.length;i ++) {
            key_controls[i] = false;
        }

        // Interface
        ctx = new UIContext();
        UiInflater inflater = new UiInflater(ctx);
        View hud = inflater.inflateFile("game/layouts/hud.yaml");
        tvTitle = (TextView) inflater.findViewById("title");
        tvAction = (TextView) inflater.findViewById("action");
        ctx.setContentView(hud);

        // Controles táctiles: se dibujan encima y reciben los toques desde onTouch
        inflater.setOnClickListener("onEnterExit", view -> toggleVehicle());
        controls = (Layout) inflater.inflateFile("game/layouts/controls.yaml");
        controls.local.set(FX.gpu.getWidth() * 0.5f, FX.gpu.getHeight() * 0.5f);
        ctx.addUntouchableView(controls);
        joystick = (Joystick) inflater.findViewById("joystick");
        btnEnterExit = (Button) inflater.findViewById("enter_exit");
        int disc = createDiscTexture(128);
        joystick.setTextureID(disc);
        joystick.setKnobTextureID(disc);

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
        // terreno procedural (la semilla fija el relieve)
        Terrain terrain = new Terrain(1337);
        PhysicObject floor = terrain.createPhysicObject();

        floor.setShadowMapCullfaceEnabled(true);
        floor.getMesh().getPart(0).material.diffuseTexture = Texture.load("game/terrain.png");


        Box root_box = new Box(0.5f, 0.5f, 0.5f);
        root_box.getPart(0).material.diffuseTexture = Texture.load("game/texture.png");
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

        // {
        //     FX3DModel test = readFX3D("game/piece.fx3d").get(0);
        //     test.mesh.initialize();
        //     PhysicObject prueba = new PhysicObject(CollisionShape.createMeshShape(test.mesh.getVertexData().vertices, test.mesh.getPart(0).index), 0f,
        //             Matrix4f.fromTransform(new Quaternion(), new Vector3f(30, 2, 20)), test.mesh);
        // //    ModelObject prueba = new ModelObject(test.mesh);
        // //    test.mesh.getPart(0).material.color.set(255,0,0);
        // //    prueba.setPosition(new Vector3f(30, 4, 15));
        //     physics_objects.add(prueba);
        // }

        {
            DFFSDK dff = DFFStream.readDFF("game/player.dff", null, null, null);
            ModelObject obj = dff.getObject(new ModelObject(), 0, false);
            static_objects.add(obj);

            {
                Mesh mesh = obj.getMesh();
                for(MeshPart part : mesh.getParts().list) {
                    // hay partes del modelo sin textura (nombre vacío)
                    if(part.material.textureName == null || part.material.textureName.isEmpty()) continue;
                    String texture_path = "game/player/" + part.material.textureName + ".png";
                    part.material.diffuseTexture = Texture.load(texture_path);
                }
            }
            player = new Player(obj, dff);
        }

        physics_objects.add(floor);

        // arboles y chimeneas sobre el terreno
        scenery = new Scenery(terrain, 2024);
        physics_objects.addAll(scenery.solids);
        static_objects.addAll(scenery.decorations);
        particles = new ParticleRenderer();
        particles.create();

        // cielo procedural; el sol sale por donde esta la luz de la escena
        sky = new SkyBox(10f);
        sky.setSunDirection(light.getPosition().x, light.getPosition().y, light.getPosition().z);

        {
            DFFSDK car_dff = DFFStream.readDFF("game/slamvan.dff", null, null, null);
            Mesh wheel_source = null, any_wheel = null;
            ArrayList<ModelObject> car_template = new ArrayList<>();
            // varios materiales comparten textura: se carga una sola vez por nombre
            HashMap<String, Integer> car_textures = new HashMap<>();
            for(int i = 0; i < car_dff.geometryCount; i++) {
                ModelObject o = car_dff.getObject(new ModelObject(), i, false);
                o.setID(i);
                if(o.getName().contains("_vlo")) {
                    o.setVisible(false);
                }

                Mesh mesh = o.getMesh();
                for (MeshPart part : mesh.getParts().list) {
                    String texture_name = part.material.textureName;
                    if(texture_name == null || texture_name.isEmpty()) continue;
                    Integer texture = car_textures.get(texture_name);
                    if(texture == null) {
                        texture = Texture.load("game/car/" + texture_name + ".png");
                        car_textures.put(texture_name, texture);
                    }
                    part.material.diffuseTexture = texture;
                }
                mesh.initialize();

                // las ruedas las dibuja Vehicle con la fisica: una sola malla de rueda (izquierda) sirve para las cuatro
                if(o.getName().startsWith("wheel")) {
                    if(any_wheel == null) {
                        any_wheel = mesh;
                    }
                    if(wheel_source == null && (o.getName().equals("wheel") || o.getName().equals("wheel_lf"))) {
                        wheel_source = mesh;
                    }
                } else {
                    car_template.add(o);
                }
            }
            if(wheel_source == null) {
                wheel_source = any_wheel;
            }

            cars = new ArrayList<>();
            Vector3f[] spawns = {
                    new Vector3f(23, 2.5f, 10),
                    new Vector3f(33, 2.5f, 10),
            };
            for(Vector3f spawn : spawns) {
                cars.add(buildCar(car_dff, wheel_source, car_template, spawn));
            }

            vehicle = cars.get(0).vehicle;
            vehicle_nodes = cars.get(0).nodes;
        }

        FX.gpu.setFPSLimit(60);

        shadowMap = new ShadowMap(light, false, false, 1024);

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

    private Car buildCar(DFFSDK dff, Mesh wheel_source, ArrayList<ModelObject> template, Vector3f spawn) {
        ArrayList<ModelObject> objects = new ArrayList<>();
        for(ModelObject src : template) {
            ModelObject o = new ModelObject(src.getMesh().clone());
            o.setID(src.getID());
            o.setName(src.getName());
            o.setVisible(src.isVisible());
            objects.add(o);
        }

        Node nodes = new Node("Vehicle ");
        framesToNodes(dff.getFrameRoot(), nodes);
        linkToObjects(nodes, objects);

        return new Car(nodes, new Vehicle(spawn, nodes, wheel_source.clone()), objects);
    }

    private void linkToObjects(Node nodes, ArrayList<ModelObject> objects) {
        if(nodes.getID() != -1) {
            for(ModelObject o : objects) {
                if(o.getID() == nodes.getID()) {
                    nodes.attach(o);
                    break;
                }
            }
        }
        for(Node c : nodes.getChildren()) {
            linkToObjects(c, objects);
        }
    }

    public static class Car {
        public final Node nodes;
        public final Vehicle vehicle;
        public final ArrayList<ModelObject> objects;

        public Car(Node nodes, Vehicle vehicle, ArrayList<ModelObject> objects) {
            this.nodes = nodes;
            this.vehicle = vehicle;
            this.objects = objects;
        }
    }

    public class FX3DModel {
        public Mesh mesh;
        public String name;
        public Vector3f position;
        public Vector3f scale;
        public Quaternion rotation;
    }

    private ArrayList<FX3DModel> readFX3D(String file) {
        BinaryStreamReader is = FX.fs.open(file, FileSystem.ReaderType.STREAM);
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
               System.out.println("No hay splits");
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

        for(Car car : cars) {
            car.vehicle.update(camera, deltaTime, car.nodes);
        }
        player.update(camera, key_controls, deltaTime);
        updateDoor(deltaTime);
        camera.update();

        for(Car car : cars) {
            car.nodes.update();
        }

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

        render_batch.setShadowMap(shadowMap);

        sky.render(camera);

        render_batch.begin(camera);
        for(Car car : cars) {
            car.vehicle.render(render_batch);
        }

        for(PhysicObject obj : physics_objects) {
            render_batch.render(obj);
        }

        render_batch.end();

        static_renderer.begin(camera);
        for(Car car : cars) {
            for(ModelObject obj : car.objects) {
                static_renderer.render(obj);
            }
        }
        for(ModelObject obj : static_objects) {
            static_renderer.render(obj);
        }

        player.render(static_renderer);
        if(!player.onFoot && vehicle.onVehicle) {
            Vector3f seat_forward = new Vector3f();
            Vector3f seat = vehicle.getDriverSeat(vehicle_nodes, seat_forward);
            player.renderSeated(static_renderer, seat, seat_forward);
        }
        static_renderer.end();

        scenery.update(particles, deltaTime, camera.getPosition());
        particles.update(deltaTime);
        particles.render(camera);

        ctx.draw();

        updateVirtualKeys();
        Car enterable = nearestCar();
        if(player.onFoot && enterable != null) {
            tvAction.setText("Press F to drive");
            tvAction.setVisibility(View.VISIBLE);
            btnEnterExit.setText("Subir");
            btnEnterExit.setVisibility(View.VISIBLE);
        } else {
            tvAction.setVisibility(View.GONE);
            if(player.onFoot) {
                btnEnterExit.setVisibility(View.GONE);
            } else {
                btnEnterExit.setText("Bajar");
                btnEnterExit.setVisibility(View.VISIBLE);
            }
        }
        if(vehicle != null) {
        tvTitle.setText(vehicle_nodes.position.toString());
        }
    }

    // puerta del auto que se esta abriendo/cerrando al subir o bajar, suavizada hacia player.door_target
    private Car door_car;
    private float door_open = 0.0f;
    private static final float DOOR_OPEN_ANGLE = -70f;
    private static final float DOOR_SPEED = 2.5f;

    private void updateDoor(float delta) {
        if(door_car == null) {
            return;
        }
        float step = DOOR_SPEED * delta;
        float diff = player.door_target - door_open;
        door_open += Math.max(-step, Math.min(step, diff));
        door_car.vehicle.setDriverDoorAngle(door_car.nodes, DOOR_OPEN_ANGLE * door_open);
        if(door_open == 0.0f && player.door_target == 0.0f && !player.entering && !player.exiting) {
            door_car = null;
        }
    }

    private static float ZOOM_CAMERA_AMOUNT = 20;
    private static float MOVING_CAMERA = 5f;
    private static final float ENTER_DISTANCE = 10f;

    private Car nearestCar() {
        Vector3f player_position = player.player.getPosition();
        Car best = null;
        float best_distance = ENTER_DISTANCE;
        for(Car car : cars) {
            float distance = GameUtils.distance(player_position, car.nodes.position);
            if(distance < best_distance) {
                best_distance = distance;
                best = car;
            }
        }
        return best;
    }

    @Override
    public void onTouch(float x, float y, byte type, int pointer) {
        if(type == EventType.MOUSE_SCROLL) {
            if(vehicle.onVehicle) {
                vehicle.distance += FX.gpu.getDeltaTime() * -y * ZOOM_CAMERA_AMOUNT;
            } else if(player.onFoot) {
                player.distance += FX.gpu.getDeltaTime() * -y * ZOOM_CAMERA_AMOUNT;
            }
            return;
        }
        if(handleTouchControls(x, y, type, pointer)) {
            return;
        }
        if (!ctx.testTouch(x, y)) {
            if(type == EventType.MOUSE_HOVER) {
                if(ox == -1) {
                    ox = x;
                    oy = y;
                }

                float dx = (x - ox) * MOVING_CAMERA * FX.gpu.getDeltaTime();
                float dy = (y - oy) * MOVING_CAMERA * FX.gpu.getDeltaTime();
                rotateCamera(dx, dy);
                ox = x;
                oy = y;
            }
        } else {
            ctx.onTouch(x, y, type);
        }
    }

    private void rotateCamera(float dx, float dy) {
        // no lock angles
        if(player.onFoot) {
            player.rot_x += dx;
            player.rot_y += dy;
            player.onCameraInput();
        } else if(vehicle.onVehicle) {
            vehicle.rot_x += dx;
            vehicle.rot_y += dy;
            vehicle.onCameraInput();
        }
    }

    // Toques de pantalla: joystick, botón subir/bajar y cámara con otro dedo. Devuelve true si lo consumió.
    private boolean handleTouchControls(float x, float y, byte type, int pointer) {
        switch(type) {
            case EventType.TOUCH_PRESSED:
            case EventType.TOUCH_PRESSED_2:
                if(controls.testIsTouching(x, y)) {
                    controls.onTouch(x, y, EventType.TOUCH_PRESSED);
                    if(joystick.isTouching() && joystick_pointer == -1) {
                        joystick_pointer = pointer;
                    } else {
                        button_pointer = pointer;
                    }
                } else if(look_pointer == -1) {
                    look_pointer = pointer;
                    ox = x;
                    oy = y;
                } else {
                    return false;
                }
                return true;
            case EventType.TOUCH_DRAGGING:
            case EventType.TOUCH_DRAGGING_2:
                if(pointer == joystick_pointer) {
                    joystick.onTouch(x, y, EventType.TOUCH_DRAGGING);
                    return true;
                }
                if(pointer == look_pointer) {
                    rotateCamera((x - ox) * TOUCH_LOOK_SENSITIVITY, (y - oy) * TOUCH_LOOK_SENSITIVITY);
                    ox = x;
                    oy = y;
                    return true;
                }
                return pointer == button_pointer;
            case EventType.TOUCH_DROPPED:
            case EventType.TOUCH_DROPPED_2:
                if(pointer == joystick_pointer) {
                    joystick.onTouch(x, y, EventType.TOUCH_DROPPED);
                    joystick_pointer = -1;
                    return true;
                }
                if(pointer == button_pointer) {
                    controls.onTouch(x, y, EventType.TOUCH_DROPPED);
                    button_pointer = -1;
                    return true;
                }
                if(pointer == look_pointer) {
                    look_pointer = -1;
                    ox = oy = -1;
                    return true;
                }
                return false;
        }
        return false;
    }

    // El joystick se traduce a las teclas W/A/S/D para reutilizar el control del teclado.
    private void updateVirtualKeys() {
        boolean forward = false, backward = false, left = false, right = false;
        if(joystick.isTouching() && joystick.porcent() > 0.25f) {
            float rad = joystick.getAngle() * Maths.toRadians; // 90 = arriba
            float dx = Maths.cos(rad);
            float dy = Maths.sin(rad);
            right = dx > 0.38f;
            left = dx < -0.38f;
            forward = dy > 0.38f;
            backward = dy < -0.38f;
        }
        // el personaje usa el eje analogico directamente para velocidad y direccion graduales
        player.joystick_active = joystick.isTouching() && joystick.porcent() > 0.25f;
        player.joy_x = joystick.getAxisX();
        player.joy_y = joystick.getAxisY();
        setVirtualKey(Key.W_KEY, forward);
        setVirtualKey(Key.S_KEY, backward);
        setVirtualKey(Key.A_KEY, left);
        setVirtualKey(Key.D_KEY, right);
    }

    private void setVirtualKey(byte key, boolean down) {
        if(virtual_keys[key] != down) {
            virtual_keys[key] = down;
            onKeyEvent(key, down);
        }
    }

    private void toggleVehicle() {
        if(player.entering || player.exiting) {
            return;
        }
        if(player.onFoot) {
            Car car = nearestCar();
            if(car != null) {
                // caminar hasta la puerta, abrirla y entrar; al terminar el jugador pasa a conducir
                door_car = car;
                door_open = 0.0f;
                Vector3f facing = new Vector3f();
                Vector3f door = car.vehicle.getDriverDoorPosition(car.nodes, player.player.getPosition().y, facing);
                player.startEnterVehicle(door, facing, () -> {
                    vehicle = car.vehicle;
                    vehicle_nodes = car.nodes;
                    vehicle.onVehicle = true;
                    player.onFoot = false;
                });
            }
        } else {
            // el personaje reaparece junto a la puerta del conductor y baja mirando hacia afuera
            for(Car car : cars) {
                if(car.vehicle == vehicle) {
                    door_car = car;
                }
            }
            door_open = 1.0f;
            Vector3f facing = new Vector3f();
            Vector3f door = vehicle.getDriverDoorPosition(vehicle_nodes, vehicle.getChassisHeight(), facing);
            vehicle.onVehicle = false;
            player.onFoot = true;
            player.startExitVehicle(door, facing.mult(-1.0f));
        }
    }

    // Disco blanco translúcido con borde, para la base y la perilla del joystick.
    private int createDiscTexture(int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(size * size * 4).order(ByteOrder.nativeOrder());
        float c = (size - 1) * 0.5f;
        for(int y = 0; y < size; y++) {
            for(int x = 0; x < size; x++) {
                float d = (float) Math.sqrt((x - c) * (x - c) + (y - c) * (y - c)) / c;
                int alpha = 0;
                if(d <= 1f) {
                    alpha = d > 0.92f ? 230 : 110;
                    if(d > 0.98f) {
                        alpha = (int) (alpha * (1f - d) / 0.02f);
                    }
                }
                buffer.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) alpha);
            }
        }
        buffer.position(0);
        return Texture.load(size, size, buffer, false);
    }

    @Override
    public void onKeyEvent(byte key, boolean down) {
        if(down && key == Key.KEY_ESC) {
            FX.device.destroy();
        }
        if(down && key == Key.F_KEY) {
            toggleVehicle();
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