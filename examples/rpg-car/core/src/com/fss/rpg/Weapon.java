package com.fss.rpg;

import com.forcex.FX;
import com.forcex.core.AL;
import com.forcex.core.gpu.Texture;
import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;
import com.forcex.gfx3d.ModelObject;
import com.forcex.gfx3d.ModelRenderer;
import com.forcex.gtasdk.DFFSDK;
import com.forcex.gtasdk.DFFStream;
import com.forcex.io.BinaryStreamReader;
import com.forcex.io.FileSystem;
import com.forcex.math.Matrix4f;
import com.forcex.math.Quaternion;
import com.forcex.math.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;

/** Fusil AN-94: flota en el mundo hasta que el personaje se acerca; entonces se equipa y dispara con sonido OpenAL. */
public class Weapon {
    private static final String DIR = "game/AN-94/";
    private static final float PICKUP_DISTANCE = 2.2f;
    private static final float FLOAT_HEIGHT = 1.1f;
    private static final float BOB_AMPLITUDE = 0.15f;
    private static final float SPIN_SPEED = 60f;
    // disparos por segundo
    private static final float FIRE_RATE = 10f;
    private static final float FLASH_TIME = 0.05f;
    private static final int SOURCE_COUNT = 6;

    private final ArrayList<ModelObject> parts = new ArrayList<>();
    private final ArrayList<Matrix4f> local = new ArrayList<>();
    private ModelObject flash;
    private final Matrix4f fix = Matrix4f.fromRotation(Quaternion.fromEulerAngles(-90, 180, 0));
    private final Matrix4f base = new Matrix4f();

    private final Vector3f spawn = new Vector3f();
    private float time = 0.0f;
    private float fire_cooldown = 0.0f;
    private float flash_time = 0.0f;
    boolean equipped = false;

    private int shoot_buffer, equip_buffer;
    private final int[] sources = new int[SOURCE_COUNT];
    private int next_source = 0;
    private boolean audio_ready = false;

    public Weapon(float x, float z, float ground_height) {
        spawn.set(x, ground_height + FLOAT_HEIGHT, z);

        DFFSDK dff = DFFStream.readDFF(DIR + "models/gta3img/ak47.dff", null, null, null);
        HashMap<String, Integer> textures = new HashMap<>();
        for(int i = 0; i < dff.geometryCount; i++) {
            ModelObject o = dff.getObject(new ModelObject(), i, false);
            if(o == null) {
                continue;
            }
            Mesh mesh = o.getMesh();
            for(MeshPart part : mesh.getParts().list) {
                String name = part.material.textureName;
                if(name == null || name.isEmpty()) {
                    continue;
                }
                Integer texture = textures.get(name);
                if(texture == null) {
                    texture = Texture.load(DIR + "tex/" + name + ".png");
                    textures.put(name, texture);
                }
                part.material.diffuseTexture = texture;
            }
            mesh.initialize();
            local.add(new Matrix4f().set(o.getTransform()));
            parts.add(o);
            if(o.getName() != null && o.getName().toLowerCase().contains("flash")) {
                flash = o;
            }
        }
        initAudio();
    }

    private void initAudio() {
        try {
            // el motor no crea el contexto de audio por su cuenta
            FX.alc.create();
            shoot_buffer = createBuffer(DIR + "EarShot/Ak47/shoot.wav");
            equip_buffer = createBuffer(DIR + "audio/SFX/GENRL/BANK_137/sound_033.wav");
            for(int i = 0; i < SOURCE_COUNT; i++) {
                sources[i] = FX.al.alGenSource();
                // sin atenuacion: suena igual donde este el jugador
                FX.al.alSourcei(sources[i], AL.AL_SOURCE_RELATIVE, AL.AL_TRUE);
                FX.al.alSource3f(sources[i], AL.AL_POSITION, 0, 0, 0);
                FX.al.alSourcef(sources[i], AL.AL_GAIN, 0.8f);
            }
            audio_ready = true;
        } catch(Throwable e) {
            System.out.println("Weapon: audio no disponible: " + e);
        }
    }

