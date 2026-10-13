package com.lastbastion.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelling DSL. Parts and boxes are declared in vanilla model space (pixels, Y down, Y=24 is the
 * ground, -Z is the front). On top of plain boxes it offers rounded primitives built Blockbench-style
 * from rotated boxes: octagonal cylinders, tapers (cones, horns, blades), spikes and spheres.
 * Baking packs every box into an atlas, paints it at {@link #density} texels per model pixel with
 * {@link TextureBaker}, and produces a vanilla {@link ModelPart} tree.
 */
public final class ModelSpec {
    public final String name;
    public final int density;
    public final Part root;
    private int anon;

    public ModelSpec(String name) {
        this(name, 4);
    }

    public ModelSpec(String name, int density) {
        this.name = name;
        this.density = Math.max(1, Math.min(6, Math.round(density * TextureBaker.detailScale())));
        this.root = new Part(this, "root", 0, 0, 0);
    }

    String nextName(String prefix) {
        return prefix + "_" + (anon++);
    }

    public static final class Box {
        public final float x, y, z, w, h, d, inflate;
        public final Paint paint;
        public int u, v;

        Box(float x, float y, float z, float w, float h, float d, float inflate, Paint paint) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
            this.h = h;
            this.d = d;
            this.inflate = inflate;
            this.paint = paint;
        }

        public int uvW() {
            return (int) Math.ceil(2 * (w + d)) + 1;
        }

        public int uvH() {
            return (int) Math.ceil(d + h) + 1;
        }
    }

    public static final class Part {
        private final ModelSpec spec;
        public final String name;
        public float px, py, pz, rx, ry, rz;
        public int minLevel = Integer.MIN_VALUE, maxLevel = Integer.MAX_VALUE;
        public final List<Box> boxes = new ArrayList<>();
        public final List<Part> children = new ArrayList<>();

        Part(ModelSpec spec, String name, float px, float py, float pz) {
            this.spec = spec;
            this.name = name;
            this.px = px;
            this.py = py;
            this.pz = pz;
        }

        // ---------------------------------------------------------- plain boxes

        public Part box(float x, float y, float z, float w, float h, float d, Paint p) {
            boxes.add(new Box(x, y, z, w, h, d, 0f, p));
            return this;
        }

        public Part box(float x, float y, float z, float w, float h, float d, float inflate, Paint p) {
            boxes.add(new Box(x, y, z, w, h, d, inflate, p));
            return this;
        }

        /** Box centred on the pivot in X and Z, spanning y..y+h. */
        public Part cbox(float y, float w, float h, float d, Paint p) {
            return box(-w / 2f, y, -d / 2f, w, h, d, p);
        }

        /** Box centred on (cx, cy, cz). */
        public Part center(float cx, float cy, float cz, float w, float h, float d, Paint p) {
            return box(cx - w / 2f, cy - h / 2f, cz - d / 2f, w, h, d, p);
        }

        // ---------------------------------------------------------- hierarchy

        public Part child(String name, float x, float y, float z) {
            Part c = new Part(spec, name, x, y, z);
            children.add(c);
            return c;
        }

        /** Child with an auto-generated unique name (for decorative sub-parts). */
        public Part sub(float x, float y, float z) {
            return child(spec.nextName(name), x, y, z);
        }

        public Part rot(float x, float y, float z) {
            this.rx = x * Mth.DEG_TO_RAD;
            this.ry = y * Mth.DEG_TO_RAD;
            this.rz = z * Mth.DEG_TO_RAD;
            return this;
        }

        public Part levels(int min, int max) {
            this.minLevel = min;
            this.maxLevel = max;
            return this;
        }

        public Part from(int min) {
            return levels(min, Integer.MAX_VALUE);
        }

        // ---------------------------------------------------------- rounded primitives

        /** Octagonal prism standing on Y: centred on (cx, cz), spanning y..y+h. Returns this. */
        public Part octo(float cx, float y, float cz, float w, float h, float d, Paint p) {
            box(cx - w / 2f, y, cz - d / 2f, w, h, d, p);
            float k = 0.84f;
            sub(cx, 0, cz).rot(0, 45, 0).box(-w * k / 2f, y, -d * k / 2f, w * k, h, d * k, p);
            return this;
        }

        /** Octagonal cylinder along Z (barrels, pipes): centred on (cx, cy), spanning z..z+len. */
        public Part octoZ(float cx, float cy, float z, float diameter, float len, Paint p) {
            box(cx - diameter / 2f, cy - diameter / 2f, z, diameter, diameter, len, p);
            float k = diameter * 0.84f;
            sub(cx, cy, 0).rot(0, 0, 45).box(-k / 2f, -k / 2f, z, k, k, len, p);
            return this;
        }

        /** Octagonal cylinder along X. */
        public Part octoX(float x, float cy, float cz, float diameter, float len, Paint p) {
            box(x, cy - diameter / 2f, cz - diameter / 2f, len, diameter, diameter, p);
            float k = diameter * 0.84f;
            sub(0, cy, cz).rot(45, 0, 0).box(x, -k / 2f, -k / 2f, len, k, k, p);
            return this;
        }

        /**
         * Tapered stack rising from yBase upward (toward -Y) by height, interpolating the footprint from
         * (w0, d0) to (w1, d1) over the given number of steps. Cones, horns, blades, crystals.
         */
        public Part taper(float cx, float yBase, float cz, float w0, float d0, float w1, float d1, float height, int steps, Paint p) {
            float seg = height / steps;
            for (int i = 0; i < steps; i++) {
                float t = (i + 0.5f) / steps;
                float w = Mth.lerp(t, w0, w1), d = Mth.lerp(t, d0, d1);
                box(cx - w / 2f, yBase - (i + 1) * seg, cz - d / 2f, w, seg + 0.01f, d, p);
            }
            return this;
        }

        /** A tapered spike/horn pointing "up" in its own rotated frame. Returns the spike part. */
        public Part spike(float x, float y, float z, float base, float length, float rotX, float rotY, float rotZ, Paint p) {
            Part s = sub(x, y, z).rot(rotX, rotY, rotZ);
            int steps = length > 6 ? 4 : 3;
            s.taper(0, 0, 0, base, base, base * 0.2f, base * 0.2f, length, steps, p);
            return s;
        }

        /** Rough sphere (octagonal bulb) centred on (cx, cy, cz). */
        public Part sphere(float cx, float cy, float cz, float r, Paint p) {
            float d = 2 * r;
            octo(cx, cy - r * 0.7f, cz, d, r * 1.4f, d, p);
            box(cx - r * 0.7f, cy - r, cz - r * 0.7f, r * 1.4f, r * 0.31f, r * 1.4f, p);
            box(cx - r * 0.7f, cy + r * 0.69f, cz - r * 0.7f, r * 1.4f, r * 0.31f, r * 1.4f, p);
            return this;
        }

        /** Square ring (frame) lying in the XZ plane, centred on the pivot. */
        public Part ring(float y, float size, float thickness, float height, Paint p) {
            float h = size / 2f;
            box(-h, y, -h, size, height, thickness, p);
            box(-h, y, h - thickness, size, height, thickness, p);
            box(-h, y, -h + thickness, thickness, height, size - 2 * thickness, p);
            box(h - thickness, y, -h + thickness, thickness, height, size - 2 * thickness, p);
            return this;
        }

        /** Octagonal ring in the XZ plane (two rotated square frames). */
        public Part octoRing(float y, float size, float thickness, float height, Paint p) {
            ring(y, size, thickness, height, p);
            sub(0, 0, 0).rot(0, 45, 0).ring(y, size * 0.95f, thickness, height, p);
            return this;
        }

        void collect(List<Box> out) {
            out.addAll(boxes);
            for (Part c : children) c.collect(out);
        }
    }

    // ------------------------------------------------------------------ baking

    public static final class Baked {
        public final ModelPart root;
        public final Map<String, ModelPart> parts = new HashMap<>();
        public final Map<ModelPart, int[]> levelGates = new HashMap<>();
        public final TextureBaker.Result texture;

        Baked(ModelPart root, TextureBaker.Result texture) {
            this.root = root;
            this.texture = texture;
        }

        public ModelPart get(String name) {
            ModelPart p = parts.get(name);
            if (p == null) throw new IllegalArgumentException("No model part " + name);
            return p;
        }

        public ModelPart opt(String name) {
            return parts.get(name);
        }
    }

    public Baked bake() {
        List<Box> all = new ArrayList<>();
        root.collect(all);
        int[] size = pack(all);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootDef = mesh.getRoot();
        for (Part c : root.children) define(rootDef, c);
        ModelPart baked = LayerDefinition.create(mesh, size[0], size[1]).bakeRoot();
        TextureBaker.Result tex = TextureBaker.paint(name, all, size[0], size[1], density);
        Baked b = new Baked(baked, tex);
        b.parts.put("root", baked);
        for (Part c : root.children) index(b, baked.getChild(c.name), c);
        return b;
    }

    private static void define(PartDefinition parent, Part p) {
        CubeListBuilder cubes = CubeListBuilder.create();
        for (Box b : p.boxes) {
            cubes.texOffs(b.u, b.v).addBox(b.x, b.y, b.z, b.w, b.h, b.d, new CubeDeformation(b.inflate));
        }
        PartDefinition def = parent.addOrReplaceChild(p.name, cubes, PartPose.offsetAndRotation(p.px, p.py, p.pz, p.rx, p.ry, p.rz));
        for (Part c : p.children) define(def, c);
    }

    private static void index(Baked b, ModelPart part, Part spec) {
        if (b.parts.containsKey(spec.name)) throw new IllegalStateException("Duplicate model part name " + spec.name);
        b.parts.put(spec.name, part);
        if (spec.minLevel != Integer.MIN_VALUE || spec.maxLevel != Integer.MAX_VALUE) {
            b.levelGates.put(part, new int[]{spec.minLevel, spec.maxLevel});
        }
        for (Part c : spec.children) index(b, part.getChild(c.name), c);
    }

    /** Shelf-pack box UV footprints (1px gutter). Returns {width, height} in model pixels (powers of two). */
    private static int[] pack(List<Box> boxes) {
        List<Box> sorted = new ArrayList<>(boxes);
        sorted.sort((a, c) -> Integer.compare(c.uvH(), a.uvH()));
        int maxW = 0;
        long area = 0;
        for (Box b : sorted) {
            maxW = Math.max(maxW, b.uvW());
            area += (long) b.uvW() * b.uvH();
        }
        int width = 64;
        while (width < maxW || (long) width * width < area * 1.35) width *= 2;
        int x = 0, y = 0, rowH = 0;
        for (Box b : sorted) {
            if (x + b.uvW() > width) {
                x = 0;
                y += rowH;
                rowH = 0;
            }
            b.u = x;
            b.v = y;
            x += b.uvW();
            rowH = Math.max(rowH, b.uvH());
        }
        int height = 32;
        while (height < y + rowH) height *= 2;
        return new int[]{width, height};
    }
}
