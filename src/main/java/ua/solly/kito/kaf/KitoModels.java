package ua.solly.kito.kaf;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.Function;

public final class KitoModels {
    private KitoModels() {
    }

    public enum Kind {
        ENTITY("entity"),
        ITEM("item"),
        BLOCK("block");

        private final String dir;

        Kind(String dir) {
            this.dir = dir;
        }

        public String dir() {
            return dir;
        }
    }

    public record Def(ResourceLocation model, ResourceLocation texture, ResourceLocation animation) {
        public Def withModel(ResourceLocation model) {
            return new Def(model, texture, animation);
        }

        public Def withTexture(ResourceLocation texture) {
            return new Def(model, texture, animation);
        }

        public Def withAnimation(ResourceLocation animation) {
            return new Def(model, texture, animation);
        }

        public <T extends GeoAnimatable> GeoModel<T> build() {
            return new Model<>(this, null, null);
        }

        public <T extends GeoAnimatable> GeoModel<T> build(KitoAsm asm) {
            return new Model<>(this, null, asm);
        }

        public <T extends GeoAnimatable> GeoModel<T> build(Function<T, ResourceLocation> textureProvider) {
            return new Model<>(this, textureProvider, null);
        }

        public <T extends GeoAnimatable> GeoModel<T> build(Function<T, ResourceLocation> textureProvider, KitoAsm asm) {
            return new Model<>(this, textureProvider, asm);
        }
    }

    public static final class Model<T extends GeoAnimatable> extends GeoModel<T> {
        private final Def def;
        private final Function<T, ResourceLocation> textureProvider;
        private final KitoAsm asm;

        private Model(Def def, Function<T, ResourceLocation> textureProvider, KitoAsm asm) {
            this.def = def;
            this.textureProvider = textureProvider;
            this.asm = asm;
        }

        @Override
        public ResourceLocation getModelResource(T animatable) {
            return def.model();
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return textureProvider == null ? def.texture() : textureProvider.apply(animatable);
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            return def.animation();
        }

        @Override
        public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
            if (asm != null) {
                asm.apply(this, animationState);
            }
        }
    }

    public static ResourceLocation id(String modId, String path) {
        return ResourceLocation.fromNamespaceAndPath(modId, path);
    }

    public static ResourceLocation geo(String modId, String name) {
        return id(modId, "geo/" + name + ".geo.json");
    }

    public static ResourceLocation animation(String modId, String name) {
        return id(modId, "animations/" + name + ".animation.json");
    }

    public static ResourceLocation texture(String modId, Kind kind, String name) {
        return id(modId, "textures/" + kind.dir() + "/" + name + ".png");
    }

    public static Def of(Kind kind, String modId, String name) {
        return new Def(geo(modId, name), texture(modId, kind, name), animation(modId, name));
    }

    public static Def of(Kind kind, String modId, String model, String texture, String animation) {
        return new Def(geo(modId, model), texture(modId, kind, texture), animation(modId, animation));
    }

    public static Def entity(String modId, String name) {
        return of(Kind.ENTITY, modId, name);
    }

    public static Def item(String modId, String name) {
        return of(Kind.ITEM, modId, name);
    }

    public static Def block(String modId, String name) {
        return of(Kind.BLOCK, modId, name);
    }
}
