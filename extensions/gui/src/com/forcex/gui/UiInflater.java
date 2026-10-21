package com.forcex.gui;

import com.forcex.FX;
import com.forcex.core.gpu.Texture;
import com.forcex.gui.widgets.Button;
import com.forcex.gui.widgets.EditText;
import com.forcex.gui.widgets.ImageView;
import com.forcex.gui.widgets.Joystick;
import com.forcex.gui.widgets.ProgressBar;
import com.forcex.gui.widgets.TextView;
import com.forcex.utils.Color;

import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Construye una interfaz a partir de un layout YAML.
 *
 * <pre>
 * type: linear
 * orientation: vertical
 * width: fill
 * height: fill
 * children:
 *   - type: text
 *     text: "Hola"
 *     textSize: 32px
 *   - type: button
 *     text: "Jugar"
 *     width: 200px
 *     height: 56px
 *     onClick: onPlay
 * </pre>
 *
 * Un layout con {@code relative: true} coloca a sus hijos respecto al centro con {@code x} / {@code y}
 * (px o %, positivo hacia la derecha / abajo). Tipos: linear, text, button, image, edit, progress, joystick.
 *
 * Dimensiones admitidas: {@code 120px}, {@code 50%} (del padre), {@code fill} y {@code wrap}.
 */
public class UiInflater {
    public interface OnClickHandler {
        void onClick(View view);
    }

    public interface ViewFactory {
        View create(Map<String, Object> attrs, float parent_width, float parent_height);
    }

    private final UIContext context;
    private final Map<String, View> views = new HashMap<>();
    private final Map<String, OnClickHandler> click_handlers = new HashMap<>();
    private final Map<String, ViewFactory> factories = new HashMap<>();

    public UiInflater(UIContext context) {
        this.context = context;
        factories.put("linear", this::createLinear);
        factories.put("text", this::createText);
        factories.put("button", this::createButton);
        factories.put("image", this::createImage);
        factories.put("edit", this::createEdit);
        factories.put("progress", this::createProgress);
        factories.put("joystick", this::createJoystick);
    }

    /**
     * Permite registrar widgets propios del juego.
     */
    public void registerView(String type, ViewFactory factory) {
        factories.put(type, factory);
    }

    public void setOnClickListener(String name, OnClickHandler handler) {
        click_handlers.put(name, handler);
    }

    public View findViewById(String id) {
        return views.get(id);
    }

    public View inflateFile(String path) {
        String yaml = FX.fs.readString(path);
        if (yaml.isEmpty()) {
            throw new IllegalArgumentException("No se pudo leer el layout: " + path);
        }
        return inflate(yaml);
    }

    public View inflate(String yaml) {
        Object data = new Yaml().load(yaml);
        Map<String, Object> attrs = asMap(data);
        if (attrs == null) {
            throw new IllegalArgumentException("El layout YAML debe definir una vista raíz");
        }
        return inflateNode(attrs, FX.gpu.getWidth(), FX.gpu.getHeight());
    }

    private View inflateNode(Map<String, Object> attrs, float parent_width, float parent_height) {
        String type = string(attrs.get("type"), "linear");
        ViewFactory factory = factories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Tipo de vista no soportado: " + type);
        }
        View view = factory.create(attrs, parent_width, parent_height);
        view.context = context;

