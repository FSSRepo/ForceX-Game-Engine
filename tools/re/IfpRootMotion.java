import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Mide el root motion (desplazamiento del hueso raiz) de animaciones ANP3 de GTA SA.
 * Uso: java tools/re/IfpRootMotion.java <ped.ifp> [prefijo_de_nombre ...]
 * Sin prefijos, lista todas las animaciones con duracion y desplazamiento del primer al ultimo fotograma.
 * Formato igual al de IFPReader: posiciones en shorts / 1024 (metros), rotaciones / 4096, tiempo / 60.
 */
public class IfpRootMotion {
    public static void main(String[] args) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(Paths.get(args[0]))).order(ByteOrder.LITTLE_ENDIAN);
        String version = str(b, 4);
        if (!version.contains("ANP3")) {
            throw new IllegalStateException("solo ANP3: " + version);
        }
        b.position(b.position() + 4);
        str(b, 24);
        int anims = b.getInt();
        for (int i = 0; i < anims; i++) {
            String name = str(b, 24);
            int objects = b.getInt();
            b.position(b.position() + 8);
            boolean wanted = args.length == 1;
            for (int a = 1; a < args.length; a++) {
                wanted |= name.toLowerCase().startsWith(args[a].toLowerCase());
            }
            double duration = 0;
            float[] first = null, last = null;
            for (int o = 0; o < objects; o++) {
                b.position(b.position() + 24);
                int frameType = b.getInt();
                int frames = b.getInt();
                int boneId = b.getInt();
                for (int f = 0; f < frames; f++) {
                    int skip = 0;
                    if (frameType == 4) {
                        b.position(b.position() + 8);
                        float t = b.getShort() / 60.0f;
                        float x = b.getShort() / 1024.0f;
                        float y = b.getShort() / 1024.0f;
                        float z = b.getShort() / 1024.0f;
                        duration = Math.max(duration, t);
                        if (o == 0) {
                            if (first == null) {
                                first = new float[]{x, y, z};
                            }
                            last = new float[]{x, y, z};
                        }
                    } else if (frameType == 3) {
                        b.position(b.position() + 8);
                        float t = b.getShort() / 60.0f;
                        duration = Math.max(duration, t);
                    } else {
                        b.position(b.position() + 32);
                    }
                }
            }
            if (wanted) {
                if (first == null) {
                    System.out.printf(Locale.ROOT, "%-28s dur=%.3fs sin posicion de raiz%n", name, duration);
                } else {
                    float dx = last[0] - first[0], dy = last[1] - first[1], dz = last[2] - first[2];
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    System.out.printf(Locale.ROOT, "%-28s dur=%.3fs  d=(%.3f, %.3f, %.3f) |d|=%.3f m  v=%.3f m/s  ratio_eje_y=%.3f m/s%n",
                            name, duration, dx, dy, dz, dist, duration > 0 ? dist / duration : 0, duration > 0 ? dy / duration : 0);
                }
            }
        }
    }

    private static String str(ByteBuffer b, int n) {
        byte[] raw = new byte[n];
        b.get(raw);
        int end = 0;
        while (end < n && raw[end] != 0) {
            end++;
        }
        return new String(raw, 0, end, StandardCharsets.US_ASCII);
    }
}
