package com.forcex.utils;

import com.forcex.FX;
import com.forcex.io.BinaryStreamReader;
import com.forcex.io.FileSystem;

import java.util.HashMap;
import java.util.Set;

public class LanguageString {
    HashMap<String, String> texts = new HashMap<>();

    public LanguageString(String path) {
        try {
            BinaryStreamReader is = FX.fs.open(path, FileSystem.ReaderType.MEMORY);
            String[] lines = new String(is.getData()).split("\n");
            for (String line : lines) {
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                line = line.replace(" = ", "=");
                String[] token = line.split("=");
                texts.put(token[0].replace(" ", ""), token[1].replace("-nl", "\n"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<String> getIds() {
        return texts.keySet();
    }

    public String get(String id, Object... data) {
        // s[format] i[id] i[]=obj
        if (texts.containsKey(id)) {
            String process = "";
            String eval = texts.get(id);
            int obj_offset = 0;
            for (int i = 0; i < eval.length(); i++) {
                char start = eval.charAt(i);
                if (i + 1 >= eval.length()) {
                    process += start;
                    return process;
                } else if (
                        (start == 's' || start == 'i' || start == 'm') &&
                                eval.charAt(i + 1) == '[') {
                    i += 2;
                    String d = "";
                    for (int j = i; j < eval.length(); j++) {
                        if (eval.charAt(j) == ']') {
                            i += d.length();
                            break;
                        } else {
                            d += eval.charAt(j);
                        }
                    }
                    switch (start) {
                        case 's': {
                            if (obj_offset >= data.length) {
                                process += "s[" + d + "]";
                            } else {
                                process += String.format(d, data[obj_offset]);
                                obj_offset++;
                            }
                        }
                        break;
                        case 'i': {
                            if (d.length() == 0) {
                                if (obj_offset >= data.length) {
                                    process += "i[]";
                                } else {
                                    process += data[obj_offset];
                                }
                                obj_offset++;
                            } else {
                                process += get(d);
                            }
                        }
                        break;
                        case 'm': {
                            String t = get(d);
                            process += t.equals(d) ? d.toUpperCase() : t.toUpperCase();
                        }
                        break;
                    }
                } else {
                    process += start;
                }
            }
            return process;
        }
        return id;
    }
}