    // lee un WAV PCM recorriendo los chunks (el LIST/INFO de estos archivos rompe una busqueda ingenua de "data")
    private int createBuffer(String path) {
        byte[] file = FX.fs.open(path, FileSystem.ReaderType.MEMORY).getData();
        BinaryStreamReader r = new BinaryStreamReader(file);
        r.skip(12);
        int channels = 1, rate = 44100, bits = 16;
        byte[] data = null;
        while(!r.isEndOfFile()) {
            String id = r.readString(4);
            int size = r.readInt();
            if(id.equals("fmt ")) {
                r.skip(2);
                channels = r.readShort();
                rate = r.readInt();
                r.skip(6);
                bits = r.readShort();
                r.skip(size - 16);
            } else if(id.equals("data")) {
                data = r.readByteArray(Math.min(size, file.length - r.getOffset()));
                break;
            } else {
                r.skip(size + (size & 1));
            }
        }
        if(data == null) {
            throw new RuntimeException("WAV sin datos: " + path);
        }
        int format = bits == 16 ? (channels == 2 ? AL.AL_FORMAT_STEREO16 : AL.AL_FORMAT_MONO16)
                : (channels == 2 ? AL.AL_FORMAT_STEREO8 : AL.AL_FORMAT_MONO8);
        int buffer = FX.al.alGenBuffer();
        FX.al.alBufferData(buffer, format, data, rate);
        return buffer;
    }

    private void play(int buffer) {
        if(!audio_ready) {
            return;
        }
        int source = sources[next_source];
        next_source = (next_source + 1) % SOURCE_COUNT;
        FX.al.alSourceStop(source);
        FX.al.alSourcei(source, AL.AL_BUFFER, buffer);
        FX.al.alSourcePlay(source);
    }

    /** Si el personaje esta cerca y aun no la tiene, la equipa. */
    public void update(float delta, Vector3f player_position) {
        time += delta;
        if(fire_cooldown > 0.0f) {
            fire_cooldown -= delta;
        }
        if(flash_time > 0.0f) {
            flash_time -= delta;
        }
        if(equipped) {
            return;
        }
        float dx = player_position.x - spawn.x;
        float dz = player_position.z - spawn.z;
        float dy = player_position.y - (spawn.y - FLOAT_HEIGHT);
        if(dx * dx + dz * dz + dy * dy < PICKUP_DISTANCE * PICKUP_DISTANCE) {
            equipped = true;
            play(equip_buffer);
        }
    }

    /** Dispara si ya paso el intervalo entre tiros. Devuelve true si salio un disparo. */
    public boolean fire() {
        if(!equipped || fire_cooldown > 0.0f) {
            return false;
        }
        fire_cooldown = 1.0f / FIRE_RATE;
        flash_time = FLASH_TIME;
        play(shoot_buffer);
        return true;
    }

    /**
     * Dibuja el arma: flotando y girando si esta en el suelo, o pegada a la mano.
     * @param character transformacion del modelo del personaje (incluye la correccion de ejes)
     * @param hand matriz del hueso de la mano en espacio del modelo, o null si no se conoce
     */
    public void render(ModelRenderer renderer, Matrix4f character, Matrix4f hand) {
        if(equipped) {
            if(hand == null) {
                return;
            }
            // como GTA SA (CVisibilityPlugins::RenderWeaponPedsForPC): el modelo del arma toma tal cual la matriz del hueso de la mano derecha (id 24)
            base.set(character.mult(null, hand));
        } else {
            float y = spawn.y + (float) Math.sin(time * 2.0f) * BOB_AMPLITUDE;
            Matrix4f spin = Matrix4f.fromRotation(Quaternion.fromEulerAngles(0, time * SPIN_SPEED, 0));
            base.set(Matrix4f.fromTransform(new Quaternion(), new Vector3f(spawn.x, y, spawn.z)).mult(null, spin).mult(null, fix));
        }
        for(int i = 0; i < parts.size(); i++) {
            ModelObject o = parts.get(i);
            if(o == flash && (!equipped || flash_time <= 0.0f)) {
                continue;
            }
            o.setTransform(base.mult(null, local.get(i)));
            renderer.render(o);
        }
    }

    public void destroy() {
        if(!audio_ready) {
            return;
        }
        for(int s : sources) {
            FX.al.alSourceStop(s);
            FX.al.alDeleteSource(s);
        }
        FX.al.alDeleteBuffer(shoot_buffer);
        FX.al.alDeleteBuffer(equip_buffer);
        FX.alc.destroy();
    }
}
