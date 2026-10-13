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
 * A tiny modelling DSL. Parts and boxes are declared in vanilla model space (pixels, Y down,
 * Y=24 is the ground, -Z is the front). Baking packs every box into a texture atlas, paints it
 * procedurally with {@link TextureBaker} and produces a vanilla {@link ModelPart} tree, so geometry
 * and texture can never fall out of sync.
 */
public final class ModelSpec {
    public final String name;
    public final Part root = new Part("root", 0, 0, 0);

    public ModelSpec(String name) {
        this.name = name;
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
            return (int) Math.ceil(2 * (Math.ceil(w) + Math.ceil(d)));
        }

        public int uvH() {
            return (int) Math.ceil(Math.ceil(d) + Math.ceil(h));
        }
    }

    public static final class Part {
        public final String name;
        public float px, py, pz, rx, ry, rz;
        public int minLevel = Integer.MIN_VALUE, maxLevel = Integer.MAX_VALUE;
        public final List<Box> boxes = new ArrayList<>();
        public final List<Part> children = new ArrayList<>();

        Part(String name, float px, float py, float pz) {
            this.name = name;
            this.px = px;
            this.py = py;
            this.pz = pz;
        }

        /** Add a box (origin relative to the part pivot). */
        public Part box(float x, float y, float z, float w, float h, float d, Paint p) {
            boxes.add(new Box(x, y, z, w, h, d, 0f, p));
            return this;
        }

        public Part box(float x, float y, float z, float w, float h, float d, float inflate, Paint p) {
            boxes.add(new Box(x, y, z, w, h, d, inflate, p));
            return this;
        }

        /** Box centred on the pivot in X and Z. */
        public Part cbox(float y, float w, float h, float d, Paint p) {
            return box(-w / 2f, y, -d / 2f, w, h, d, p);
        }

        /** Create a child part with its pivot at (x,y,z) relative to this part. */
        public Part child(String name, float x, float y, float z) {
            Part c = new Part(name, x, y, z);
            children.add(c);
            return c;
        }

        /** Initial rotation in degrees. */
        public Part rot(float x, float y, float z) {
            this.rx = x * Mth.DEG_TO_RAD;
            this.ry = y * Mth.DEG_TO_RAD;
            this.rz = z * Mth.DEG_TO_RAD;
            return this;
        }

        /** Only visible when the model's level is within [min, max] (turret level, zombie rank, hero level). */
        public Part levels(int min, int max) {
            this.minLevel = min;
            this.maxLevel = max;
            return this;
        }

        public Part from(int min) {
            return levels(min, Integer.MAX_VALUE);
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
        TextureBaker.Result tex = TextureBaker.paint(name, all, size[0], size[1]);
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

    /** Shelf-pack box UV footprints. Returns {width, height} (powers of two). */
    private static int[] pack(List<Box> boxes) {
        List<Box> sorted = new ArrayList<>(boxes);
        sorted.sort((a, c) -> Integer.compare(c.uvH(), a.uvH()));
        int maxW = 0;
        int area = 0;
        for (Box b : sorted) {
            maxW = Math.max(maxW, b.uvW());
            area += b.uvW() * b.uvH();
        }
        int width = 64;
        while (width < maxW || width * width < area * 1.3) width *= 2;
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