        String id = string(attrs.get("id"), null);
        if (id != null) {
            views.put(id, view);
            try {
                view.setId(Integer.parseInt(id));
            } catch (NumberFormatException e) {
                // el id es un nombre; solo se usa con findViewById(String)
            }
        }
        applyMargins(view, attrs, parent_width, parent_height);
        float rel_x = dimension(attrs.get("x"), parent_width);
        float rel_y = dimension(attrs.get("y"), parent_height);
        if (!Float.isNaN(rel_x) || !Float.isNaN(rel_y)) {
            view.setRelativePosition(Float.isNaN(rel_x) ? 0f : rel_x, Float.isNaN(rel_y) ? 0f : rel_y);
        }
        view.setAlignment(alignment(attrs.get("align")));
        applyVisibility(view, attrs);
        applyClick(view, attrs);
        return view;
    }

    private View createLinear(Map<String, Object> attrs, float parent_width, float parent_height) {
        Layout layout = new Layout(context);
        if (Boolean.TRUE.equals(attrs.get("relative"))) {
            layout.setIsRelative(true);
        }
        layout.setOrientation("horizontal".equalsIgnoreCase(string(attrs.get("orientation"), "vertical"))
                ? Layout.HORIZONTAL : Layout.VERTICAL);
        float width = applyWidth(layout, attrs.get("width"), parent_width);
        float height = applyHeight(layout, attrs.get("height"), parent_height);

        Color background = color(attrs.get("background"));
        if (background != null) {
            layout.setBackgroundColor(background.r, background.g, background.b, background.a);
        }
        layout.setPadding(dimension(attrs.get("padding"), parent_width));

        float child_width = Float.isNaN(width) ? parent_width : width;
        float child_height = Float.isNaN(height) ? parent_height : height;
        Object children = attrs.get("children");
        if (children instanceof List) {
            for (Object child : (List<?>) children) {
                Map<String, Object> child_attrs = asMap(child);
                if (child_attrs != null) {
                    layout.add(inflateNode(child_attrs, child_width, child_height));
                }
            }
        }
        return layout;
    }

    private View createText(Map<String, Object> attrs, float parent_width, float parent_height) {
        TextView view = new TextView(UIContext.default_font);
        float text_size = dimension(attrs.get("textSize"), parent_height);
        if (Float.isNaN(text_size)) {
            text_size = parent_height * 0.03f;
        }
        view.setTextSize(text_size);
        view.setTextAlignment(textAlignment(attrs.get("textAlign")));

        float constraint = resolveSize(attrs.get("width"), parent_width, Float.NaN);
        if (!Float.isNaN(constraint)) {
            view.setConstraintWidth(constraint);
        }
        view.setText(string(attrs.get("text"), ""));

        Color text_color = color(attrs.get("textColor"));
        if (text_color != null) {
            view.setTextColor(text_color.r, text_color.g, text_color.b);
        }
        applyWidth(view, attrs.get("width"), parent_width);
        applyHeight(view, attrs.get("height"), parent_height);
        return view;
    }

    private View createButton(Map<String, Object> attrs, float parent_width, float parent_height) {
        float width = resolveSize(attrs.get("width"), parent_width, 160f);
        float height = resolveSize(attrs.get("height"), parent_height, 48f);
        Button view = new Button(string(attrs.get("text"), ""), UIContext.default_font, width, height);
        applyWidth(view, attrs.get("width"), parent_width);
        applyHeight(view, attrs.get("height"), parent_height);

        float text_size = dimension(attrs.get("textSize"), Float.NaN);
        if (!Float.isNaN(text_size)) {
            view.setTextSize(text_size);
        }
        Color text_color = color(attrs.get("textColor"));
        if (text_color != null) {
            view.setTextColor(text_color.r, text_color.g, text_color.b);
        }
        Color background = color(attrs.get("background"));
        if (background != null) {
            view.setBackgroundColor(background.r, background.g, background.b, background.a);
        }
        return view;
    }

    private View createImage(Map<String, Object> attrs, float parent_width, float parent_height) {
        String src = string(attrs.get("src"), null);
        int texture = src == null ? -1 : Texture.load(src);
        float width = resolveSize(attrs.get("width"), parent_width, 120f);
        float height = resolveSize(attrs.get("height"), parent_height, width);
        float ratio = dimension(attrs.get("ratio"), 0f);
        if (attrs.get("height") == null && ratio > 0f) {
            height = width / ratio;
        }
        ImageView view = new ImageView(texture, width, height);
        applyWidth(view, attrs.get("width"), parent_width);
        applyHeight(view, attrs.get("height"), parent_height);
        return view;
    }

    private View createEdit(Map<String, Object> attrs, float parent_width, float parent_height) {
        float text_size = dimension(attrs.get("textSize"), parent_height);
        if (Float.isNaN(text_size)) {
            text_size = parent_height * 0.025f;
        }
        float width = resolveSize(attrs.get("width"), parent_width, parent_width * 0.3f);
        float height = resolveSize(attrs.get("height"), parent_height, text_size);
        EditText view = new EditText(context, width, height, text_size);
        applyWidth(view, attrs.get("width"), parent_width);
        applyHeight(view, attrs.get("height"), parent_height);

        String hint = string(attrs.get("hint"), null);
        if (hint != null) {
            view.setHint(hint);
        }
        String text = string(attrs.get("text"), null);
        if (text != null) {
            view.setText(text);
        }
        Color text_color = color(attrs.get("textColor"));
        if (text_color != null) {
            view.setTextColor(text_color.r, text_color.g, text_color.b);
        }
        if (Boolean.TRUE.equals(attrs.get("numbers"))) {
            view.setNumbersMode(true);
        }
        if (Boolean.TRUE.equals(attrs.get("password"))) {
            view.setPasswordMode(true);
        }
        return view;
    }

    private View createProgress(Map<String, Object> attrs, float parent_width, float parent_height) {
        float width = resolveSize(attrs.get("width"), parent_width, 240f);
        float height = resolveSize(attrs.get("height"), parent_height, 16f);
        ProgressBar view = new ProgressBar(width, height);
        applyWidth(view, attrs.get("width"), parent_width);
        applyHeight(view, attrs.get("height"), parent_height);

        Color background = color(attrs.get("background"));
        Color bar = color(attrs.get("barColor"));
        if (background != null || bar != null) {
            view.setColor(background == null ? new Color(230, 230, 230) : background,
                    bar == null ? new Color(10, 190, 40, 240) : bar);
        }
        if (attrs.get("progress") != null) {
            float progress = number(attrs.get("progress"));
            if (!Float.isNaN(progress)) {
                view.setProgress(progress);
            }
        }
        return view;
    }

    private View createJoystick(Map<String, Object> attrs, float parent_width, float parent_height) {
        float radius = dimension(attrs.get("radius"), parent_height);
        if (Float.isNaN(radius)) {
            radius = parent_height * 0.12f;
        }
        return new Joystick(radius);
    }

    private void applyMargins(View view, Map<String, Object> attrs, float parent_width, float parent_height) {
        float all = dimension(attrs.get("margin"), Float.NaN);
        if (Float.isNaN(all)) {
            all = 0f;
        }
        float top = dimension(attrs.get("marginTop"), parent_height);
        float bottom = dimension(attrs.get("marginBottom"), parent_height);
        float left = dimension(attrs.get("marginLeft"), parent_width);
        float right = dimension(attrs.get("marginRight"), parent_width);
        view.setMarginTop(Float.isNaN(top) ? all : top);
        view.setMarginBottom(Float.isNaN(bottom) ? all : bottom);
        view.setMarginLeft(Float.isNaN(left) ? all : left);
        view.setMarginRight(Float.isNaN(right) ? all : right);
    }

    private void applyVisibility(View view, Map<String, Object> attrs) {
        String visibility = string(attrs.get("visibility"), null);
        if (visibility == null) {
            return;
        }
        switch (visibility.toLowerCase(Locale.ROOT)) {
            case "invisible":
                view.setVisibility(View.INVISIBLE);
                break;
            case "gone":
                view.setVisibility(View.GONE);
                break;
            default:
                view.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void applyClick(View view, Map<String, Object> attrs) {
        String name = string(attrs.get("onClick"), null);
        if (name == null) {
            return;
        }
        if (!(view instanceof Button)) {
            throw new IllegalArgumentException("onClick solo está soportado en botones (" + name + ")");
        }
        final OnClickHandler handler = click_handlers.get(name);
        if (handler == null) {
            throw new IllegalArgumentException("No hay handler registrado para onClick: " + name);
        }
        ((Button) view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void OnClick(View v) {
                handler.onClick(v);
            }
        });
    }

    /**
     * Aplica el ancho declarado y devuelve el ancho efectivo en px (NaN si es wrap/sin definir).
     */
    private float applyWidth(View view, Object value, float parent_width) {
        String spec = string(value, null);
        if (spec == null) {
            return Float.NaN;
        }
        if (isFill(spec)) {
            view.setWidthType(Layout.FILL_PARENT);
            return parent_width;
        }
        if (isWrap(spec)) {
            view.setWidthType(Layout.WRAP_CONTENT);
            return Float.NaN;
        }
        float width = parseDimension(spec, parent_width);
        if (Float.isNaN(width)) {
            return Float.NaN;
        }
        view.setWidth(width);
        if (view instanceof Layout) {
            ((Layout) view).setUseWidthCustom(true);
        }
        return width;
    }

    private float applyHeight(View view, Object value, float parent_height) {
        String spec = string(value, null);
        if (spec == null) {
            return Float.NaN;
        }
        if (isFill(spec)) {
            view.setHeightType(Layout.FILL_PARENT);
            return parent_height;
        }
        if (isWrap(spec)) {
            view.setHeightType(Layout.WRAP_CONTENT);
            return Float.NaN;
        }
        float height = parseDimension(spec, parent_height);
        if (Float.isNaN(height)) {
            return Float.NaN;
        }
        view.setHeight(height);
        if (view instanceof Layout) {
            ((Layout) view).setUseHeightCustom(true);
        }
        return height;
    }

    private float resolveSize(Object value, float parent, float def) {
        String spec = string(value, null);
        if (spec == null) {
            return def;
        }
        if (isFill(spec)) {
            return parent;
        }
        if (isWrap(spec)) {
            return def;
        }
        float size = parseDimension(spec, parent);
        return Float.isNaN(size) ? def : size;
    }

    private float dimension(Object value, float parent) {
        String spec = string(value, null);
        return spec == null ? Float.NaN : parseDimension(spec, parent);
    }

    private static float parseDimension(String spec, float parent) {
        String value = spec.trim().toLowerCase(Locale.ROOT);
        try {
            if (value.endsWith("px")) {
                return Float.parseFloat(value.substring(0, value.length() - 2).trim());
            }
            if (value.endsWith("%")) {
                return parent * Float.parseFloat(value.substring(0, value.length() - 1).trim()) / 100f;
            }
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    private static boolean isFill(String spec) {
        return spec.equalsIgnoreCase("fill") || spec.equalsIgnoreCase("match_parent");
    }

    private static boolean isWrap(String spec) {
        return spec.equalsIgnoreCase("wrap") || spec.equalsIgnoreCase("wrap_content");
    }

    private static byte alignment(Object value) {
        String align = string(value, "left");
        switch (align.toLowerCase(Locale.ROOT)) {
            case "center":
                return Layout.CENTER;
            case "right":
                return Layout.RIGHT;
            default:
                return Layout.LEFT;
        }
    }

    private static byte textAlignment(Object value) {
        String align = string(value, "center");
        switch (align.toLowerCase(Locale.ROOT)) {
            case "left":
                return TextView.TEXT_ALIGNMENT_CENTER_LEFT;
            case "right":
                return TextView.TEXT_ALIGNMENT_CENTER_RIGHT;
            default:
                return TextView.TEXT_ALIGNMENT_CENTER;
        }
    }

    /**
     * Convierte "#rrggbb" / "#aarrggbb" / "0xrrggbb" en un color.
     */
    private static Color color(Object value) {
        String spec = string(value, null);
        if (spec == null) {
            return null;
        }
        spec = spec.trim();
        if (spec.startsWith("#")) {
            spec = spec.substring(1);
        } else if (spec.startsWith("0x") || spec.startsWith("0X")) {
            spec = spec.substring(2);
        }
        try {
            long argb = Long.parseLong(spec, 16);
            if (spec.length() <= 6) {
                argb |= 0xFF000000L;
            }
            return new Color((int) argb);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Color no válido: " + value);
        }
    }

    private static String string(Object value, String def) {
        return value == null ? def : String.valueOf(value);
    }

    private static float number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }
}
