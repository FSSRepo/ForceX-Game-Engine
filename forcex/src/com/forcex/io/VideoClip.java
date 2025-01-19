package com.forcex.io;

import com.forcex.FX;
import com.forcex.app.threading.Task;
import com.forcex.core.CoreJni;
import com.forcex.core.GL;
import com.forcex.core.gpu.Texture;
import com.forcex.gui.Toast;
import com.forcex.utils.Image;
import com.forcex.utils.Logger;
import com.forcex.utils.VideoStack;

import java.io.FileOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class VideoClip {
    public float time = 0;
    private boolean play = false, stop = false, loop = false, first_frame = true;
    private int texture = -1;
	private final int blockSize;
	private int fileOffset;
	private int type = 0;
    private final float duration;
	private final float frame_time;
	private float speed = 1;
    private final short width;
	private final short height;
	private final short numFrames;
	private short oldFrameOffset;
	private final GL gl;
    private byte[] next_frame;
    private boolean hasAlpha = false;
    private String video_path;
    private OnVideoClipListener listener;

    public VideoClip(String path, VideoStack stack) {
        this.video_path = path;
        {
            BinaryStreamReader is = FX.fs.open(path, FileSystem.ReaderType.STREAM);
            width = is.readShort();
            height = is.readShort();
            blockSize = is.readInt();
            hasAlpha = is.readByte() == 1;
            byte frame_rate = is.readByte();
            numFrames = is.readShort();
            fileOffset = 12;
            frame_time = 1.0f / frame_rate;
            duration = numFrames * frame_time;
            is.clear();
        }
        gl = FX.gl;
        type = FX.gpu.isOpenGLES() ? GL.GL_TEXTURE_ETC1 : GL.GL_TEXTURE_DXT1;
        stack.add(this);
        next_frame = new byte[blockSize];
    }

    public static void convertFromSequence(String frames_path, int frame_idx_start, int frame_idx_end, int framerate, int numFrames, boolean dxt, boolean alpha, OnVideoProcessListener listener) {
        try{
            BinaryStreamWriter os = new BinaryStreamWriter(new FileOutputStream(frames_path + "result.fvp"));
            int block_size = 0, byte_count = 0;
            float real_sz = 0, bitrate = 0;
            String append = "";
            boolean is_zip = false;
            float ratio = 1f, predict = 0;

            for (int i = frame_idx_start; i <= frame_idx_end; i++) {
                long start = System.currentTimeMillis();
                is_zip = false;
                if (i < 10) {
                    append = "000";
                } else if (i >= 10 && i < 100) {
                    append = "00";
                } else if (i >= 100 && i < 1000) {
                    append = "0";
                } else if (i >= 1000) {
                    append = "";
                }
                Image img = new Image(frames_path + append + i + ".png");
                if (i == frame_idx_start) {
                    block_size = (img.width * img.height) / 2;
                    real_sz = (block_size * numFrames) / 1048576f;
                    os.writeShort(img.width);
                    os.writeShort(img.height);
                    os.writeInt(block_size);
                    os.writeByte(alpha ? 1 : 0);
                    os.writeByte(framerate);
                    os.writeShort(numFrames);
                }
                if (!dxt) {
                    byte[] buffer = CoreJni.etc1compress(img.getRGBAImage(), img.width, img.height, CoreJni.ETC1_LOW_QUALITY);
                    byte[] zip = encode(buffer);
                    if (zip.length > block_size) {
                        os.writeByte(0);
                        os.writeByteArray(buffer);
                        ratio = 1;
                        byte_count += block_size;
                    } else {
                        os.writeByte(1);
                        is_zip = true;
                        os.writeInt(zip.length);
                        ratio = (float) zip.length / block_size;
                        os.writeByteArray(zip);
                        byte_count += zip.length;
                    }
                    if (alpha) {
                        BitStream bs = new BitStream(img.width * img.height);
                        for (int j = 0; j < (img.width * img.height); j++) {
                            bs.put(img.getRGBAImage()[j * 4 + 3] < 240);
                        }
                        zip = encode(bs.data);
                        os.writeInt(zip.length);
                        os.writeByteArray(zip);
                    }
                } else {
                    byte[] buffer = CoreJni.dxtcompress(img.getRGBAImage(), img.width, img.height, CoreJni.DXTC_1 | CoreJni.DXTC_RANGE_FIT);
                    os.writeByteArray(buffer);
                    byte[] zip = encode(buffer);
                    if (zip.length > block_size) {
                        os.writeByte(0);
                        os.writeByteArray(buffer);
                        byte_count += block_size;
                        ratio = 1;
                    } else {
                        os.writeByte(1);
                        is_zip = true;
                        os.writeInt(zip.length);
                        ratio = (float) zip.length / block_size;
                        byte_count += zip.length;
                        os.writeByteArray(zip);
                    }
                    if (alpha) {
                        BitStream bs = new BitStream(img.width * img.height);
                        for (int j = 0; j < (img.width * img.height); j++) {
                            bs.put(img.getRGBAImage()[j * 4 + 3] < 240);
                        }
                        zip = encode(bs.data);
                        os.writeInt(zip.length);
                        os.writeByteArray(zip);
                    }
                }
                if ((i % framerate) == 0) {
                    bitrate = (byte_count * 8) / 1000000.0f;
                    byte_count = 0;
                }
                img.clear();
                predict += ratio;
                if (i > 4) {
                    float ratio_f = (predict / i);
                    float delta = ((float) (i - frame_idx_start + 1) / numFrames);
                    float predicted_size = real_sz * ratio_f;
                    listener.process(
                            String.format("%.2f", delta * 100f) + "%\n" +
                                    "Last: " + (is_zip ? "ZLib comp" : "Norm") + (bitrate > 0 ? "Bitrate: " + String.format("%.2f", bitrate) + "Mbps" : "") + "\n" +
                                    "Ratio: " + String.format("%.2f", ratio * 100f) + "%\n" +
                                    "Final: " + String.format("%.2f", predicted_size) + "MB)");
                }
            }
            os.finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] encode(byte[] input) {
        Deflater df = new Deflater(Deflater.BEST_COMPRESSION);
        df.setInput(input);
        df.finish();
        byte[] temp = new byte[input.length];
        int length = df.deflate(temp);
        df.end();
        byte[] buffer = new byte[length];
		System.arraycopy(temp, 0, buffer, 0, length);
        return buffer;
    }

    public int init() {
        texture = Texture.genTextureWhite();
        return texture;
    }

    public void play() {
        play = true;
        if (listener != null) {
            listener.play();
        }
    }

    public void pause() {
        play = false;
        if (listener != null) {
            listener.pause();
        }
    }

    public void stop() {
        stop = true;
    }

    public void setSpeed(float spd) {
        speed = spd;
    }

    public void setLoop(boolean z) {
        loop = z;
    }

    public boolean perform() {

        short frameOffset = (short) (time / frame_time);
        if (frameOffset == 0 && first_frame) {
            loadNextFrame();
            updateGLMemory();
            loadNextFrame();
            first_frame = false;
            if (listener != null) {
                listener.start();
            }
        } else if ((frameOffset - oldFrameOffset) > 0 && frameOffset < (numFrames - 1)) {
            updateGLMemory();
            loadNextFrame();
        }
        oldFrameOffset = frameOffset;
        return stop;
    }

    public void update() {
        if (time + FX.gpu.getDeltaTime() >= duration && !stop) {
            if (loop) {
                oldFrameOffset = 0;
                first_frame = true;
                fileOffset = 12;
                time = 0;
            } else {
                time = duration;
                if (listener != null) {
                    listener.end();
                }
                stop = true;
            }
        } else {
            time += play && !stop && !first_frame ? FX.gpu.getDeltaTime() * speed : 0.0f;
        }
    }

    private void updateGLMemory() {
        FX.gpu.queueTask(new Task() {
            @Override
            public boolean execute() {
                gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
                gl.glTexImage2D(GL.GL_TEXTURE_2D, width, height, type, next_frame);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
                return true;
            }
        });
        FX.gpu.waitEmptyQueue();
    }

    private void loadNextFrame() {
        try {
            BinaryStreamReader is = FX.fs.open(video_path, FileSystem.ReaderType.STREAM);
            is.skip(fileOffset);
            boolean zip = is.readBoolean();
            if (zip) {
                int size = is.readInt();
                decode(is.readByteArray(size));
                fileOffset += 5 + size;
            } else {
                is.readByteArray(next_frame);
                fileOffset += 1 + blockSize;
            }
            is.clear();
        } catch (Exception e) {
            Logger.log("ERROR: Video Streaming: " + e);
        }
    }

    public int getTexture() {
        return texture;
    }

    private void decode(byte[] input) {
        try {
            Inflater inf = new Inflater();
            inf.setInput(input);
            inf.inflate(next_frame);
            inf.end();
        } catch (DataFormatException e) {
        }
    }

    public void setOnVideoClipListener(OnVideoClipListener listener) {
        this.listener = listener;
    }

    public void delete() {
        gl.glDeleteTexture(texture);
        next_frame = null;
        video_path = null;
    }

    public interface OnVideoClipListener {
        void end();

        void pause();

        void play();

        void start();
    }

    public interface OnVideoProcessListener {
        void process(String info);
    }
}
